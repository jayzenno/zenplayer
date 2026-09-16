package com.zenplayer.tv

import android.content.Context
import android.net.Uri
import com.zenplayer.tv.data.epg.EpgRepository
import com.zenplayer.tv.data.epg.XmlTvParser
import com.zenplayer.tv.data.m3u.M3uParser
import com.zenplayer.tv.domain.model.Channel
import com.zenplayer.tv.domain.model.EpgProgramme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** User-owned playlist/EPG source. No station logos are bundled. */
class PlaylistStore(private val context: Context) {
    private val playlistFile get() = File(context.filesDir, "playlist.m3u")
    private val epgFile get() = File(context.filesDir, "epg.xml")
    private val epgRepository = EpgRepository()
    var channels: List<Channel> = loadChannels()
        private set
    var programmes: List<EpgProgramme> = loadProgrammes()
        private set

    fun importPlaylist(text: String) {
        require(text.isNotEmpty()) { "Die M3U ist leer." }
        val parsed = M3uParser.parse(text)
        require(parsed.isNotEmpty()) { "Die M3U enthält keine gültigen Sender." }
        playlistFile.writeText(text)
        channels = parsed
        // Do not synchronously re-parse a potentially huge XMLTV file here.
        // EPG is loaded on startup / explicit EPG refresh instead.
        programmes = emptyList()
    }

    suspend fun importPlaylistFromUri(uri: Uri) = withContext(Dispatchers.IO) {
        val temp = File.createTempFile("zenplaylist", ".m3u", context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output, 64 * 1024) }
            } ?: error("Datei konnte nicht gelesen werden.")
            val parsed = temp.inputStream().bufferedReader(Charsets.UTF_8).use { M3uParser.parse(it) }
            require(parsed.isNotEmpty()) { "Die M3U enthält keine gültigen Sender." }
            temp.inputStream().use { input -> playlistFile.outputStream().use { output -> input.copyTo(output, 64 * 1024) } }
            channels = parsed
            programmes = emptyList()
        } finally { temp.delete() }
    }

    suspend fun importPlaylistFromFile(file: File) = withContext(Dispatchers.IO) {
        require(file.exists()) { "Datei nicht gefunden." }
        val temp = File.createTempFile("zenplaylist", ".m3u", context.cacheDir)
        try {
            file.inputStream().use { input -> temp.outputStream().use { output -> input.copyTo(output, 64 * 1024) } }
            val parsed = temp.inputStream().bufferedReader(Charsets.UTF_8).use { M3uParser.parse(it) }
            require(parsed.isNotEmpty()) { "Die M3U enthält keine gültigen Sender." }
            temp.inputStream().use { input -> playlistFile.outputStream().use { output -> input.copyTo(output, 64 * 1024) } }
            channels = parsed
            programmes = emptyList()
        } finally { temp.delete() }
    }

    suspend fun importPlaylistFromUrl(url: String) = withContext(Dispatchers.IO) {
        val clean = url.trim()
        require(clean.startsWith("http://") || clean.startsWith("https://")) { "Bitte eine gültige http(s)-URL eingeben." }
        val connection = (URL(clean).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "ZenPlayer/1.0 AndroidTV")
            setRequestProperty("Accept", "application/x-mpegURL, audio/x-mpegurl, text/plain, */*")
        }
        val temp = File.createTempFile("zenplaylist", ".m3u", context.cacheDir)
        try {
            require(connection.responseCode in 200..399) { "Server antwortet mit HTTP ${connection.responseCode}." }
            connection.inputStream.use { input -> temp.outputStream().use { output -> input.copyTo(output, 64 * 1024) } }
            val parsed = temp.inputStream().bufferedReader(Charsets.UTF_8).use { M3uParser.parse(it) }
            require(parsed.isNotEmpty()) { "Die M3U enthält keine gültigen Sender." }
            temp.inputStream().use { input -> playlistFile.outputStream().use { output -> input.copyTo(output, 64 * 1024) } }
            channels = parsed
            programmes = emptyList()
        } finally { temp.delete(); connection.disconnect() }
    }

    fun importXtream(channels: List<Channel>, epg: List<EpgProgramme> = emptyList()) {
        require(channels.isNotEmpty()) { "Xtream hat keine Live-Sender geliefert." }
        playlistFile.delete()
        this.channels = channels
        this.programmes = epg
    }

    fun importEpg(text: String) {
        epgFile.writeText(text)
        programmes = epgRepository.replaceFromXmlTv(text, channels)
    }

    fun clear() {
        playlistFile.delete(); epgFile.delete(); channels = emptyList(); programmes = emptyList()
    }

    fun channelLogo(channel: Channel): String? = channel.logoUrl ?: channel.tvgId?.let { id ->
        XmlTvParser.parseChannelLogos(epgFile.takeIf { it.exists() }?.readText().orEmpty())[id]
    }

    private fun loadChannels(): List<Channel> = runCatching {
        if (playlistFile.exists()) playlistFile.inputStream().bufferedReader(Charsets.UTF_8).use { M3uParser.parse(it) } else emptyList()
    }.getOrDefault(emptyList())

    private fun loadProgrammes(): List<EpgProgramme> = runCatching {
        if (!epgFile.exists()) emptyList() else epgRepository.replaceFromXmlTv(epgFile.readText(), channels)
    }.getOrDefault(emptyList())
}
