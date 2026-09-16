package com.zenplayer.tv.domain.model

/** A programme entry normalized from XMLTV/EPG providers. Times are UTC epoch milliseconds. */
data class EpgProgramme(
    val id: String,
    val channelId: String,
    val title: String,
    val subtitle: String? = null,
    val description: String? = null,
    val start: Long,
    val end: Long,
    val category: String? = null,
    val imageUrl: String? = null,
    val isCatchupAvailable: Boolean = false,
) {
    val durationSeconds: Long get() = ((end - start) / 1000L).coerceAtLeast(0)
}

data class EpgDay(val dateKey: String, val programmes: List<EpgProgramme>)

enum class EpgWindow { PREVIOUS, CURRENT, NEXT }

data class EpgCursor(
    val windowStart: Long,
    val windowEnd: Long,
    val selectedChannelIndex: Int = 0,
)
