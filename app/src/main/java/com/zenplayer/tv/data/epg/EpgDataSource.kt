package com.zenplayer.tv.data.epg

import com.zenplayer.tv.domain.model.Channel
import com.zenplayer.tv.domain.model.EpgProgramme

/** Converts a raw XMLTV payload into queryable guide data. */
class EpgDataSource {
    private val repository = EpgRepository()

    fun loadXmlTv(xml: String, channels: List<Channel>): List<EpgProgramme> =
        repository.replaceFromXmlTv(xml, channels)

    fun programmesFor(channel: Channel, start: Long, end: Long): List<EpgProgramme> =
        EpgChannelMatcher.programmesFor(channel, repository.all())
            .filter { it.end > start && it.start < end }
}
