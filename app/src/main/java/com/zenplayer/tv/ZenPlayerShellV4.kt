package com.zenplayer.tv

import android.net.Uri
import android.os.SystemClock
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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.zenplayer.tv.domain.model.Channel
import com.zenplayer.tv.domain.model.EpgProgramme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val V4Text = Color(0xFFF7F8FC)
private val V4Muted = Color(0xFF8D96AA)
private val V4Base = Color(0xFF05070D)

@Composable
fun ZenPlayerShellV4(settings: SettingsStore) {
    val context = LocalContext.current
    val store = remember { PlaylistStore(context) }
    val accent = when (settings.ui.theme) {
        ZenTheme.AURORA -> Color(0xFF70E6FF)
        ZenTheme.OBSIDIAN -> Color(0xFFAAA8FF)
        ZenTheme.FROST -> Color(0xFF9FEAFF)
        ZenTheme.AMBER -> Color(0xFFFFC46E)
    }
    var page by remember { mutableStateOf("home") }
    var sourceOpen by remember { mutableStateOf(false) }
    var playerIndex by remember { mutableIntStateOf(-1) }
    var preview by remember { mutableStateOf<Channel?>(null) }
    var exitDialog by remember { mutableStateOf(false) }
    val sidebarRequesters = remember { List(4) { FocusRequester() } }
    val ids = listOf("home", "epg", "search", "settings")
    val channels = store.channels

    LaunchedEffect(page, sourceOpen, playerIndex) {
        if (!sourceOpen && playerIndex < 0) {
            val index = ids.indexOf(page).coerceAtLeast(0)
            runCatching { sidebarRequesters[index].requestFocus() }
        }
    }

    androidx.activity.compose.BackHandler {
        when {
            exitDialog -> exitDialog = false
            preview != null -> preview = null
            sourceOpen -> sourceOpen = false
            playerIndex >= 0 -> playerIndex = -1
            page != "home" -> runCatching { sidebarRequesters[ids.indexOf(page).coerceAtLeast(0)].requestFocus() }
            else -> exitDialog = true
        }
    }

    Box(Modifier.fillMaxSize().background(V4Base)) {
        ZenAnimatedBackdrop(settings.ui.theme, settings.ui.animatedBackdrop, !settings.ui.reducedMotion)
        when {
            sourceOpen -> V4Sources(store, settings, accent, onDone = { sourceOpen = false })
            playerIndex >= 0 && playerIndex < channels.size -> V4Player(channels, playerIndex, settings, accent)
            else -> Row(Modifier.fillMaxSize().padding(22.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                V4Sidebar(page, accent, ids, sidebarRequesters) { page = it }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    when (page) {
                        "home" -> V4Home(channels, accent, settings.ui.glassIntensity) { playerIndex = channels.indexOf(it) }
                        "epg" -> V4Epg(store, accent, settings.ui.glassIntensity, preview) { channel, second -> if (second) { playerIndex = channels.indexOf(channel); preview = null } else preview = channel }
                        "search" -> V4Search(channels, accent) { playerIndex = channels.indexOf(it) }
                        else -> V4Settings(settings, accent, settings.ui.glassIntensity, onSources = { sourceOpen = true })
                    }
                }
            }
        }
        if (exitDialog) {
            AlertDialog(
                onDismissRequest = { exitDialog = false },
                title = { Text("ZenPlayer schließen?") },
                text = { Text("Möchtest du ZenPlayer wirklich beenden?") },
                confirmButton = { TextButton(onClick = { (context as? android.app.Activity)?.finish() }) { Text("Beenden") } },
                dismissButton = { TextButton(onClick = { exitDialog = false }) { Text("Abbrechen") } }
            )
        }
    }
}

@Composable
private fun V4Sidebar(page: String, accent: Color, ids: List<String>, requesters: List<FocusRequester>, onPage: (String) -> Unit) {
    val icons = listOf(Icons.Default.Home, Icons.Default.PlayArrow, Icons.Default.Search, Icons.Default.Settings)
    val labels = listOf("Home", "EPG", "Suche", "Settings")
    Column(Modifier.width(74.dp).fillMaxHeight().background(Color.White.copy(.045f), RoundedCornerShape(24.dp)).border(1.dp, Color.White.copy(.12f), RoundedCornerShape(24.dp)).padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.width(54.dp).height(54.dp).background(accent.copy(.16f), RoundedCornerShape(17.dp)), Alignment.Center) { Text("Z", color = V4Text, fontSize = 24.sp, fontWeight = FontWeight.Black) }
        Spacer(Modifier.height(14.dp))
        ids.forEachIndexed { i, id ->
            V4NavButton(labels[i], icons[i], page == id, accent, requesters[i]) { onPage(id) }
            if (i < ids.lastIndex) Spacer(Modifier.height(9.dp))
        }
    }
}

@Composable
private fun V4NavButton(label: String, icon: ImageVector, selected: Boolean, accent: Color, requester: FocusRequester, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.width(54.dp).height(54.dp).focusRequester(requester).focusable().onFocusChanged { focused = it.isFocused }.background(if (selected || focused) accent.copy(.17f) else Color.Transparent, RoundedCornerShape(17.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent.copy(.9f) else if (selected) accent.copy(.45f) else Color.Transparent, RoundedCornerShape(17.dp)).tvAction(onClick), Alignment.Center) {
        Icon(icon, label, tint = if (selected || focused) V4Text else V4Muted)
    }
}

@Composable
private fun V4Home(channels: List<Channel>, accent: Color, glass: Int, onPlay: (Channel) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        V4Header("Home", "Live-TV · schnell · remote-first")
        Spacer(Modifier.height(18.dp))
        if (channels.isEmpty()) {
            V4Card("Noch keine Quelle", "Füge eine M3U/M3U8-Playlist oder Xtream Codes hinzu.", accent) {}
        } else {
            Text("${channels.size} Sender", color = V4Muted, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(channels) { V4Channel(it, accent, glass, onPlay) }
            }
        }
    }
}

@Composable
private fun V4Header(title: String, subtitle: String) {
    Text(title, color = V4Text, fontSize = 34.sp, fontWeight = FontWeight.Black)
    Text(subtitle, color = V4Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 3.dp))
}

@Composable
private fun V4Channel(channel: Channel, accent: Color, glass: Int, onPlay: (Channel) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(76.dp).focusable().onFocusChanged { focused = it.isFocused }.background(if (focused) accent.copy(.13f) else Color.White.copy(.045f), RoundedCornerShape(18.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent.copy(.8f) else Color.White.copy(.1f), RoundedCornerShape(18.dp)).tvAction { onPlay(channel) }.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        if (!channel.logoUrl.isNullOrBlank()) AsyncImage(channel.logoUrl, channel.name, Modifier.width(48.dp).height(48.dp), contentScale = ContentScale.Fit) else Box(Modifier.width(48.dp).height(48.dp).background(accent.copy(.12f), RoundedCornerShape(13.dp)), Alignment.Center) { Text(initials3(channel.name), color = accent, fontWeight = FontWeight.Bold) }
        Column(Modifier.weight(1f).padding(start = 14.dp)) { Text(channel.name, color = V4Text, fontSize = 16.sp, fontWeight = FontWeight.Bold); Text(channel.group ?: "Live TV", color = V4Muted, fontSize = 11.sp) }
        Text(channel.resolutionHint ?: "LIVE", color = if (focused) accent else V4Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun V4Epg(store: PlaylistStore, accent: Color, glass: Int, preview: Channel?, onProgram: (Channel, Boolean) -> Unit) {
    val channels = store.channels
    Column(Modifier.fillMaxSize()) {
        V4Header("EPG", "Sender links · Programme kompakt · OK = Vorschau · OK erneut = Fullscreen")
        Spacer(Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
            items(channels) { channel ->
                val programmes = store.programmes.filter { it.channelId == channel.id }.sortedBy { it.start }.take(12)
                V4EpgRow(channel, programmes, accent, glass, preview == channel, onProgram)
            }
        }
    }
}

@Composable
private fun V4EpgRow(channel: Channel, programmes: List<EpgProgramme>, accent: Color, glass: Int, selected: Boolean, onProgram: (Channel, Boolean) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White.copy(.04f), RoundedCornerShape(18.dp)).border(if (selected) 1.dp else 0.dp, accent.copy(.45f), RoundedCornerShape(18.dp)).padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!channel.logoUrl.isNullOrBlank()) AsyncImage(channel.logoUrl, channel.name, Modifier.width(52.dp).height(52.dp), contentScale = ContentScale.Fit) else Box(Modifier.width(52.dp).height(52.dp).background(accent.copy(.12f), RoundedCornerShape(14.dp)), Alignment.Center) { Text(initials3(channel.name), color = accent, fontWeight = FontWeight.Bold) }
            Column(Modifier.padding(start = 11.dp).width(150.dp)) { Text(channel.name, color = V4Text, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text("LIVE", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(end = 6.dp)) {
                items(programmes) { p ->
                    var focused by remember { mutableStateOf(false) }
                    val same = selected
                    Column(Modifier.width(148.dp).height(64.dp).focusable().onFocusChanged { focused = it.isFocused }.background(if (focused || same) accent.copy(.12f) else Color.White.copy(.035f), RoundedCornerShape(12.dp)).border(if (focused) 1.dp else 0.dp, accent.copy(.65f), RoundedCornerShape(12.dp)).tvAction { onProgram(channel, selected) }.padding(horizontal = 9.dp, vertical = 7.dp)) {
                        Text(formatTime(p.start), color = if (focused) accent else V4Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(p.title, color = V4Text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                    }
                }
            }
        }
        if (selected) {
            Spacer(Modifier.height(8.dp))
            Text("Vorschau aktiv · OK erneut öffnet ${channel.name} fullscreen", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun V4Search(channels: List<Channel>, accent: Color, onPlay: (Channel) -> Unit) {
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    val requester = remember { FocusRequester() }
    val filtered = channels.filter { it.name.contains(query, true) || it.group.orEmpty().contains(query, true) }
    Column(Modifier.fillMaxSize()) {
        V4Header("Suche", "OK im Feld öffnet die Tastatur")
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(query, { query = it }, label = { Text("Sender suchen") }, readOnly = !editing, singleLine = true, colors = V4FieldColors(accent), modifier = Modifier.fillMaxWidth().focusRequester(requester).onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && (e.key == Key.Enter || e.key == Key.DirectionCenter)) { editing = true; true } else false })
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(filtered) { V4Channel(it, accent, 5, onPlay) } }
    }
}

@Composable
private fun V4Sources(store: PlaylistStore, settings: SettingsStore, accent: Color, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        V4Header("Quelle hinzufügen", "Fokus bewegt sich mit dem D-Pad · OK öffnet erst dann die Tastatur")
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) scope.launch { status = "Playlist wird eingelesen…"; runCatching { store.importPlaylistFromUri(uri) }.onSuccess { status = "${store.channels.size} Sender importiert"; onDone() }.onFailure { status = it.message ?: "M3U konnte nicht gelesen werden" } }
            }
            V4SourceCard("M3U / M3U8", accent, Modifier.weight(1f)) {
                V4Action("Datei auswählen", accent) { picker.launch(arrayOf("*/*")) }
                V4EditableField("M3U / M3U8 URL", url, { url = it }, accent)
                V4Action("URL importieren", accent) { scope.launch { status = "URL wird geladen…"; runCatching { store.importPlaylistFromUrl(url.trim()) }.onSuccess { status = "${store.channels.size} Sender importiert"; onDone() }.onFailure { status = it.message ?: "URL konnte nicht geladen werden" } } }
            }
            V4SourceCard("Xtream Codes", accent, Modifier.weight(1f)) {
                V4EditableField("Server URL oder Host", server, { server = it }, accent)
                V4EditableField("Benutzername", user, { user = it }, accent)
                V4EditableField("Passwort", pass, { pass = it }, accent)
                V4Action("Xtream verbinden", accent) { scope.launch { status = "Xtream wird verbunden…"; XtreamClient(server.trim(), user.trim(), pass).load().onSuccess { list -> store.importXtream(list); status = "${list.size} Sender importiert"; onDone() }.onFailure { status = it.message ?: "Xtream-Verbindung fehlgeschlagen" } } }
            }
        }
        if (status.isNotBlank()) Text(status, color = accent, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun V4SourceCard(title: String, accent: Color, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.background(Color.White.copy(.045f), RoundedCornerShape(22.dp)).border(1.dp, Color.White.copy(.1f), RoundedCornerShape(22.dp)).padding(16.dp)) {
        Text(title, color = V4Text, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun V4EditableField(label: String, value: String, onValue: (String) -> Unit, accent: Color) {
    var editing by remember { mutableStateOf(false) }
    val requester = remember { FocusRequester() }
    OutlinedTextField(value, onValue, label = { Text(label) }, readOnly = !editing, singleLine = true, colors = V4FieldColors(accent), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).focusRequester(requester).onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && (e.key == Key.Enter || e.key == Key.DirectionCenter)) { editing = true; runCatching { requester.requestFocus() }; true } else false })
}

@Composable
private fun V4Action(title: String, accent: Color, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().height(46.dp).focusable().onFocusChanged { focused = it.isFocused }.background(if (focused) accent.copy(.15f) else Color.White.copy(.04f), RoundedCornerShape(13.dp)).border(if (focused) 1.dp else 0.dp, accent.copy(.7f), RoundedCornerShape(13.dp)).tvAction(onClick), Alignment.CenterStart) { Text(title, color = V4Text, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 13.dp)) }
}

@Composable
private fun V4Card(title: String, subtitle: String, accent: Color, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.width(520.dp).height(130.dp).focusable().onFocusChanged { focused = it.isFocused }.background(if (focused) accent.copy(.13f) else Color.White.copy(.045f), RoundedCornerShape(22.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(.1f), RoundedCornerShape(22.dp)).tvAction(onClick).padding(20.dp)) { Text(title, color = V4Text, fontSize = 18.sp, fontWeight = FontWeight.Black); Text(subtitle, color = V4Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 7.dp)) }
}

@Composable
private fun V4Settings(settings: SettingsStore, accent: Color, glass: Int, onSources: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        V4Header("Einstellungen", "D-Pad · OK · Back · ohne Fokus-Fallen")
        Spacer(Modifier.height(12.dp))
        V4Action("Quellen verwalten", accent, onSources)
        Spacer(Modifier.height(8.dp))
        V4Action("Theme: ${settings.ui.theme.label}", accent) { settings.updateUi { it.copy(theme = ZenTheme.values()[(ZenTheme.values().indexOf(settings.ui.theme) + 1) % ZenTheme.values().size]) } }
        Spacer(Modifier.height(8.dp))
        V4Action("Glasstärke: ${settings.ui.glassIntensity}", accent) { settings.updateUi { it.copy(glassIntensity = if (it.glassIntensity >= 10) 1 else it.glassIntensity + 1) } }
        Spacer(Modifier.height(8.dp))
        V4Action("UI-Skalierung: ${settings.ui.uiScale}%", accent) { settings.updateUi { it.copy(uiScale = if (it.uiScale >= 125) 75 else it.uiScale + 5) } }
    }
}

@Composable
private fun V4Player(channels: List<Channel>, index: Int, settings: SettingsStore, accent: Color) {
    val context = LocalContext.current
    val player = remember(channels[index].streamUrl) { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(Uri.parse(channels[index].streamUrl))); prepare(); playWhenReady = settings.player.startLiveImmediately } }
    DisposablePlayer(player)
    val focusRequester = remember { FocusRequester() }
    var controls by remember { mutableStateOf(true) }
    LaunchedEffect(index) { runCatching { focusRequester.requestFocus() } }
    Box(Modifier.fillMaxSize().background(Color.Black).focusRequester(focusRequester).focusable().onKeyEvent { e ->
        if (e.type != KeyEventType.KeyUp) return@onKeyEvent true
        when (e.key) {
            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { controls = !controls; true }
            Key.MediaPlayPause -> { player.playWhenReady = !player.isPlaying; true }
            Key.DirectionLeft -> { player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0)); true }
            Key.DirectionRight -> { player.seekTo((player.currentPosition + 10_000).coerceAtMost(player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE)); true }
            Key.DirectionUp -> { true }
            Key.DirectionDown -> { true }
            else -> false
        }
    }) {
        AndroidView(factory = { PlayerView(it).apply { useController = false; this.player = player; isFocusable = false } }, update = { it.player = player }, modifier = Modifier.fillMaxSize())
        if (controls) {
            Box(Modifier.fillMaxWidth().align(Alignment.BottomCenter).background(Color.Black.copy(.72f)).padding(horizontal = 28.dp, vertical = 20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) { Text(channels[index].name, color = V4Text, fontSize = 22.sp, fontWeight = FontWeight.Black); Text("OK: Overlay · ←/→: 10s · Back: Overlay schließen", color = V4Muted, fontSize = 11.sp) }
                    V4Action("${if (player.isPlaying) "PAUSE" else "PLAY"}", accent) { player.playWhenReady = !player.isPlaying }
                }
            }
        }
    }
}

@Composable
private fun DisposablePlayer(player: ExoPlayer) {
    androidx.compose.runtime.DisposableEffect(player) { onDispose { player.release() } }
}

@Composable
private fun V4FieldColors(accent: Color) = OutlinedTextFieldDefaults.colors(focusedTextColor = V4Text, unfocusedTextColor = V4Text, focusedLabelColor = accent, unfocusedLabelColor = V4Muted, cursorColor = accent, focusedBorderColor = accent, unfocusedBorderColor = Color.White.copy(.2f))

private fun formatTime(value: Long): String = runCatching { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(value)) }.getOrDefault("--:--")
