package com.zenplayer.tv

import com.zenplayer.tv.domain.model.Channel
import com.zenplayer.tv.domain.model.EpgProgramme

object DemoData {
    // Public test stream used only for the optional in-app demo mode.
    private const val DEMO_STREAM = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"

    fun channels(): List<Channel> = listOf(
        Channel("demo:one", "Zen Cinema", DEMO_STREAM, "Demo · Movies", resolutionHint = "1080P"),
        Channel("demo:two", "Zen News", DEMO_STREAM, "Demo · News", resolutionHint = "1080P"),
        Channel("demo:three", "Zen Sports", DEMO_STREAM, "Demo · Sports", resolutionHint = "HD"),
        Channel("demo:four", "Zen World", DEMO_STREAM, "Demo · Documentary", resolutionHint = "HD"),
        Channel("demo:five", "Zen Kids", DEMO_STREAM, "Demo · Kids", resolutionHint = "HD"),
        Channel("demo:six", "Zen Music", DEMO_STREAM, "Demo · Music", resolutionHint = "HD")
    )

    fun programmes(now: Long = System.currentTimeMillis()): List<EpgProgramme> {
        val start = now - (now % 1_800_000L)
        val titles = listOf("The Morning Show", "City Stories", "Live Showcase", "World Tonight", "Prime Time", "Late Night")
        return channels().flatMapIndexed { channelIndex, channel ->
            (0 until 8).map { slot ->
                val s = start + slot * 1_800_000L
                EpgProgramme(
                    id = "${channel.id}:$slot",
                    channelId = channel.id,
                    title = titles[(slot + channelIndex) % titles.size],
                    description = "Demo-Programm für die ZenPlayer-Oberfläche.",
                    start = s,
                    end = s + 1_800_000L,
                    category = channel.group?.removePrefix("Demo · "),
                    isCatchupAvailable = slot < 4
                )
            }
        }
    }
}
