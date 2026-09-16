package com.zenplayer.tv.data.m3u

import com.zenplayer.tv.domain.model.Channel
import java.io.Reader

/** Tolerant M3U/M3U8 parser for common IPTV variants. */
object M3uParser {
    fun parse(text: String): List<Channel> = parse(text.reader())

    /** Streaming parser: keeps only the current line in memory instead of creating a second full playlist copy. */
    fun parse(reader: Reader): List<Channel> {
        val result = ArrayList<Channel>()
        var pending: Attributes? = null
        var extGroup: String? = null
        reader.buffered().forEachLine { raw ->
            val line = raw.trim().removePrefix("\uFEFF")
            if (line.isEmpty()) return@forEachLine
            when {
                line.startsWith("#EXTINF", true) -> pending = parseAttributes(line)
                line.startsWith("#EXTGRP", true) -> extGroup = line.substringAfter(':', "").trim().ifBlank { null }
                line.startsWith("#") -> Unit
                pending != null -> {
                    val a = pending!!
                    val group = a.group ?: extGroup
                    result += Channel(
                        id = a.tvgId ?: stableId(a.name, line), name = a.name, streamUrl = line,
                        group = group, logoUrl = a.logo, tvgId = a.tvgId,
                        catchupType = a.catchupType, catchupDays = a.catchupDays,
                        catchupSourceUrl = a.catchupSource,
                        isCatchupCapable = !a.catchupSource.isNullOrBlank() || !a.catchupType.isNullOrBlank(),
                        resolutionHint = detectResolution(a.name), codecHint = detectCodec(a.name)
                    )
                    pending = null
                }
            }
        }
        return result
    }

    private data class Attributes(val name: String, val tvgId: String?, val group: String?, val logo: String?, val catchupType: String?, val catchupDays: Int, val catchupSource: String?)

    private fun parseAttributes(line: String): Attributes {
        val comma = line.indexOf(',')
        val name = if (comma >= 0) line.substring(comma + 1).trim() else "Unnamed channel"
        fun attr(key: String): String? {
            val pattern = Regex("(?:^|\\s)$key=(?:\\\"([^\\\"]*)\\\"|([^\\s]+))", RegexOption.IGNORE_CASE)
            val m = pattern.find(line) ?: return null
            return (m.groups[1]?.value ?: m.groups[2]?.value)?.trim()?.ifBlank { null }
        }
        return Attributes(name, attr("tvg-id"), attr("group-title"), attr("tvg-logo"), attr("catchup"), attr("catchup-days")?.toIntOrNull() ?: 0, attr("catchup-source"))
    }

    private fun stableId(name: String, url: String) = "${name.lowercase().replace(Regex("\\s+"), "-")}:${url.hashCode()}"
    private fun detectResolution(name: String): String? = Regex("\\b(4K|UHD|FHD|1080P|HD|720P|SD)\\b", RegexOption.IGNORE_CASE).find(name)?.value?.uppercase()
    private fun detectCodec(name: String): String? = Regex("\\b(H265|HEVC|H264|AV1)\\b", RegexOption.IGNORE_CASE).find(name)?.value?.uppercase()
}
