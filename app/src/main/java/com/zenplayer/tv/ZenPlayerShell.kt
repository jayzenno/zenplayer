package com.zenplayer.tv

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zenplayer.tv.domain.model.Channel
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

private val Primary = Color(0xFFF6F7FB)
private val Secondary = Color(0xFF969EAF)
private val Surface = Color.White.copy(alpha = .055f)
private val OkKeys = setOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)

@Composable
fun ZenPlayerShell(settings: SettingsStore) {
    val context = LocalContext.current
    val store = remember { PlaylistStore(context) }
    val ui = settings.ui
    val accent = when (ui.theme) {
        ZenTheme.AURORA -> Color(0xFF70E6FF)
        ZenTheme.OBSIDIAN -> Color(0xFFB7B9FF)
        ZenTheme.FROST -> Color(0xFFB9E9FF)
        ZenTheme.AMBER -> Color(0xFFFFC66D)
    }
    var page by remember { mutableIntStateOf(0) }
    var sourceOpen by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf<Channel?>(null) }
    var demo by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var homeBackArmed by remember { mutableStateOf(false) }
    val homeFocus = remember { FocusRequester() }
    val channels = if (demo) DemoData.channels() else store.channels

    BackHandler {
        when {
            showExitDialog -> showExitDialog = false
            playing != null -> playing = null
            sourceOpen -> sourceOpen = false
            page != 0 -> { page = 0; homeBackArmed = false; runCatching { homeFocus.requestFocus() } }
            !homeBackArmed -> { homeBackArmed = true; runCatching { homeFocus.requestFocus() } }
            else -> showExitDialog = true
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("ZenPlayer beenden?") },
            text = { Text("Möchtest du ZenPlayer wirklich schließen?") },
            confirmButton = { TextButton(onClick = { (context as? Activity)?.finish() }) { Text("Beenden") } },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("Abbrechen") } },
        )
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF05070C))) {
        ZenAnimatedBackdrop(ui.theme, ui.animatedBackdrop, ui.animations && !ui.reducedMotion)
        when {
            sourceOpen -> SourcesScreen(store, accent, onBack = { sourceOpen = false }, onDone = { sourceOpen = false })
            playing != null -> ZenPlayerScreen(playing!!, settings, onBack = { playing = null })
            else -> Row(Modifier.fillMaxSize().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ZenSidebar(page, accent, homeFocus) { selected -> if (selected != page) homeBackArmed = false; page = selected }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    when (page) {
                        0 -> HomeScreen(store, accent, demo, { sourceOpen = true }, { demo = true; homeBackArmed = false }, { demo = false; homeBackArmed = false }) { playing = it }
                        1 -> EpgScreen(store, accent, demo) { playing = it }
                        2 -> SearchScreen(store, accent, demo) { playing = it }
                        else -> SettingsScreen(settings, accent) { sourceOpen = true }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZenSidebar(page: Int, accent: Color, homeFocus: FocusRequester, onPage: (Int) -> Unit) {
    val ids = listOf("home", "epg", "search", "settings")
    val labels = listOf("Home", "EPG", "Suche", "Settings")
    val icons = listOf(Icons.Default.Home, Icons.Default.PlayArrow, Icons.Default.Search, Icons.Default.Settings)
    val requesters = remember { List(ids.size) { FocusRequester() } }
    LaunchedEffect(Unit) { runCatching { homeFocus.requestFocus() } }
    LaunchedEffect(page) { runCatching { requesters[page].requestFocus() } }
    Column(
        Modifier.width(68.dp).wrapContentHeight().background(Color.White.copy(.04f), RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(.09f), RoundedCornerShape(20.dp)).padding(7.dp).focusGroup(),
        verticalArrangement = Arrangement.spacedBy(7.dp), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(48.dp).background(accent.copy(.15f), RoundedCornerShape(15.dp)).border(1.dp, accent.copy(.35f), RoundedCornerShape(15.dp)), Alignment.Center) {
            Text("Z", color = Primary, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
        ids.forEachIndexed { i, id ->
            var focused by remember { mutableStateOf(false) }
            val active = focused || page == i
            Box(
                Modifier.size(48.dp).focusRequester(if (i == 0) homeFocus else requesters[i]).focusable().onFocusChanged { focused = it.isFocused }
                    .onKeyEvent { event -> if (event.type == KeyEventType.KeyUp && event.key in OkKeys) { onPage(i); true } else false }
                    .background(if (active) accent.copy(.18f) else Color.Transparent, RoundedCornerShape(14.dp))
                    .border(if (focused) 2.dp else 1.dp, if (active) accent else Color.Transparent, RoundedCornerShape(14.dp)), Alignment.Center
            ) { Icon(icons[i], labels[i], tint = if (active) Primary else Secondary) }
        }
    }
}

@Composable
private fun HomeScreen(store: PlaylistStore, accent: Color, demo: Boolean, openSources: () -> Unit, startDemo: () -> Unit, stopDemo: () -> Unit, onPlay: (Channel) -> Unit) {
    val channels = if (demo) DemoData.channels() else store.channels
    Column(Modifier.fillMaxSize()) {
        Text(if (demo) "Demo-Modus" else "Für dich", color = Primary, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text(if (demo) "Dummy-Sender, EPG und Player" else "Dein persönlicher Live-TV-Startbildschirm", color = Secondary, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
        if (channels.isEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionCard("Playlist hinzufügen", "M3U / URL / Xtream", Icons.Default.Source, accent, openSources)
                ActionCard("Demo starten", "Ohne Quelle testen", Icons.Default.PlayArrow, accent, startDemo)
            }
        } else {
            ActionButton(if (demo) "Demo beenden" else "Quelle verwalten", accent) { if (demo) stopDemo() else openSources() }
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(channels) { ChannelCard(it, accent, onPlay) }
            }
        }
    }
}

@Composable
private fun ActionCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, action: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.width(330.dp).height(92.dp).focusable().onFocusChanged { focused = it.isFocused }.onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key in OkKeys) { action(); true } else false }
        .background(if (focused) accent.copy(.14f) else Surface, RoundedCornerShape(18.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(.08f), RoundedCornerShape(18.dp)).padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = accent)
        Column(Modifier.padding(start = 14.dp)) { Text(title, color = Primary, fontSize = 16.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = Secondary, fontSize = 11.sp) }
    }
}

@Composable
private fun ActionButton(title: String, accent: Color, action: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.focusable().onFocusChanged { focused = it.isFocused }.onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key in OkKeys) { action(); true } else false }
        .background(if (focused) accent.copy(.15f) else Surface, RoundedCornerShape(12.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(.08f), RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 9.dp)) {
        Text(title, color = Primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ChannelCard(channel: Channel, accent: Color, onPlay: (Channel) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(64.dp).focusable().onFocusChanged { focused = it.isFocused }.onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key in OkKeys) { onPlay(channel); true } else false }
        .background(if (focused) accent.copy(.13f) else Surface, RoundedCornerShape(16.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(.07f), RoundedCornerShape(16.dp)).padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        if (!channel.logoUrl.isNullOrBlank()) AsyncImage(channel.logoUrl, channel.name, Modifier.width(46.dp).height(46.dp), contentScale = ContentScale.Fit)
        else Box(Modifier.size(46.dp).background(accent.copy(.12f), RoundedCornerShape(12.dp)), Alignment.Center) { Text(initials(channel.name), color = accent, fontWeight = FontWeight.Bold) }
        Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(channel.name, color = Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text(channel.group ?: "Live TV", color = Secondary, fontSize = 10.sp) }
        Text(if (channel.isCatchupCapable) "CATCHUP" else "LIVE", color = if (focused) accent else Secondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EpgScreen(store: PlaylistStore, accent: Color, demo: Boolean, onPlay: (Channel) -> Unit) {
    val channels = if (demo) DemoData.channels() else store.channels
    val programmes = if (demo) DemoData.programmes() else store.programmes
    Column(Modifier.fillMaxSize()) {
        Text("EPG", color = Primary, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text("Programmübersicht · OK startet den Sender", color = Secondary, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            channels.forEach { channel ->
                val list = programmes.filter { it.channelId == channel.id }.sortedBy { it.start }.take(8)
                if (list.isNotEmpty()) {
                    item { Text(channel.name, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    items(list) { programme ->
                        var focused by remember(programme.id) { mutableStateOf(false) }
                        Row(Modifier.fillMaxWidth().height(50.dp).focusable().onFocusChanged { focused = it.isFocused }.onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key in OkKeys) { onPlay(channel); true } else false }
                            .background(if (focused) accent.copy(.12f) else Surface, RoundedCornerShape(13.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.Transparent, RoundedCornerShape(13.dp)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(java.text.SimpleDateFormat("HH:mm", java.util.Locale.GERMANY).format(java.util.Date(programme.start)), color = accent, fontSize = 10.sp, modifier = Modifier.width(52.dp))
                            Column(Modifier.weight(1f)) { Text(programme.title, color = Primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold); Text(programme.category ?: "TV", color = Secondary, fontSize = 9.sp) }
                            if (programme.isCatchupAvailable) Text("REPLAY", color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchScreen(store: PlaylistStore, accent: Color, demo: Boolean, onPlay: (Channel) -> Unit) {
    var query by remember { mutableStateOf("") }
    val channels = if (demo) DemoData.channels() else store.channels
    val filtered = channels.filter { query.isBlank() || it.name.contains(query, true) || it.group.orEmpty().contains(query, true) }
    Column(Modifier.fillMaxSize()) {
        Text("Suche", color = Primary, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text("Sender und Gruppen", color = Secondary, fontSize = 13.sp)
        OutlinedTextField(query, { query = it }, label = { Text("Suchen…") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) { items(filtered) { ChannelCard(it, accent, onPlay) } }
    }
}

@Composable
private fun SourcesScreen(store: PlaylistStore, accent: Color, onBack: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            status = "Lese Playlist…"
            runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("Datei ist leer") }
                .onSuccess { text -> runCatching { store.importPlaylist(text) }.onSuccess { status = "${store.channels.size} Sender importiert"; onDone() }.onFailure { status = it.message ?: "M3U ungültig" } }
                .onFailure { status = it.message ?: "Datei konnte nicht gelesen werden" }
        }
    }
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().padding(4.dp)) {
        Text("Quellen", color = Primary, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text("M3U / M3U8 oder Xtream Codes", color = Secondary, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))
        SourcePanel("M3U / M3U8", "Datei oder direkte URL") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton("Datei auswählen", accent) { picker.launch(arrayOf("*/*")) }
                ActionButton("URL laden", accent) { scope.launch { status = "URL wird geladen…"; runCatching { store.importPlaylistFromUrl(url.trim()) }.onSuccess { status = "${store.channels.size} Sender importiert"; onDone() }.onFailure { status = it.message ?: "URL konnte nicht geladen werden" } } }
            }
            OutlinedTextField(url, { url = it }, label = { Text("M3U / M3U8 URL") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
        Spacer(Modifier.height(8.dp))
        SourcePanel("Xtream Codes", "Server · Benutzername · Passwort") {
            OutlinedTextField(server, { server = it }, label = { Text("Server URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(user, { user = it }, label = { Text("Benutzername") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            OutlinedTextField(pass, { pass = it }, label = { Text("Passwort") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            Spacer(Modifier.height(8.dp))
            ActionButton("Xtream verbinden", accent) { scope.launch { status = "Xtream wird verbunden…"; XtreamClient(server.trim(), user.trim(), pass).load().onSuccess { list -> store.importXtream(list); status = "${list.size} Sender importiert"; onDone() }.onFailure { status = it.message ?: "Xtream-Verbindung fehlgeschlagen" } } }
        }
        Spacer(Modifier.height(8.dp)); Text(status, color = accent, fontSize = 11.sp); Spacer(Modifier.height(6.dp)); ActionButton("Zurück", accent, onBack)
    }
}

@Composable
private fun SourcePanel(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(18.dp)).border(1.dp, Color.White.copy(.08f), RoundedCornerShape(18.dp)).padding(14.dp)) {
        Text(title, color = Primary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Secondary, fontSize = 10.sp)
        Spacer(Modifier.height(8.dp)); content()
    }
}

@Composable
private fun SettingsScreen(settings: SettingsStore, accent: Color, openSources: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Player", "Live TV", "EPG", "Look & Feel", "Quellen")
    Column(Modifier.fillMaxSize()) {
        Text("Einstellungen", color = Primary, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text("Deine ZenPlayer-Konfiguration", color = Secondary, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.focusGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { tabs.forEachIndexed { i, title -> ActionButton(title, accent) { tab = i } } }
        Spacer(Modifier.height(10.dp))
        when (tab) {
            0 -> SettingsPanel("Player", "Engine und Buffer") {
                Choice("Engine", settings.player.engine.label, PlaybackEngine.entries.map { it.label }, accent) { v -> settings.updatePlayer(settings.player.copy(engine = PlaybackEngine.entries.first { it.label == v })) }
                Choice("Buffer", settings.player.bufferMode.label, BufferMode.entries.map { it.label }, accent) { v -> settings.updatePlayer(settings.player.copy(bufferMode = BufferMode.entries.first { it.label == v })) }
            }
            1 -> SettingsPanel("Live TV", "Live und Catch-up") {
                Toggle("Shared Catch-up bevorzugen", settings.player.preferCatchupSibling, accent) { settings.updatePlayer(settings.player.copy(preferCatchupSibling = it)) }
                Toggle("Timeshift wenn verfügbar", settings.player.enableTimeshiftWhenAvailable, accent) { settings.updatePlayer(settings.player.copy(enableTimeshiftWhenAvailable = it)) }
            }
            2 -> SettingsPanel("EPG", "Programmansicht") {
                Choice("Zeitraum", settings.epg.pageSize.label, EpgPageSize.entries.map { it.label }, accent) { v -> settings.updateEpg(settings.epg.copy(pageSize = EpgPageSize.entries.first { it.label == v })) }
            }
            3 -> LookAndFeelStudio(settings, Modifier.fillMaxSize())
            else -> SettingsPanel("Quellen", "Playlist-Verwaltung") {
                Text("M3U / M3U8 / Xtream hinzufügen, wechseln oder neu laden.", color = Secondary, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp)); ActionButton("Quellen öffnen", accent, openSources)
            }
        }
    }
}

@Composable
private fun SettingsPanel(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(18.dp)).border(1.dp, Color.White.copy(.08f), RoundedCornerShape(18.dp)).padding(15.dp)) {
        Text(title, color = Primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Secondary, fontSize = 10.sp)
        Spacer(Modifier.height(9.dp)); content()
    }
}

@Composable
private fun Choice(label: String, value: String, options: List<String>, accent: Color, onChange: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    var index by remember(value) { mutableIntStateOf(options.indexOf(value).coerceAtLeast(0)) }
    Row(Modifier.fillMaxWidth().height(48.dp).focusable().onFocusChanged { focused = it.isFocused }
        .onKeyEvent { e -> if (e.type != KeyEventType.KeyUp) return@onKeyEvent false; when (e.key) { Key.DirectionLeft -> { index = (index - 1 + options.size) % options.size; onChange(options[index]); true }; Key.DirectionRight, Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { index = (index + 1) % options.size; onChange(options[index]); true }; else -> false } }
        .background(if (focused) accent.copy(.1f) else Surface, RoundedCornerShape(12.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.Transparent, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Secondary, fontSize = 11.sp); Spacer(Modifier.weight(1f)); Text(value, color = Primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold); Text("  ${index + 1}/${options.size}", color = accent, fontSize = 9.sp)
    }
}

@Composable
private fun Toggle(label: String, checked: Boolean, accent: Color, onChange: (Boolean) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(44.dp).focusable().onFocusChanged { focused = it.isFocused }.onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key in OkKeys) { onChange(!checked); true } else false }
        .background(if (focused) accent.copy(.1f) else Color.Transparent, RoundedCornerShape(11.dp)).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Primary, fontSize = 12.sp); Spacer(Modifier.weight(1f)); Text(if (checked) "AN" else "AUS", color = if (checked) accent else Secondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

private fun initials(name: String): String = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercaseChar().toString() }.ifBlank { "TV" }
