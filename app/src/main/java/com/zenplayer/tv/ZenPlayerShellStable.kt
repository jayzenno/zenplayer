package com.zenplayer.tv

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zenplayer.tv.domain.model.Channel
import com.zenplayer.tv.domain.model.EpgProgramme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ZText = Color(0xFFF7F8FC)
private val ZMuted = Color(0xFF8D96AA)
private val ZBg = Color(0xFF05070D)
private val ZOk = setOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)

@Composable
fun ZenPlayerShellStable(settings: SettingsStore) {
    val context = LocalContext.current
    val store = remember { PlaylistStore(context) }
    val scope = rememberCoroutineScope()
    val accent = when (settings.ui.theme) {
        ZenTheme.AURORA -> Color(0xFF70E6FF)
        ZenTheme.OBSIDIAN -> Color(0xFFAAA8FF)
        ZenTheme.FROST -> Color(0xFF9FEAFF)
        ZenTheme.AMBER -> Color(0xFFFFC46E)
    }
    var page by remember { mutableStateOf("home") }
    var sidebarFocused by remember { mutableStateOf(true) }
    var lastSidebar by remember { mutableStateOf("home") }
    var sidebarRestore by remember { mutableIntStateOf(0) }
    var playerIndex by remember { mutableIntStateOf(-1) }
    var sourceOpen by remember { mutableStateOf(false) }
    var sourceReturn by remember { mutableStateOf("home") }
    var exitArmed by remember { mutableStateOf(false) }
    var exitDialog by remember { mutableStateOf(false) }
    val sidebarRequesters = remember { List(4) { FocusRequester() } }
    val contentFocus = remember { FocusRequester() }
    val channels = store.channels

    fun restoreSidebar() { sidebarFocused = true; sidebarRestore++ }

    BackHandler(true) {
        when {
            exitDialog -> exitDialog = false
            playerIndex >= 0 -> playerIndex = -1
            sourceOpen -> { sourceOpen = false; page = sourceReturn; restoreSidebar() }
            !sidebarFocused -> restoreSidebar()
            page != "home" -> { page = "home"; lastSidebar = "home"; exitArmed = false; restoreSidebar() }
            !exitArmed -> exitArmed = true
            else -> exitDialog = true
        }
    }

    LaunchedEffect(sidebarRestore) {
        withFrameNanos { }
        val ids = listOf("home", "epg", "search", "settings")
        val i = ids.indexOf(lastSidebar).coerceAtLeast(0)
        runCatching { sidebarRequesters[i].requestFocus() }
    }

    if (exitDialog) AlertDialog(
        onDismissRequest = { exitDialog = false },
        title = { Text("ZenPlayer beenden?") },
        text = { Text("Möchtest du ZenPlayer wirklich schließen?") },
        confirmButton = { TextButton(onClick = { (context as? Activity)?.finish() }) { Text("Beenden") } },
        dismissButton = { TextButton(onClick = { exitDialog = false }) { Text("Abbrechen") } }
    )

    Box(Modifier.fillMaxSize().background(ZBg)) {
        ZenAnimatedBackdrop(settings.ui.theme, settings.ui.animatedBackdrop, !settings.ui.reducedMotion)
        when {
            playerIndex in channels.indices -> {
                ZenPlayerScreen(channels[playerIndex], settings, { playerIndex = -1 }, { key ->
                    when (key) {
                        Key.DirectionUp -> { if (channels.isNotEmpty()) playerIndex = (playerIndex - 1 + channels.size) % channels.size; true }
                        Key.DirectionDown -> { if (channels.isNotEmpty()) playerIndex = (playerIndex + 1) % channels.size; true }
                        else -> false
                    }
                })
            }
            sourceOpen -> SourceManagerStable(store, accent, onDone = { sourceOpen = false; page = sourceReturn; restoreSidebar() })
            else -> Row(Modifier.fillMaxSize().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                StableSidebar(page, lastSidebar, accent, sidebarRequesters, sidebarRestore,
                    onFocus = { sidebarFocused = it },
                    onRight = { sidebarFocused = false; contentFocus.requestFocus() },
                    onSelect = { selected -> lastSidebar = selected; page = selected; exitArmed = false; sidebarFocused = true }
                )
                Box(
                    Modifier.weight(1f).fillMaxHeight().focusRequester(contentFocus)
                        .focusProperties { left = FocusRequester.Cancel }
                        .focusGroup()
                ) {
                    when (page) {
                        "home" -> StableHome(store, accent, onSource = { sourceReturn = page; sourceOpen = true }, onPlay = { playerIndex = channels.indexOf(it).coerceAtLeast(0) })
                        "epg" -> StableEpg(store, accent) { playerIndex = channels.indexOf(it).coerceAtLeast(0) }
                        "search" -> StableSearch(store, accent) { playerIndex = channels.indexOf(it).coerceAtLeast(0) }
                        "settings" -> StableSettings(settings, accent) { sourceReturn = page; sourceOpen = true }
                    }
                }
            }
        }
    }
}

@Composable
private fun StableSidebar(page: String, selected: String, accent: Color, requesters: List<FocusRequester>, restore: Int, onFocus: (Boolean) -> Unit, onRight: () -> Unit, onSelect: (String) -> Unit) {
    val ids = listOf("home", "epg", "search", "settings")
    val labels = listOf("Home", "EPG", "Suche", "Settings")
    val icons = listOf(Icons.Default.Home, Icons.Default.PlayArrow, Icons.Default.Search, Icons.Default.Settings)
    LaunchedEffect(restore) { withFrameNanos { }; runCatching { requesters[ids.indexOf(selected).coerceAtLeast(0)].requestFocus() } }
    Column(Modifier.width(86.dp).wrapContentHeight().background(Color.White.copy(.045f), RoundedCornerShape(24.dp)).border(1.dp, Color.White.copy(.10f), RoundedCornerShape(24.dp)).padding(10.dp).focusGroup(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(58.dp).background(accent.copy(.12f), RoundedCornerShape(18.dp)), Alignment.Center) { Text("Z", color = ZText, fontSize = 24.sp, fontWeight = FontWeight.Black) }
        ids.forEachIndexed { i, id ->
            var focused by remember { mutableStateOf(false) }
            val isSelected = page == id
            Box(Modifier.size(58.dp).focusRequester(requesters[i]).onFocusChanged { focused = it.isFocused; onFocus(it.isFocused) }.focusable()
                .onKeyEvent { e ->
                    if (e.type == KeyEventType.KeyUp && e.key == Key.DirectionRight) { onRight(); true }
                    else if (e.type == KeyEventType.KeyUp && e.key in ZOk) { onSelect(id); true } else false
                }
                .background(if (focused) accent.copy(.20f) else if (isSelected) Color.White.copy(.055f) else Color.Transparent, RoundedCornerShape(18.dp))
                .border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(if (isSelected) .12f else .0f), RoundedCornerShape(18.dp)), Alignment.Center) {
                Icon(icons[i], labels[i], tint = if (focused) ZText else if (isSelected) accent else ZMuted)
                if (isSelected && !focused) Box(Modifier.align(Alignment.BottomCenter).width(20.dp).height(3.dp).background(accent, RoundedCornerShape(2.dp)))
            }
        }
    }
}

@Composable
private fun StableHome(store: PlaylistStore, accent: Color, onSource: () -> Unit, onPlay: (Channel) -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Für dich", color = ZText, fontSize = 34.sp, fontWeight = FontWeight.Black)
        Text(if (store.channels.isEmpty()) "Noch keine Playlist geladen" else "${store.channels.size} Sender verfügbar", color = ZMuted, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StableAction("Playlist hinzufügen", "M3U Datei · URL · Xtream", Icons.Default.Source, accent, onSource)
        }
        if (store.channels.isNotEmpty()) LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(store.channels) { channel -> ChannelCard(channel, accent) { onPlay(channel) } }
        }
    }
}

@Composable
private fun StableEpg(store: PlaylistStore, accent: Color, onPlay: (Channel) -> Unit) {
    val programmes = store.programmes
    Column(Modifier.fillMaxSize()) {
        Text("EPG", color = ZText, fontSize = 34.sp, fontWeight = FontWeight.Black)
        Text("Horizontaler TV-Guide · Fokus bleibt im Programmraster", color = ZMuted, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        if (store.channels.isEmpty()) Text("Lade zuerst eine Playlist mit EPG-Daten.", color = ZMuted, fontSize = 14.sp)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(store.channels) { channel ->
                Row(Modifier.fillMaxWidth().height(82.dp).focusGroup(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(150.dp).fillMaxHeight().background(Color.White.copy(.045f), RoundedCornerShape(16.dp)).padding(10.dp), Alignment.CenterStart) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!channel.logoUrl.isNullOrBlank()) AsyncImage(channel.logoUrl, channel.name, Modifier.size(38.dp), contentScale = ContentScale.Fit)
                            Column(Modifier.padding(start = 8.dp)) { Text(channel.name, color = ZText, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1); Text(channel.group ?: "Live", color = ZMuted, fontSize = 9.sp) }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
                        items(programmes.filter { it.channelId.equals(channel.id, true) }.sortedBy { it.start }.take(10)) { p -> EpgCard(p, accent) { onPlay(channel) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpgCard(p: EpgProgramme, accent: Color, onPlay: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.width(235.dp).fillMaxHeight().focusable().onFocusChanged { focused = it.isFocused }.onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key in ZOk) { onPlay(); true } else false }
        .background(if (focused) accent.copy(.15f) else Color.White.copy(.05f), RoundedCornerShape(15.dp))
        .border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(.07f), RoundedCornerShape(15.dp)).padding(10.dp)) {
        Column { Text(p.title, color = ZText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2); Spacer(Modifier.height(4.dp)); Text("${SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(p.start))} – ${SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(p.end))}", color = if (focused) accent else ZMuted, fontSize = 9.sp); if (p.isCatchupAvailable) Text("REPLAY", color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun StableSearch(store: PlaylistStore, accent: Color, onPlay: (Channel) -> Unit) {
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        Text("Suche", color = ZText, fontSize = 34.sp, fontWeight = FontWeight.Black)
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Sender oder Gruppe") }, modifier = Modifier.fillMaxWidth().focusGroup())
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(store.channels.filter { query.isBlank() || it.name.contains(query, true) || it.group.orEmpty().contains(query, true) }) { ChannelCard(it, accent) { onPlay(it) } } }
    }
}

@Composable
private fun StableSettings(settings: SettingsStore, accent: Color, onSources: () -> Unit) {
    var tab by remember { mutableStateOf("Allgemein") }
    val tabs = listOf("Allgemein", "Player", "Darstellung", "Quellen")
    Column(Modifier.fillMaxSize()) {
        Text("Settings", color = ZText, fontSize = 34.sp, fontWeight = FontWeight.Black)
        Row(Modifier.fillMaxWidth().focusGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { tabs.forEach { TabButton(it, it == tab, accent) { tab = it } } }
        Spacer(Modifier.height(14.dp))
        when (tab) {
            "Allgemein" -> SettingInfo("Navigation", "Back aus dem Content geht immer zur zuletzt ausgewählten Sidebar-Position. Links aus dem Content bleibt im Content.", accent)
            "Player" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingCycle("Engine", settings.player.engine.name, accent) { settings.updatePlayer(settings.player.copy(engine = when (settings.player.engine) { PlaybackEngine.EXO -> PlaybackEngine.VLC; PlaybackEngine.VLC -> PlaybackEngine.EXTERNAL; PlaybackEngine.EXTERNAL -> PlaybackEngine.EXO })) }
                SettingCycle("Buffer", settings.player.bufferMode.name, accent) { settings.updatePlayer(settings.player.copy(bufferMode = when (settings.player.bufferMode) { BufferMode.LOW -> BufferMode.BALANCED; BufferMode.BALANCED -> BufferMode.HIGH; BufferMode.HIGH -> BufferMode.AUTO; BufferMode.AUTO -> BufferMode.LOW })) }
            }
            "Darstellung" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingCycle("Theme", settings.ui.theme.name, accent) { settings.updateUi(settings.ui.copy(theme = when (settings.ui.theme) { ZenTheme.AURORA -> ZenTheme.OBSIDIAN; ZenTheme.OBSIDIAN -> ZenTheme.FROST; ZenTheme.FROST -> ZenTheme.AMBER; ZenTheme.AMBER -> ZenTheme.AURORA })) }
                SettingInfo("Fokus", "Fokus = heller Glow · ausgewählt = dezente Linie. Die beiden Zustände sind bewusst verschieden.", accent)
            }
            "Quellen" -> StableAction("Quellen öffnen", "M3U / URL / Xtream", Icons.Default.Source, accent, onSources)
        }
    }
}

@Composable private fun SettingInfo(title: String, text: String, accent: Color) { Column(Modifier.fillMaxWidth().background(Color.White.copy(.05f), RoundedCornerShape(16.dp)).border(1.dp, Color.White.copy(.08f), RoundedCornerShape(16.dp)).padding(16.dp)) { Text(title, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(5.dp)); Text(text, color = ZText, fontSize = 12.sp) } }

@Composable private fun SettingCycle(title: String, value: String, accent: Color, onClick: () -> Unit) { StableAction(title, "Aktuell: $value · OK wechseln", Icons.Default.Settings, accent, onClick) }

@Composable private fun TabButton(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) { var focused by remember { mutableStateOf(false) }; Row(Modifier.width(150.dp).height(50.dp).focusable().onFocusChanged { focused = it.isFocused }.onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key in ZOk) { onClick(); true } else false }.background(if (focused) accent.copy(.18f) else if (selected) accent.copy(.08f) else Color.White.copy(.04f), RoundedCornerShape(14.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent else if (selected) accent.copy(.55f) else Color.White.copy(.07f), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, color = if (focused || selected) ZText else ZMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) } }

@Composable private fun StableAction(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, action: () -> Unit) { var focused by remember { mutableStateOf(false) }; Row(Modifier.width(360.dp).height(82.dp).focusable().onFocusChanged { focused = it.isFocused }.onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key in ZOk) { action(); true } else false }.background(if (focused) accent.copy(.17f) else Color.White.copy(.05f), RoundedCornerShape(18.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(.07f), RoundedCornerShape(18.dp)).padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = if (focused) ZText else accent); Column(Modifier.padding(start = 12.dp)) { Text(title, color = ZText, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = ZMuted, fontSize = 10.sp) } } }

@Composable private fun ChannelCard(channel: Channel, accent: Color, action: () -> Unit) { var focused by remember { mutableStateOf(false) }; Row(Modifier.fillMaxWidth().height(68.dp).focusable().onFocusChanged { focused = it.isFocused }.onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key in ZOk) { action(); true } else false }.background(if (focused) accent.copy(.13f) else Color.White.copy(.045f), RoundedCornerShape(16.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(.07f), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) { if (!channel.logoUrl.isNullOrBlank()) AsyncImage(channel.logoUrl, channel.name, Modifier.size(44.dp), contentScale = ContentScale.Fit) else Box(Modifier.size(44.dp).background(accent.copy(.10f), RoundedCornerShape(12.dp)), Alignment.Center) { Text(channel.name.take(2).uppercase(), color = accent, fontWeight = FontWeight.Bold) }; Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(channel.name, color = ZText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Text(channel.group ?: "Live TV", color = ZMuted, fontSize = 9.sp) } }

@Composable
private fun SourceManagerStable(store: PlaylistStore, accent: Color, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf("M3U Datei") }
    var url by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { busy = true; status = "M3U wird eingelesen …"; runCatching { store.importPlaylistFromUri(uri) }.onSuccess { status = "✓ ${store.channels.size} Sender geladen" }.onFailure { status = "✕ ${it.message}" }; busy = false }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Quellen", color = ZText, fontSize = 32.sp, fontWeight = FontWeight.Black); Text("${store.channels.size} Sender", color = ZMuted, fontSize = 12.sp) }
        Row(Modifier.fillMaxWidth().focusGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("M3U Datei", "M3U URL", "Xtream").forEach { TabButton(it, it == mode, accent) { mode = it } } }
        Spacer(Modifier.height(14.dp))
        when (mode) {
            "M3U Datei" -> StableAction("Datei auswählen", "Lokale .m3u / .m3u8 Playlist", Icons.Default.Source, accent) { picker.launch(arrayOf("*/*")) }
            "M3U URL" -> Column(verticalArrangement = Arrangement.spacedBy(9.dp)) { OutlinedTextField(url, { url = it }, label = { Text("M3U URL") }, modifier = Modifier.fillMaxWidth()); StableAction(if (busy) "Lädt …" else "Playlist laden", "URL abrufen und validieren", Icons.Default.Source, accent) { if (!busy) scope.launch { busy = true; status = "M3U wird geladen …"; runCatching { store.importPlaylistFromUrl(url) }.onSuccess { status = "✓ ${store.channels.size} Sender geladen" }.onFailure { status = "✕ ${it.message}" }; busy = false } } }
            "Xtream" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(server, { server = it }, label = { Text("Server / Host:Port") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(user, { user = it }, label = { Text("Benutzername") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(pass, { pass = it }, label = { Text("Passwort") }, modifier = Modifier.fillMaxWidth()); StableAction(if (busy) "Verbinde …" else "Xtream verbinden", "Anmelden → Kategorien → Live-Sender", Icons.Default.Source, accent) { if (!busy) scope.launch { busy = true; status = "Xtream wird verbunden …"; XtreamClient(server, user, pass).load().onSuccess { store.importXtream(it); status = "✓ ${it.size} Xtream-Sender geladen" }.onFailure { status = "✕ ${it.message}" }; busy = false } } }
        }
        if (status.isNotBlank()) { Spacer(Modifier.height(12.dp)); Text(status, color = if (status.startsWith("✓")) accent else Color(0xFFFF8D8D), fontSize = 12.sp) }
        Spacer(Modifier.height(18.dp)); TextButton(onClick = { store.clear(); status = "Quelle entfernt" }) { Text("Quelle entfernen") }; TextButton(onClick = onDone) { Text("Zurück") }
    }
}
