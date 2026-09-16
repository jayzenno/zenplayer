package com.zenplayer.tv

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.zenplayer.tv.domain.model.EpgProgramme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val V6Text = Color(0xFFF7F8FC)
private val V6Muted = Color(0xFF8D96AA)
private val V6Bg = Color(0xFF05070D)
private val V6Surface = Color(0xE6111722)

@Composable
fun ZenPlayerShellV6(settings: SettingsStore) {
    val store = remember { PlaylistStore(settings) }
    var page by remember { mutableStateOf("home") }
    var demo by remember { mutableStateOf(false) }
    var playerIndex by remember { mutableIntStateOf(0) }
    var playerActive by remember { mutableStateOf(false) }
    var sources by remember { mutableStateOf(false) }
    var sourceReturn by remember { mutableStateOf("home") }
    var zapUntil by remember { mutableLongStateOf(0L) }
    val channels = if (demo) DemoData.channels() else store.channels
    val accent = Color(0xFF7C8CFF)

    fun openSources(returnPage: String) {
        sourceReturn = returnPage
        sources = true
    }
    fun playChannel(channel: Channel) {
        val list = if (demo) DemoData.channels() else store.channels
        playerIndex = list.indexOfFirst { it.id == channel.id }.coerceAtLeast(0)
        playerActive = true
        zapUntil = System.currentTimeMillis() + 2800L
    }
    fun closePlayer() { playerActive = false }

    BackHandler {
        when {
            playerActive -> closePlayer()
            sources -> { sources = false; page = sourceReturn }
            page != "home" -> page = "home"
            demo -> demo = false
            else -> Unit
        }
    }

    Box(Modifier.fillMaxSize().background(V6Bg)) {
        ZenAnimatedBackdrop(settings)
        Row(Modifier.fillMaxSize().padding(18.dp)) {
            V6Sidebar(page, accent) { page = it }
            Spacer(Modifier.width(22.dp))
            Box(Modifier.weight(1f).fillMaxHeight()) {
                when {
                    sources -> V6Sources(store, accent, { sources = false; page = sourceReturn }, { sources = false; page = sourceReturn })
                    playerActive && channels.isNotEmpty() -> {
                        val safeIndex = playerIndex.coerceIn(0, channels.lastIndex)
                        val current = channels[safeIndex]
                        ZenPlayerScreen(
                            channel = current,
                            settings = settings,
                            onBack = ::closePlayer,
                            showChrome = false,
                            onRemoteKey = { key ->
                                when (key) {
                                    Key.DirectionUp, Key.DirectionDown -> {
                                        if (channels.isNotEmpty()) {
                                            val delta = if (key == Key.DirectionUp) -1 else 1
                                            playerIndex = (safeIndex + delta + channels.size) % channels.size
                                            zapUntil = System.currentTimeMillis() + 2800L
                                        }
                                        true
                                    }
                                    else -> false
                                }
                            }
                        )
                        V6ZapOverlay(current, accent, zapUntil)
                    }
                    page == "home" -> V6Home(store, demo, accent, { openSources("home") }, { demo = true }, { demo = false }, ::playChannel)
                    page == "epg" -> V6Epg(store, demo, accent, ::playChannel)
                    page == "search" -> V6Search(store, demo, accent, ::playChannel)
                    page == "settings" -> V6Settings(settings, accent) { openSources("settings") }
                    else -> V6Home(store, demo, accent, { openSources("home") }, { demo = true }, { demo = false }, ::playChannel)
                }
            }
        }
    }
}

@Composable
private fun V6Sidebar(page: String, accent: Color, onPage: (String) -> Unit) {
    val items = listOf("home", "epg", "search", "settings")
    val labels = listOf("Home", "EPG", "Suche", "Settings")
    val icons = listOf(Icons.Default.Home, Icons.Default.PlayArrow, Icons.Default.Search, Icons.Default.Settings)
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    Column(Modifier.width(66.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
        items.forEachIndexed { i, id ->
            var focused by remember { mutableStateOf(false) }
            val active = focused || page == id
            Box(Modifier.width(58.dp).height(58.dp)
                .then(if (i == 0) Modifier.focusRequester(first) else Modifier)
                .focusable().onFocusChanged { focused = it.isFocused }
                .onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key == Key.DirectionCenter) { onPage(id); true } else false }
                .background(if (active) accent.copy(.18f) else Color.Transparent, RoundedCornerShape(18.dp))
                .border(if (focused) 2.dp else 1.dp, if (active) accent.copy(.85f) else Color.Transparent, RoundedCornerShape(18.dp)), Alignment.Center) {
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
        if (channels.isEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                V6Action("Playlist hinzufügen", "M3U / URL / Xtream", Icons.Default.Source, accent, onSources)
                V6Action("Demo starten", "Ohne Quelle testen", Icons.Default.PlayArrow, accent, onDemo)
            }
        } else {
            V6Action("Quelle verwalten", "Playlist / Xtream", Icons.Default.Source, accent, onSources)
            Spacer(Modifier.height(14.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 24.dp)) { items(channels) { channel -> V6Channel(channel, accent) { onPlay(channel) } } }
        }
    }
}

@Composable
private fun V6Action(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, action: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.width(360.dp).height(96.dp).focusable().onFocusChanged { focused = it.isFocused }
        .onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key == Key.DirectionCenter) { action(); true } else false }
        .background(if (focused) accent.copy(.16f) else Color.White.copy(.055f), RoundedCornerShape(20.dp))
        .border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(.08f), RoundedCornerShape(20.dp)).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = accent)
        Column(Modifier.padding(start = 14.dp)) { Text(title, color = V6Text, fontSize = 16.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = V6Muted, fontSize = 11.sp) }
    }
}

@Composable
private fun V6Channel(channel: Channel, accent: Color, action: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(72.dp).focusable().onFocusChanged { focused = it.isFocused }
        .onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key == Key.DirectionCenter) { action(); true } else false }
        .background(if (focused) accent.copy(.13f) else Color.White.copy(.05f), RoundedCornerShape(18.dp))
        .border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(.08f), RoundedCornerShape(18.dp)).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        if (!channel.logoUrl.isNullOrBlank()) AsyncImage(channel.logoUrl, channel.name, Modifier.width(48.dp).height(48.dp), contentScale = ContentScale.Fit)
        else Box(Modifier.width(48.dp).height(48.dp).background(accent.copy(.12f), RoundedCornerShape(13.dp)), Alignment.Center) { Text(initialsV6(channel.name), color = accent, fontWeight = FontWeight.Bold) }
        Column(Modifier.weight(1f).padding(start = 14.dp)) { Text(channel.name, color = V6Text, fontSize = 15.sp, fontWeight = FontWeight.Bold); Text(channel.group ?: "Live TV", color = V6Muted, fontSize = 10.sp) }
        Text(if (channel.isCatchupCapable) "CATCHUP" else "LIVE", color = if (focused) accent else V6Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun V6ZapOverlay(channel: Channel, accent: Color, visibleUntil: Long) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(visibleUntil) { while (visibleUntil > System.currentTimeMillis()) { now = System.currentTimeMillis(); delay(80) } }
    if (visibleUntil == 0L || visibleUntil <= now) return
    Box(Modifier.fillMaxSize().padding(start = 28.dp, top = 28.dp), contentAlignment = Alignment.TopStart) {
        Row(Modifier.width(430.dp).background(Color(0xEC111721), RoundedCornerShape(24.dp)).border(1.dp, accent.copy(.55f), RoundedCornerShape(24.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!channel.logoUrl.isNullOrBlank()) AsyncImage(channel.logoUrl, channel.name, Modifier.width(68.dp).height(68.dp), contentScale = ContentScale.Fit)
            else Box(Modifier.width(68.dp).height(68.dp).background(accent.copy(.12f), RoundedCornerShape(16.dp)), Alignment.Center) { Text(initialsV6(channel.name), color = accent, fontSize = 18.sp, fontWeight = FontWeight.Black) }
            Column(Modifier.padding(start = 16.dp)) {
                Text(channel.name, color = V6Text, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Text(channel.group ?: "Live TV", color = V6Muted, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (channel.isCatchupCapable) "CATCHUP" else "● LIVE", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text(SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date()), color = V6Muted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun V6Epg(store: PlaylistStore, demo: Boolean, accent: Color, onPlay: (Channel) -> Unit) {
    val channels = if (demo) DemoData.channels() else store.channels
    val programmes = if (demo) DemoData.programmes() else store.programmes
    var preview by remember { mutableStateOf<Pair<Channel, EpgProgramme>?>(null) }
    Column(Modifier.fillMaxSize()) {
        Text("EPG", color = V6Text, fontSize = 34.sp, fontWeight = FontWeight.Black)
        Text("OK = Vorschau · OK auf Vorschau = Vollbild", color = V6Muted, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            channels.forEach { channel ->
                val ps = programmes.filter { it.channelId == channel.id }.sortedBy { it.start }.take(8)
                if (ps.isNotEmpty()) {
                    item { Text(channel.name, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp)) }
                    items(ps) { p ->
                        var focused by remember(p.id) { mutableStateOf(false) }
                        Row(Modifier.fillMaxWidth().height(58.dp).focusable().onFocusChanged { focused = it.isFocused }
                            .onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key == Key.DirectionCenter) { if (preview?.second?.id == p.id) onPlay(channel) else preview = channel to p; true } else false }
                            .background(if (focused) accent.copy(.13f) else Color.White.copy(.05f), RoundedCornerShape(14.dp))
                            .border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.Transparent, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(p.start)), color = accent, fontSize = 11.sp, modifier = Modifier.width(52.dp))
                            Column(Modifier.weight(1f)) { Text(p.title, color = V6Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Text(p.category ?: "TV", color = V6Muted, fontSize = 9.sp) }
                            if (p.isCatchupAvailable) Text("REPLAY", color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
    preview?.let { (channel, p) ->
        Box(Modifier.fillMaxSize().background(Color.Black.copy(.62f)), contentAlignment = Alignment.Center) {
            Column(Modifier.width(520.dp).background(V6Surface, RoundedCornerShape(24.dp)).border(1.dp, accent.copy(.45f), RoundedCornerShape(24.dp)).padding(24.dp)) {
                Text(p.title, color = V6Text, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text(channel.name, color = accent, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Text("Noch einmal OK zum Abspielen", color = V6Muted, fontSize = 12.sp)
                Text("Zurück schließt die Vorschau", color = V6Muted, fontSize = 10.sp)
            }
        }
        BackHandler { preview = null }
    }
}

@Composable
private fun V6Search(store: PlaylistStore, demo: Boolean, accent: Color, onPlay: (Channel) -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    val channels = if (demo) DemoData.channels() else store.channels
    val filtered = channels.filter { query.isBlank() || it.name.contains(query, true) || it.group.orEmpty().contains(query, true) }
    Column(Modifier.fillMaxSize()) {
        Text("Suche", color = V6Text, fontSize = 34.sp, fontWeight = FontWeight.Black)
        Text("Fokus öffnet nichts · erst OK startet die Eingabe", color = V6Muted, fontSize = 13.sp)
        OutlinedTextField(query, { query = it }, label = { Text("Sender suchen…") }, readOnly = !editing, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).onFocusChanged { keyboard?.hide() }
                .onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key == Key.DirectionCenter) { editing = true; keyboard?.show(); true } else false })
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(filtered) { V6Channel(it, accent) { onPlay(it) } } }
    }
}

@Composable
private fun V6Sources(store: PlaylistStore, accent: Color, onBack: () -> Unit, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            V6SourceCard("M3U / M3U8", accent, Modifier.weight(1f)) {
                V6SourceButton("Datei auswählen", accent) { picker.launch(arrayOf("*/*")) }
                V6TvInput("M3U / M3U8 URL", url, { url = it }, "url", editing, { editing = "url"; keyboard?.show() }, keyboard)
                V6SourceButton("URL importieren", accent) { scope.launch { status = "URL wird geladen…"; store.importPlaylistFromUrl(url.trim()).onSuccess { status = "${store.channels.size} Sender importiert"; onDone() }.onFailure { status = it.message ?: "M3U konnte nicht geladen werden" } } }
            }
            V6SourceCard("Xtream Codes", accent, Modifier.weight(1f)) {
                V6TvInput("Server URL oder Host", server, { server = it }, "server", editing, { editing = "server"; keyboard?.show() }, keyboard)
                V6TvInput("Benutzername", user, { user = it }, "user", editing, { editing = "user"; keyboard?.show() }, keyboard)
                V6TvInput("Passwort", pass, { pass = it }, "pass", editing, { editing = "pass"; keyboard?.show() }, keyboard)
                V6SourceButton("Xtream verbinden", accent) {
                    scope.launch {
                        status = "Xtream wird verbunden…"
                        val result = XtreamClient(server.trim(), user.trim(), pass).load()
                        if (result.isSuccess) {
                            val list = result.getOrThrow()
                            store.importXtream(list)
                            status = "${list.size} Sender importiert"
                            onDone()
                        } else {
                            status = result.exceptionOrNull()?.message ?: "Xtream-Verbindung fehlgeschlagen"
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp)); Text(status, color = accent, fontSize = 12.sp); Spacer(Modifier.height(12.dp)); V6SourceButton("Zurück", accent, onBack)
    }
}

@Composable
private fun V6TvInput(label: String, value: String, onValue: (String) -> Unit, id: String, editing: String, startEditing: () -> Unit, keyboard: androidx.compose.ui.platform.SoftwareKeyboardController?) {
    OutlinedTextField(value, onValue, label = { Text(label) }, singleLine = true, readOnly = editing != id,
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).onFocusChanged { keyboard?.hide() }
            .onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key == Key.DirectionCenter) { startEditing(); true } else false })
}

@Composable
private fun V6SourceCard(title: String, accent: Color, modifier: Modifier, content: @Composable () -> Unit) {
    Column(modifier.background(Color.White.copy(.045f), RoundedCornerShape(22.dp)).border(1.dp, Color.White.copy(.08f), RoundedCornerShape(22.dp)).padding(18.dp)) {
        Text(title, color = V6Text, fontSize = 17.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(10.dp)); content()
    }
}

@Composable
private fun V6SourceButton(title: String, accent: Color, action: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().height(54.dp).focusable().onFocusChanged { focused = it.isFocused }
        .onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key == Key.DirectionCenter) { action(); true } else false }
        .background(if (focused) accent.copy(.18f) else Color.White.copy(.05f), RoundedCornerShape(15.dp))
        .border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(.08f), RoundedCornerShape(15.dp)), Alignment.Center) {
        Text(title, color = V6Text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun V6Settings(settings: SettingsStore, accent: Color, onSources: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text("Settings", color = V6Text, fontSize = 34.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(12.dp))
        Text("Player: ${settings.player.engine.label}", color = V6Muted, fontSize = 13.sp)
        Text("Buffer: ${settings.player.bufferMode.label}", color = V6Muted, fontSize = 13.sp)
        Text("Theme: ${settings.ui.theme.label}", color = V6Muted, fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))
        V6SourceButton("Quellen verwalten", accent, onSources)
    }
}

private fun initialsV6(name: String): String = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "TV" }
