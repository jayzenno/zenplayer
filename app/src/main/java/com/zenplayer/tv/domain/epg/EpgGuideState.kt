package com.zenplayer.tv.domain.epg

/**
 * Stateful guide cursor used by the TV UI. Moving left/right changes the time window,
 * while jumpToNow restores the live position. The cursor deliberately does not clamp to
 * today: providers may expose several days of history and future schedules.
 */
data class EpgGuideState(
    val windowStart: Long,
    val pageMillis: Long = EpgNavigation.PAGE_MILLIS,
) {
    val windowEnd: Long get() = windowStart + pageMillis

    fun previousPage(): EpgGuideState = copy(windowStart = EpgNavigation.previous(windowStart))
    fun nextPage(): EpgGuideState = copy(windowStart = EpgNavigation.next(windowStart))

    fun jumpToNow(now: Long, leadInMillis: Long = pageMillis / 3L): EpgGuideState =
        copy(windowStart = now - leadInMillis)
}

/** Describes an action exposed by the programme context menu. */
enum class EpgProgrammeAction {
    PLAY_LIVE,
    START_FROM_BEGINNING,
    OPEN_DETAILS,
    ADD_FAVOURITE,
}
