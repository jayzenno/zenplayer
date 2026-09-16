package com.zenplayer.tv.domain.epg

import com.zenplayer.tv.domain.model.EpgProgramme
import java.time.Duration
import java.time.Instant

/**
 * Pure EPG navigation helpers. The guide can move indefinitely through the provider's
 * available programme data; the UI decides how much data to request around the cursor.
 */
object EpgNavigation {
    private val page = Duration.ofHours(6)

    fun previous(windowStart: Instant): Instant = windowStart.minus(page)
    fun next(windowStart: Instant): Instant = windowStart.plus(page)

    fun currentProgramme(programmes: List<EpgProgramme>, now: Instant): EpgProgramme? =
        programmes.firstOrNull { !now.isBefore(it.start) && now.isBefore(it.end) }

    fun startFromBeginning(programme: EpgProgramme, now: Instant): CatchupStart {
        require(programme.isCatchupAvailable) { "Catch-up is not available for this programme" }
        return CatchupStart(programme.start, now.coerceIn(programme.start, programme.end))
    }

    private fun Instant.coerceIn(min: Instant, max: Instant): Instant =
        when {
            isBefore(min) -> min
            isAfter(max) -> max
            else -> this
        }
}

data class CatchupStart(val programmeStart: Instant, val liveReferenceTime: Instant)
