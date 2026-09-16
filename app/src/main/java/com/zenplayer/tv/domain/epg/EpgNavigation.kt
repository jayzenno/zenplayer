package com.zenplayer.tv.domain.epg

import com.zenplayer.tv.domain.model.EpgProgramme

/** Pure EPG navigation helpers. The UI can request more pages as the cursor moves. */
object EpgNavigation {
    const val PAGE_MILLIS: Long = 6L * 60L * 60L * 1000L

    fun previous(windowStart: Long): Long = windowStart - PAGE_MILLIS
    fun next(windowStart: Long): Long = windowStart + PAGE_MILLIS

    fun currentProgramme(programmes: List<EpgProgramme>, now: Long): EpgProgramme? =
        programmes.firstOrNull { now >= it.start && now < it.end }

    /** Starts a catch-up playback at the exact beginning of the selected programme. */
    fun startFromBeginning(programme: EpgProgramme, now: Long): CatchupStart {
        require(programme.isCatchupAvailable) { "Catch-up is not available for this programme" }
        return CatchupStart(programmeStart = programme.start, liveReferenceTime = now.coerceIn(programme.start, programme.end))
    }

    private fun Long.coerceIn(min: Long, max: Long): Long = when {
        this < min -> min
        this > max -> max
        else -> this
    }
}

data class CatchupStart(val programmeStart: Long, val liveReferenceTime: Long)
