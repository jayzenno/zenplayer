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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import coil.compose.AsyncImage
import com.zenplayer.tv.domain.model.Channel
import com.zenplayer.tv.domain.model.EpgProgramme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private val V3Text = Color(0xFFF7F8FC)
private val V3Muted = Color(0xFF8D96AA)
private val V3Base = Color(0xFF05070D)

private fun glassAlpha(intensity: Int, base: Float): Float = (base * (0.55f + intensity.coerceIn(1, 10) / 20f)).coerceIn(0.035f, 0.16f)

@Composable
fun ZenPlayerShellV3(settings: SettingsStore) {
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
    var demo by remember { mutableStateOf(false) }
    var playerIndex by remember { mutableIntStateOf(-1) }
    var sourceOpen by remember { mutableStateOf(false) }
    val channels = if (demo) DemoData.channels() else store.channels

    Box(Modifier.fillMaxSize().background(V3Base)) {
        ZenAnimatedBackdrop(ui.theme, ui.animatedBackdrop, !ui.reducedMotion)
        if (sourceOpen) {
            V3Sources(store, settings, accent, onBack = { sourceOpen = false }, onDone = { sourceOpen = false })
        } else if (playerIndex >= 0 && playerIndex < channels.size) {
            V3Player(channels, playerIndex, settings, accent, onClose = { playerIndex = -1 }, onChannel = { playerIndex = it })
        } else {
            Row(Modifier.fillMaxSize().padding(22.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                Box(Modifier.fillMaxHeight().width(74.dp), contentAlignment = Alignment.Center) {
                    V3Sidebar(settings, page, accent) { page = it }
                }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    when (page) {
                        "home" -> V3Home(store, demo, ui.glassIntensity, accent, onAdd = { sourceOpen = true }, onDemo = { demo = true }, onExitDemo = { demo = false }, onPlay = { playerIndex = if (demo) DemoData.channels().indexOf(it) else store.channels.indexOf(it) })
                        "epg" -> V3Epg(store, demo, ui.glassIntensity, accent, onPlay = { playerIndex = if (demo) DemoData.channels().indexOf(it) else store.channels.indexOf(it) })
                        "search" -> V3Search(store, demo, ui.glassIntensity, accent, onPlay = { playerIndex = if (demo) DemoData.channels().indexOf(it) else store.channels.indexOf(it) })
                        else -> V3Settings(settings, ui.glassIntensity, accent, onSources = { sourceOpen = true })
                    }
                }
            }
        }
    }
}

@Composable
private fun V3Sidebar(settings: SettingsStore, page: String, accent: Color, onPage: (String) -> Unit) {
    val ids = listOf("home", "epg", "search", "settings")
    val labels = mapOf("home" to "Home", "epg" to "EPG", "search" to "Suche", "settings" to "Settings")
    val icons = mapOf("home" to Icons.Default.Home, "epg" to Icons.Default.PlayArrow, "search" to Icons.Default.Search, "settings" to Icons.Default.Settings)
    val ordered = settings.ui.sidebarOrder.filter { it in ids && it !in settings.ui.sidebarHidden }.let { if (it.isEmpty()) ids else it }
    val first = remember { FocusRequester() }
    LaunchedEffect(ordered) { runCatching { first.requestFocus() } }
    Column(Modifier.width(74.dp).background(Color.White.copy(glassAlpha(settings.ui.glassIntensity, .055f)), RoundedCornerShape(24.dp)).border(1.dp, Color.White.copy(.13f), RoundedCornerShape(24.dp)).padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.width(54.dp).height(54.dp).background(accent.copy(.16f), RoundedCornerShape(17.dp)).border(1.dp, accent.copy(.55f), RoundedCornerShape(17.dp)), Alignment.Center) { Text("Z", color = V3Text, fontSize = 24.sp, fontWeight = FontWeight.Black) }
        ordered.forEachIndexed { i, id ->
            V3NavButton(labels.getValue(id), icons.getValue(id), page == id, accent, if (i == 0) first else null) { onPage(id) }
        }
    }
}

@Composable
private fun V3NavButton(label: String, icon: ImageVector, selected: Boolean, accent: Color, requester: FocusRequester?, onClick: () -> Unit) {
    var focus by remember { mutableStateOf(false) }
    val active = selected || focus
    Box(Modifier.width(54.dp).height(54.dp).then(if (requester != null) Modifier.focusRequester(requester) else Modifier).onFocusChanged { focus = it.isFocused }.background(if (active) accent.copy(.18f) else Color.Transparent, RoundedCornerShape(17.dp)).border(if (focus) 2.dp else 1.dp, if (active) accent.copy(if (focus) .95f else .45f) else Color.Transparent, RoundedCornerShape(17.dp)).tvAction(onClick), Alignment.Center) {
        Icon(icon, label, tint = if (active) V3Text else V3Muted)
    }
}

@Composable
private fun V3Home(store: PlaylistStore, demo: Boolean, glass: Int, accent: Color, onAdd: () -> Unit, onDemo: () -> Unit, onExitDemo: () -> Unit, onPlay: (Channel) -> Unit) {
    val channels = if (demo) DemoData.channels() else store.channels
    Column(Modifier.fillMaxSize()) {
        V3Header(if (demo) "Demo" else "Home", if (demo) "Sechs Testsender · EPG · echter Player" else "Dein Live-TV in einer Oberfläche")
        if (channels.isEmpty()) {
            Spacer(Modifier.height(30.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                V3BigButton("Playlist hinzufügen", "M3U-Datei · URL · Xtream Codes", Icons.Default.Source, accent, onAdd)
                V3BigButton("Demo-Modus", "Ohne Quelle sofort testen", Icons.Default.PlayArrow, accent, onDemo)
            }
        } else {
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                V3Pill(if (demo) "Demo beenden" else "Quelle verwalten", accent) { if (demo) onExitDemo() else onAdd() }
                Text("${channels.size} Sender", color = V3Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 10.dp))
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 30.dp)) { items(channels) { V3ChannelRow(it, glass, accent, onPlay) } }
        }
    }
}

@Composable
private fun V3Header(title: String, subtitle: String) {
    Text(title, color = V3Text, fontSize = 34.sp, fontWeight = FontWeight.Black)
    Text(subtitle, color = V3Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 3.dp))
}

@Composable
private fun V3ChannelRow(channel: Channel, glass: Int, accent: Color, onPlay: (Channel) -> Unit) {
    var f by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(72.dp).onFocusChanged { f = it.isFocused }.focusable().background(if (f) accent.copy(.12f) else Color.White.copy(glassAlpha(glass, .055f)), RoundedCornerShape(18.dp)).border(if (f) 2.dp else 1.dp, if (f) accent.copy(if (f) .8f else .2f) else Color.White.copy(.10f), RoundedCornerShape(18.dp)).tvAction { onPlay(channel) }.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        if (!channel.logoUrl.isNullOrBlank()) AsyncImage(channel.logoUrl, channel.name, Modifier.width(46.dp).height(46.dp), contentScale = ContentScale.Fit) else Box(Modifier.width(46.dp).height(46.dp).background(accent.copy(.12f), RoundedCornerShape(13.dp)), Alignment.Center) { Text(initials3(channel.name), color = accent, fontWeight = FontWeight.Bold) }
        Column(Modifier.weight(1f).padding(start = 14.dp)) { Text(channel.name, color = V3Text, fontSize = 15.sp, fontWeight = FontWeight.Bold); Text(channel.group ?: "Live TV", color = V3Muted, fontSize = 10.sp) }
        Text(channel.resolutionHint ?: "LIVE", color = if (f) accent else V3Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun V3BigButton(title: String, subtitle: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    var f by remember { mutableStateOf(false) }
    Row(Modifier.width(390.dp).height(112.dp).onFocusChanged { f = it.isFocused }.focusable().background(if (f) accent.copy(.15f) else Color.White.copy(.055f), RoundedCornerShape(24.dp)).border(if (f) 2.dp else 1.dp, if (f) accent else Color.White.copy(.11f), RoundedCornerShape(24.dp)).tvAction(onClick).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = accent, modifier = Modifier.width(34.dp).height(34.dp))
        Column(Modifier.padding(start = 17.dp)) { Text(title, color = V3Text, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = V3Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)) }
    }
}

@Composable
private fun V3Pill(title: String, accent: Color, onClick: () -> Unit) {
    var f by remember { mutableStateOf(false) }
    Box(Modifier.onFocusChanged { f = it.isFocused }.focusable().background(if (f) accent.copy(.16f) else Color.White.copy(.055f), RoundedCornerShape(13.dp)).border(if (f) 2.dp else 1.dp, if (f) accent else Color.White.copy(.10f), RoundedCornerShape(13.dp)).tvAction(onClick).padding(horizontal = 15.dp, vertical = 10.dp)) { Text(title, color = V3Text, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun V3Sources(store: PlaylistStore, settings: SettingsStore, accent: Color, onBack: () -> Unit, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    val fieldColors = OutlinedTextFieldDefaults.colors(focusedTextColor = V3Text, unfocusedTextColor = V3Text, disabledTextColor = V3Muted, focusedLabelColor = accent, unfocusedLabelColor = V3Muted, cursorColor = accent, focusedBorderColor = accent, unfocusedBorderColor = Color.White.copy(.22f))
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) scope.launch { status = "Playlist wird eingelesen…"; runCatching { store.importPlaylistFromUri(uri) }.onSuccess { status = "${store.channels.size} Sender importiert"; onDone() }.onFailure { status = it.message ?: "M3U konnte nicht gelesen werden" } } }
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize()) {
        V3Header("Quelle hinzufügen", "M3U, direkte URL oder Xtream Codes")
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            V3SourceCard("M3U / M3U8", "Datei oder direkte URL", accent, Modifier.weight(1f)) { V3Action("Datei auswählen", accent) { picker.launch(arrayOf("*/*")) }; OutlinedTextField(url, { url = it }, label = { Text("M3U / M3U8 URL") }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)); V3Action("URL importieren", accent) { scope.launch { status = "URL wird geladen…"; runCatching { store.importPlaylistFromUrl(url.trim()) }.onSuccess { status = "${store.channels.size} Sender importiert"; onDone() }.onFailure { status = it.message ?: "URL konnte nicht geladen werden" } } } }
            V3SourceCard("Xtream Codes", "Server · Benutzername · Passwort", accent, Modifier.weight(1f)) { OutlinedTextField(server, { server = it }, label = { Text("Server URL oder Host") }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth()); OutlinedTextField(user, { user = it }, label = { Text("Benutzername") }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)); OutlinedTextField(pass, { pass = it }, label = { Text("Passwort") }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)); V3Action("Xtream verbinden", accent) { scope.launch { status = "Xtream wird verbunden…"; XtreamClient(server.trim(), user.trim(), pass).load().onSuccess { list -> store.importXtream(list); status = "${list.size} Sender importiert"; onDone() }.onFailure { status = it.message ?: "Xtream-Verbindung fehlgeschlagen" } } } }
        }
        Spacer(Modifier.height(14.dp)); Text(status, color = accent, fontSize = 12.sp); Spacer(Modifier.height(12.dp)); V3Action("Zurück", accent, onBack)
    }
}

@Composable
private fun V3SourceCard(title: String, subtitle: String, accent: Color, modifier: Modifier, content: @Composable () -> Unit) { Column(modifier.background(Color.White.copy(.065f), RoundedCornerShape(24.dp)).border(1.dp, Color.White.copy(.12f), RoundedCornerShape(24.dp)).padding(20.dp)) { Text(title, color = V3Text, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = V3Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp)); Spacer(Modifier.height(14.dp)); content() } }

@Composable
private fun V3Action(title: String, accent: Color, onClick: () -> Unit) { var f by remember { mutableStateOf(false) }; Box(Modifier.padding(top = 9.dp).onFocusChanged { f = it.isFocused }.background(if (f) accent.copy(.18f) else Color.White.copy(.055f), RoundedCornerShape(12.dp)).border(if (f) 2.dp else 1.dp, if (f) accent else Color.White.copy(.10f), RoundedCornerShape(12.dp)).tvAction(onClick).padding(horizontal = 15.dp, vertical = 10.dp)) { Text(title, color = V3Text, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun V3Epg(store: PlaylistStore, demo: Boolean, glass: Int, accent: Color, onPlay: (Channel) -> Unit) { val channels = if (demo) DemoData.channels() else store.channels; val programmes = if (demo) DemoData.programmes() else store.programmes; Column(Modifier.fillMaxSize()) { V3Header("EPG", "Kompakte Programmübersicht · D-Pad-first"); Spacer(Modifier.height(14.dp)); if (channels.isEmpty()) Text("Keine Quelle geladen. Füge zuerst eine Playlist hinzu oder starte den Demo-Modus.", color = V3Muted, fontSize = 15.sp) else LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(bottom = 30.dp)) { items(channels) { channel -> val ps = programmes.filter { it.channelId == channel.id }.sortedBy { it.start }.take(10); V3EpgRow(channel, ps, glass, accent, onPlay) } } } }

@Composable
private fun V3EpgRow(channel: Channel, programmes: List<EpgProgramme>, glass: Int, accent: Color, onPlay: (Channel) -> Unit) { Row(Modifier.fillMaxWidth().height(92.dp).background(Color.White.copy(glassAlpha(glass, .055f)), RoundedCornerShape(17.dp)).border(1.dp, Color.White.copy(.08f), RoundedCornerShape(17.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.width(142.dp).fillMaxHeight(), contentAlignment = Alignment.CenterStart) { Row(verticalAlignment = Alignment.CenterVertically) { if (!channel.logoUrl.isNullOrBlank()) AsyncImage(channel.logoUrl, channel.name, Modifier.width(42.dp).height(42.dp), contentScale = ContentScale.Fit) else Box(Modifier.width(42.dp).height(42.dp).background(accent.copy(.10f), RoundedCornerShape(11.dp)), Alignment.Center) { Text(initials3(channel.name), color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold) }; Text(channel.name, color = V3Text, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2, modifier = Modifier.padding(start = 9.dp).width(82.dp)) } }; LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) { items(programmes) { p -> var f by remember(p.id) { mutableStateOf(false) }; val now = System.currentTimeMillis(); val live = now in p.start..p.end; Column(Modifier.width(132.dp).height(72.dp).onFocusChanged { f = it.isFocused }.focusable().background(if (f) accent.copy(.13f) else Color.Black.copy(.13f), RoundedCornerShape(12.dp)).border(if (f) 1.dp else 0.dp, if (f) accent.copy(.85f) else Color.Transparent, RoundedCornerShape(12.dp)).tvAction { onPlay(channel) }.padding(8.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text(SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(p.start)), color = if (live) accent else V3Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold); if (live) Text("  LIVE", color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold) }; Text(p.title, color = V3Text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, modifier = Modifier.padding(top = 4.dp)); if (p.isCatchupAvailable) Text("REPLAY", color = accent, fontSize = 7.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 3.dp)) } } } } }

@Composable
private fun V3Search(store: PlaylistStore, demo: Boolean, glass: Int, accent: Color, onPlay: (Channel) -> Unit) { var q by remember { mutableStateOf("") }; val channels = if (demo) DemoData.channels() else store.channels; val filtered = channels.filter { q.isBlank() || it.name.contains(q, true) || it.group.orEmpty().contains(q, true) }; val fieldColors = OutlinedTextFieldDefaults.colors(focusedTextColor = V3Text, unfocusedTextColor = V3Text, focusedLabelColor = accent, unfocusedLabelColor = V3Muted, cursorColor = accent, focusedBorderColor = accent, unfocusedBorderColor = Color.White.copy(.22f)); Column(Modifier.fillMaxSize()) { V3Header("Suche", "Sender und Gruppen"); OutlinedTextField(q, { q = it }, label = { Text("Suchen…") }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)); Spacer(Modifier.height(12.dp)); LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(filtered) { V3ChannelRow(it, glass, accent, onPlay) } } } }

@Composable
private fun V3Settings(settings: SettingsStore, glass: Int, accent: Color, onSources: () -> Unit) { var tab by remember { mutableIntStateOf(0) }; val tabs = listOf("Player", "Live TV", "EPG", "Darstellung", "Seitenleiste", "Quellen"); Column(Modifier.fillMaxSize()) { V3Header("Einstellungen", "Mit OK ändern · ←/→ Werte wechseln"); Spacer(Modifier.height(12.dp)); LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) { itemsIndexed(tabs) { i, t -> V3Pill(if (tab == i) "● $t" else t, accent) { tab = i } } }; Spacer(Modifier.height(12.dp)); when (tab) { 0 -> V3Panel("Player", "Wiedergabe", glass) { V3Choice("Engine", settings.player.engine.label, PlaybackEngine.entries.map { it.label }, accent) { v -> settings.updatePlayer(settings.player.copy(engine = PlaybackEngine.entries.first { it.label == v })) }; V3Choice("Buffer", settings.player.bufferMode.label, BufferMode.entries.map { it.label }, accent) { v -> settings.updatePlayer(settings.player.copy(bufferMode = BufferMode.entries.first { it.label == v })) } }; 1 -> V3Panel("Live TV", "Zapping und Catch-up", glass) { V3Toggle("Shared Catch-up bevorzugen", settings.player.preferCatchupSibling, accent) { settings.updatePlayer(settings.player.copy(preferCatchupSibling = it)) }; V3Toggle("Timeshift wenn verfügbar", settings.player.enableTimeshiftWhenAvailable, accent) { settings.updatePlayer(settings.player.copy(enableTimeshiftWhenAvailable = it)) } }; 2 -> V3Panel("EPG", "Ansicht", glass) { V3Choice("Zeitraum", settings.epg.pageSize.label, EpgPageSize.entries.map { it.label }, accent) { v -> settings.updateEpg(settings.epg.copy(pageSize = EpgPageSize.entries.first { it.label == v })) }; V3Toggle("Programmfortschritt", settings.epg.showProgrammeProgress, accent) { settings.updateEpg(settings.epg.copy(showProgrammeProgress = it)) } }; 3 -> V3Panel("Darstellung", "Änderungen wirken direkt", glass) { V3Choice("Theme", settings.ui.theme.label, ZenTheme.entries.map { it.label }, accent) { v -> settings.updateUi(settings.ui.copy(theme = ZenTheme.entries.first { it.label == v })) }; V3Choice("Hintergrund", settings.ui.animatedBackdrop.label, AnimatedBackdrop.entries.map { it.label }, accent) { v -> settings.updateUi(settings.ui.copy(animatedBackdrop = AnimatedBackdrop.entries.first { it.label == v })) }; V3Choice("Glasstärke", settings.ui.glassIntensity.toString(), (1..10).map { it.toString() }, accent) { v -> settings.updateUi(settings.ui.copy(glassIntensity = v.toInt())) }; V3Choice("UI-Skalierung", "${settings.ui.uiScale}%", (75..125 step 5).map { "$it%" }, accent) { v -> settings.updateUi(settings.ui.copy(uiScale = v.removeSuffix("%").toInt())) }; V3Toggle("Animationen", settings.ui.animations, accent) { settings.updateUi(settings.ui.copy(animations = it)) } }; 4 -> V3Panel("Seitenleiste", "Reihenfolge und Sichtbarkeit", glass) { SidebarCustomizer(settings, accent) }; else -> V3Panel("Quellen", "M3U, URL und Xtream", glass) { Text("Playlist-Verwaltung bleibt bewusst in den Einstellungen.", color = V3Muted, fontSize = 12.sp); V3Action("Quelle hinzufügen / wechseln", accent, onSources) } } } }

@Composable
private fun V3Panel(title: String, subtitle: String, glass: Int, content: @Composable () -> Unit) { Column(Modifier.fillMaxWidth().background(Color.White.copy(glassAlpha(glass, .065f)), RoundedCornerShape(22.dp)).border(1.dp, Color.White.copy(.11f), RoundedCornerShape(22.dp)).padding(18.dp)) { Text(title, color = V3Text, fontSize = 19.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = V3Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp)); Spacer(Modifier.height(10.dp)); content() } }

@Composable
private fun V3Choice(label: String, value: String, options: List<String>, accent: Color, onChange: (String) -> Unit) { var f by remember { mutableStateOf(false) }; var index by remember(value) { mutableIntStateOf(options.indexOf(value).coerceAtLeast(0)) }; fun change(delta: Int) { index = (index + delta + options.size) % options.size; onChange(options[index]) }; Row(Modifier.fillMaxWidth().height(50.dp).onFocusChanged { f = it.isFocused }.background(if (f) accent.copy(.11f) else Color.Transparent, RoundedCornerShape(13.dp)).border(if (f) 1.dp else 0.dp, if (f) accent.copy(.75f) else Color.Transparent, RoundedCornerShape(13.dp)).tvAction { change(1) }.onKeyEvent { e -> if (e.type != KeyEventType.KeyUp) return@onKeyEvent false; when (e.key) { Key.DirectionLeft -> { change(-1); true }; Key.DirectionRight -> { change(1); true }; else -> false } }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, color = V3Muted, fontSize = 12.sp); Spacer(Modifier.weight(1f)); Text(value, color = V3Text, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text("  ← / →", color = accent, fontSize = 9.sp) } }

@Composable
private fun V3Toggle(label: String, checked: Boolean, accent: Color, onChange: (Boolean) -> Unit) { var f by remember { mutableStateOf(false) }; Row(Modifier.fillMaxWidth().height(50.dp).onFocusChanged { f = it.isFocused }.background(if (f) accent.copy(.10f) else Color.Transparent, RoundedCornerShape(13.dp)).tvAction { onChange(!checked) }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, color = V3Text, fontSize = 12.sp); Spacer(Modifier.weight(1f)); Text(if (checked) "AN" else "AUS", color = if (checked) accent else V3Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold) } }

@Composable
private fun V3Player(channels: List<Channel>, index: Int, settings: SettingsStore, accent: Color, onClose: () -> Unit, onChannel: (Int) -> Unit) { BackHandler(onBack = onClose); var controls by remember { mutableStateOf(true) }; var current by remember(index) { mutableIntStateOf(index.coerceIn(0, channels.lastIndex)) }; val channel = channels[current]; Box(Modifier.fillMaxSize().background(Color.Black).onKeyEvent { e -> if (e.type != KeyEventType.KeyUp) return@onKeyEvent false; when (e.key) { Key.DirectionUp, Key.ChannelUp -> { current = (current - 1 + channels.size) % channels.size; onChannel(current); true }; Key.DirectionDown, Key.ChannelDown -> { current = (current + 1) % channels.size; onChannel(current); true }; Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { controls = !controls; true }; Key.Back -> { onClose(); true }; else -> false } }) { ZenPlayerScreen(channel, settings, onBack = onClose); if (controls) { Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp).background(Color.Black.copy(.70f), RoundedCornerShape(22.dp)).border(1.dp, Color.White.copy(.14f), RoundedCornerShape(22.dp)).padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(channel.name, color = V3Text, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text(channel.group ?: "Live TV", color = V3Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp)) }; V3PlayerButton("−", accent) { current = (current - 1 + channels.size) % channels.size; onChannel(current) }; Spacer(Modifier.width(8.dp)); V3PlayerButton("+", accent) { current = (current + 1) % channels.size; onChannel(current) }; Spacer(Modifier.width(8.dp)); V3PlayerButton("×", accent, onClose) }; Text("↑/↓ Sender wechseln · OK Overlay · Zurück schließen", color = V3Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 10.dp)) } } } }

@Composable
private fun V3PlayerButton(label: String, accent: Color, onClick: () -> Unit) { var f by remember { mutableStateOf(false) }; Box(Modifier.width(50.dp).height(50.dp).onFocusChanged { f = it.isFocused }.background(if (f) accent.copy(.20f) else Color.White.copy(.07f), RoundedCornerShape(15.dp)).border(if (f) 1.dp else 0.dp, if (f) accent else Color.Transparent, RoundedCornerShape(15.dp)).tvAction(onClick), Alignment.Center) { Text(label, color = V3Text, fontSize = 21.sp, fontWeight = FontWeight.Bold) } }

private fun initials3(name: String): String = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercaseChar().toString() }.ifBlank { "TV" }
