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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zenplayer.tv.domain.model.Channel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private val V6Text = Color(0xFFF7F8FC)
private val V6Muted = Color(0xFF8D96AA)
private val V6Bg = Color(0xFF05070D)
private val V6OkKeys = setOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)

@Composable
fun ZenPlayerShellV6(settings: SettingsStore) {
    val context = LocalContext.current
    val store = remember { PlaylistStore(context) }
    val ui = settings.ui
    val accent = when (ui.theme) {
        ZenTheme.AURORA -> Color(0xFF70E6FF)
        ZenTheme.OBSIDIAN -> Color(0xFFAAA8FF)
        ZenTheme.FROST -> Color(0xFF9FEAFF)
        ZenTheme.AMBER -> Color(0xFFFFC46E)
    }
    var page by remember { mutableStateOf("home") }
    var sourceReturn by remember { mutableStateOf("home") }
    var sources by remember { mutableStateOf(false) }
    var demo by remember { mutableStateOf(false) }
    var playerIndex by remember { mutableIntStateOf(-1) }
    var showExitDialog by remember { mutableStateOf(false) }
    var homeBackReset by remember { mutableStateOf(false) }
    val homeFocusRequester = remember { FocusRequester() }
    val channels = if (demo) DemoData.channels() else store.channels

    BackHandler(enabled = true) {
        when {
            showExitDialog -> showExitDialog = false
            playerIndex >= 0 -> playerIndex = -1
            sources -> { sources = false; page = sourceReturn; homeBackReset = false }
            page != "home" -> {
                page = "home"
                homeBackReset = false
                homeFocusRequester.requestFocus()
            }
            !homeBackReset -> {
                homeBackReset = true
                homeFocusRequester.requestFocus()
            }
            else -> showExitDialog = true
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("ZenPlayer beenden?") },
            text = { Text("Möchtest du ZenPlayer wirklich schließen?") },
            confirmButton = {
                TextButton(onClick = { (context as? Activity)?.finish() }) { Text("Beenden") }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("Abbrechen") }
            }
        )
    }

    Box(Modifier.fillMaxSize().background(V6Bg)) {
        ZenAnimatedBackdrop(ui.theme, ui.animatedBackdrop, !ui.reducedMotion)
        when {
            sources -> V6Sources(store, accent, { sources = false; page = sourceReturn }, { sources = false; page = sourceReturn })
            playerIndex in channels.indices -> {
                val channel = channels[playerIndex]
                ZenPlayerScreen(channel, settings, { playerIndex = -1 }, { key ->
                    when (key) {
                        Key.DirectionUp -> { if (channels.isNotEmpty()) playerIndex = (playerIndex - 1 + channels.size) % channels.size; true }
                        Key.DirectionDown -> { if (channels.isNotEmpty()) playerIndex = (playerIndex + 1) % channels.size; true }
                        else -> false
                    }
                }, true)
            }
            else -> Row(
                Modifier.fillMaxSize().padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.Top
            ) {
                V6Sidebar(page, accent, homeFocusRequester) { selected ->
                    if (selected != page) homeBackReset = false
                    page = selected
                }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    when (page) {
                        "home" -> V6Home(store, demo, accent, { sourceReturn = page; sources = true }, { demo = true; homeBackReset = false }, { demo = false; homeBackReset = false }) { c -> playerIndex = channels.indexOf(c).coerceAtLeast(0) }
                        "epg" -> V6Epg(store, demo, accent) { c -> playerIndex = channels.indexOf(c).coerceAtLeast(0) }
                        "search" -> V6Search(store, demo, accent) { c -> playerIndex = channels.indexOf(c).coerceAtLeast(0) }
                        else -> V6Settings(settings, accent) { sourceReturn = page; sources = true }
                    }
                }
            }
        }
    }
}

@Composable
private fun V6Sidebar(page: String, accent: Color, homeFocusRequester: FocusRequester, onPage: (String) -> Unit) {
    val ids = listOf("home", "epg", "search", "settings")
    val labels = listOf("Home", "EPG", "Suche", "Settings")
    val icons = listOf(Icons.Default.Home, Icons.Default.PlayArrow, Icons.Default.Search, Icons.Default.Settings)
    val requesters = remember { List(ids.size) { FocusRequester() } }

    LaunchedEffect(homeFocusRequester) {
        withFrameNanos { }
        homeFocusRequester.requestFocus()
    }

    LaunchedEffect(page) {
        withFrameNanos { }
        requesters[ids.indexOf(page).coerceAtLeast(0)].requestFocus()
    }

    Column(
        Modifier
            .width(82.dp)
            .wrapContentHeight()
            .background(Color.White.copy(.045f), RoundedCornerShape(24.dp))
            .border(1.dp, Color.White.copy(.10f), RoundedCornerShape(24.dp))
            .padding(10.dp)
            .focusRestorer(homeFocusRequester)
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(58.dp).background(accent.copy(.16f), RoundedCornerShape(18.dp)), Alignment.Center) {
            Text("Z", color = V6Text, fontSize = 25.sp, fontWeight = FontWeight.Black)
        }
        ids.forEachIndexed { i, id ->
            var focused by remember { mutableStateOf(false) }
            val active = focused || page == id
            val requester = if (i == 0) homeFocusRequester else requesters[i]
            Box(
                Modifier
                    .size(58.dp)
                    .focusRequester(requester)
                    .onFocusChanged {
                        focused = it.isFocused
                        if (id == "home" && it.isFocused) homeBackReset = false
                    }
                    .focusable()
                    .onKeyEvent { e ->
                        if (e.type == KeyEventType.KeyUp && e.key in V6OkKeys) {
                            onPage(id)
                            true
                        } else false
                    }
                    .background(if (active) accent.copy(.18f) else Color.Transparent, RoundedCornerShape(18.dp))
                    .border(if (focused) 2.dp else 1.dp, if (active) accent.copy(.85f) else Color.Transparent, RoundedCornerShape(18.dp)),
                Alignment.Center
            ) {
                Icon(icons[i], labels[i], tint = if (active) V6Text else V6Muted)
            }
        }
    }
}

@Composable
private fun V6Home(store: PlaylistStore, demo: Boolean, accent: Color, onSources: () -> Unit, onDemo: () -> Unit, onExitDemo: () -> Unit, onPlay: (Channel) -> Unit) {
    val channels = if (demo) DemoData.channels() else store.channels
    Column(Modifier.fillMaxSize()) {
        Text(if (demo) "Demo-Modus" else "Für dich", color = V6Text, fontSize = 34.sp, fontWeight = FontWeight.Black)
        Text(if (demo) "Testsender · EPG · Player" else "Dein persönliches Live-TV", color = V6Muted, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))
        if (channels.isEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            V6Action("Playlist hinzufügen", "M3U / URL / Xtream", Icons.Default.Source, accent, onSources)
            V6Action("Demo starten", "Ohne Quelle testen", Icons.Default.PlayArrow, accent, onDemo)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                V6Action("Quelle verwalten", "Playlist / Xtream", Icons.Default.Source, accent, onSources)
                if (demo) V6Action("Demo beenden", "Zur Playlist zurück", Icons.Default.Home, accent, onExitDemo)
            }
            Spacer(Modifier.height(14.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 24.dp)) { items(channels) { V6Channel(it, accent) { onPlay(it) } } }
        }
    }
}

@Composable
private fun V6Action(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, action: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.width(360.dp).height(96.dp).focusable().onFocusChanged { focused = it.isFocused }.onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key in V6OkKeys) { action(); true } else false }
        .background(if (focused) accent.copy(.16f) else Color.White.copy(.055f), RoundedCornerShape(20.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(.08f), RoundedCornerShape(20.dp)).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = accent); Column(Modifier.padding(start = 14.dp)) { Text(title, color = V6Text, fontSize = 16.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = V6Muted, fontSize = 11.sp) }
    }
}

@Composable
private fun V6Channel(channel: Channel, accent: Color, action: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(72.dp).focusable().onFocusChanged { focused = it.isFocused }.onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key in V6OkKeys) { action(); true } else false }
        .background(if (focused) accent.copy(.13f) else Color.White.copy(.05f), RoundedCornerShape(18.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(.08f), RoundedCornerShape(18.dp)).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        if (!channel.logoUrl.isNullOrBlank()) AsyncImage(channel.logoUrl, channel.name, Modifier.size(48.dp), contentScale = ContentScale.Fit) else Box(Modifier.size(48.dp).background(accent.copy(.12f), RoundedCornerShape(13.dp)), Alignment.Center) { Text(initialsV6(channel.name), color = accent, fontWeight = FontWeight.Bold) }
        Column(Modifier.weight(1f).padding(start = 14.dp)) { Text(channel.name, color = V6Text, fontSize = 15.sp, fontWeight = FontWeight.Bold); Text(channel.group ?: "Live TV", color = V6Muted, fontSize = 10.sp) }
        Text(if (channel.isCatchupCapable) "CATCHUP" else "LIVE", color = if (focused) accent else V6Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun V6Epg(store: PlaylistStore, demo: Boolean, accent: Color, onPlay: (Channel) -> Unit) {
    val channels = if (demo) DemoData.channels() else store.channels
    val programmes = if (demo) DemoData.programmes() else store.programmes
    Column(Modifier.fillMaxSize()) {
        Text("EPG", color = V6Text, fontSize = 34.sp, fontWeight = FontWeight.Black)
        Text("OK startet den gewählten Sender", color = V6Muted, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            channels.forEach { channel ->
                val ps = programmes.filter { it.channelId == channel.id }.sortedBy { it.start }.take(8)
                if (ps.isNotEmpty()) {
                    item { Text(channel.name, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    items(ps) { p ->
                        var focused by remember(p.id) { mutableStateOf(false) }
                        Row(Modifier.fillMaxWidth().height(58.dp).focusable().onFocusChanged { focused = it.isFocused }.onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key in V6OkKeys) { onPlay(channel); true } else false }
                            .background(if (focused) accent.copy(.13f) else Color.White.copy(.05f), RoundedCornerShape(14.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.Transparent, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(p.start)), color = accent, fontSize = 11.sp, modifier = Modifier.width(52.dp))
                            Column(Modifier.weight(1f)) { Text(p.title, color = V6Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Text(p.category ?: "TV", color = V6Muted, fontSize = 9.sp) }
                            if (p.isCatchupAvailable) Text("REPLAY", color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V6Search(store: PlaylistStore, demo: Boolean, accent: Color, onPlay: (Channel) -> Unit) {
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    val channels = if (demo) DemoData.channels() else store.channels
    val filtered = channels.filter { query.isBlank() || it.name.contains(query, true) || it.group.orEmpty().contains(query, true) }
    Column(Modifier.fillMaxSize()) {
        Text("Suche", color = V6Text, fontSize = 34.sp, fontWeight = FontWeight.Black)
        Text("Fokus öffnet nichts · erst OK startet die Eingabe", color = V6Muted, fontSize = 13.sp)
        V6TvInput("Sender suchen…", query, { query = it }, "search", if (editing) "search" else "") { editing = true }
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(filtered) { V6Channel(it, accent) { onPlay(it) } } }
    }
}

@Composable
private fun V6Sources(store: PlaylistStore, accent: Color, onBack: () -> Unit, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) scope.launch { status = "Playlist wird eingelesen…"; runCatching { store.importPlaylistFromUri(uri) }.onSuccess { status = "${store.channels.size} Sender importiert"; onDone() }.onFailure { status = it.message ?: "M3U konnte nicht gelesen werden" } } }
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize()) {
        Text("Quelle hinzufügen", color = V6Text, fontSize = 34.sp, fontWeight = FontWeight.Black)
        Text("D-Pad navigieren · OK startet die Eingabe · Zurück geht zurück", color = V6Muted, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            item { V6SourceCard("M3U / M3U8", accent, Modifier.fillMaxWidth()) {
                V6SourceButton("Datei auswählen", accent) { picker.launch(arrayOf("*/*")) }
                V6TvInput("M3U / M3U8 URL", url, { url = it }, "url", editing) { editing = "url" }
                V6SourceButton("URL importieren", accent) { scope.launch { status = "URL wird geladen…"; runCatching { store.importPlaylistFromUrl(url.trim()) }.onSuccess { status = "${store.channels.size} Sender importiert"; onDone() }.onFailure { status = it.message ?: "M3U konnte nicht geladen werden" } } }
            } }
            item { V6SourceCard("Xtream Codes", accent, Modifier.fillMaxWidth()) {
                V6TvInput("Server URL oder Host", server, { server = it }, "server", editing) { editing = "server" }
                V6TvInput("Benutzername", user, { user = it }, "user", editing) { editing = "user" }
                V6TvInput("Passwort", pass, { pass = it }, "pass", editing) { editing = "pass" }
                V6SourceButton("Xtream verbinden", accent) { scope.launch { status = "Xtream wird verbunden…"; val result = XtreamClient(server.trim(), user.trim(), pass).load(); if (result.isSuccess) { val list = result.getOrNull().orEmpty(); store.importXtream(list); status = "${list.size} Sender importiert"; onDone() } else status = result.exceptionOrNull()?.message ?: "Xtream-Verbindung fehlgeschlagen" } }
            } }
            item { Text(status, color = accent, fontSize = 12.sp); V6SourceButton("Zurück", accent, onBack) }
        }
    }
}

@Composable
private fun V6TvInput(label: String, value: String, onValue: (String) -> Unit, id: String, editing: String, startEditing: () -> Unit) {
    val active = editing == id
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(active) { if (active) keyboard?.show() }
    if (!active) {
        var focused by remember { mutableStateOf(false) }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp).height(58.dp).focusable().onFocusChanged { focused = it.isFocused }
            .onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key in V6OkKeys) { startEditing(); true } else false }
            .background(if (focused) Color.White.copy(.10f) else Color.White.copy(.035f), RoundedCornerShape(14.dp))
            .border(if (focused) 2.dp else 1.dp, if (focused) Color.White.copy(.55f) else Color.White.copy(.10f), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(label, color = if (focused) V6Text else V6Muted, fontSize = 9.sp); Text(if (value.isBlank()) "OK zum Eingeben" else value, color = V6Text, fontSize = 13.sp, maxLines = 1) }
            if (focused) Text("OK", color = V6Text, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedTextField(value, onValue, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
    }
}

@Composable
private fun V6SourceCard(title: String, accent: Color, modifier: Modifier, content: @Composable () -> Unit) {
    Column(modifier.background(Color.White.copy(.055f), RoundedCornerShape(20.dp)).border(1.dp, Color.White.copy(.09f), RoundedCornerShape(20.dp)).padding(18.dp)) { Text(title, color = V6Text, fontSize = 18.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp)); content() }
}

@Composable
private fun V6SourceButton(title: String, accent: Color, action: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.padding(top = 8.dp).focusable().onFocusChanged { focused = it.isFocused }.onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key in V6OkKeys) { action(); true } else false }
        .background(if (focused) accent.copy(.17f) else Color.White.copy(.055f), RoundedCornerShape(12.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(.08f), RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 10.dp)) { Text(title, color = V6Text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun V6Settings(settings: SettingsStore, accent: Color, onSources: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text("Einstellungen", color = V6Text, fontSize = 34.sp, fontWeight = FontWeight.Black)
        Text("Alles mit D-Pad + OK", color = V6Muted, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        V6SourceCard("Player", accent, Modifier.fillMaxWidth()) { Text("Engine: ${settings.player.engine.label}", color = V6Text); Text("Buffer: ${settings.player.bufferMode.label}", color = V6Muted, modifier = Modifier.padding(top = 5.dp)) }
        Spacer(Modifier.height(10.dp)); V6SourceCard("Live TV", accent, Modifier.fillMaxWidth()) { Text("Shared Catch-up: ${if (settings.player.preferCatchupSibling) "an" else "aus"}", color = V6Muted) }
        Spacer(Modifier.height(10.dp)); V6SourceCard("Darstellung", accent, Modifier.fillMaxWidth()) { Text("Theme: ${settings.ui.theme.label}", color = V6Text); Text("Glasstärke: ${settings.ui.glassIntensity}/10", color = V6Muted, modifier = Modifier.padding(top = 5.dp)) }
        V6SourceButton("Quellen öffnen", accent, onSources)
    }
}

private fun initialsV6(name: String): String = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercaseChar().toString() }
