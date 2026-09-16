package com.zenplayer.tv

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ZenTheme(val label: String) { AURORA("Aurora"), OBSIDIAN("Obsidian"), FROST("Frost"), AMBER("Amber") }
enum class BufferMode(val label: String) { AUTO("Automatisch"), LOW("Niedrig"), BALANCED("Ausgewogen"), HIGH("Hoch") }
enum class ChannelListStyle(val label: String) { CINEMATIC("Cinematic"), COMPACT("Kompakt"), CARDS("Karten") }
enum class EpgPageSize(val label: String, val hours: Int) { FOUR("4 Stunden", 4), SIX("6 Stunden", 6), EIGHT("8 Stunden", 8), TWELVE("12 Stunden", 12) }

data class PlayerSettings(
    val bufferMode: BufferMode = BufferMode.AUTO,
    val startLiveImmediately: Boolean = true,
    val autoPlayNext: Boolean = true,
    val rememberPosition: Boolean = true,
    val hardwareAcceleration: Boolean = true,
    val deinterlacing: Boolean = true,
    val audioNormalization: Boolean = false,
    val showPlayerStats: Boolean = false,
    val preferCatchupSibling: Boolean = true,
    val catchupConfirmation: Boolean = false,
    val startCurrentProgrammeFromBeginning: Boolean = true,
    val liveEdgeOnChannelSwitch: Boolean = true,
    val enableTimeshiftWhenAvailable: Boolean = true,
)

data class UiSettings(
    val theme: ZenTheme = ZenTheme.AURORA,
    val channelListStyle: ChannelListStyle = ChannelListStyle.CINEMATIC,
    val glassIntensity: Int = 7,
    val showLogos: Boolean = true,
    val showGroupHeaders: Boolean = true,
    val animations: Boolean = true,
    val reducedMotion: Boolean = false,
    val clock24h: Boolean = true,
)

data class EpgSettings(
    val pageSize: EpgPageSize = EpgPageSize.SIX,
    val showPreviousHours: Boolean = true,
    val showFutureHours: Boolean = true,
    val autoCenterOnNow: Boolean = true,
    val showProgrammeProgress: Boolean = true,
    val showDescriptions: Boolean = true,
    val showCatchupBadge: Boolean = true,
    val startFromBeginningLabel: Boolean = true,
)

data class RemoteSettings(
    val channelUpDown: String = "Sender wechseln",
    val leftRight: String = "EPG / Zeitleiste",
    val longPressUpDown: String = "Schnell zappen",
    val backAction: String = "Overlay schließen",
    val okAction: String = "Wiedergabe / Auswahl",
    val numericKeys: Boolean = true,
    val channelHistory: Boolean = true,
    val longPressLeftRight: String = "10 Sekunden springen",
    val doublePressOk: String = "Von vorne starten",
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("zenplayer_settings", Context.MODE_PRIVATE)

    var ui by mutableStateOf(loadUi())
        private set
    var player by mutableStateOf(loadPlayer())
        private set
    var epg by mutableStateOf(loadEpg())
        private set
    var remote by mutableStateOf(loadRemote())
        private set

    fun updateUi(value: UiSettings) { ui = value; saveUi(value) }
    fun updatePlayer(value: PlayerSettings) { player = value; savePlayer(value) }
    fun updateEpg(value: EpgSettings) { epg = value; saveEpg(value) }
    fun updateRemote(value: RemoteSettings) { remote = value; saveRemote(value) }
    fun reset() { prefs.edit().clear().apply(); ui = UiSettings(); player = PlayerSettings(); epg = EpgSettings(); remote = RemoteSettings() }

    private fun loadUi() = UiSettings(
        theme = enum("theme", ZenTheme.AURORA), channelListStyle = enum("listStyle", ChannelListStyle.CINEMATIC),
        glassIntensity = prefs.getInt("glassIntensity", 7).coerceIn(1, 10),
        showLogos = prefs.getBoolean("showLogos", true), showGroupHeaders = prefs.getBoolean("groupHeaders", true),
        animations = prefs.getBoolean("animations", true), reducedMotion = prefs.getBoolean("reducedMotion", false), clock24h = prefs.getBoolean("clock24h", true)
    )
    private fun loadPlayer() = PlayerSettings(
        bufferMode = enum("buffer", BufferMode.AUTO), startLiveImmediately = prefs.getBoolean("startLive", true),
        autoPlayNext = prefs.getBoolean("autoNext", true), rememberPosition = prefs.getBoolean("rememberPosition", true),
        hardwareAcceleration = prefs.getBoolean("hardware", true), deinterlacing = prefs.getBoolean("deinterlace", true),
        audioNormalization = prefs.getBoolean("audioNorm", false), showPlayerStats = prefs.getBoolean("stats", false),
        preferCatchupSibling = prefs.getBoolean("catchupSibling", true), catchupConfirmation = prefs.getBoolean("catchupConfirm", false),
        startCurrentProgrammeFromBeginning = prefs.getBoolean("programmeStart", true), liveEdgeOnChannelSwitch = prefs.getBoolean("liveEdge", true),
        enableTimeshiftWhenAvailable = prefs.getBoolean("timeshift", true)
    )
    private fun loadEpg() = EpgSettings(
        pageSize = enum("epgPage", EpgPageSize.SIX), showPreviousHours = prefs.getBoolean("epgPrevious", true),
        showFutureHours = prefs.getBoolean("epgFuture", true), autoCenterOnNow = prefs.getBoolean("epgCenterNow", true),
        showProgrammeProgress = prefs.getBoolean("epgProgress", true), showDescriptions = prefs.getBoolean("epgDescriptions", true),
        showCatchupBadge = prefs.getBoolean("epgCatchupBadge", true), startFromBeginningLabel = prefs.getBoolean("epgStartLabel", true)
    )
    private fun loadRemote() = RemoteSettings(
        channelUpDown = prefs.getString("remoteUpDown", "Sender wechseln") ?: "Sender wechseln",
        leftRight = prefs.getString("remoteLeftRight", "EPG / Zeitleiste") ?: "EPG / Zeitleiste",
        longPressUpDown = prefs.getString("remoteLong", "Schnell zappen") ?: "Schnell zappen",
        backAction = prefs.getString("remoteBack", "Overlay schließen") ?: "Overlay schließen",
        okAction = prefs.getString("remoteOk", "Wiedergabe / Auswahl") ?: "Wiedergabe / Auswahl",
        numericKeys = prefs.getBoolean("numeric", true), channelHistory = prefs.getBoolean("history", true),
        longPressLeftRight = prefs.getString("remoteLongLeft", "10 Sekunden springen") ?: "10 Sekunden springen",
        doublePressOk = prefs.getString("remoteDoubleOk", "Von vorne starten") ?: "Von vorne starten"
    )

    private fun saveUi(v: UiSettings) = prefs.edit().putString("theme", v.theme.name).putString("listStyle", v.channelListStyle.name)
        .putInt("glassIntensity", v.glassIntensity.coerceIn(1, 10)).putBoolean("showLogos", v.showLogos)
        .putBoolean("groupHeaders", v.showGroupHeaders).putBoolean("animations", v.animations)
        .putBoolean("reducedMotion", v.reducedMotion).putBoolean("clock24h", v.clock24h).apply()
    private fun savePlayer(v: PlayerSettings) = prefs.edit().putString("buffer", v.bufferMode.name).putBoolean("startLive", v.startLiveImmediately)
        .putBoolean("autoNext", v.autoPlayNext).putBoolean("rememberPosition", v.rememberPosition).putBoolean("hardware", v.hardwareAcceleration)
        .putBoolean("deinterlace", v.deinterlacing).putBoolean("audioNorm", v.audioNormalization).putBoolean("stats", v.showPlayerStats)
        .putBoolean("catchupSibling", v.preferCatchupSibling).putBoolean("catchupConfirm", v.catchupConfirmation)
        .putBoolean("programmeStart", v.startCurrentProgrammeFromBeginning).putBoolean("liveEdge", v.liveEdgeOnChannelSwitch)
        .putBoolean("timeshift", v.enableTimeshiftWhenAvailable).apply()
    private fun saveEpg(v: EpgSettings) = prefs.edit().putString("epgPage", v.pageSize.name).putBoolean("epgPrevious", v.showPreviousHours)
        .putBoolean("epgFuture", v.showFutureHours).putBoolean("epgCenterNow", v.autoCenterOnNow).putBoolean("epgProgress", v.showProgrammeProgress)
        .putBoolean("epgDescriptions", v.showDescriptions).putBoolean("epgCatchupBadge", v.showCatchupBadge).putBoolean("epgStartLabel", v.startFromBeginningLabel).apply()
    private fun saveRemote(v: RemoteSettings) = prefs.edit().putString("remoteUpDown", v.channelUpDown).putString("remoteLeftRight", v.leftRight)
        .putString("remoteLong", v.longPressUpDown).putString("remoteBack", v.backAction).putString("remoteOk", v.okAction)
        .putBoolean("numeric", v.numericKeys).putBoolean("history", v.channelHistory).putString("remoteLongLeft", v.longPressLeftRight)
        .putString("remoteDoubleOk", v.doublePressOk).apply()

    private inline fun <reified T : Enum<T>> enum(key: String, fallback: T): T = runCatching { enumValueOf<T>(prefs.getString(key, fallback.name)!!) }.getOrDefault(fallback)
}
