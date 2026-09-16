package com.zenplayer.tv

import android.content.Context
import com.zenplayer.tv.data.epg.EpgRepository
import com.zenplayer.tv.data.epg.XmlTvParser
import com.zenplayer.tv.data.m3u.M3uParser
import com.zenplayer.tv.domain.model.Channel
import com.zenplayer.tv.domain.model.EpgProgramme
import java.io.File

/** Local user-owned playlist/EPG source. No bundled station logos are used. */
class PlaylistStore(private val context: Context) {
    private val playlistFile get() = File(context.filesDir, "playlist.m3u")
    private val epgFile get() = File(context.filesDir, "epg.xml")
    private val epgRepository = EpgRepository()

    var channels: List<Channel> = loadChannels()
        private set
    var programmes: List<EpgProgramme> = loadProgrammes()
        private set

    fun importPlaylist(text: String) {
        playlistFile.writeText(text)
        channels = M3uParser.parse(text)
        programmes = loadProgrammes()
    }

    fun importEpg(text: String) {
        epgFile.writeText(text)
        programmes = epgRepository.replaceFromXmlTv(text, channels)
    }

    fun clear() {
        playlistFile.delete()
        epgFile.delete()
        channels = emptyList()
        programmes = emptyList()
    }

    fun channelLogo(channel: Channel): String? = channel.logoUrl
        ?: channel.tvgId?.let { id -> XmlTvParser.parseChannelLogos(epgFile.takeIf { it.exists() }?.readText().orEmpty())[id] }

    private fun loadChannels(): List<Channel> = runCatching {
        if (playlistFile.exists()) M3uParser.parse(playlistFile.readText()) else emptyList()
    }.getOrDefault(emptyList())

    private fun loadProgrammes(): List<EpgProgramme> = runCatching {
        if (!epgFile.exists()) emptyList() else epgRepository.replaceFromXmlTv(epgFile.readText(), channels)
    }.getOrDefault(emptyList())
}
