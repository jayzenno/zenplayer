package com.zenplayer.tv

import com.zenplayer.tv.domain.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class XtreamClient(server: String, private val username: String, private val password: String) {
    private val base = normalizeServer(server)

    suspend fun load(): Result<List<Channel>> = withContext(Dispatchers.IO) {
        runCatching {
            require(base.isNotBlank()) { "Bitte einen Xtream-Server eingeben." }
            val auth = request("player_api.php?username=${enc(username)}&password=${enc(password)}")
            val userInfo = auth.optJSONObject("user_info") ?: error("Ungültige Xtream-Antwort")
            require(userInfo.optString("status").equals("Active", true)) { "Xtream-Konto ist nicht aktiv." }
            val categories = requestArray("player_api.php?username=${enc(username)}&password=${enc(password)}&action=get_live_categories")
            val categoryNames = mutableMapOf<String, String>()
            for (i in 0 until categories.length()) {
                val c = categories.optJSONObject(i) ?: continue
                categoryNames[c.optString("category_id")] = c.optString("category_name")
            }
            val streams = requestArray("player_api.php?username=${enc(username)}&password=${enc(password)}&action=get_live_streams")
            buildList {
                for (i in 0 until streams.length()) {
                    val s = streams.optJSONObject(i) ?: continue
                    val id = s.optString("stream_id").ifBlank { continue }
                    val name = s.optString("name").ifBlank { "Sender $id" }
                    add(Channel(
                        id = "xtream:$id", name = name,
                        streamUrl = "$base/live/$username/$password/$id.ts",
                        group = categoryNames[s.optString("category_id")],
                        logoUrl = s.optString("stream_icon").ifBlank { null },
                        tvgId = s.optString("epg_channel_id").ifBlank { null },
                        isCatchupCapable = s.optInt("tv_archive", 0) == 1,
                        catchupDays = s.optInt("tv_archive_duration", 0),
                        resolutionHint = s.optString("stream_type").ifBlank { null }
                    ))
                }
            }
        }
    }

    private fun request(path: String): JSONObject = JSONObject(requestText(path))
    private fun requestArray(path: String): JSONArray = JSONArray(requestText(path))

    private fun requestText(path: String): String {
        val c = (URL("$base/$path").openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000; readTimeout = 20_000; instanceFollowRedirects = true
            setRequestProperty("User-Agent", "ZenPlayer/1.0 AndroidTV")
        }
        return try {
            require(c.responseCode in 200..399) { "HTTP ${c.responseCode}" }
            c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
    }

    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")

    companion object {
        private fun normalizeServer(input: String): String {
            var value = input.trim().trimEnd('/')
            if (value.isBlank()) return ""
            if (!value.startsWith("http://", true) && !value.startsWith("https://", true)) {
                // Xtream providers commonly publish only a hostname. Java URL then uses
                // the protocol's normal default port (80/443); no explicit port is required.
                value = "http://$value"
            }
            return value
        }
    }
}
