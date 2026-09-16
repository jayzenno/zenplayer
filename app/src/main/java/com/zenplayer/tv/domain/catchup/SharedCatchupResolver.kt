package com.zenplayer.tv.domain.catchup

import com.zenplayer.tv.domain.model.CatchupTarget
import com.zenplayer.tv.domain.model.Channel

/**
 * Resolves a live channel to the best archive-capable sibling.
 *
 * The resolver never changes the live channel itself. It only supplies a catch-up
 * target, which lets the UI/player transparently use a sibling stream when the
 * selected rendition has no archive.
 */
class SharedCatchupResolver {

    fun resolve(channel: Channel, channels: List<Channel>): CatchupTarget? {
        if (channel.isCatchupCapable && channel.catchupSourceUrl != null) {
            return CatchupTarget(channel, 100, "selected channel has native catch-up")
        }

        return channels
            .asSequence()
            .filter { it.id != channel.id && it.isCatchupCapable && it.catchupSourceUrl != null }
            .mapNotNull { candidate -> score(channel, candidate)?.let { CatchupTarget(candidate, it.first, it.second) } }
            .maxByOrNull { it.score }
    }

    private fun score(source: Channel, candidate: Channel): Pair<Int, String>? {
        var score = 0
        val reasons = mutableListOf<String>()

        val sourceTvg = source.tvgId?.normalizedId()
        val candidateTvg = candidate.tvgId?.normalizedId()
        if (sourceTvg != null && sourceTvg == candidateTvg) {
            score += 60
            reasons += "same tvg-id"
        }

        val sourceName = source.name.normalizedName()
        val candidateName = candidate.name.normalizedName()
        if (sourceName == candidateName) {
            score += 30
            reasons += "same normalized name"
        } else if (similarName(sourceName, candidateName)) {
            score += 18
            reasons += "similar channel name"
        }

        if (source.group != null && source.group.equals(candidate.group, ignoreCase = true)) {
            score += 8
            reasons += "same group"
        }

        // Prefer a sibling with a comparable rendition, but don't require it:
        // the whole point of shared catch-up is that another rendition may have the archive.
        if (source.resolutionHint != null && source.resolutionHint == candidate.resolutionHint) {
            score += 3
            reasons += "same resolution"
        }

        if (score < 25) return null
        return score to reasons.joinToString(", ")
    }

    private fun String.normalizedId(): String =
        lowercase().replace(Regex("[^a-z0-9]"), "")

    private fun String.normalizedName(): String =
        lowercase()
            .replace(Regex("\\b(fhd|uhd|hd|sd|4k|1080p|720p|576p|h264|h265|hevc|av1)\\b"), "")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()

    private fun similarName(a: String, b: String): Boolean {
        if (a.isBlank() || b.isBlank()) return false
        return a == b || a.startsWith(b) || b.startsWith(a)
    }
}
