package com.zenplayer.tv

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
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
    val focusManager = LocalFocusManager.current
    val accent = when (settings.ui.theme) {
        ZenTheme.AURORA -> Color(0xFF70E6FF)
        ZenTheme.OBSIDIAN -> Color(0xFFAAA8FF)
        ZenTheme.FROST -> Color(0xFF9FEAFF)
        ZenTheme.AMBER -> Color(0xFFFFC46E)
    }
    val ids = listOf("home", "epg", "search", "settings")
    val requesters = remember { List(4) { FocusRequester() } }
    var page by remember { mutableStateOf("home") }
    var sourceOpen by remember { mutableStateOf(false) }
    var playerIndex by remember { mutableIntStateOf(-1) }
    var preview by remember { mutableStateOf<Channel?>(null) }
    var exitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(page, sourceOpen, playerIndex) {
        if (!sourceOpen && playerIndex < 0) runCatching { requesters[ids.indexOf(page).coerceAtLeast(0)].requestFocus() }
    }

    BackHandler {
        when {
            exitDialog -> exitDialog = false
            playerIndex >= 0 -> playerIndex = -1
            sourceOpen -> { sourceOpen = false; runCatching { requesters[ids.indexOf(page).coerceAtLeast(0)].requestFocus() } }
            preview != null -> preview = null
            page != "home" -> { page = "home"; runCatching { requesters[0].requestFocus() } }
            else -> exitDialog = true
        }
    }

    Box(Modifier.fillMaxSize().background(V4Base)) {
        ZenAnimatedBackdrop(settings.ui.theme, settings.ui.animatedBackdrop, !settings.ui.reducedMotion)
        when {
            sourceOpen -> V4Sources(store, accent) { sourceOpen = false }
            playerIndex in store.channels.indices -> V4Player(
                channels = store.channels,
                index = playerIndex,
                settings = settings,
                accent = accent,
                onSwitch = { next -> playerIndex = next.coerceIn(0, store.channels.lastIndex) },
                onClose = { playerIndex = -1 }
            )
            else -> Row(
                Modifier.fillMaxSize().padding(22.dp)
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
                        if (event.key == Key.DirectionLeft && page != "epg") {
                            val moved = focusManager.moveFocus(FocusDirection.Left)
                            if (!moved) runCatching { requesters[ids.indexOf(page).coerceAtLeast(0)].requestFocus() }
                            moved
                        } else false
                    },
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                V4Sidebar(page, ids, accent, requesters) { page = it; preview = null }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    when (page) {
                        "home" -> V4Home(store.channels, accent, settings.ui.glassIntensity) { playerIndex = store.channels.indexOf(it) }
                        "epg" -> V4Epg(store, accent, preview) { channel, second ->
                            if (second) { playerIndex = store.channels.indexOf(channel); preview = null }
                            else preview = channel
                        }
                        "search" -> V4Search(store.channels, accent) { playerIndex = store.channels.indexOf(it) }
                        else -> V4Settings(accent) { sourceOpen = true }
                    }
                }
            }
        }
        if (exitDialog) AlertDialog(
            onDismissRequest = { exitDialog = false },
            title = { Text("ZenPlayer schließen?") },
            text = { Text("Möchtest du ZenPlayer wirklich beenden?") },
            confirmButton = { TextButton(onClick = { (context as? android.app.Activity)?.finish() }) { Text("Beenden") } },
            dismissButton = { TextButton(onClick = { exitDialog = false }) { Text("Abbrechen") } }
        )
    }
}

@Composable
private fun V4Sidebar(page: String, ids: List<String>, accent: Color, requesters: List<FocusRequester>, onPage: (String) -> Unit) {
    val icons = listOf(Icons.Default.Home, Icons.Default.PlayArrow, Icons.Default.Search, Icons.Default.Settings)
    val labels = listOf("Home", "EPG", "Suche", "Settings")
    Column(
        Modifier.width(74.dp).fillMaxHeight().focusGroup()
            .background(Color.White.copy(.045f), RoundedCornerShape(24.dp))
            .border(1.dp, Color.White.copy(.12f), RoundedCornerShape(24.dp)).padding(9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.width(54.dp).height(54.dp).background(accent.copy(.16f), RoundedCornerShape(17.dp)), Alignment.Center) { Text("Z", color = V4Text, fontSize = 24.sp, fontWeight = FontWeight.Black) }
        Spacer(Modifier.height(14.dp))
        ids.forEachIndexed { i, id ->
            V4NavButton(labels[i], icons[i], page == id, accent, requesters[i]) { onPage(id) }
            if (i != ids.lastIndex) Spacer(Modifier.height(9.dp))
        }
    }
}

@Composable
private fun V4NavButton(label: String, icon: ImageVector, selected: Boolean, accent: Color, requester: FocusRequester, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.width(54.dp).height(54.dp).focusRequester(requester).focusable().onFocusChanged { focused = it.isFocused }
        .background(if (focused) accent.copy(.20f) else if (selected) accent.copy(.13f) else Color.Transparent, RoundedCornerShape(17.dp))
        .border(if (focused) 2.dp else 0.dp, accent.copy(.8f), RoundedCornerShape(17.dp)).tvAction(onClick), Alignment.Center) {
        Icon(icon, label, tint = if (focused || selected) V4Text else V4Muted)
    }
}

@Composable
private fun V4Home(channels: List<Channel>, accent: Color, glass: Int, onPlay: (Channel) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        V4Header("Home", "Live-TV · ein OK zum Starten")
        Spacer(Modifier.height(16.dp))
        if (channels.isEmpty()) V4ActionCard("Noch keine Quelle", "M3U / M3U8 oder Xtream Codes hinzufügen", accent) {}
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) { items(channels) { V4Channel(it, accent, onPlay) } }
    }
}

@Composable private fun V4Header(title: String, subtitle: String) {
    Text(title, color = V4Text, fontSize = 34.sp, fontWeight = FontWeight.Black)
    Text(subtitle, color = V4Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 3.dp))
}

@Composable
private fun V4Channel(channel: Channel, accent: Color, onPlay: (Channel) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(74.dp).focusable().onFocusChanged { focused = it.isFocused }
        .background(if (focused) accent.copy(.12f) else Color.White.copy(.045f), RoundedCornerShape(17.dp))
        .border(if (focused) 1.dp else 0.dp, accent.copy(.7f), RoundedCornerShape(17.dp)).tvAction { onPlay(channel) }
        .padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        V4Logo(channel, accent)
        Column(Modifier.weight(1f).padding(start = 14.dp)) { Text(channel.name, color = V4Text, fontSize = 16.sp, fontWeight = FontWeight.Bold); Text(channel.group ?: "Live TV", color = V4Muted, fontSize = 11.sp) }
        Text(channel.resolutionHint ?: "LIVE", color = if (focused) accent else V4Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun V4Logo(channel: Channel, accent: Color) {
    if (!channel.logoUrl.isNullOrBlank()) AsyncImage(channel.logoUrl, channel.name, Modifier.width(48.dp).height(48.dp), contentScale = ContentScale.Fit)
    else Box(Modifier.width(48.dp).height(48.dp).background(accent.copy(.12f), RoundedCornerShape(13.dp)), Alignment.Center) { Text(v4Initials(channel.name), color = accent, fontWeight = FontWeight.Bold) }
}

@Composable
private fun V4Epg(store: PlaylistStore, accent: Color, preview: Channel?, onProgram: (Channel, Boolean) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        V4Header("EPG", "← → Timeline · 1× OK Vorschau · 2× OK Fullscreen")
        Spacer(Modifier.height(10.dp))
        if (preview != null) { V4Preview(preview, accent); Spacer(Modifier.height(9.dp)) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
            items(store.channels) { channel ->
                val programmes = store.programmes.filter { it.channelId == channel.id }.sortedBy { it.start }.take(14)
                V4EpgRow(channel, programmes, accent, preview == channel, onProgram)
            }
        }
    }
}

@Composable
private fun V4EpgRow(channel: Channel, programmes: List<EpgProgramme>, accent: Color, selected: Boolean, onProgram: (Channel, Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().height(82.dp).background(Color.White.copy(.038f), RoundedCornerShape(15.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.width(118.dp), horizontalAlignment = Alignment.CenterHorizontally) { V4Logo(channel, accent); Text(channel.name, color = V4Text, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
        LazyRow(Modifier.focusGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(end = 6.dp)) {
            items(programmes) { p ->
                var focused by remember { mutableStateOf(false) }
                Column(Modifier.width(136.dp).height(62.dp).focusable().onFocusChanged { focused = it.isFocused }
                    .background(if (focused || selected) accent.copy(.10f) else Color.White.copy(.03f), RoundedCornerShape(11.dp))
                    .border(if (focused) 1.dp else 0.dp, accent.copy(.7f), RoundedCornerShape(11.dp)).tvAction { onProgram(channel, selected) }.padding(8.dp)) {
                    Text(formatTime(p.start), color = if (focused) accent else V4Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(p.title, color = V4Text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                }
            }
        }
    }
}

@Composable
private fun V4Preview(channel: Channel, accent: Color) {
    val context = LocalContext.current
    val player = remember(channel.streamUrl) { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(Uri.parse(channel.streamUrl))); prepare(); playWhenReady = true } }
    androidx.compose.runtime.DisposableEffect(player) { onDispose { player.release() } }
    Row(Modifier.fillMaxWidth().height(145.dp).background(Color.Black.copy(.60f), RoundedCornerShape(17.dp)).border(1.dp, accent.copy(.35f), RoundedCornerShape(17.dp)).padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
        AndroidView(factory = { ctx -> PlayerView(ctx).apply { useController = false; isFocusable = false; isFocusableInTouchMode = false; this.player = player } }, modifier = Modifier.width(225.dp).fillMaxHeight())
        Column(Modifier.padding(start = 15.dp)) { Text("VORSCHAU", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black); Text(channel.name, color = V4Text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 3.dp)); Text("OK erneut → Fullscreen", color = V4Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp)) }
    }
}

@Composable
private fun V4Search(channels: List<Channel>, accent: Color, onPlay: (Channel) -> Unit) {
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val filtered = channels.filter { it.name.contains(query, true) || it.group.orEmpty().contains(query, true) }
    Column(Modifier.fillMaxSize()) {
        V4Header("Suche", "Fokus ≠ Eingabe · erst OK öffnet die Tastatur")
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(query, { query = it }, label = { Text("Sender suchen") }, readOnly = !editing, singleLine = true, colors = V4FieldColors(accent), modifier = Modifier.fillMaxWidth().onPreviewKeyEvent { e ->
            if (e.type == KeyEventType.KeyUp && (e.key == Key.DirectionCenter || e.key == Key.Enter)) { editing = true; keyboard?.show(); true } else false
        })
        Spacer(Modifier.height(9.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(filtered) { V4Channel(it, accent, onPlay) } }
    }
}

@Composable
private fun V4Sources(store: PlaylistStore, accent: Color, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var urlEdit by remember { mutableStateOf(false) }
    var serverEdit by remember { mutableStateOf(false) }
    var userEdit by remember { mutableStateOf(false) }
    var passEdit by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) scope.launch { status = "Playlist wird eingelesen…"; runCatching { store.importPlaylistFromUri(uri) }.onSuccess { onDone() }.onFailure { status = it.message ?: "M3U konnte nicht gelesen werden" } } }
    Column(Modifier.fillMaxSize().focusGroup()) {
        V4Header("Quelle hinzufügen", "D-Pad bewegt · Hover öffnet nichts · OK aktiviert Eingabe")
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().focusGroup(), horizontalArrangement = Arrangement.spacedBy(15.dp)) {
            V4SourceCard("M3U / M3U8", accent, Modifier.weight(1f)) {
                V4Action("Datei auswählen", accent) { picker.launch(arrayOf("*/*")) }
                V4GatedField(url, { url = it }, "M3U / M3U8 URL", urlEdit, { urlEdit = true }, accent)
                V4Action("URL importieren", accent) { scope.launch { status = "URL wird geladen…"; runCatching { store.importPlaylistFromUrl(url.trim()) }.onSuccess { onDone() }.onFailure { status = it.message ?: "URL konnte nicht geladen werden" } } }
            }
            V4SourceCard("Xtream Codes", accent, Modifier.weight(1f)) {
                V4GatedField(server, { server = it }, "Server URL oder Host", serverEdit, { serverEdit = true }, accent)
                V4GatedField(user, { user = it }, "Benutzername", userEdit, { userEdit = true }, accent)
                V4GatedField(pass, { pass = it }, "Passwort", passEdit, { passEdit = true }, accent)
                V4Action("Xtream verbinden", accent) { scope.launch { status = "Xtream wird verbunden…"; XtreamClient(server.trim(), user.trim(), pass).load().onSuccess { list -> store.importXtream(list); onDone() }.onFailure { status = it.message ?: "Xtream-Verbindung fehlgeschlagen" } } }
            }
        }
        if (status.isNotBlank()) Text(status, color = accent, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun V4GatedField(value: String, onValue: (String) -> Unit, label: String, editing: Boolean, onEdit: () -> Unit, accent: Color) {
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(value, onValue, label = { Text(label) }, readOnly = !editing, singleLine = true, colors = V4FieldColors(accent), modifier = Modifier.fillMaxWidth().padding(top = 7.dp).onPreviewKeyEvent { e ->
        if (e.type == KeyEventType.KeyUp && (e.key == Key.DirectionCenter || e.key == Key.Enter)) { onEdit(); keyboard?.show(); true } else false
    })
}

@Composable
private fun V4SourceCard(title: String, accent: Color, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.background(Color.White.copy(.045f), RoundedCornerShape(20.dp)).border(1.dp, Color.White.copy(.1f), RoundedCornerShape(20.dp)).padding(15.dp)) { Text(title, color = V4Text, fontSize = 18.sp, fontWeight = FontWeight.Bold); content() }
}

@Composable
private fun V4Action(title: String, accent: Color, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().height(42.dp).padding(top = 7.dp).focusable().onFocusChanged { focused = it.isFocused }
        .background(if (focused) accent.copy(.16f) else Color.White.copy(.04f), RoundedCornerShape(11.dp))
        .border(if (focused) 1.dp else 0.dp, accent.copy(.7f), RoundedCornerShape(11.dp)).tvAction(onClick), Alignment.CenterStart) {
        Text(title, color = V4Text, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 11.dp))
    }
}

@Composable
private fun V4ActionCard(title: String, subtitle: String, accent: Color, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.width(420.dp).focusable().onFocusChanged { focused = it.isFocused }
        .background(if (focused) accent.copy(.13f) else Color.White.copy(.045f), RoundedCornerShape(20.dp))
        .border(if (focused) 1.dp else 0.dp, accent.copy(.7f), RoundedCornerShape(20.dp)).tvAction(onClick).padding(20.dp)) {
        Text(title, color = V4Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = V4Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
private fun V4Settings(accent: Color, onSources: () -> Unit) {
    Column(Modifier.fillMaxSize().focusGroup()) {
        V4Header("Einstellungen", "D-Pad · OK")
        Spacer(Modifier.height(14.dp))
        V4ActionCard("Quellen", "M3U / M3U8 / Xtream Codes", accent, onSources)
        Spacer(Modifier.height(10.dp))
        V4ActionCard("Seitenleiste", "Reihenfolge und Sichtbarkeit", accent) {}
        Spacer(Modifier.height(10.dp))
        V4ActionCard("Darstellung", "Theme · Glas · Animationen", accent) {}
    }
}

@Composable
private fun V4Player(channels: List<Channel>, index: Int, settings: SettingsStore, accent: Color, onSwitch: (Int) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val channel = channels[index]
    val player = remember(channel.streamUrl) { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(Uri.parse(channel.streamUrl))); prepare(); playWhenReady = settings.player.startLiveImmediately } }
    var overlay by remember { mutableStateOf(false) }
    val rootRequester = remember { FocusRequester() }
    val firstButtonRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { rootRequester.requestFocus() } }
    LaunchedEffect(overlay) { if (overlay) runCatching { firstButtonRequester.requestFocus() } else runCatching { rootRequester.requestFocus() } }
    androidx.compose.runtime.DisposableEffect(player) { onDispose { player.release() } }
    BackHandler { if (overlay) overlay = false else onClose() }
    Box(Modifier.fillMaxSize().background(Color.Black).focusRequester(rootRequester).focusable().onPreviewKeyEvent { e ->
        if (e.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
        if (overlay) return@onPreviewKeyEvent false
        when (e.key) {
            Key.DirectionCenter -> { overlay = true; true }
            Key.DirectionLeft -> { player.seekTo((player.currentPosition - 10_000L).coerceAtLeast(0L)); overlay = true; true }
            Key.DirectionRight -> { player.seekTo(player.currentPosition + 10_000L); overlay = true; true }
            Key.DirectionUp -> { if (channels.isNotEmpty()) { onSwitch((index - 1 + channels.size) % channels.size); true } else false }
            Key.DirectionDown -> { if (channels.isNotEmpty()) { onSwitch((index + 1) % channels.size); true } else false }
            Key.MediaPlayPause -> { if (player.isPlaying) player.pause() else player.play(); overlay = true; true }
            Key.MediaPlay -> { player.play(); overlay = true; true }
            Key.MediaPause -> { player.pause(); overlay = true; true }
            else -> false
        }
    }) {
        AndroidView(factory = { ctx -> PlayerView(ctx).apply { useController = false; isFocusable = false; isFocusableInTouchMode = false; this.player = player } }, modifier = Modifier.fillMaxSize())
        if (overlay) Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(.76f)).padding(17.dp).focusGroup()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                V4Logo(channel, accent)
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(channel.name, color = V4Text, fontSize = 19.sp, fontWeight = FontWeight.Black)
                    Text("LIVE · ↑/↓ Sender · Back Overlay schließen", color = V4Muted, fontSize = 10.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 9.dp)) {
                V4PlayerButton(if (player.isPlaying) "Pause" else "Play", accent, firstButtonRequester) { if (player.isPlaying) player.pause() else player.play() }
                V4PlayerButton("−10s", accent, null) { player.seekTo((player.currentPosition - 10_000L).coerceAtLeast(0L)) }
                V4PlayerButton("+10s", accent, null) { player.seekTo(player.currentPosition + 10_000L) }
            }
        }
    }
}

@Composable
private fun V4PlayerButton(title: String, accent: Color, requester: FocusRequester?, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.width(105.dp).height(40.dp).focusRequesterCompat(requester).focusable().onFocusChanged { focused = it.isFocused }
        .background(if (focused) accent.copy(.18f) else Color.White.copy(.06f), RoundedCornerShape(10.dp))
        .border(if (focused) 1.dp else 0.dp, accent.copy(.8f), RoundedCornerShape(10.dp)).tvAction(onClick), Alignment.Center) {
        Text(title, color = V4Text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private fun Modifier.focusRequesterCompat(requester: FocusRequester?): Modifier = if (requester != null) focusRequester(requester) else this

@Composable private fun V4FieldColors(accent: Color) = OutlinedTextFieldDefaults.colors(focusedTextColor = V4Text, unfocusedTextColor = V4Text, focusedLabelColor = accent, unfocusedLabelColor = V4Muted, cursorColor = accent, focusedBorderColor = accent, unfocusedBorderColor = Color.White.copy(.2f))
private fun formatTime(date: Date): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
private fun v4Initials(name: String): String = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "TV" }
