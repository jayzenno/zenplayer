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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZenPlayerApp(SettingsStore(this)) }
    }
}

private val Background = Color(0xFF07080D)
private val Glass = Color(0x22FFFFFF)
private val GlassStrong = Color(0x3DFFFFFF)
private val TextPrimary = Color(0xFFF5F6FA)
private val TextSecondary = Color(0xFF9EA3B3)

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
            GlassNavigation(page, accent) { page = it }
            Spacer(Modifier.width(24.dp))
            when (page) {
                "settings" -> SettingsScreen(settings, accent)
                "epg" -> EpgGuide(accent)
                else -> HomeContent(settings, accent)
            }
        }
    }
}

@Composable
private fun GlassNavigation(page: String, accent: Color, onNavigate: (String) -> Unit) {
    Column(Modifier.fillMaxHeight().width(78.dp).clip(RoundedCornerShape(26.dp)).background(Glass)
        .border(1.dp, Color.White.copy(.09f), RoundedCornerShape(26.dp)).padding(vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        NavIcon(Icons.Default.Home, "Home", page == "home", accent) { onNavigate("home") }
        NavIcon(Icons.Default.LiveTv, "TV Guide", page == "epg", accent) { onNavigate("epg") }
        NavIcon(Icons.Default.Search, "Search", false, accent) { onNavigate("home") }
        Spacer(Modifier.weight(1f))
        NavIcon(Icons.Default.Settings, "Settings", page == "settings", accent) { onNavigate("settings") }
    }
}

@Composable
private fun NavIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.width(54.dp).height(54.dp).clip(RoundedCornerShape(18.dp))
        .background(if (selected || focused) GlassStrong else Color.Transparent)
        .border(if (focused) 1.5.dp else 0.dp, if (focused) accent else Color.Transparent, RoundedCornerShape(18.dp))
        .onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick), Alignment.Center) {
        Icon(icon, label, tint = if (selected || focused) TextPrimary else TextSecondary)
    }
}

@Composable
private fun HomeContent(settings: SettingsStore, accent: Color) {
    Column(Modifier.fillMaxSize()) {
        Text("ZENPLAYER", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(12.dp))
        HeroCard(accent)
        Spacer(Modifier.height(22.dp))
        Section("Live jetzt", listOf("Das Erste", "ZDF", "RTL", "ProSieben", "VOX"), accent)
        Spacer(Modifier.height(20.dp))
        Section("Für dich", listOf("ARD", "ZDFneo", "3sat", "arte", "ONE"), accent)
    }
}

@Composable
private fun HeroCard(accent: Color) {
    Box(Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(30.dp))
        .background(Brush.linearGradient(listOf(accent.copy(.35f), Color(0xFF11131D), Color(0x99111420))))
        .border(1.dp, Color.White.copy(.12f), RoundedCornerShape(30.dp)).padding(30.dp)) {
        Column(Modifier.align(Alignment.BottomStart)) {
            Text("LIVE · DAS ERSTE", color = Color.White.copy(.72f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp)); Text("Tagesschau", color = TextPrimary, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp)); Text("20:00 – 20:15  ·  Nachrichten", color = TextSecondary, fontSize = 15.sp)
        }
    }
}

@Composable
private fun Section(title: String, channels: List<String>, accent: Color) {
    Column { Text(title, color = TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) { items(channels) { ChannelCard(it, accent) } }
    }
}

@Composable
private fun ChannelCard(name: String, accent: Color) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.055f else 1f, label = "focus")
    Box(Modifier.scale(scale).width(190.dp).height(108.dp).clip(RoundedCornerShape(22.dp))
        .background(if (focused) GlassStrong else Glass)
        .border(1.5.dp, if (focused) accent.copy(.8f) else Color.White.copy(.08f), RoundedCornerShape(22.dp))
        .onFocusChanged { focused = it.isFocused }.focusable().clickable { }.padding(18.dp)) {
        Column(Modifier.align(Alignment.BottomStart)) { Text(name, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold); Text("LIVE", color = TextSecondary, fontSize = 11.sp) }
    }
}

@Composable
private fun SettingsScreen(settings: SettingsStore, accent: Color) {
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Einstellungen", color = TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold); Text("ZenPlayer bis ins Detail anpassen", color = TextSecondary, fontSize = 14.sp) }; Icon(Icons.Default.Tune, null, tint = accent, modifier = Modifier.padding(12.dp)) } }
        item { SettingsSection("Darstellung", "Look & Feel") {
            ThemePicker(settings, accent)
            ChoiceRow("Senderdarstellung", settings.ui.channelListStyle.label, ChannelListStyle.entries.map { it.label }) { value -> settings.updateUi(settings.ui.copy(channelListStyle = ChannelListStyle.entries.first { it.label == value })) }
            ToggleRow("Senderlogos anzeigen", "Logos aus der M3U/EPG verwenden", settings.ui.showLogos) { settings.updateUi(settings.ui.copy(showLogos = it)) }
            ToggleRow("Gruppenüberschriften", "Kategorien klar voneinander trennen", settings.ui.showGroupHeaders) { settings.updateUi(settings.ui.copy(showGroupHeaders = it)) }
            ToggleRow("Focus-Animationen", "Sanfte Skalierung und Glow-Effekte", settings.ui.animations) { settings.updateUi(settings.ui.copy(animations = it)) }
            ToggleRow("Reduzierte Bewegung", "Animationen minimieren", settings.ui.reducedMotion) { settings.updateUi(settings.ui.copy(reducedMotion = it)) }
        }}
        item { SettingsSection("Player", "Wiedergabe & Streaming") {
            ChoiceRow("Buffering", settings.player.bufferMode.label, BufferMode.entries.map { it.label }) { value -> settings.updatePlayer(settings.player.copy(bufferMode = BufferMode.entries.first { it.label == value })) }
            ToggleRow("Live sofort starten", "Beim Öffnen eines Senders direkt abspielen", settings.player.startLiveImmediately) { settings.updatePlayer(settings.player.copy(startLiveImmediately = it)) }
            ToggleRow("Auto-Play", "Nächsten Eintrag automatisch abspielen", settings.player.autoPlayNext) { settings.updatePlayer(settings.player.copy(autoPlayNext = it)) }
            ToggleRow("Position merken", "Replay- und VOD-Fortschritt speichern", settings.player.rememberPosition) { settings.updatePlayer(settings.player.copy(rememberPosition = it)) }
            ToggleRow("Hardware-Decoding", "Hardwarepfad bevorzugen", settings.player.hardwareAcceleration) { settings.updatePlayer(settings.player.copy(hardwareAcceleration = it)) }
            ToggleRow("Deinterlacing", "Interlaced Streams verbessern", settings.player.deinterlacing) { settings.updatePlayer(settings.player.copy(deinterlacing = it)) }
            ToggleRow("Lautstärke normalisieren", "Sprünge zwischen Sendern reduzieren", settings.player.audioNormalization) { settings.updatePlayer(settings.player.copy(audioNormalization = it)) }
            ToggleRow("Player-Statistiken", "Decoder, Bitrate und Buffer anzeigen", settings.player.showPlayerStats) { settings.updatePlayer(settings.player.copy(showPlayerStats = it)) }
        }}
        item { SettingsSection("Catch-up & Replay", "Probleme automatisch abfangen") {
            ToggleRow("Shared Catch-up bevorzugen", "Archiv eines passenden Schwester-Senders verwenden", settings.player.preferCatchupSibling) { settings.updatePlayer(settings.player.copy(preferCatchupSibling = it)) }
            ToggleRow("Catch-up vor Wechsel bestätigen", "Vor alternativem Archiv nachfragen", settings.player.catchupConfirmation) { settings.updatePlayer(settings.player.copy(catchupConfirmation = it)) }
        }}
        item { SettingsSection("Fernbedienung", "D-Pad-first für Android TV") {
            ChoiceRow("▲ / ▼", settings.remote.channelUpDown, listOf("Sender wechseln", "EPG bewegen", "Lautstärke")) { settings.updateRemote(settings.remote.copy(channelUpDown = it)) }
            ChoiceRow("◀ / ▶", settings.remote.leftRight, listOf("EPG / Zeitleiste", "Sender wechseln", "Player Controls")) { settings.updateRemote(settings.remote.copy(leftRight = it)) }
            ChoiceRow("Long Press", settings.remote.longPressUpDown, listOf("Schnell zappen", "Gruppen wechseln", "Lautstärke")) { settings.updateRemote(settings.remote.copy(longPressUpDown = it)) }
            ChoiceRow("Zurück", settings.remote.backAction, listOf("Overlay schließen", "Zum Live-TV", "App verlassen")) { settings.updateRemote(settings.remote.copy(backAction = it)) }
            ChoiceRow("OK / Enter", settings.remote.okAction, listOf("Wiedergabe / Auswahl", "EPG öffnen", "Player Overlay")) { settings.updateRemote(settings.remote.copy(okAction = it)) }
            ToggleRow("Nummerntasten", "Optional für Fernbedienungen mit Ziffern", settings.remote.numericKeys) { settings.updateRemote(settings.remote.copy(numericKeys = it)) }
            ToggleRow("Senderhistorie", "Schnell zum vorherigen Sender zurück", settings.remote.channelHistory) { settings.updateRemote(settings.remote.copy(channelHistory = it)) }
        }}
        item { SettingsSection("EPG & Guide", "Elektronischer Programmführer") {
            ToggleRow("EPG automatisch aktualisieren", "Guide im Hintergrund aktuell halten", true) { }
            ToggleRow("Jetzt / Als Nächstes", "Programminfos auf Live-Karten", true) { }
            ToggleRow("Zeitleiste beim Zappen", "Aktuelle Sendung und Fortschritt", true) { }
            ToggleRow("Vergangenheit durchsuchen", "EPG nach hinten navigieren", true) { }
            ToggleRow("Zukunft durchsuchen", "EPG in die Zukunft navigieren", true) { }
            ToggleRow("Von vorne starten", "Aktuelle Sendung per Catch-up ab Start abspielen", true) { }
        }}
        item { SettingsSection("Playlist & Daten", "Quellen und lokale Daten") {
            ActionRow("M3U / Xtream Quellen", "Quellen verwalten und synchronisieren", Icons.Default.LiveTv) { }
            ActionRow("Favoriten & Senderordnung", "Favoriten, versteckte Sender und Reihenfolge", Icons.Default.Tune) { }
            ActionRow("Cache leeren", "EPG-, Logo- und Metadaten-Cache löschen", Icons.Default.Info) { }
        }}
        item { SettingsSection("System", "Zurücksetzen & Informationen") {
            ActionRow("Auf Standard zurücksetzen", "Alle ZenPlayer-Einstellungen zurücksetzen", Icons.Default.ArrowBack) { settings.reset() }
            ActionRow("Über ZenPlayer", "Version und Open-Source-Komponenten", Icons.Default.Info) { }
        }}
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SettingsSection(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Glass).border(1.dp, Color.White.copy(.08f), RoundedCornerShape(26.dp)).padding(22.dp)) {
        Text(title, color = TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp)); content()
    }
}

@Composable
private fun ThemePicker(settings: SettingsStore, accent: Color) {
    Column {
        Text("Theme", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Text("Live-Vorschau für den gesamten Look", color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(ZenTheme.entries) { theme ->
                val selected = theme == settings.ui.theme
                val themeColor = when (theme) { ZenTheme.AURORA -> Color(0xFF8D7CFF); ZenTheme.OBSIDIAN -> Color(0xFF65A8FF); ZenTheme.FROST -> Color(0xFF72D7D2); ZenTheme.AMBER -> Color(0xFFFFB45E) }
                Box(Modifier.width(170.dp).height(86.dp).clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(themeColor.copy(.5f), Color(0xFF11131D))))
                    .border(if (selected) 2.dp else 1.dp, if (selected) accent else Color.White.copy(.12f), RoundedCornerShape(20.dp))
                    .focusable().clickable { settings.updateUi(settings.ui.copy(theme = theme)) }.padding(14.dp)) {
                    Column(Modifier.fillMaxSize()) { Text(theme.label, color = TextPrimary, fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); Text(if (selected) "AKTIV" else "Vorschau", color = TextSecondary, fontSize = 11.sp) }
                }
            }
        }
    }
}

@Composable
private fun ChoiceRow(title: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    var index by remember(value) { mutableStateOf(options.indexOf(value).coerceAtLeast(0)) }
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, color = TextPrimary, fontSize = 15.sp); Text(value, color = TextSecondary, fontSize = 12.sp) }
        Box(Modifier.clip(RoundedCornerShape(14.dp)).background(GlassStrong).border(1.dp, Color.White.copy(.08f), RoundedCornerShape(14.dp)).focusable().clickable { index = (index + 1) % options.size; onSelect(options[index]) }.padding(horizontal = 16.dp, vertical = 10.dp)) { Text("Ändern", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    var value by remember(checked) { mutableStateOf(checked) }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { value = !value; onCheckedChange(value) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, color = TextPrimary, fontSize = 15.sp); Text(subtitle, color = TextSecondary, fontSize = 12.sp) }
        Text(if (value) "AN" else "AUS", color = if (value) TextPrimary else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActionRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.padding(end = 14.dp))
        Column { Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium); Text(subtitle, color = TextSecondary, fontSize = 12.sp) }
    }
}
