package com.zenplayer.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZenPlayerApp(SettingsStore(this)) }
    }
}

private val Background = Color(0xFF07080D)
private val TextPrimary = Color(0xFFF5F6FA)
private val TextSecondary = Color(0xFF9EA3B3)

private fun glassFill(intensity: Int, selected: Boolean = false): Color {
    val level = intensity.coerceIn(1, 10)
    val alpha = if (selected) 0.13f + level * 0.022f else 0.035f + level * 0.014f
    return Color.White.copy(alpha.coerceIn(0.035f, 0.35f))
}

private fun glassBorder(intensity: Int, focused: Boolean, accent: Color): Color =
    if (focused) accent.copy(.95f) else Color.White.copy((.04f + intensity * .012f).coerceIn(.045f, .16f))

@Composable
fun ZenPlayerApp(settings: SettingsStore) {
    var page by remember { mutableStateOf("home") }
    val accent = when (settings.ui.theme) {
        ZenTheme.AURORA -> Color(0xFF8D7CFF)
        ZenTheme.OBSIDIAN -> Color(0xFF65A8FF)
        ZenTheme.FROST -> Color(0xFF72D7D2)
        ZenTheme.AMBER -> Color(0xFFFFB45E)
    }
    val glow = when (settings.ui.theme) {
        ZenTheme.AURORA -> Color(0xFF342B70)
        ZenTheme.OBSIDIAN -> Color(0xFF142B4B)
        ZenTheme.FROST -> Color(0xFF153B3B)
        ZenTheme.AMBER -> Color(0xFF49301B)
    }
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(glow, Background, Background), radius = 1200f))) {
        Row(Modifier.fillMaxSize().padding(26.dp)) {
            GlassNavigation(page, accent, settings.ui.glassIntensity) { page = it }
            Spacer(Modifier.width(20.dp))
            when (page) {
                "settings" -> SettingsScreen(settings, accent)
                "epg" -> EpgGuide(accent)
                else -> HomeContent(settings, accent)
            }
        }
    }
}

@Composable
private fun GlassNavigation(page: String, accent: Color, intensity: Int, onNavigate: (String) -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { first.requestFocus() }
    Column(
        Modifier.width(82.dp).fillMaxHeight().clip(RoundedCornerShape(26.dp))
            .background(glassFill(intensity)).border(1.dp, glassBorder(intensity, false, accent), RoundedCornerShape(26.dp))
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NavIcon(Icons.Default.Home, "Home", page == "home", accent, intensity, first) { onNavigate("home") }
        NavIcon(Icons.Default.LiveTv, "TV Guide", page == "epg", accent, intensity) { onNavigate("epg") }
        NavIcon(Icons.Default.Search, "Search", page == "search", accent, intensity) { onNavigate("search") }
        Spacer(Modifier.weight(1f))
        NavIcon(Icons.Default.Settings, "Settings", page == "settings", accent, intensity) { onNavigate("settings") }
    }
}

@Composable
private fun NavIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, accent: Color, intensity: Int, requester: FocusRequester? = null, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val modifier = Modifier.width(58.dp).height(54.dp)
        .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
        .clip(RoundedCornerShape(17.dp)).background(glassFill(intensity, selected || focused))
        .border(if (focused) 2.dp else 1.dp, glassBorder(intensity, focused, accent), RoundedCornerShape(17.dp))
        .onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick)
        .onKeyEvent { event ->
            if (event.type == KeyEventType.KeyUp && (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter)) { onClick(); true } else false
        }
    Box(modifier, Alignment.Center) { Icon(icon, label, tint = if (selected || focused) TextPrimary else TextSecondary) }
}

@Composable
private fun HomeContent(settings: SettingsStore, accent: Color) {
    Column(Modifier.fillMaxSize()) {
        Text("ZENPLAYER", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(10.dp))
        HeroCard(accent, settings.ui.glassIntensity)
        Spacer(Modifier.height(18.dp))
        Section("Live jetzt", listOf("Das Erste", "ZDF", "RTL", "ProSieben", "VOX"), accent, settings.ui.glassIntensity)
        Spacer(Modifier.height(18.dp))
        Section("Für dich", listOf("ARD", "ZDFneo", "3sat", "arte", "ONE"), accent, settings.ui.glassIntensity)
    }
}

@Composable
private fun HeroCard(accent: Color, intensity: Int) {
    Box(Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(28.dp))
        .background(Brush.linearGradient(listOf(accent.copy(.24f), glassFill(intensity, true), Color.Black.copy(.38f))))
        .border(1.dp, Color.White.copy(.12f), RoundedCornerShape(28.dp)).padding(26.dp)) {
        Column(Modifier.align(Alignment.BottomStart)) {
            Text("LIVE · DAS ERSTE", color = Color.White.copy(.72f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp)); Text("Tagesschau", color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp)); Text("20:00 – 20:15 · Nachrichten", color = TextSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun Section(title: String, channels: List<String>, accent: Color, intensity: Int) {
    Column { Text(title, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) { items(channels) { ChannelCard(it, accent, intensity) } }
    }
}

@Composable
private fun ChannelCard(name: String, accent: Color, intensity: Int) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.025f else 1f, label = "focus")
    Box(Modifier.scale(scale).width(178.dp).height(100.dp).clip(RoundedCornerShape(20.dp))
        .background(glassFill(intensity, focused)).border(if (focused) 2.dp else 1.dp, glassBorder(intensity, focused, accent), RoundedCornerShape(20.dp))
        .onFocusChanged { focused = it.isFocused }.focusable().clickable { }
        .padding(12.dp)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(Color.White.copy(.045f)), Alignment.Center) {
                Text(initials(name), color = if (focused) accent else TextSecondary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(5.dp))
            Text(name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun SettingsScreen(settings: SettingsStore, accent: Color) {
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Einstellungen", color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold); Text("ZenPlayer bis ins Detail anpassen", color = TextSecondary, fontSize = 13.sp) }; Icon(Icons.Default.Tune, null, tint = accent, modifier = Modifier.padding(10.dp)) } }
        item { SettingsSection("Darstellung", "Look & Feel", settings.ui.glassIntensity, accent) {
            ThemePicker(settings, accent)
            ChoiceRow("Senderdarstellung", settings.ui.channelListStyle.label, ChannelListStyle.entries.map { it.label }, accent) { value -> settings.updateUi(settings.ui.copy(channelListStyle = ChannelListStyle.entries.first { it.label == value })) }
            IntensityRow("Milchglas / Glasstärke", settings.ui.glassIntensity, accent) { settings.updateUi(settings.ui.copy(glassIntensity = it)) }
            ToggleRow("Senderlogos anzeigen", "Logos aus der M3U/EPG verwenden", settings.ui.showLogos, accent) { settings.updateUi(settings.ui.copy(showLogos = it)) }
            ToggleRow("Gruppenüberschriften", "Kategorien klar voneinander trennen", settings.ui.showGroupHeaders, accent) { settings.updateUi(settings.ui.copy(showGroupHeaders = it)) }
            ToggleRow("Focus-Animationen", "Sanfte Skalierung und Glow-Effekte", settings.ui.animations, accent) { settings.updateUi(settings.ui.copy(animations = it)) }
            ToggleRow("Reduzierte Bewegung", "Animationen minimieren", settings.ui.reducedMotion, accent) { settings.updateUi(settings.ui.copy(reducedMotion = it)) }
        }}
        item { SettingsSection("Player", "Wiedergabe & Streaming", settings.ui.glassIntensity, accent) {
            ChoiceRow("Buffering", settings.player.bufferMode.label, BufferMode.entries.map { it.label }, accent) { value -> settings.updatePlayer(settings.player.copy(bufferMode = BufferMode.entries.first { it.label == value })) }
            ToggleRow("Live sofort starten", "Beim Öffnen eines Senders direkt abspielen", settings.player.startLiveImmediately, accent) { settings.updatePlayer(settings.player.copy(startLiveImmediately = it)) }
            ToggleRow("Auto-Play", "Nächsten Eintrag automatisch abspielen", settings.player.autoPlayNext, accent) { settings.updatePlayer(settings.player.copy(autoPlayNext = it)) }
            ToggleRow("Position merken", "Replay- und VOD-Fortschritt speichern", settings.player.rememberPosition, accent) { settings.updatePlayer(settings.player.copy(rememberPosition = it)) }
            ToggleRow("Hardware-Decoding", "Hardwarepfad bevorzugen", settings.player.hardwareAcceleration, accent) { settings.updatePlayer(settings.player.copy(hardwareAcceleration = it)) }
            ToggleRow("Deinterlacing", "Interlaced Streams verbessern", settings.player.deinterlacing, accent) { settings.updatePlayer(settings.player.copy(deinterlacing = it)) }
            ToggleRow("Lautstärke normalisieren", "Sprünge zwischen Sendern reduzieren", settings.player.audioNormalization, accent) { settings.updatePlayer(settings.player.copy(audioNormalization = it)) }
            ToggleRow("Player-Statistiken", "Decoder, Bitrate und Buffer anzeigen", settings.player.showPlayerStats, accent) { settings.updatePlayer(settings.player.copy(showPlayerStats = it)) }
        }}
        item { SettingsSection("Catch-up & Replay", "Probleme automatisch abfangen", settings.ui.glassIntensity, accent) {
            ToggleRow("Shared Catch-up bevorzugen", "Archiv eines passenden Schwester-Senders verwenden", settings.player.preferCatchupSibling, accent) { settings.updatePlayer(settings.player.copy(preferCatchupSibling = it)) }
            ToggleRow("Catch-up vor Wechsel bestätigen", "Vor alternativem Archiv nachfragen", settings.player.catchupConfirmation, accent) { settings.updatePlayer(settings.player.copy(catchupConfirmation = it)) }
        }}
        item { SettingsSection("Fernbedienung", "D-Pad-first für Android TV", settings.ui.glassIntensity, accent) {
            ChoiceRow("▲ / ▼", settings.remote.channelUpDown, listOf("Sender wechseln", "EPG bewegen", "Lautstärke"), accent) { settings.updateRemote(settings.remote.copy(channelUpDown = it)) }
            ChoiceRow("◀ / ▶", settings.remote.leftRight, listOf("EPG / Zeitleiste", "Sender wechseln", "Player Controls"), accent) { settings.updateRemote(settings.remote.copy(leftRight = it)) }
            ChoiceRow("Long Press", settings.remote.longPressUpDown, listOf("Schnell zappen", "Gruppen wechseln", "Lautstärke"), accent) { settings.updateRemote(settings.remote.copy(longPressUpDown = it)) }
            ChoiceRow("Zurück", settings.remote.backAction, listOf("Overlay schließen", "Zum Live-TV", "App verlassen"), accent) { settings.updateRemote(settings.remote.copy(backAction = it)) }
            ChoiceRow("OK / Enter", settings.remote.okAction, listOf("Wiedergabe / Auswahl", "EPG öffnen", "Player Overlay"), accent) { settings.updateRemote(settings.remote.copy(okAction = it)) }
            ToggleRow("Nummerntasten", "Optional für Fernbedienungen mit Ziffern", settings.remote.numericKeys, accent) { settings.updateRemote(settings.remote.copy(numericKeys = it)) }
            ToggleRow("Senderhistorie", "Schnell zum vorherigen Sender zurück", settings.remote.channelHistory, accent) { settings.updateRemote(settings.remote.copy(channelHistory = it)) }
        }}
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun SettingsSection(title: String, subtitle: String, intensity: Int, accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(glassFill(intensity)).border(1.dp, glassBorder(intensity, false, accent), RoundedCornerShape(24.dp)).padding(20.dp)) {
        Text(title, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp)); content()
    }
}

@Composable
private fun IntensityRow(title: String, value: Int, accent: Color, onChange: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("STUFE $value / 10", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(7.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            (1..10).forEach { level ->
                var focused by remember { mutableStateOf(false) }
                val active = level == value
                Box(Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(9.dp))
                    .background(if (active) accent.copy(.30f) else if (focused) accent.copy(.15f) else Color.White.copy(.045f))
                    .border(if (focused || active) 2.dp else 1.dp, if (focused || active) accent.copy(.9f) else Color.White.copy(.06f), RoundedCornerShape(9.dp))
                    .onFocusChanged { focused = it.isFocused }.focusable().clickable { onChange(level) }
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyUp && (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter)) { onChange(level); true } else false
                    }, Alignment.Center) {
                    Text(level.toString(), color = if (active || focused) TextPrimary else TextSecondary, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("← / → wählen · OK setzen", color = TextSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun ThemePicker(settings: SettingsStore, accent: Color) {
    ChoiceRow("Theme", settings.ui.theme.label, ZenTheme.entries.map { it.label }, accent) { value -> settings.updateUi(settings.ui.copy(theme = ZenTheme.entries.first { it.label == value })) }
}

@Composable
private fun ChoiceRow(title: String, value: String, options: List<String>, accent: Color, onChange: (String) -> Unit) {
    var index by remember(value) { mutableStateOf(options.indexOf(value).coerceAtLeast(0)) }
    Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium); Text(options[index], color = TextSecondary, fontSize = 11.sp) }
            Box(Modifier.width(52.dp).height(40.dp).clip(RoundedCornerShape(13.dp)).background(Color.White.copy(.06f)).focusable().clickable { index = (index + 1) % options.size; onChange(options[index]) }.onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp) when (event.key) {
                    Key.DirectionLeft -> { index = (index - 1 + options.size) % options.size; onChange(options[index]); true }
                    Key.DirectionRight -> { index = (index + 1) % options.size; onChange(options[index]); true }
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> { onChange(options[index]); true }
                    else -> false
                } else false
            }, Alignment.Center) { Text("›", color = accent, fontSize = 24.sp) }
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, accent: Color, onChange: (Boolean) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(if (focused) glassFill(10, true) else Color.Transparent).onFocusChanged { focused = it.isFocused }.focusable().clickable { onChange(!checked) }.onKeyEvent { event ->
        if (event.type == KeyEventType.KeyUp && (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter)) { onChange(!checked); true } else false
    }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium); Text(subtitle, color = TextSecondary, fontSize = 11.sp) }
        Text(if (checked) "AN" else "AUS", color = if (checked) accent else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private fun initials(name: String): String = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercaseChar().toString() }
