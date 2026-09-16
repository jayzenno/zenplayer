package com.zenplayer.tv.domain.epg

import com.zenplayer.tv.domain.catchup.SharedCatchupResolver
import com.zenplayer.tv.domain.model.Channel
import com.zenplayer.tv.domain.model.EpgProgramme

/** Player-neutral request produced by the guide before Media3 is invoked. */
data class CatchupPlaybackRequest(
    val programme: EpgProgramme,
    val channel: Channel,
    val catchupChannel: Channel,
    val startPositionMillis: Long,
    val reason: String,
)

object CatchupPlaybackRequestFactory {
    fun create(
        programme: EpgProgramme,
        channel: Channel,
        channels: List<Channel>,
        now: Long,
    ): CatchupPlaybackRequest? {
        if (now < programme.start) return null

        val target = SharedCatchupResolver().resolve(channel, channels) ?: return null
        if (target.channel.catchupSourceUrl.isNullOrBlank()) return null

        val start = EpgNavigation.startFromBeginning(programme.copy(isCatchupAvailable = true), now)
        return CatchupPlaybackRequest(
            programme = programme,
            channel = channel,
            catchupChannel = target.channel,
            startPositionMillis = start.programmeStart,
            reason = target.reason,
        )
    }
}
