package com.zenplayer.tv

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TextPrimary = Color(0xFFF5F6FA)
private val TextSecondary = Color(0xFF9EA3B3)

@Composable
fun ZenPlayerShell(settings: SettingsStore) {
    val ui = settings.ui
    val scale = ui.uiScale.coerceIn(75, 125) / 100f
    val baseDensity = LocalDensity.current
    val context = LocalContext.current
    val store = remember { PlaylistStore(context) }
    var page by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf<com.zenplayer.tv.domain.model.Channel?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val accent = when (ui.theme) {
        ZenTheme.AURORA -> Color(0xFF70E6FF)
        ZenTheme.OBSIDIAN -> Color(0xFFB7B9FF)
        ZenTheme.FROST -> Color(0xFFB9E9FF)
        ZenTheme.AMBER -> Color(0xFFFFC66D)
    }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides Density(baseDensity.density * scale, baseDensity.fontScale)
    ) {
        Box(Modifier.fillMaxSize().background(Color(0xFF070A10))) {
            ZenAnimatedBackdrop(ui.theme, ui.animatedBackdrop, !ui.reducedMotion)
            Row(Modifier.fillMaxSize().padding(14.dp)) {
                Sidebar(page, accent) { page = it }
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    when (page) {
                        0 -> HomeScreen(store, accent, refresh) { playing = it }
                        1 -> EpgScreen(store, accent, refresh) { playing = it }
                        2 -> PlaylistScreen(store, accent) { refresh++ }
                        3 -> SearchScreen(store, accent) { playing = it }
                        else -> SettingsScreen(settings, accent)
                    }
                }
            }
            playing?.let { channel -> ZenPlayerScreen(channel, settings) { playing = null } }
        }
    }
}

@Composable
private fun Sidebar(page: Int, accent: Color, onPage: (Int) -> Unit) {
    val entries = listOf(
        Icons.Default.Home to "Home",
        Icons.Default.PlayArrow to "EPG",
        Icons.Default.List to "Playlist",
        Icons.Default.Search to "Suche",
        Icons.Default.Settings to "Settings"
    )
    Column(
        Modifier.width(64.dp).fillMaxHeight()
            .background(Color.White.copy(.045f), RoundedCornerShape(18.dp))
            .border(1.dp, Color.White.copy(.07f), RoundedCornerShape(18.dp)).padding(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        entries.forEachIndexed { index, entry ->
            var focused by remember { mutableStateOf(false) }
            Box(
                Modifier.width(46.dp).height(46.dp)
                    .background(if (page == index || focused) accent.copy(.16f) else Color.Transparent, RoundedCornerShape(14.dp))
                    .border(if (focused) 2.dp else 1.dp, if (focused || page == index) accent else Color.Transparent, RoundedCornerShape(14.dp))
                    .onFocusChanged { focused = it.isFocused }.focusable().clickable { onPage(index) },
                Alignment.Center
            ) { Icon(entry.first, entry.second, tint = if (page == index || focused) TextPrimary else TextSecondary) }
        }
    }
}

@Composable
private fun HomeScreen(store: PlaylistStore, accent: Color, refresh: Int, onPlay: (com.zenplayer.tv.domain.model.Channel) -> Unit) {
    val channels = store.channels
    Column(Modifier.fillMaxSize()) {
        Text("Willkommen bei ZenPlayer", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(if (channels.isEmpty()) "Importiere zuerst deine M3U-Playlist." else "${channels.size} Sender bereit", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))
        if (channels.isEmpty()) Text("Playlist → M3U / M3U8 importieren", color = accent, fontSize = 15.sp)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            items(channels) { ChannelRow(it, accent) { onPlay(it) } }
        }
    }
}

@Composable
private fun ChannelRow(channel: com.zenplayer.tv.domain.model.Channel, accent: Color, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(56.dp)
            .background(if (focused) accent.copy(.13f) else Color.White.copy(.045f), RoundedCornerShape(15.dp))
            .border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(.06f), RoundedCornerShape(15.dp))
            .onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!channel.logoUrl.isNullOrBlank()) AsyncImage(channel.logoUrl, channel.name, Modifier.width(42.dp).height(42.dp), contentScale = ContentScale.Fit)
        else Text(initials(channel.name), color = if (focused) accent else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))
        Column(Modifier.weight(1f)) {
            Text(channel.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(channel.group ?: "Live TV", color = TextSecondary, fontSize = 10.sp)
        }
        Icon(Icons.Default.PlayArrow, null, tint = if (focused) TextPrimary else TextSecondary)
    }
}

@Composable
private fun PlaylistScreen(store: PlaylistStore, accent: Color, onImported: () -> Unit) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    val playlistPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("empty")
        }.onSuccess { text -> store.importPlaylist(text); message = "${store.channels.size} Sender importiert"; onImported() }
            .onFailure { message = "M3U konnte nicht gelesen werden" }
    }
    val epgPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("empty")
        }.onSuccess { text -> store.importEpg(text); message = "${store.programmes.size} EPG-Einträge importiert"; onImported() }
            .onFailure { message = "XMLTV konnte nicht gelesen werden" }
    }
    Column(Modifier.fillMaxSize()) {
        Text("Playlist", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("M3U und XMLTV werden lokal gespeichert", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        ImportButton("M3U / M3U8 Playlist", "Sender · Gruppen · Logos · Catch-up", accent) { playlistPicker.launch(arrayOf("*/*")) }
        Spacer(Modifier.height(9.dp))
        ImportButton("XMLTV / EPG", "Programme · Zeiten · Nutzer-Logos", accent) { epgPicker.launch(arrayOf("*/*")) }
        Spacer(Modifier.height(14.dp))
        Text("${store.channels.size} Sender · ${store.programmes.size} Programme", color = TextSecondary, fontSize = 12.sp)
        if (message.isNotBlank()) Text(message, color = accent, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun ImportButton(title: String, subtitle: String, accent: Color, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(64.dp).background(if (focused) accent.copy(.14f) else Color.White.copy(.045f), RoundedCornerShape(15.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(.07f), RoundedCornerShape(15.dp)).onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, color = TextSecondary, fontSize = 10.sp) }
        Icon(Icons.Default.List, null, tint = if (focused) accent else TextSecondary)
    }
}

@Composable
private fun EpgScreen(store: PlaylistStore, accent: Color, refresh: Int, onPlay: (com.zenplayer.tv.domain.model.Channel) -> Unit) {
    val groups = store.programmes.groupBy { it.channelId }
    Column(Modifier.fillMaxSize()) {
        Text("EPG", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Echtes XMLTV · kompakte TV-Ansicht", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        if (groups.isEmpty()) Text("Noch kein XMLTV importiert.", color = accent, fontSize = 14.sp)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            groups.forEach { (id, programmes) ->
                val channel = store.channels.firstOrNull { it.id == id || it.tvgId == id || it.name == id }
                item {
                    Row(Modifier.fillMaxWidth().height(58.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (!channel?.logoUrl.isNullOrBlank()) AsyncImage(channel?.logoUrl, channel?.name, Modifier.width(52.dp).height(52.dp), contentScale = ContentScale.Fit)
                        else Text(initials(channel?.name ?: id), color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(52.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(channel?.name ?: id, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                items(programmes.sortedBy { it.start }.take(12)) { p ->
                    Row(Modifier.fillMaxWidth().height(50.dp).background(Color.White.copy(.045f), RoundedCornerShape(13.dp)).clickable { channel?.let(onPlay) }.padding(horizontal = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(p.start)), color = accent, fontSize = 11.sp, modifier = Modifier.width(52.dp))
                        Column(Modifier.weight(1f)) { Text(p.title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Text(p.category ?: "TV", color = TextSecondary, fontSize = 9.sp) }
                        if (p.isCatchupAvailable) Text("REPLAY", color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
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
    Column(Modifier.fillMaxSize()) {
        Text("Suche", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Sender durchsuchen", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Text(if (query.isBlank()) "TV-Tastatur verwenden · Suche wird als Nächstes erweitert" else query, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().background(Color.White.copy(.05f), RoundedCornerShape(14.dp)).padding(14.dp))
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) { items(filtered) { ChannelRow(it, accent) { onPlay(it) } } }
    }
}

@Composable
private fun SettingsScreen(settings: SettingsStore, accent: Color) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Player", "Live TV", "EPG", "Darstellung", "System")
    Column(Modifier.fillMaxSize()) {
        Text("Einstellungen", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Kompakte Reiter statt einer endlosen Seite", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            tabs.forEachIndexed { index, title ->
                FocusTab(title, index == tab, accent) { tab = index }
            }
        }
        Spacer(Modifier.height(10.dp))
        when (tab) {
            0 -> PlayerTab(settings, accent)
            1 -> LiveTab(settings, accent)
            2 -> EpgSettingsTab(settings, accent)
            3 -> AppearanceTab(settings, accent)
            else -> SystemTab(settings, accent)
        }
    }
}

@Composable
private fun FocusTab(title: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.background(if (selected || focused) accent.copy(.16f) else Color.White.copy(.045f), RoundedCornerShape(11.dp)).border(if (selected || focused) 2.dp else 1.dp, if (selected || focused) accent else Color.White.copy(.06f), RoundedCornerShape(11.dp)).onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(title, color = TextPrimary, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun PlayerTab(settings: SettingsStore, accent: Color) {
    val context = LocalContext.current
    var picker by remember { mutableStateOf(false) }
    val apps = remember { context.packageManager.queryIntentActivities(Intent(Intent.ACTION_VIEW).apply { type = "video/*" }, 0) }
    SettingsPanel("Player Engine", "Interner Player oder externe App") {
        ChoiceRow("Engine", settings.player.engine.label, PlaybackEngine.entries.map { it.label }, accent) { value -> settings.updatePlayer(settings.player.copy(engine = PlaybackEngine.entries.first { it.label == value })) }
        Spacer(Modifier.height(7.dp))
        ActionRow("Externen Player wählen", settings.player.externalPlayerPackage ?: "System-Auswahl", accent) { picker = true }
        Spacer(Modifier.height(7.dp))
        ChoiceRow("Buffer", settings.player.bufferMode.label, BufferMode.entries.map { it.label }, accent) { value -> settings.updatePlayer(settings.player.copy(bufferMode = BufferMode.entries.first { it.label == value })) }
    }
    if (picker) ExternalPlayerDialog(apps, accent, { picker = false }) { packageName -> settings.updatePlayer(settings.player.copy(engine = PlaybackEngine.EXTERNAL, externalPlayerPackage = packageName)); picker = false }
}

@Composable
private fun LiveTab(settings: SettingsStore, accent: Color) {
    SettingsPanel("Live TV", "Wiedergabe und Catch-up") {
        ToggleRow("Live sofort starten", settings.player.startLiveImmediately, accent) { settings.updatePlayer(settings.player.copy(startLiveImmediately = it)) }
        ToggleRow("Shared Catch-up bevorzugen", settings.player.preferCatchupSibling, accent) { settings.updatePlayer(settings.player.copy(preferCatchupSibling = it)) }
        ToggleRow("Timeshift wenn verfügbar", settings.player.enableTimeshiftWhenAvailable, accent) { settings.updatePlayer(settings.player.copy(enableTimeshiftWhenAvailable = it)) }
        ToggleRow("Beim Senderwechsel an Live-Edge", settings.player.liveEdgeOnChannelSwitch, accent) { settings.updatePlayer(settings.player.copy(liveEdgeOnChannelSwitch = it)) }
    }
}

@Composable
private fun EpgSettingsTab(settings: SettingsStore, accent: Color) {
    SettingsPanel("EPG", "Zeitraum und Programminformationen") {
        ChoiceRow("Zeitraum", settings.epg.pageSize.label, EpgPageSize.entries.map { it.label }, accent) { value -> settings.updateEpg(settings.epg.copy(pageSize = EpgPageSize.entries.first { it.label == value })) }
        ToggleRow("Automatisch auf Jetzt zentrieren", settings.epg.autoCenterOnNow, accent) { settings.updateEpg(settings.epg.copy(autoCenterOnNow = it)) }
        ToggleRow("Programmbeschreibung", settings.epg.showDescriptions, accent) { settings.updateEpg(settings.epg.copy(showDescriptions = it)) }
        ToggleRow("Replay-Badge", settings.epg.showCatchupBadge, accent) { settings.updateEpg(settings.epg.copy(showCatchupBadge = it)) }
    }
}

@Composable
private fun AppearanceTab(settings: SettingsStore, accent: Color) {
    SettingsPanel("Darstellung", "Alles auf die persönliche TV-Größe anpassen") {
        ScaleSelector(settings.ui.uiScale, accent) { settings.updateUi(settings.ui.copy(uiScale = it)) }
        Spacer(Modifier.height(10.dp))
        ChoiceRow("Theme", settings.ui.theme.label, ZenTheme.entries.map { it.label }, accent) { value -> settings.updateUi(settings.ui.copy(theme = ZenTheme.entries.first { it.label == value })) }
        ChoiceRow("Hintergrund", settings.ui.animatedBackdrop.label, AnimatedBackdrop.entries.map { it.label }, accent) { value -> settings.updateUi(settings.ui.copy(animatedBackdrop = AnimatedBackdrop.entries.first { it.label == value })) }
        IntensitySelector(settings.ui.glassIntensity, accent) { settings.updateUi(settings.ui.copy(glassIntensity = it)) }
        ToggleRow("Senderlogos anzeigen", settings.ui.showLogos, accent) { settings.updateUi(settings.ui.copy(showLogos = it)) }
        ToggleRow("Animationen", settings.ui.animations, accent) { settings.updateUi(settings.ui.copy(animations = it)) }
    }
}

@Composable
private fun SystemTab(settings: SettingsStore, accent: Color) {
    SettingsPanel("System", "ZenPlayer Diagnose und Bedienung") {
        Text("ZenPlayer · Android TV", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        ToggleRow("Zahlen-Tasten optional", settings.remote.numericKeys, accent) { settings.updateRemote(settings.remote.copy(numericKeys = it)) }
        ToggleRow("Sender-Historie", settings.remote.channelHistory, accent) { settings.updateRemote(settings.remote.copy(channelHistory = it)) }
    }
}

@Composable
private fun SettingsPanel(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White.copy(.055f), RoundedCornerShape(18.dp)).border(1.dp, Color.White.copy(.07f), RoundedCornerShape(18.dp)).padding(14.dp)) {
        Text(title, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = TextSecondary, fontSize = 10.sp)
        Spacer(Modifier.height(9.dp))
        content()
    }
}

@Composable
private fun ChoiceRow(label: String, value: String, options: List<String>, accent: Color, onChange: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    var index by remember(value) { mutableIntStateOf(options.indexOf(value).coerceAtLeast(0)) }
    Row(Modifier.fillMaxWidth().height(46.dp).background(if (focused) accent.copy(.12f) else Color.White.copy(.035f), RoundedCornerShape(12.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.Transparent, RoundedCornerShape(12.dp)).onFocusChanged { focused = it.isFocused }.focusable().onKeyEvent { event ->
        if (event.type != KeyEventType.KeyUp) return@onKeyEvent false
        when (event.key) {
            Key.DirectionLeft -> { index = (index - 1 + options.size) % options.size; onChange(options[index]); true }
            Key.DirectionRight, Key.DirectionCenter -> { index = (index + 1) % options.size; onChange(options[index]); true }
            else -> false
        }
    }.clickable { index = (index + 1) % options.size; onChange(options[index]) }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = if (focused) TextPrimary else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(8.dp))
        Text("${index + 1}/${options.size}", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, accent: Color, onChange: (Boolean) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(42.dp).background(if (focused) accent.copy(.10f) else Color.Transparent, RoundedCornerShape(11.dp)).onFocusChanged { focused = it.isFocused }.focusable().clickable { onChange(!checked) }.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextPrimary, fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        Text(if (checked) "AN" else "AUS", color = if (checked) accent else TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActionRow(title: String, value: String, accent: Color, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(46.dp).background(if (focused) accent.copy(.12f) else Color.White.copy(.035f), RoundedCornerShape(12.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.Transparent, RoundedCornerShape(12.dp)).onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = TextSecondary, fontSize = 11.sp); Spacer(Modifier.weight(1f)); Text(value, color = TextPrimary, fontSize = 11.sp)
    }
}

@Composable
private fun ScaleSelector(value: Int, accent: Color, onChange: (Int) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) { Text("UI Scale", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); Text("$value%", color = accent, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { (75..125 step 5).forEach { number -> NumberBox(number, number == value, accent) { onChange(number) } } }
    }
}

@Composable
private fun IntensitySelector(value: Int, accent: Color, onChange: (Int) -> Unit) {
    Column {
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Milchglas / Glasstärke", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); Text("STUFE $value / 10", color = accent, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { (1..10).forEach { number -> NumberBox(number, number == value, accent) { onChange(number) } } }
    }
}

@Composable
private fun NumberBox(number: Int, selected: Boolean, accent: Color, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.width(if (number >= 75) 34.dp else 30.dp).height(32.dp).background(if (selected || focused) accent.copy(.18f) else Color.White.copy(.035f), RoundedCornerShape(9.dp)).border(if (selected || focused) 2.dp else 1.dp, if (selected || focused) accent else Color.White.copy(.05f), RoundedCornerShape(9.dp)).onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick), Alignment.Center) {
        Text(number.toString(), color = TextPrimary, fontSize = 9.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun ExternalPlayerDialog(apps: List<android.content.pm.ResolveInfo>, accent: Color, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    BackHandler(onBack = onDismiss)
    Box(Modifier.fillMaxSize().background(Color.Black.copy(.72f)), Alignment.Center) {
        Column(Modifier.width(520.dp).background(Color(0xFF111722), RoundedCornerShape(22.dp)).border(1.dp, accent.copy(.35f), RoundedCornerShape(22.dp)).padding(18.dp)) {
            Text("Externen Player wählen", color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            if (apps.isEmpty()) Text("Keine passende Video-App gefunden.", color = TextSecondary, fontSize = 13.sp)
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.height(280.dp)) {
                items(apps) { info ->
                    ActionRow(info.loadLabel(info.activityInfo.packageManager).toString(), info.activityInfo.packageName, accent) { onSelect(info.activityInfo.packageName) }
                }
            }
            Spacer(Modifier.height(8.dp))
            ActionRow("Abbrechen", "", accent, onDismiss)
        }
    }
}

private fun initials(name: String): String = name.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "TV" }
