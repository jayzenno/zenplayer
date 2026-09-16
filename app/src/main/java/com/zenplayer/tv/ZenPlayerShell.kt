package com.zenplayer.tv

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.zenplayer.tv.domain.model.Channel
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

private val Primary = Color(0xFFF6F7FB)
private val Secondary = Color(0xFF969EAF)
private val Surface = Color.White.copy(alpha = .055f)

@Composable
fun ZenPlayerShell(settings: SettingsStore) {
    val ui = settings.ui
    val density = LocalDensity.current
    val context = LocalContext.current
    val store = remember { PlaylistStore(context) }
    val scope = rememberCoroutineScope()
    var page by remember { mutableIntStateOf(0) }
    var sourceMode by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf<Channel?>(null) }
    var demo by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    val accent = when (ui.theme) {
        ZenTheme.AURORA -> Color(0xFF70E6FF)
        ZenTheme.OBSIDIAN -> Color(0xFFB7B9FF)
        ZenTheme.FROST -> Color(0xFFB9E9FF)
        ZenTheme.AMBER -> Color(0xFFFFC66D)
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalDensity provides Density(density.density * ui.uiScale.coerceIn(75,125) / 100f, density.fontScale)) {
        Box(Modifier.fillMaxSize().background(Color(0xFF05070C))) {
            ZenAnimatedBackdrop(ui.theme, ui.animatedBackdrop, !ui.reducedMotion)
            if (sourceMode != 0) {
                SourcesScreen(store, accent, sourceMode, { sourceMode = 0 }, { refresh++; sourceMode = 0 })
            } else {
                Row(Modifier.fillMaxSize().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ZenSidebarV2(page, accent, { page = it })
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        when (page) {
                            0 -> HomeV2(store, accent, demo, { sourceMode = 1 }, { demo = true; refresh++ }, { demo = false; store.clear(); refresh++ }, { playing = it })
                            1 -> EpgV2(store, accent, demo, refresh, { playing = it })
                            2 -> SearchV2(store, accent, refresh, { playing = it })
                            else -> SettingsV2(settings, accent, { sourceMode = 1 })
                        }
                    }
                }
            }
            playing?.let { channel -> ZenPlayerScreen(channel, settings) { playing = null } }
        }
    }
}

@Composable
private fun ZenSidebarV2(page: Int, accent: Color, onPage: (Int) -> Unit) {
    val items = listOf("Home" to Icons.Default.Home, "EPG" to Icons.Default.PlayArrow, "Suche" to Icons.Default.Search, "Settings" to Icons.Default.Settings)
    val requester = remember { FocusRequester() }
    LaunchedEffect(page) { if (page >= 0) runCatching { requester.requestFocus() } }
    Column(Modifier.width(64.dp).wrapContentHeight().background(Color.White.copy(.035f), RoundedCornerShape(18.dp)).border(1.dp, Color.White.copy(.08f), RoundedCornerShape(18.dp)).padding(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.width(46.dp).height(46.dp).background(accent.copy(.12f), RoundedCornerShape(14.dp)).border(1.dp, accent.copy(.3f), RoundedCornerShape(14.dp)), Alignment.Center) { Text("Z", color = Primary, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        items.forEachIndexed { index, pair ->
            var focused by remember(pair.first) { mutableStateOf(false) }
            Box(Modifier.width(46.dp).height(46.dp).background(if (page == index || focused) accent.copy(.18f) else Color.Transparent, RoundedCornerShape(14.dp)).border(if (focused) 2.dp else 1.dp, if (page == index || focused) accent else Color.Transparent, RoundedCornerShape(14.dp)).onFocusChanged { focused = it.isFocused }.then(if (index == 0) Modifier.focusRequester(requester) else Modifier).tvAction { onPage(index) }, Alignment.Center) {
                Icon(pair.second, pair.first, tint = if (page == index || focused) Primary else Secondary)
            }
        }
    }
}

@Composable
private fun HomeV2(store: PlaylistStore, accent: Color, demo: Boolean, add: () -> Unit, startDemo: () -> Unit, stopDemo: () -> Unit, onPlay: (Channel) -> Unit) {
    val channels = if (demo) DemoData.channels() else store.channels
    Column(Modifier.fillMaxSize()) {
        Text(if (demo) "Demo-Modus" else "Für dich", color = Primary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(if (demo) "Dummy-Sender und Dummy-EPG · alles ist testbar" else "Dein persönlicher TV-Startbildschirm", color = Secondary, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
        if (channels.isEmpty()) {
            Text("Teste ZenPlayer direkt – ohne eigene Playlist.", color = Primary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigAction("Playlist hinzufügen", "M3U / URL / Xtream", Icons.Default.Source, accent, add)
                BigAction("Demo-Modus", "Dummy-Sender + Player", Icons.Default.PlayArrow, accent, startDemo)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallAction(if (demo) "Demo beenden" else "Quelle verwalten", accent) { if (demo) stopDemo() else add() }
            }
            Spacer(Modifier.height(14.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(bottom = 20.dp)) { items(channels) { ChannelCardV2(it, accent) { onPlay(it) } } }
        }
    }
}

@Composable
private fun BigAction(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, action: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.width(330.dp).height(92.dp).background(if (focused) accent.copy(.14f) else Surface, RoundedCornerShape(20.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(.08f), RoundedCornerShape(20.dp)).onFocusChanged { focused = it.isFocused }.tvAction(action).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = accent); Column(Modifier.padding(start = 15.dp)) { Text(title, color = Primary, fontSize = 16.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = Secondary, fontSize = 11.sp) }
    }
}

@Composable
private fun SmallAction(title: String, accent: Color, action: () -> Unit) { var f by remember { mutableStateOf(false) }; Box(Modifier.background(if (f) accent.copy(.15f) else Surface, RoundedCornerShape(12.dp)).border(if (f) 2.dp else 1.dp, if (f) accent else Color.White.copy(.08f), RoundedCornerShape(12.dp)).onFocusChanged { f = it.isFocused }.tvAction(action).padding(horizontal = 14.dp, vertical = 9.dp)) { Text(title, color = Primary, fontSize = 11.sp) } }

@Composable
private fun ChannelCardV2(channel: Channel, accent: Color, action: () -> Unit) {
    var f by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(62.dp).background(if (f) accent.copy(.13f) else Surface, RoundedCornerShape(16.dp)).border(if (f) 2.dp else 1.dp, if (f) accent else Color.White.copy(.07f), RoundedCornerShape(16.dp)).onFocusChanged { f = it.isFocused }.tvAction(action).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        if (!channel.logoUrl.isNullOrBlank()) AsyncImage(channel.logoUrl, channel.name, Modifier.width(44.dp).height(44.dp), contentScale = ContentScale.Fit) else Text(initials2(channel.name), color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))
        Column(Modifier.weight(1f)) { Text(channel.name, color = Primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold); Text(channel.group ?: "Live TV", color = Secondary, fontSize = 10.sp) }
        Text(channel.resolutionHint ?: "LIVE", color = if (f) Primary else Secondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SourcesScreen(store: PlaylistStore, accent: Color, mode: Int, back: () -> Unit, done: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var m3uUrl by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            status = "Lese Playlist…"
            runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("Datei ist leer") }
                .onSuccess { text -> runCatching { store.importPlaylist(text) }.onSuccess { status = "${store.channels.size} Sender importiert"; done() }.onFailure { status = it.message ?: "M3U ungültig" } }
                .onFailure { status = it.message ?: "Datei konnte nicht gelesen werden" }
        }
    }
    BackHandler(onBack = back)
    Column(Modifier.fillMaxSize().padding(4.dp)) {
        Text("Quellen", color = Primary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("M3U-Datei, M3U-URL oder Xtream Codes", color = Secondary, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        if (mode == 1) {
            SourceCard("M3U / M3U8", "Datei oder direkte URL") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SourceButton("Datei auswählen", accent) { picker.launch(arrayOf("*/*")) }
                    SourceButton("URL laden", accent) { scope.launch { status = "Lade URL…"; runCatching { store.importPlaylistFromUrl(m3uUrl) }.onSuccess { status = "${store.channels.size} Sender importiert"; done() }.onFailure { status = it.message ?: "URL konnte nicht geladen werden" } } }
                }
                OutlinedTextField(m3uUrl, { m3uUrl = it }, label = { Text("M3U / M3U8 URL") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 9.dp))
            }
            Spacer(Modifier.height(10.dp))
            SourceCard("Xtream Codes", "Server · Benutzername · Passwort") {
                OutlinedTextField(server, { server = it }, label = { Text("Server URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(user, { user = it }, label = { Text("Benutzername") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 7.dp))
                OutlinedTextField(pass, { pass = it }, label = { Text("Passwort") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 7.dp))
                Spacer(Modifier.height(9.dp))
                SourceButton("Xtream verbinden", accent) { scope.launch { status = "Verbinde…"; XtreamClient(server, user, pass).load().onSuccess { list -> store.importXtream(list); status = "${list.size} Sender importiert"; done() }.onFailure { status = it.message ?: "Xtream-Verbindung fehlgeschlagen" } } }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(status, color = accent, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        SourceButton("Zurück", accent, back)
    }
}

@Composable
private fun SourceCard(title: String, subtitle: String, content: @Composable () -> Unit) { Column(Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(18.dp)).border(1.dp, Color.White.copy(.08f), RoundedCornerShape(18.dp)).padding(15.dp)) { Text(title, color = Primary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, color = Secondary, fontSize = 10.sp); Spacer(Modifier.height(10.dp)); content() } }

@Composable
private fun SourceButton(title: String, accent: Color, action: () -> Unit) { var f by remember { mutableStateOf(false) }; Box(Modifier.background(if (f) accent.copy(.17f) else Surface, RoundedCornerShape(11.dp)).border(if (f) 2.dp else 1.dp, if (f) accent else Color.White.copy(.08f), RoundedCornerShape(11.dp)).onFocusChanged { f = it.isFocused }.tvAction(action).padding(horizontal = 13.dp, vertical = 9.dp)) { Text(title, color = Primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) } }

@Composable
private fun EpgV2(store: PlaylistStore, accent: Color, demo: Boolean, refresh: Int, onPlay: (Channel) -> Unit) {
    val programmes = if (demo) DemoData.programmes() else store.programmes
    val channels = if (demo) DemoData.channels() else store.channels
    Column(Modifier.fillMaxSize()) {
        Text("EPG", color = Primary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("Programmübersicht · ${programmes.size} Einträge", color = Secondary, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            channels.forEach { channel ->
                val ps = programmes.filter { it.channelId == channel.id }.sortedBy { it.start }.take(8)
                if (ps.isNotEmpty()) {
                    item { Text(channel.name, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp)) }
                    items(ps) { p -> var f by remember(p.id) { mutableStateOf(false) }; Row(Modifier.fillMaxWidth().height(50.dp).background(if (f) accent.copy(.11f) else Surface, RoundedCornerShape(13.dp)).border(if (f) 2.dp else 1.dp, if (f) accent else Color.Transparent, RoundedCornerShape(13.dp)).onFocusChanged { f = it.isFocused }.tvAction { onPlay(channel) }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) { Text(java.text.SimpleDateFormat("HH:mm", java.util.Locale.GERMANY).format(java.util.Date(p.start)), color = accent, fontSize = 11.sp, modifier = Modifier.width(52.dp)); Column(Modifier.weight(1f)) { Text(p.title, color = Primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Text(p.category ?: "TV", color = Secondary, fontSize = 9.sp) }; if (p.isCatchupAvailable) Text("REPLAY", color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold) } }
                }
            }
        }
    }
}

@Composable
private fun SearchV2(store: PlaylistStore, accent: Color, refresh: Int, onPlay: (Channel) -> Unit) {
    var q by remember { mutableStateOf("") }
    val filtered = store.channels.filter { q.isBlank() || it.name.contains(q, true) || it.group.orEmpty().contains(q, true) }
    Column(Modifier.fillMaxSize()) { Text("Suche", color = Primary, fontSize = 30.sp, fontWeight = FontWeight.Bold); Text("Sender und Gruppen", color = Secondary, fontSize = 13.sp); OutlinedTextField(q, { q = it }, label = { Text("Suchen…") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)); Spacer(Modifier.height(10.dp)); LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) { items(filtered) { ChannelCardV2(it, accent) { onPlay(it) } } } }
}

@Composable
private fun SettingsV2(settings: SettingsStore, accent: Color, openSources: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Player", "Live TV", "EPG", "Darstellung", "Quellen")
    Column(Modifier.fillMaxSize()) { Text("Einstellungen", color = Primary, fontSize = 30.sp, fontWeight = FontWeight.Bold); Text("Player, EPG, Darstellung und Quellen zentral", color = Secondary, fontSize = 13.sp); Spacer(Modifier.height(12.dp)); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { tabs.forEachIndexed { i, title -> SmallAction(title, accent) { tab = i } } }; Spacer(Modifier.height(10.dp)); when (tab) { 0 -> SettingsPanelV2("Player", "Engine und Buffer") { ChoiceV2("Engine", settings.player.engine.label, PlaybackEngine.entries.map { it.label }, accent) { v -> settings.updatePlayer(settings.player.copy(engine = PlaybackEngine.entries.first { it.label == v })) }; ChoiceV2("Buffer", settings.player.bufferMode.label, BufferMode.entries.map { it.label }, accent) { v -> settings.updatePlayer(settings.player.copy(bufferMode = BufferMode.entries.first { it.label == v })) } }; 1 -> SettingsPanelV2("Live TV", "Live und Catch-up") { ToggleV2("Shared Catch-up bevorzugen", settings.player.preferCatchupSibling, accent) { settings.updatePlayer(settings.player.copy(preferCatchupSibling = it)) }; ToggleV2("Timeshift wenn verfügbar", settings.player.enableTimeshiftWhenAvailable, accent) { settings.updatePlayer(settings.player.copy(enableTimeshiftWhenAvailable = it)) } }; 2 -> SettingsPanelV2("EPG", "Programmansicht") { ChoiceV2("Zeitraum", settings.epg.pageSize.label, EpgPageSize.entries.map { it.label }, accent) { v -> settings.updateEpg(settings.epg.copy(pageSize = EpgPageSize.entries.first { it.label == v })) } }; 3 -> SettingsPanelV2("Darstellung", "Milchglas, Theme und Animation") { ChoiceV2("Theme", settings.ui.theme.label, ZenTheme.entries.map { it.label }, accent) { v -> settings.updateUi(settings.ui.copy(theme = ZenTheme.entries.first { it.label == v })) }; ChoiceV2("Hintergrund", settings.ui.animatedBackdrop.label, AnimatedBackdrop.entries.map { it.label }, accent) { v -> settings.updateUi(settings.ui.copy(animatedBackdrop = AnimatedBackdrop.entries.first { it.label == v })) }; ChoiceV2("Glasstärke", settings.ui.glassIntensity.toString(), (1..10).map { it.toString() }, accent) { v -> settings.updateUi(settings.ui.copy(glassIntensity = v.toInt())) }; ToggleV2("Animationen", settings.ui.animations, accent) { settings.updateUi(settings.ui.copy(animations = it)) } }; else -> SettingsPanelV2("Quellen", "Playlist-Verwaltung gehört hierher") { Text("${settingsTextPlaceholder()}", color = Secondary, fontSize = 12.sp); Spacer(Modifier.height(8.dp)); SourceButton("Quellen öffnen", accent, openSources) } } }
}

private fun settingsTextPlaceholder() = "M3U / M3U8 / Xtream hinzufügen, wechseln oder neu laden."

@Composable private fun SettingsPanelV2(title: String, subtitle: String, content: @Composable () -> Unit) { Column(Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(18.dp)).border(1.dp, Color.White.copy(.08f), RoundedCornerShape(18.dp)).padding(15.dp)) { Text(title, color = Primary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, color = Secondary, fontSize = 10.sp); Spacer(Modifier.height(10.dp)); content() } }

@Composable private fun ChoiceV2(label: String, value: String, options: List<String>, accent: Color, change: (String) -> Unit) { var f by remember { mutableStateOf(false) }; var index by remember(value) { mutableIntStateOf(options.indexOf(value).coerceAtLeast(0)) }; Row(Modifier.fillMaxWidth().height(48.dp).background(if (f) accent.copy(.1f) else Surface, RoundedCornerShape(12.dp)).border(if (f) 2.dp else 1.dp, if (f) accent else Color.Transparent, RoundedCornerShape(12.dp)).onFocusChanged { f = it.isFocused }.focusable().onKeyEvent { e -> if (e.type != KeyEventType.KeyUp) return@onKeyEvent false; when(e.key) { Key.DirectionLeft -> { index = (index - 1 + options.size) % options.size; change(options[index]); true }; Key.DirectionRight, Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { index = (index + 1) % options.size; change(options[index]); true }; else -> false } }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, color = Secondary, fontSize = 11.sp); Spacer(Modifier.weight(1f)); Text(value, color = Primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold); Text("  ${index + 1}/${options.size}", color = accent, fontSize = 9.sp) } }

@Composable private fun ToggleV2(label: String, checked: Boolean, accent: Color, change: (Boolean) -> Unit) { var f by remember { mutableStateOf(false) }; Row(Modifier.fillMaxWidth().height(44.dp).onFocusChanged { f = it.isFocused }.tvAction { change(!checked) }.background(if (f) accent.copy(.1f) else Color.Transparent, RoundedCornerShape(11.dp)).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, color = Primary, fontSize = 12.sp); Spacer(Modifier.weight(1f)); Text(if (checked) "AN" else "AUS", color = if (checked) accent else Secondary, fontSize = 10.sp, fontWeight = FontWeight.Bold) } }

private fun initials2(name: String): String = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercaseChar().toString() }.ifBlank { "TV" }
