package com.zenplayer.tv.data.epg

import com.zenplayer.tv.domain.model.EpgProgramme

/** Small pure helpers used by the guide to keep only programmes touching the visible window. */
object EpgWindow {
    fun visible(programmes: List<EpgProgramme>, start: Long, end: Long): List<EpgProgramme> =
        programmes.asSequence()
            .filter { it.end > start && it.start < end }
            .sortedBy { it.start }
            .toList()

    fun current(programmes: List<EpgProgramme>, now: Long): EpgProgramme? =
        programmes.firstOrNull { now >= it.start && now < it.end }
}
