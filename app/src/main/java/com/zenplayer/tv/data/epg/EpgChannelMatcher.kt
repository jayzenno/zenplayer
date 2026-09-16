package com.zenplayer.tv.data.epg

import com.zenplayer.tv.domain.epg.EpgProgrammeMatcher
import com.zenplayer.tv.domain.model.Channel
import com.zenplayer.tv.domain.model.EpgProgramme

/** Joins provider XMLTV channel ids to playlist channels without forcing one exact id format. */
object EpgChannelMatcher {
    fun programmesFor(channel: Channel, programmes: List<EpgProgramme>): List<EpgProgramme> =
        programmes.filter { EpgProgrammeMatcher.matches(it, channel) }.sortedBy { it.start }
}
