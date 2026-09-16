package com.zenplayer.tv

import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zenplayer.tv.domain.model.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val V5Text = Color(0xFFF7F8FC)
private val V5Muted = Color(0xFF8F98AA)
private val V5Panel = Color(0xD9141822)
private val V5Bg = Color(0xFF05070D)

@Composable
fun ZenPlayerShellV5(settings: SettingsStore) {
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
    var sourceOpen by remember { mutableStateOf(false) }
    var demo by remember { mutableStateOf(false) }
    var playerIndex by remember { mutableIntStateOf(-1) }
    var zapUntil by remember { mutableLongStateOf(0L) }
    var previousIndex by remember { mutableIntStateOf(-1) }
    val channels = if (demo) DemoData.channels() else store.channels

    BackHandler {
        when {
            playerIndex >= 0 -> playerIndex = -1
            sourceOpen -> sourceOpen = false
            page != "home" -> page = "home"
            else -> Unit
        }
    }

    Box(Modifier.fillMaxSize().background(V5Bg)) {
        ZenAnimatedBackdrop(ui.theme, ui.animatedBackdrop, !ui.reducedMotion)
        when {
            sourceOpen -> V5Sources(store, accent, onBack = { sourceOpen = false }, onDone = { sourceOpen = false })
            playerIndex >= 0 && playerIndex < channels.size -> {
                val channel = channels[playerIndex]
                ZenPlayerScreen(
                    channel = channel,
                    settings = settings,
                    showChrome = false,
                    onBack = { playerIndex = -1 },
                    onRemoteKey = { key ->
                        when (key) {
                            Key.DirectionUp -> {
                                if (channels.isNotEmpty()) {
                                    previousIndex = playerIndex
                                    playerIndex = (playerIndex - 1 + channels.size) % channels.size
                                    zapUntil = System.currentTimeMillis() + 2600L
                                }
                                true
                            }
                            Key.DirectionDown -> {
                                if (channels.isNotEmpty()) {
                                    previousIndex = playerIndex
                                    playerIndex = (playerIndex + 1) % channels.size
                                    zapUntil = System.currentTimeMillis() + 2600L
                                }
                                true
                            }
                            else -> false
                        }
                    }
                )
                V5ZapOverlay(channel, accent, visibleUntil = zapUntil)
            }
            else -> {
                Row(Modifier.fillMaxSize().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    V5Sidebar(page, accent) { page = it }
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        when (page) {
                            "home" -> V5Home(store, demo, accent, onSources = { sourceOpen = true }, onDemo = { demo = true }, onExitDemo = { demo = false }, onPlay = { c -> playerIndex = channels.indexOf(c); zapUntil = System.currentTimeMillis() + 2600L })
                            "epg" -> V5Epg(store, demo, accent) { c -> playerIndex = channels.indexOf(c); zapUntil = System.currentTimeMillis() + 2600L }
                            "search" -> V5Search(store, demo, accent) { c -> playerIndex = channels.indexOf(c); zapUntil = System.currentTimeMillis() + 2600L }
                            else -> V5Settings(settings, accent) { sourceOpen = true }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V5Sidebar(page: String, accent: Color, onPage: (String) -> Unit) {
    val ids = listOf("home", "epg", "search", "settings")
    val labels = listOf("Home", "EPG", "Suche", "Settings")
    val icons = listOf(Icons.Default.Home, Icons.Default.PlayArrow, Icons.Default.Search, Icons.Default.Settings)
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    Column(Modifier.width(82.dp).fillMaxHeight().focusGroup().background(Color.White.copy(.045f), RoundedCornerShape(24.dp)).border(1.dp, Color.White.copy(.10f), RoundedCornerShape(24.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.width(58.dp).height(58.dp).background(accent.copy(.16f), RoundedCornerShape(18.dp)).border(1.dp, accent.copy(.55f), RoundedCornerShape(18.dp)), Alignment.Center) { Text("Z", color = V5Text, fontSize = 25.sp, fontWeight = FontWeight.Black) }
        ids.forEachIndexed { i, id ->
            V5NavItem(labels[i], icons[i], page == id, accent, if (i == 0) first else null) { onPage(id) }
        }
    }
}

@Composable
private fun V5NavItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, accent: Color, requester: FocusRequester?, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val active = selected || focused
    Box(Modifier.width(58.dp).height(58.dp).then(if (requester != null) Modifier.focusRequester(requester) else Modifier).onFocusChanged { focused = it.isFocused }.focusable().onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && (e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter)) { onClick(); true } else false }.background(if (active) accent.copy(.19f) else Color.Transparent, RoundedCornerShape(18.dp)).border(if (focused) 2.dp else 1.dp, if (active) accent.copy(if (focused) .95f else .45f) else Color.Transparent, RoundedCornerShape(18.dp)), Alignment.Center) {
        Icon(icon, label, tint = if (active) V5Text else V5Muted)
    }
}

@Composable
private fun V5Home(store: PlaylistStore, demo: Boolean, accent: Color, onSources: () -> Unit, onDemo: () -> Unit, onExitDemo: () -> Unit, onPlay: (Channel) -> Unit) {
    val channels = if (demo) DemoData.channels() else store.channels
    Column(Modifier.fillMaxSize()) {
        Text(if (demo) "Demo-Modus" else "Für dich", color = V5Text, fontSize = 34.sp, fontWeight = FontWeight.Black)
        Text(if (demo) "Testsender · EPG · Player" else "Dein persönliches Live-TV", color = V5Muted, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))
        if (channels.isEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                V5Action("Playlist hinzufügen", "M3U / URL / Xtream", Icons.Default.Source, accent, onSources)
                V5Action("Demo starten", "Ohne Quelle testen", Icons.Default.PlayArrow, accent, onDemo)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { V5Pill(if (demo) "Demo beenden" else "Quelle verwalten", accent) { if (demo) onExitDemo() else onSources() }; Text("${channels.size} Sender", color = V5Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 10.dp)) }
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 24.dp)) { items(channels) { V5Channel(it, accent, onPlay) } }
        }
    }
}

@Composable private fun V5Action(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, onClick: () -> Unit) { var f by remember { mutableStateOf(false) }; Row(Modifier.width(360.dp).height(104.dp).focusable().onFocusChanged { f = it.isFocused }.onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key == Key.DirectionCenter) { onClick(); true } else false }.background(if (f) accent.copy(.16f) else Color.White.copy(.055f), RoundedCornerShape(22.dp)).border(if (f) 2.dp else 1.dp, if (f) accent else Color.White.copy(.09f), RoundedCornerShape(22.dp)).padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = accent); Column(Modifier.padding(start = 15.dp)) { Text(title, color = V5Text, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = V5Muted, fontSize = 11.sp) } } }
@Composable private fun V5Pill(title: String, accent: Color, onClick: () -> Unit) { var f by remember { mutableStateOf(false) }; Box(Modifier.focusable().onFocusChanged { f = it.isFocused }.onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key == Key.DirectionCenter) { onClick(); true } else false }.background(if (f) accent.copy(.16f) else Color.White.copy(.055f), RoundedCornerShape(13.dp)).border(if (f) 2.dp else 1.dp, if (f) accent else Color.White.copy(.09f), RoundedCornerShape(13.dp)).padding(horizontal = 15.dp, vertical = 10.dp)) { Text(title, color = V5Text, fontSize = 11.sp, fontWeight = FontWeight.Bold) } }

@Composable private fun V5Channel(channel: Channel, accent: Color, onPlay: (Channel) -> Unit) { var f by remember { mutableStateOf(false) }; Row(Modifier.fillMaxWidth().height(74.dp).focusable().onFocusChanged { f = it.isFocused }.onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key == Key.DirectionCenter) { onPlay(channel); true } else false }.background(if (f) accent.copy(.13f) else Color.White.copy(.055f), RoundedCornerShape(18.dp)).border(if (f) 2.dp else 1.dp, if (f) accent.copy(.82f) else Color.White.copy(.08f), RoundedCornerShape(18.dp)).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) { if (!channel.logoUrl.isNullOrBlank()) AsyncImage(channel.logoUrl, channel.name, Modifier.width(48.dp).height(48.dp), contentScale = ContentScale.Fit) else Box(Modifier.width(48.dp).height(48.dp).background(accent.copy(.12f), RoundedCornerShape(13.dp)), Alignment.Center) { Text(initialsV5(channel.name), color = accent, fontWeight = FontWeight.Bold) }; Column(Modifier.weight(1f).padding(start = 14.dp)) { Text(channel.name, color = V5Text, fontSize = 15.sp, fontWeight = FontWeight.Bold); Text(channel.group ?: "Live TV", color = V5Muted, fontSize = 10.sp) }; Text(if (channel.isCatchupCapable) "CATCHUP" else "LIVE", color = if (f) accent else V5Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold) } }

@Composable
private fun V5ZapOverlay(channel: Channel, accent: Color, visibleUntil: Long) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(visibleUntil) { while (visibleUntil > System.currentTimeMillis()) { now = System.currentTimeMillis(); delay(100) } }
    if (visibleUntil <= now || visibleUntil == 0L) return
    Box(Modifier.fillMaxSize().padding(start = 28.dp, top = 28.dp), contentAlignment = Alignment.TopStart) {
        Row(Modifier.width(410.dp).background(Color(0xE9151923), RoundedCornerShape(24.dp)).border(1.dp, accent.copy(.55f), RoundedCornerShape(24.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!channel.logoUrl.isNullOrBlank()) AsyncImage(channel.logoUrl, channel.name, Modifier.width(62.dp).height(62.dp), contentScale = ContentScale.Fit) else Box(Modifier.width(62.dp).height(62.dp).background(accent.copy(.12f), RoundedCornerShape(16.dp)), Alignment.Center) { Text(initialsV5(channel.name), color = accent, fontSize = 17.sp, fontWeight = FontWeight.Black) }
            Column(Modifier.padding(start = 15.dp).weight(1f)) {
                Text(channel.name, color = V5Text, fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Text(channel.group ?: "Live TV", color = V5Muted, fontSize = 11.sp)
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.background(accent.copy(.18f), RoundedCornerShape(7.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) { Text(if (channel.isCatchupCapable) "CATCHUP" else "● LIVE", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                    Text(SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date()), color = V5Muted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun V5Sources(store: PlaylistStore, accent: Color, onBack: () -> Unit, onDone: () -> Unit) {
    val scope = rememberCoroutineScope(); val keyboard = LocalSoftwareKeyboardController.current
    var url by remember { mutableStateOf("") }; var server by remember { mutableStateOf("") }; var user by remember { mutableStateOf("") }; var pass by remember { mutableStateOf("") }; var status by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) scope.launch { status = "Playlist wird eingelesen…"; runCatching { store.importPlaylistFromUri(uri) }.onSuccess { status = "${store.channels.size} Sender importiert"; onDone() }.onFailure { status = it.message ?: "M3U konnte nicht gelesen werden" } } }
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize()) {
        Text("Quelle hinzufügen", color = V5Text, fontSize = 34.sp, fontWeight = FontWeight.Black); Text("Fokus = auswählen · OK = Eingabe starten", color = V5Muted, fontSize = 13.sp); Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            V5SourceCard("M3U / M3U8", accent, Modifier.weight(1f)) {
                V5SourceButton("Datei auswählen", accent) { picker.launch(arrayOf("*/*")) }
                V5TvInput("M3U / M3U8 URL", url, { url = it }, accent, keyboard)
                V5SourceButton("URL importieren", accent) { scope.launch { status = "URL wird geladen…"; runCatching { store.importPlaylistFromUrl(url.trim()) }.onSuccess { status = "${store.channels.size} Sender importiert"; onDone() }.onFailure { status = it.message ?: "M3U konnte nicht geladen werden" } } }
            }
            V5SourceCard("Xtream Codes", accent, Modifier.weight(1f)) {
                V5TvInput("Server URL oder Host", server, { server = it }, accent, keyboard); V5TvInput("Benutzername", user, { user = it }, accent, keyboard); V5TvInput("Passwort", pass, { pass = it }, accent, keyboard)
                V5SourceButton("Xtream verbinden", accent) { scope.launch { status = "Xtream wird verbunden…"; runCatching { XtreamClient(server.trim(), user.trim(), pass).load() }.onSuccess { list -> store.importXtream(list); status = "${list.size} Sender importiert"; onDone() }.onFailure { status = it.message ?: "Xtream-Verbindung fehlgeschlagen" } } }
            }
        }
        Spacer(Modifier.height(14.dp)); Text(status, color = accent, fontSize = 12.sp); Spacer(Modifier.height(12.dp)); V5SourceButton("Zurück", accent, onBack)
    }
}

@Composable private fun V5TvInput(label: String, value: String, onValue: (String) -> Unit, accent: Color, keyboard: androidx.compose.ui.platform.SoftwareKeyboardController?) { val requester = remember { FocusRequester() }; var focused by remember { mutableStateOf(false) }; OutlinedTextField(value, onValue, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(showKeyboardOnFocus = false), modifier = Modifier.fillMaxWidth().padding(top = 8.dp).focusRequester(requester).onFocusChanged { focused = it.isFocused }.onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && (e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter)) { requester.requestFocus(); keyboard?.show(); true } else false }, colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = Color.White.copy(.18f), focusedLabelColor = accent, unfocusedLabelColor = V5Muted, cursorColor = accent, focusedTextColor = V5Text, unfocusedTextColor = V5Text)) }
@Composable private fun V5SourceCard(title: String, accent: Color, modifier: Modifier, content: @Composable () -> Unit) { Column(modifier.background(Color.White.copy(.055f), RoundedCornerShape(22.dp)).border(1.dp, Color.White.copy(.09f), RoundedCornerShape(22.dp)).padding(18.dp)) { Text(title, color = V5Text, fontSize = 19.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp)); content() } }
@Composable private fun V5SourceButton(title: String, accent: Color, onClick: () -> Unit) { var f by remember { mutableStateOf(false) }; Box(Modifier.focusable().onFocusChanged { f = it.isFocused }.onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && (e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter)) { onClick(); true } else false }.background(if (f) accent.copy(.18f) else Color.White.copy(.055f), RoundedCornerShape(12.dp)).border(if (f) 2.dp else 1.dp, if (f) accent else Color.White.copy(.08f), RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 10.dp)) { Text(title, color = V5Text, fontSize = 11.sp, fontWeight = FontWeight.Bold) } }

@Composable
private fun V5Epg(store: PlaylistStore, demo: Boolean, accent: Color, onPlay: (Channel) -> Unit) {
    val channels = if (demo) DemoData.channels() else store.channels; val programmes = if (demo) DemoData.programmes() else store.programmes; val fmt = SimpleDateFormat("HH:mm", Locale.GERMANY)
    Column(Modifier.fillMaxSize()) { Text("TV Guide", color = V5Text, fontSize = 34.sp, fontWeight = FontWeight.Black); Text("Erstes OK = Vorschau · zweites OK = abspielen", color = V5Muted, fontSize = 13.sp); Spacer(Modifier.height(16.dp)); LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 30.dp)) { channels.forEach { channel -> val ps = programmes.filter { it.channelId == channel.id }.sortedBy { it.start }.take(10); if (ps.isNotEmpty()) { item { Text(channel.name, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold) }; items(ps) { p -> var focus by remember(p.id) { mutableStateOf(false) }; var preview by remember(p.id) { mutableStateOf(false) }; Row(Modifier.fillMaxWidth().height(64.dp).focusable().onFocusChanged { focus = it.isFocused }.onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && (e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter)) { if (preview) onPlay(channel) else preview = true; true } else false }.background(if (focus || preview) accent.copy(.12f) else Color.White.copy(.05f), RoundedCornerShape(16.dp)).border(if (focus) 2.dp else 1.dp, if (focus) accent else Color.White.copy(.07f), RoundedCornerShape(16.dp)).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) { Text(fmt.format(Date(p.start)), color = accent, fontSize = 11.sp, modifier = Modifier.width(58.dp)); Column(Modifier.weight(1f)) { Text(p.title, color = V5Text, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text(if (preview) "OK erneut zum Abspielen · ${p.category ?: "TV"}" else (p.category ?: "TV"), color = V5Muted, fontSize = 9.sp) }; if (p.isCatchupAvailable) Text("CATCHUP", color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold) } } } } } }
}

@Composable private fun V5Search(store: PlaylistStore, demo: Boolean, accent: Color, onPlay: (Channel) -> Unit) { val keyboard = LocalSoftwareKeyboardController.current; var q by remember { mutableStateOf("") }; val channels = if (demo) DemoData.channels() else store.channels; val filtered = channels.filter { q.isBlank() || it.name.contains(q, true) || it.group.orEmpty().contains(q, true) }; Column(Modifier.fillMaxSize()) { Text("Suche", color = V5Text, fontSize = 34.sp, fontWeight = FontWeight.Black); V5TvInput("Sender suchen", q, { q = it }, accent, keyboard); Spacer(Modifier.height(12.dp)); LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(filtered) { V5Channel(it, accent, onPlay) } } } }

@Composable private fun V5Settings(settings: SettingsStore, accent: Color, onSources: () -> Unit) { var tab by remember { mutableIntStateOf(0) }; val tabs = listOf("Player", "Live TV", "EPG", "Darstellung", "Quellen"); Column(Modifier.fillMaxSize()) { Text("Einstellungen", color = V5Text, fontSize = 34.sp, fontWeight = FontWeight.Black); Text("Alles mit D-Pad + OK bedienbar", color = V5Muted, fontSize = 13.sp); Spacer(Modifier.height(15.dp)); Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { tabs.forEachIndexed { i, t -> V5Pill(t, accent) { tab = i } } }; Spacer(Modifier.height(12.dp)); Column(Modifier.fillMaxWidth().background(Color.White.copy(.05f), RoundedCornerShape(20.dp)).padding(18.dp)) { when (tab) { 0 -> { Text("Player", color = V5Text, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("Engine: ${settings.player.engine.label}", color = V5Muted); Text("Buffer: ${settings.player.bufferMode.label}", color = V5Muted) }; 1 -> { Text("Live TV", color = V5Text, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("Shared Catch-up: ${if (settings.player.preferCatchupSibling) "an" else "aus"}", color = V5Muted) }; 2 -> { Text("EPG", color = V5Text, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("Zeitraum: ${settings.epg.pageSize.label}", color = V5Muted) }; 3 -> { Text("Darstellung", color = V5Text, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("Theme: ${settings.ui.theme.label}", color = V5Muted); Text("Glasstärke: ${settings.ui.glassIntensity}/10", color = V5Muted) }; else -> { Text("Quellen", color = V5Text, fontSize = 18.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); V5SourceButton("Quellen öffnen", accent, onSources) } } } } }

private fun initialsV5(name: String): String = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercaseChar().toString() }
