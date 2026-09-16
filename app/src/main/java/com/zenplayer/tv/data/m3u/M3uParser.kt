package com.zenplayer.tv.data.m3u

import com.zenplayer.tv.domain.model.Channel

/** Lightweight M3U parser for IPTV playlists. Unknown attributes are safely ignored. */
object M3uParser {
    fun parse(text: String): List<Channel> {
        val lines = text.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
        val result = ArrayList<Channel>()
        var pending: Attributes? = null

        for (line in lines) {
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> pending = parseAttributes(line)
                line.startsWith("#") -> Unit
                pending != null -> {
                    val attributes = pending!!
                    val id = attributes.tvgId ?: stableId(attributes.name, line)
                    result += Channel(
                        id = id,
                        name = attributes.name,
                        streamUrl = line,
                        group = attributes.group,
                        logoUrl = attributes.logo,
                        tvgId = attributes.tvgId,
                        catchupType = attributes.catchupType,
                        catchupDays = attributes.catchupDays,
                        catchupSourceUrl = attributes.catchupSource,
                        isCatchupCapable = !attributes.catchupSource.isNullOrBlank() || !attributes.catchupType.isNullOrBlank(),
                        resolutionHint = detectResolution(attributes.name),
                        codecHint = detectCodec(attributes.name),
                    )
                    pending = null
                }
            }
        }
        return result
    }

    private data class Attributes(
        val name: String,
        val tvgId: String?,
        val group: String?,
        val logo: String?,
        val catchupType: String?,
        val catchupDays: Int,
        val catchupSource: String?,
    )

    private fun parseAttributes(line: String): Attributes {
        val name = line.substringAfter(",", "Unnamed channel").trim()
        fun attr(key: String): String? = Regex("(?:^|\\s)$key=\\\"([^\\\"]*)\\\"").find(line)?.groupValues?.getOrNull(1)?.ifBlank { null }

        return Attributes(
            name = name,
            tvgId = attr("tvg-id"),
            group = attr("group-title"),
            logo = attr("tvg-logo"),
            catchupType = attr("catchup"),
            catchupDays = attr("catchup-days")?.toIntOrNull() ?: 0,
            catchupSource = attr("catchup-source"),
        )
    }

    private fun stableId(name: String, url: String): String =
        "${name.lowercase().replace(Regex("\\s+"), "-")}:${url.hashCode()}"

    private fun detectResolution(name: String): String? =
        Regex("\\b(4K|UHD|FHD|1080P|HD|720P|SD)\\b", RegexOption.IGNORE_CASE)
            .find(name)?.value?.uppercase()

    private fun detectCodec(name: String): String? =
        Regex("\\b(H265|HEVC|H264|AV1)\\b", RegexOption.IGNORE_CASE)
            .find(name)?.value?.uppercase()
}
