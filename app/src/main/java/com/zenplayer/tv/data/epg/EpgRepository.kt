package com.zenplayer.tv.data.epg

import com.zenplayer.tv.domain.model.Channel
import com.zenplayer.tv.domain.model.EpgProgramme

/** In-memory EPG facade. Networking/storage can plug into this without changing the UI model. */
class EpgRepository {
    private var programmes: List<EpgProgramme> = emptyList()

    fun replaceFromXmlTv(xml: String, channels: List<Channel>): List<EpgProgramme> {
        val catchupDays = channels
            .flatMap { channel ->
                listOfNotNull(channel.tvgId?.let { it to channel.catchupDays }, channel.id to channel.catchupDays)
            }
            .toMap()

        programmes = XmlTvParser.parse(xml, catchupDays)
        return programmes
    }

    fun all(): List<EpgProgramme> = programmes

    fun forWindow(channelId: String, windowStart: Long, windowEnd: Long): List<EpgProgramme> =
        programmes.asSequence()
            .filter { it.channelId.equals(channelId, ignoreCase = true) }
            .filter { it.end > windowStart && it.start < windowEnd }
            .sortedBy { it.start }
            .toList()

    fun forChannels(channelIds: Set<String>, windowStart: Long, windowEnd: Long): List<EpgProgramme> =
        programmes.asSequence()
            .filter { channelIds.any { id -> id.equals(it.channelId, ignoreCase = true) } }
            .filter { it.end > windowStart && it.start < windowEnd }
            .sortedWith(compareBy<EpgProgramme> { it.channelId.lowercase() }.thenBy { it.start })
            .toList()
}
