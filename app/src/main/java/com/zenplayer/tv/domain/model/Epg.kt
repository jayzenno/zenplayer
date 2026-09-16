package com.zenplayer.tv.domain.model

import java.time.Instant

/** A programme entry normalized from XMLTV/EPG providers. */
data class EpgProgramme(
    val id: String,
    val channelId: String,
    val title: String,
    val subtitle: String? = null,
    val description: String? = null,
    val start: Instant,
    val end: Instant,
    val category: String? = null,
    val imageUrl: String? = null,
    val isCatchupAvailable: Boolean = false,
) {
    val durationSeconds: Long get() = (end.epochSecond - start.epochSecond).coerceAtLeast(0)
}

data class EpgDay(val dateKey: String, val programmes: List<EpgProgramme>)

enum class EpgWindow { PREVIOUS, CURRENT, NEXT }

data class EpgCursor(
    val windowStart: Instant,
    val windowEnd: Instant,
    val selectedChannelIndex: Int = 0,
)
