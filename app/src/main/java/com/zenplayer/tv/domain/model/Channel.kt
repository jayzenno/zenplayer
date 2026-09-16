package com.zenplayer.tv.domain.model

/** A normalized live channel independent from the source format (M3U/Xtream/etc.). */
data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val group: String? = null,
    val logoUrl: String? = null,
    val tvgId: String? = null,
    val catchupType: String? = null,
    val catchupDays: Int = 0,
    val catchupSourceUrl: String? = null,
    val isCatchupCapable: Boolean = false,
    val resolutionHint: String? = null,
    val codecHint: String? = null,
)

data class CatchupTarget(
    val channel: Channel,
    val score: Int,
    val reason: String,
)
