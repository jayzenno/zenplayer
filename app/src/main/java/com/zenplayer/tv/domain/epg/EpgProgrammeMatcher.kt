package com.zenplayer.tv.domain.epg

import com.zenplayer.tv.domain.model.Channel
import com.zenplayer.tv.domain.model.EpgProgramme

/** Matches XMLTV channel ids to playlist channels with safe tvg-id/name fallbacks. */
object EpgProgrammeMatcher {
    fun channelKey(channel: Channel): String = channel.tvgId?.normalized() ?: channel.id.normalized()

    fun matches(programme: EpgProgramme, channel: Channel): Boolean {
        val programmeId = programme.channelId.normalized()
        val tvgId = channel.tvgId?.normalized()
        if (tvgId != null && programmeId == tvgId) return true
        if (programmeId == channel.id.normalized()) return true
        return normalizeName(programme.channelId) == normalizeName(channel.name)
    }

    private fun String.normalized(): String = lowercase().replace(Regex("[^a-z0-9]"), "")

    private fun normalizeName(value: String): String =
        value.lowercase()
            .replace(Regex("\\b(fhd|uhd|hd|sd|4k|1080p|720p|576p|h264|h265|hevc|av1)\\b"), "")
            .replace(Regex("[^a-z0-9]+"), "")
}
