package com.zenplayer.tv

import com.zenplayer.tv.domain.model.Channel
import com.zenplayer.tv.domain.model.EpgProgramme
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Resolves common M3U catch-up URL templates without inventing provider-specific endpoints. */
object CatchupResolver {
    fun resolve(channel: Channel, programme: EpgProgramme, siblings: List<Channel> = emptyList(), preferSibling: Boolean = false): Channel? {
        build(channel, programme)?.let { return it }
        if (!preferSibling) return null
        return siblings.filter { it.id != channel.id && it.isCatchupCapable }
            .sortedByDescending { score(channel, it) }
            .asSequence()
            .mapNotNull { build(it, programme)?.copy(name = "${channel.name} · ${programme.title}", logoUrl = channel.logoUrl ?: it.logoUrl, group = channel.group ?: it.group) }
            .firstOrNull()
    }

    private fun build(channel: Channel, p: EpgProgramme): Channel? {
        val template = channel.catchupSourceUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val startSec = p.start / 1000L
        val endSec = p.end / 1000L
        val duration = ((p.end - p.start) / 1000L).coerceAtLeast(0L)
        val values = mapOf(
            "{start}" to startSec.toString(), "{end}" to endSec.toString(),
            "{timestamp}" to startSec.toString(), "{utc}" to startSec.toString(),
            "{duration}" to duration.toString(), "{channel}" to channel.id,
            "{id}" to channel.id, "{name}" to URLEncoder.encode(channel.name, StandardCharsets.UTF_8.toString())
        )
        var url = template
        values.forEach { (key, value) -> url = url.replace(key, value, ignoreCase = true) }
        if (!url.startsWith("http://") && !url.startsWith("https://")) return null
        return channel.copy(streamUrl = url, name = "${channel.name} · ${p.title}", isCatchupCapable = true)
    }

    private fun score(a: Channel, b: Channel): Int {
        var s = 0
        if (!a.tvgId.isNullOrBlank() && a.tvgId.equals(b.tvgId, true)) s += 100
        if (!a.group.isNullOrBlank() && a.group.equals(b.group, true)) s += 30
        if (a.name.equals(b.name, true)) s += 50
        return s
    }
}
