package com.zenplayer.tv

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import java.util.Locale

private val ShellText = Color(0xFFF5F6FA)
private val ShellSecondary = Color(0xFF9EA3B3)

@Composable
fun ZenPlayerShell(settings: SettingsStore) {
    val ui = settings.ui
    val scale = ui.uiScale.coerceIn(75, 125) / 100f
    val baseDensity = LocalDensity.current
    val store = remember { PlaylistStore(LocalContext.current) }
    var page by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf<com.zenplayer.tv.domain.model.Channel?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    val accent = when (ui.theme) {
        ZenTheme.AURORA -> Color(0xFF70E6FF)
        ZenTheme.OBSIDIAN -> Color(0xFFB7B9FF)
        ZenTheme.FROST -> Color(0xFFB9E9FF)
        ZenTheme.AMBER -> Color(0xFFFFC66D)
    }

    CompositionLocalProvider(LocalDensity provides Density(baseDensity.density * scale, baseDensity.fontScale)) {
        Box(Modifier.fillMaxSize().background(Color(0xFF070A10))) {
            ZenAnimatedBackdrop(ui.animatedBackdrop, accent, ui.reducedMotion)
            Row(Modifier.fillMaxSize().padding(14.dp)) {
                Sidebar(page, accent) { page = it }
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f).fillMaxHeight().padding(4.dp)) {
                    when (page) {
                        0 -> HomeScreen(store, accent, reload) { playing = it }
                        1 -> RealEpgScreen(store, accent, reload) { playing = it }
                        2 -> PlaylistScreen(store, accent) { reload++ }
                        3 -> SearchScreen(store, accent) { playing = it }
                        else -> SettingsScreen(settings, accent)
                    }
                }
            }
            playing?.let { channel -> ZenPlayerScreen(channel, settings, onBack = { playing = null }) }
        }
    }
}

@Composable
private fun Sidebar(page: Int, accent: Color, onPage: (Int) -> Unit) {
    val items = listOf(Icons.Default.Home to "Home", Icons.Default.PlayArrow to "EPG", Icons.Default.List to "Playlist", Icons.Default.Search to "Suche", Icons.Default.Settings to "Settings")
    Column(Modifier.width(64.dp).fillMaxHeight().background(Color.White.copy(.045f), RoundedCornerShape(18.dp)).border(1.dp, Color.White.copy(.07f), RoundedCornerShape(18.dp)).padding(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        items.forEachIndexed { index, item ->
            var focused by remember { mutableStateOf(false) }
            Box(Modifier.width(46.dp).height(46.dp).background(if (page == index || focused) accent.copy(.16f) else Color.Transparent, RoundedCornerShape(14.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.Transparent, RoundedCornerShape(14.dp)).onFocusChanged { focused = it.isFocused }.focusable().clickable { onPage(index) }, Alignment.Center) {
                Icon(item.first, item.second, tint = if (page == index || focused) ShellText else ShellSecondary)
            }
        }
    }
}

@Composable
private fun HomeScreen(store: PlaylistStore, accent: Color, reload: Int, onPlay: (com.zenplayer.tv.domain.model.Channel) -> Unit) {
    val channels = store.channels
    Column(Modifier.fillMaxSize()) {
        Text("Willkommen bei ZenPlayer", color = ShellText, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(if (channels.isEmpty()) "Importiere zuerst deine M3U-Playlist." else "${channels.size} Sender bereit", color = ShellSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
        if (channels.isEmpty()) Text("Playlist → M3U / M3U8 importieren", color = accent, fontSize = 16.sp)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(bottom = 20.dp)) { items(channels) { channel -> ChannelRow(channel, accent) { onPlay(channel) } } }
    }
}

@Composable
private fun ChannelRow(channel: com.zenplayer.tv.domain.model.Channel, accent: Color, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(56.dp).background(if (focused) accent.copy(.13f) else Color.White.copy(.045f), RoundedCornerShape(15.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(.06f), RoundedCornerShape(15.dp)).onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (!channel.logoUrl.isNullOrBlank()) AsyncImage(channel.logoUrl, contentDescription = channel.name, modifier = Modifier.width(42.dp).height(42.dp), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
        else Text(initials(channel.name), color = if (focused) accent else ShellSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))
        Column(Modifier.weight(1f)) { Text(channel.name, color = ShellText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold); Text(channel.group ?: "Live TV", color = ShellSecondary, fontSize = 10.sp) }
        Icon(Icons.Default.PlayArrow, null, tint = if (focused) ShellText else ShellSecondary)
    }
}

@Composable
private fun PlaylistScreen(store: PlaylistStore, accent: Color, onImported: () -> Unit) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    val playlistPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { runCatching { context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> reader.readText() } ?: error("empty") }.onSuccess { text -> store.importPlaylist(text); message = "${store.channels.size} Sender importiert" }.onFailure { message = "M3U konnte nicht gelesen werden" }; onImported() } }
    val epgPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { runCatching { context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> reader.readText() } ?: error("empty") }.onSuccess { text -> store.importEpg(text); message = "${store.programmes.size} EPG-Einträge importiert" }.onFailure { message = "XMLTV konnte nicht gelesen werden" }; onImported() } }
    Column(Modifier.fillMaxSize()) {
        Text("Playlist", color = ShellText, fontSize = 30.sp, fontWeight = FontWeight.Bold); Text("Deine Quellen · lokal gespeichert", color = ShellSecondary, fontSize = 13.sp); Spacer(Modifier.height(18.dp))
        ImportCard("M3U / M3U8 Playlist", "Sender, Gruppen, tvg-id, Logos und Catch-up", accent) { playlistPicker.launch(arrayOf("*/*", "application/x-mpegURL", "audio/x-mpegurl")) }
        Spacer(Modifier.height(10.dp)); ImportCard("XMLTV / EPG", "Programme, Zeiten und vom Nutzer gelieferte Logos", accent) { epgPicker.launch(arrayOf("*/*", "application/xml", "text/xml")) }
        Spacer(Modifier.height(18.dp)); Text("${store.channels.size} Sender · ${store.programmes.size} Programme", color = ShellSecondary, fontSize = 13.sp); if (message.isNotBlank()) Text(message, color = accent, fontSize = 13.sp, modifier = Modifier.padding(top = 7.dp))
    }
}

@Composable
private fun ImportCard(title: String, subtitle: String, accent: Color, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(68.dp).background(if (focused) accent.copy(.14f) else Color.White.copy(.045f), RoundedCornerShape(16.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(.07f), RoundedCornerShape(16.dp)).onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, color = ShellText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, color = ShellSecondary, fontSize = 10.sp) }; Icon(Icons.Default.List, null, tint = if (focused) accent else ShellSecondary) }
}

@Composable
private fun RealEpgScreen(store: PlaylistStore, accent: Color, reload: Int, onPlay: (com.zenplayer.tv.domain.model.Channel) -> Unit) {
    val programmes = store.programmes
    Column(Modifier.fillMaxSize()) {
        Text("EPG", color = ShellText, fontSize = 30.sp, fontWeight = FontWeight.Bold); Text("Echtes XMLTV-Programm", color = ShellSecondary, fontSize = 13.sp); Spacer(Modifier.height(16.dp))
        if (programmes.isEmpty()) Text("Noch kein XMLTV importiert.", color = accent, fontSize = 15.sp)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val byChannel = programmes.groupBy { it.channelId }
            byChannel.keys.forEach { id ->
                val channel = store.channels.firstOrNull { it.id == id || it.tvgId == id || it.name == id }
                item { Text(channel?.name ?: id, color = ShellText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
                items(byChannel[id].orEmpty().take(12)) { p ->
                    val parent = channel
                    Row(Modifier.fillMaxWidth().height(52.dp).background(Color.White.copy(.045f), RoundedCornerShape(14.dp)).clickable { if (parent != null) onPlay(parent) }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(java.text.SimpleDateFormat("HH:mm", Locale.GERMANY).format(java.util.Date(p.start)), color = accent, fontSize = 11.sp, modifier = Modifier.width(55.dp)); Column(Modifier.weight(1f)) { Text(p.title, color = ShellText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Text(p.category ?: "TV", color = ShellSecondary, fontSize = 10.sp) }; if (p.isCatchupAvailable) Text("REPLAY", color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchScreen(store: PlaylistStore, accent: Color, onPlay: (com.zenplayer.tv.domain.model.Channel) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = store.channels.filter { query.isBlank() || it.name.contains(query, true) || it.group.orEmpty().contains(query, true) }
    Column(Modifier.fillMaxSize()) { Text("Suche", color = ShellText, fontSize = 30.sp, fontWeight = FontWeight.Bold); Text("Sender durchsuchen", color = ShellSecondary, fontSize = 13.sp); Spacer(Modifier.height(12.dp)); Row(Modifier.fillMaxWidth().height(50.dp).background(Color.White.copy(.05f), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) { Text(query.ifBlank { "Suche mit der Fernbedienung über die TV-Tastatur" }, color = if (query.isBlank()) ShellSecondary else ShellText, fontSize = 13.sp) }; Spacer(Modifier.height(12.dp)); LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) { items(filtered) { ChannelRow(it, accent) { onPlay(it) } } } }
}

@Composable
private fun SettingsScreen(settings: SettingsStore, accent: Color) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Player", "Live TV", "Playlist & EPG", "Darstellung", "System")
    Column(Modifier.fillMaxSize()) {
        Text("Einstellungen", color = ShellText, fontSize = 30.sp, fontWeight = FontWeight.Bold); Text("Kompakt organisiert statt endlos scrollen", color = ShellSecondary, fontSize = 13.sp); Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { tabs.forEachIndexed { i, title -> var focused by remember { mutableStateOf(false) }; Box(Modifier.background(if (tab == i || focused) accent.copy(.16f) else Color.White.copy(.045f), RoundedCornerShape(12.dp)).border(if (tab == i || focused) 2.dp else 1.dp, if (tab == i || focused) accent else Color.White.copy(.06f), RoundedCornerShape(12.dp)).onFocusChanged { focused = it.isFocused }.focusable().clickable { tab = i }.padding(horizontal = 11.dp, vertical = 8.dp)) { Text(title, color = ShellText, fontSize = 10.sp, fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal) } } }
        Spacer(Modifier.height(12.dp))
        when (tab) {
            0 -> PlayerSettingsTab(settings, accent)
            1 -> SettingsCard("Live TV", "Wiedergabe und Catch-up") { SettingChoice(settings.player.bufferMode.label, BufferMode.entries.map { it.label }, accent) { value -> settings.updatePlayer(settings.player.copy(bufferMode = BufferMode.entries.first { it.label == value })) } }
            2 -> SettingsCard("Playlist & EPG", "Datenquellen und Zuordnung") { Text("Import und Datenverwaltung findest du im Playlist-Reiter.", color = ShellSecondary, fontSize = 13.sp) }
            3 -> SettingsCard("Darstellung", "Persönliche TV-Größe") { ScaleSelector(settings.ui.uiScale, accent) { settings.updateUi(settings.ui.copy(uiScale = it)) }; SettingChoice(settings.ui.animatedBackdrop.label, AnimatedBackdrop.entries.map { it.label }, accent) { value -> settings.updateUi(settings.ui.copy(animatedBackdrop = AnimatedBackdrop.entries.first { it.label == value })) }; SettingChoice(settings.ui.theme.label, ZenTheme.entries.map { it.label }, accent) { value -> settings.updateUi(settings.ui.copy(theme = ZenTheme.entries.first { it.label == value })) }; IntensitySelector(settings.ui.glassIntensity, accent) { settings.updateUi(settings.ui.copy(glassIntensity = it)) } }
            else -> SettingsCard("System", "Diagnose und Verhalten") { Text("ZenPlayer · Android TV", color = ShellText, fontSize = 13.sp) }
        }
    }
}

@Composable
private fun PlayerSettingsTab(settings: SettingsStore, accent: Color) {
    val context = LocalContext.current
    var externalPicker by remember { mutableStateOf(false) }
    val externalApps = remember { context.packageManager.queryIntentActivities(Intent(Intent.ACTION_VIEW).apply { type = "video/*" }, 0) }
    SettingsCard("Player Engine", "Interner oder externer Player") {
        SettingChoice(settings.player.engine.label, PlaybackEngine.entries.map { it.label }, accent) { value -> settings.updatePlayer(settings.player.copy(engine = PlaybackEngine.entries.first { it.label == value })) }
        Spacer(Modifier.height(7.dp)); SettingsButton("Externen Player wählen", settings.player.externalPlayerPackage ?: "System-Auswahl", accent) { externalPicker = true }
    }
    if (externalPicker) PlayerPicker(externalApps, accent, onDismiss = { externalPicker = false }) { packageName -> settings.updatePlayer(settings.player.copy(externalPlayerPackage = packageName, engine = PlaybackEngine.EXTERNAL)); externalPicker = false }
}

@Composable
private fun SettingsCard(title: String, subtitle: String, accent: Color, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White.copy(.055f), RoundedCornerShape(20.dp)).border(1.dp, Color.White.copy(.07f), RoundedCornerShape(20.dp)).padding(17.dp)) { Text(title, color = ShellText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, color = ShellSecondary, fontSize = 11.sp); Spacer(Modifier.height(10.dp)); content() }
}

@Composable
private fun SettingChoice(value: String, options: List<String>, accent: Color, onChange: (String) -> Unit) {
    var index by remember(value) { mutableIntStateOf(options.indexOf(value).coerceAtLeast(0)) }
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(48.dp).background(if (focused) accent.copy(.12f) else Color.White.copy(.035f), RoundedCornerShape(13.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.Transparent, RoundedCornerShape(13.dp)).onFocusChanged { focused = it.isFocused }.focusable().onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && (e.key == Key.DirectionLeft || e.key == Key.DirectionRight || e.key == Key.DirectionCenter)) { index = if (e.key == Key.DirectionLeft) (index - 1 + options.size) % options.size else (index + 1) % options.size; onChange(options[index]); true } else false }.clickable { index = (index + 1) % options.size; onChange(options[index]) }.padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) { Text(value, color = if (focused) ShellText else ShellSecondary, fontSize = 13.sp); Spacer(Modifier.weight(1f)); Text("${index + 1}/${options.size}", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun ScaleSelector(value: Int, accent: Color, onChange: (Int) -> Unit) {
    Column { Row(verticalAlignment = Alignment.CenterVertically) { Text("UI Scale", color = ShellText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); Text("$value%", color = accent, fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(7.dp)); Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { (75..125 step 5).forEach { v -> SettingNumber(v, v == value, accent) { onChange(v) } } } }
}

@Composable
private fun IntensitySelector(value: Int, accent: Color, onChange: (Int) -> Unit) {
    Column { Spacer(Modifier.height(7.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Text("Milchglas / Glasstärke", color = ShellText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); Text("STUFE $value / 10", color = accent, fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(7.dp)); Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { (1..10).forEach { v -> SettingNumber(v, v == value, accent) { onChange(v) } } } }
}

@Composable
private fun SettingNumber(label: Int, selected: Boolean, accent: Color, onClick: () -> Unit) { var focused by remember { mutableStateOf(false) }; Box(Modifier.width(if (label >= 75) 36.dp else 30.dp).height(32.dp).background(if (selected || focused) accent.copy(.22f) else Color.White.copy(.045f), RoundedCornerShape(8.dp)).border(if (selected || focused) 2.dp else 1.dp, if (selected || focused) accent else Color.Transparent, RoundedCornerShape(8.dp)).onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick), Alignment.Center) { Text(if (label >= 75) "$label" else label.toString(), color = if (selected || focused) ShellText else ShellSecondary, fontSize = 9.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) } }

@Composable
private fun SettingsButton(title: String, subtitle: String, accent: Color, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().background(Color.White.copy(.045f), RoundedCornerShape(13.dp)).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text(title, color = ShellText, fontSize = 13.sp); Spacer(Modifier.weight(1f)); Text(subtitle, color = accent, fontSize = 10.sp) } }

@Composable
private fun PlayerPicker(apps: List<android.content.pm.ResolveInfo>, accent: Color, onDismiss: () -> Unit, onPick: (String) -> Unit) { Box(Modifier.fillMaxSize().background(Color.Black.copy(.72f)).focusable().onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key == Key.Back) { onDismiss(); true } else false }, Alignment.Center) { Column(Modifier.width(520.dp).background(Color(0xF0161922), RoundedCornerShape(22.dp)).border(1.dp, accent.copy(.5f), RoundedCornerShape(22.dp)).padding(20.dp)) { Text("Externen Player auswählen", color = ShellText, fontSize = 20.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp)); apps.take(12).forEach { info -> SettingsButton(info.loadLabel(LocalContext.current.packageManager).toString(), info.activityInfo.packageName, accent) { onPick(info.activityInfo.packageName) } }; if (apps.isEmpty()) Text("Keine passende Video-App gefunden.", color = ShellSecondary, modifier = Modifier.padding(12.dp)); Spacer(Modifier.height(8.dp)); Text("Zurück = schließen", color = ShellSecondary, fontSize = 10.sp) } } }

private fun initials(name: String): String = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercaseChar().toString() }
