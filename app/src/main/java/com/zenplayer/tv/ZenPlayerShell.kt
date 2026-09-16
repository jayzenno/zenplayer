package com.zenplayer.tv

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.graphicsLayer
import com.zenplayer.tv.domain.model.Channel
import com.zenplayer.tv.domain.model.EpgProgramme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ShellBg = Color(0xFF07080D)
private val ShellText = Color(0xFFF5F6FA)
private val ShellSecondary = Color(0xFF9EA3B3)

@Composable
fun ZenPlayerShell(settings: SettingsStore) {
    val context = LocalContext.current
    val store = remember { PlaylistStore(context) }
    var page by remember { mutableStateOf("home") }
    var playing by remember { mutableStateOf<Channel?>(null) }
    var dataVersion by remember { mutableIntStateOf(0) }
    val accent = when (settings.ui.theme) {
        ZenTheme.AURORA -> Color(0xFF8D7CFF)
        ZenTheme.OBSIDIAN -> Color(0xFF65A8FF)
        ZenTheme.FROST -> Color(0xFF72D7D2)
        ZenTheme.AMBER -> Color(0xFFFFB45E)
    }
    val glow = when (settings.ui.theme) {
        ZenTheme.AURORA -> Color(0xFF342B70)
        ZenTheme.OBSIDIAN -> Color(0xFF142B4B)
        ZenTheme.FROST -> Color(0xFF153B3B)
        ZenTheme.AMBER -> Color(0xFF49301B)
    }
    val baseDensity = LocalDensity.current
    val scale = settings.ui.uiScale / 100f

    CompositionLocalProvider(LocalDensity provides Density(baseDensity.density * scale, baseDensity.fontScale)) {
        Box(Modifier.fillMaxSize().background(ShellBg)) {
            Box(Modifier.fillMaxSize().graphicsLayer(alpha = if (settings.ui.reducedMotion) .35f else .5f)) {
                ZenAnimatedBackdrop(settings.ui.theme, settings.ui.animatedBackdrop, settings.ui.animations && !settings.ui.reducedMotion)
            }
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(glow.copy(.65f), ShellBg, ShellBg), radius = 1250f)))
            if (playing != null) {
                ZenPlayerScreen(playing!!, settings) { playing = null }
            } else {
                Row(Modifier.fillMaxSize().padding(20.dp)) {
                    ShellNav(page, accent) { page = it }
                    Spacer(Modifier.width(16.dp))
                    when (page) {
                        "epg" -> RealEpgScreen(store.channels, store.programmes, store, accent) { playing = it }
                        "playlist" -> PlaylistScreen(store, accent) { dataVersion++; page = "home" }
                        "settings" -> ShellSettings(settings, accent)
                        "search" -> SearchScreen(store.channels, accent) { playing = it }
                        else -> ShellHome(store.channels, accent) { playing = it; page = "home" }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShellNav(page: String, accent: Color, onNavigate: (String) -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { first.requestFocus() }
    Column(Modifier.width(64.dp).fillMaxHeight().clipGlass(accent), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ShellNavItem(Icons.Default.Home, page == "home", accent, first) { onNavigate("home") }
        ShellNavItem(Icons.Default.LiveTv, page == "epg", accent) { onNavigate("epg") }
        ShellNavItem(Icons.Default.FolderOpen, page == "playlist", accent) { onNavigate("playlist") }
        ShellNavItem(Icons.Default.Search, page == "search", accent) { onNavigate("search") }
        Spacer(Modifier.weight(1f))
        ShellNavItem(Icons.Default.Settings, page == "settings", accent) { onNavigate("settings") }
    }
}

private fun Modifier.clipGlass(accent: Color): Modifier = this
    .background(Color.White.copy(.055f), RoundedCornerShape(21.dp))
    .border(1.dp, Color.White.copy(.08f), RoundedCornerShape(21.dp))
    .padding(vertical = 13.dp)

@Composable
private fun ShellNavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, accent: Color, requester: FocusRequester? = null, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.width(46.dp).height(46.dp).then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
        .background(if (focused || selected) accent.copy(.17f) else Color.Transparent, RoundedCornerShape(14.dp))
        .border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.Transparent, RoundedCornerShape(14.dp))
        .onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick)
        .onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && (e.key == Key.Enter || e.key == Key.NumPadEnter || e.key == Key.DirectionCenter)) { onClick(); true } else false }, Alignment.Center) {
        Icon(icon, null, tint = if (focused || selected) ShellText else ShellSecondary)
    }
}

@Composable
private fun ShellHome(channels: List<Channel>, accent: Color, onPlay: (Channel) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text("ZENPLAYER", color = ShellSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(205.dp).background(Brush.linearGradient(listOf(accent.copy(.22f), Color.White.copy(.06f), Color.Black.copy(.38f))), RoundedCornerShape(25.dp)).border(1.dp, Color.White.copy(.1f), RoundedCornerShape(25.dp)).padding(24.dp)) {
            Column(Modifier.align(Alignment.BottomStart)) {
                Text(if (channels.isEmpty()) "PLAYLIST FEHLT" else "LIVE TV", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(if (channels.isEmpty()) "Importiere deine M3U, um loszulegen" else "Deine Sender sind bereit", color = ShellText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(if (channels.isEmpty()) "Playlist → M3U importieren" else "${channels.size} Sender · OK zum Abspielen", color = ShellSecondary, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Sender", color = ShellText, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(9.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            items(channels.take(30)) { channel -> ChannelRow(channel, accent, onPlay) }
        }
    }
}

@Composable
private fun ChannelRow(channel: Channel, accent: Color, onPlay: (Channel) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(58.dp).background(if (focused) accent.copy(.13f) else Color.White.copy(.045f), RoundedCornerShape(15.dp))
        .border(if (focused) 2.dp else 1.dp, if (focused) accent.copy(.8f) else Color.White.copy(.06f), RoundedCornerShape(15.dp))
        .onFocusChanged { focused = it.isFocused }.focusable().clickable { onPlay(channel) }
        .onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && (e.key == Key.Enter || e.key == Key.NumPadEnter || e.key == Key.DirectionCenter)) { onPlay(channel); true } else false }
        .padding(horizontal = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(initials(channel.name), color = if (focused) accent else ShellSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))
        Column(Modifier.weight(1f)) { Text(channel.name, color = ShellText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold); Text(channel.group ?: "Live TV", color = ShellSecondary, fontSize = 10.sp) }
        Icon(Icons.Default.PlayArrow, null, tint = if (focused) ShellText else ShellSecondary)
    }
}

@Composable
private fun PlaylistScreen(store: PlaylistStore, accent: Color, onImported: () -> Unit) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    val playlistPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { runCatching { context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> reader.readText() } }.onSuccess { text -> store.importPlaylist(text); message = "${store.channels.size} Sender importiert" }.onFailure { message = "M3U konnte nicht gelesen werden" }; onImported() }
    }
    val epgPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { runCatching { context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> reader.readText() } }.onSuccess { text -> store.importEpg(text); message = "${store.programmes.size} EPG-Einträge importiert" }.onFailure { message = "XMLTV konnte nicht gelesen werden" }; onImported() }
    }
    Column(Modifier.fillMaxSize()) {
        Text("Playlist", color = ShellText, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("Deine Quellen · lokal gespeichert", color = ShellSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
        ImportCard("M3U / M3U8 Playlist", "Sender, Gruppen, tvg-id, Logos und Catch-up", accent) { playlistPicker.launch(arrayOf("*/*", "application/x-mpegURL", "audio/x-mpegurl")) }
        Spacer(Modifier.height(10.dp))
        ImportCard("XMLTV / EPG", "Programme, Zeiten und vom Nutzer gelieferte Logos", accent) { epgPicker.launch(arrayOf("*/*", "application/xml", "text/xml")) }
        Spacer(Modifier.height(18.dp))
        Text("${store.channels.size} Sender · ${store.programmes.size} Programme", color = ShellSecondary, fontSize = 13.sp)
        if (message.isNotBlank()) Text(message, color = accent, fontSize = 13.sp, modifier = Modifier.padding(top = 7.dp))
    }
}

@Composable
private fun ImportCard(title: String, subtitle: String, accent: Color, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(78.dp).background(if (focused) accent.copy(.14f) else Color.White.copy(.055f), RoundedCornerShape(18.dp))
        .border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(.07f), RoundedCornerShape(18.dp))
        .onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick)
        .onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && (e.key == Key.Enter || e.key == Key.NumPadEnter || e.key == Key.DirectionCenter)) { onClick(); true } else false }
        .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.FolderOpen, null, tint = accent); Spacer(Modifier.width(14.dp)); Column { Text(title, color = ShellText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, color = ShellSecondary, fontSize = 11.sp) }
    }
}

@Composable
private fun RealEpgScreen(channels: List<Channel>, programmes: List<EpgProgramme>, store: PlaylistStore, accent: Color, onPlay: (Channel) -> Unit) {
    val now = System.currentTimeMillis()
    Column(Modifier.fillMaxSize()) {
        Text("TV Guide", color = ShellText, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(if (programmes.isEmpty()) "Noch kein XMLTV importiert" else "Echtes XMLTV · ${programmes.size} Programme", color = ShellSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        if (channels.isEmpty()) {
            Text("Importiere zuerst eine M3U-Playlist.", color = ShellSecondary, fontSize = 15.sp)
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(channels) { channel ->
                val list = programmes.filter { it.channelId.equals(channel.tvgId ?: channel.id, true) || it.channelId.equals(channel.id, true) }.filter { it.end > now - 30 * 60 * 1000L }.take(8)
                Row(Modifier.fillMaxWidth().height(72.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.width(125.dp).height(66.dp).background(Color.White.copy(.055f), RoundedCornerShape(14.dp)).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(initials(channel.name), color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(channel.name, color = ShellText, fontSize = 10.sp, maxLines = 2)
                    }
                    Spacer(Modifier.width(8.dp))
                    if (list.isEmpty()) Text("Keine EPG-Daten für diesen Sender", color = ShellSecondary, fontSize = 11.sp)
                    else Column(verticalArrangement = Arrangement.spacedBy(5.dp)) { list.take(2).forEach { p ->
                        Row(Modifier.fillMaxWidth().height(28.dp).background(if (now >= p.start && now < p.end) accent.copy(.14f) else Color.White.copy(.045f), RoundedCornerShape(9.dp)).padding(horizontal = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(p.start)), color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(9.dp)); Text(p.title, color = ShellText, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        }
                    } }
                }
            }
        }
    }
}

@Composable
private fun SearchScreen(channels: List<Channel>, accent: Color, onPlay: (Channel) -> Unit) {
    Column(Modifier.fillMaxSize()) { Text("Sender suchen", color = ShellText, fontSize = 30.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(14.dp)); LazyColumn { items(channels) { ChannelRow(it, accent, onPlay) } } }
}

@Composable
private fun ShellSettings(settings: SettingsStore, accent: Color) {
    var externalPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val externalApps = remember {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).apply { type = "video/*" }
        context.packageManager.queryIntentActivities(intent, 0).distinctBy { it.activityInfo.packageName }
    }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Einstellungen", color = ShellText, fontSize = 30.sp, fontWeight = FontWeight.Bold); Text("Player, UI und Theme", color = ShellSecondary, fontSize = 12.sp) }
        item { SettingsCard("Player Engine", "Interner Player oder externe App", accent) {
            SettingChoice(settings.player.engine.label, PlaybackEngine.entries.map { it.label }, accent) { value -> settings.updatePlayer(settings.player.copy(engine = PlaybackEngine.entries.first { it.label == value })) }
            if (settings.player.engine == PlaybackEngine.EXTERNAL) {
                Spacer(Modifier.height(7.dp)); SettingsButton("Externen Player wählen", settings.player.externalPlayerPackage ?: "System-Auswahl", accent) { externalPicker = true }
            }
        }}
        item { SettingsCard("Darstellung", "Persönliche TV-Größe", accent) {
            ScaleSelector(settings.ui.uiScale, accent) { settings.updateUi(settings.ui.copy(uiScale = it)) }
            SettingChoice(settings.ui.animatedBackdrop.label, AnimatedBackdrop.entries.map { it.label }, accent) { value -> settings.updateUi(settings.ui.copy(animatedBackdrop = AnimatedBackdrop.entries.first { it.label == value })) }
            SettingChoice(settings.ui.theme.label, ZenTheme.entries.map { it.label }, accent) { value -> settings.updateUi(settings.ui.copy(theme = ZenTheme.entries.first { it.label == value })) }
            IntensitySelector(settings.ui.glassIntensity, accent) { settings.updateUi(settings.ui.copy(glassIntensity = it)) }
        }}
    }
    if (externalPicker) {
        PlayerPicker(externalApps, accent, onDismiss = { externalPicker = false }) { packageName -> settings.updatePlayer(settings.player.copy(externalPlayerPackage = packageName, engine = PlaybackEngine.EXTERNAL)); externalPicker = false }
    }
}

@Composable
private fun SettingsCard(title: String, subtitle: String, accent: Color, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White.copy(.055f), RoundedCornerShape(20.dp)).border(1.dp, Color.White.copy(.07f), RoundedCornerShape(20.dp)).padding(17.dp)) { Text(title, color = ShellText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, color = ShellSecondary, fontSize = 11.sp); Spacer(Modifier.height(10.dp)); content() }
}

@Composable
private fun SettingChoice(value: String, options: List<String>, accent: Color, onChange: (String) -> Unit) {
    var index by remember(value) { mutableIntStateOf(options.indexOf(value).coerceAtLeast(0)) }
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(48.dp).background(if (focused) accent.copy(.12f) else Color.White.copy(.035f), RoundedCornerShape(13.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.Transparent, RoundedCornerShape(13.dp)).onFocusChanged { focused = it.isFocused }.focusable().onKeyEvent { e ->
        if (e.type == KeyEventType.KeyUp && (e.key == Key.DirectionLeft || e.key == Key.DirectionRight || e.key == Key.DirectionCenter)) { index = if (e.key == Key.DirectionLeft) (index - 1 + options.size) % options.size else (index + 1) % options.size; onChange(options[index]); true } else false
    }.clickable { index = (index + 1) % options.size; onChange(options[index]) }.padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(value, color = if (focused) ShellText else ShellSecondary, fontSize = 13.sp); Spacer(Modifier.weight(1f)); Text("${index + 1}/${options.size}", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ScaleSelector(value: Int, accent: Color, onChange: (Int) -> Unit) {
    Column { Row(verticalAlignment = Alignment.CenterVertically) { Text("UI Scale", color = ShellText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); Text("$value%", color = accent, fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(7.dp)); Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { (75..125 step 5).forEach { v -> SettingNumber(v / 5, v == value, accent) { onChange(v) } } } }
}

@Composable
private fun IntensitySelector(value: Int, accent: Color, onChange: (Int) -> Unit) {
    Column { Spacer(Modifier.height(7.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Text("Milchglas / Glasstärke", color = ShellText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); Text("STUFE $value / 10", color = accent, fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(7.dp)); Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { (1..10).forEach { v -> SettingNumber(v, v == value, accent) { onChange(v) } } } }
}

@Composable
private fun SettingNumber(label: Int, selected: Boolean, accent: Color, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.width(30.dp).height(32.dp).background(if (selected || focused) accent.copy(.22f) else Color.White.copy(.045f), RoundedCornerShape(8.dp)).border(if (selected || focused) 2.dp else 1.dp, if (selected || focused) accent else Color.Transparent, RoundedCornerShape(8.dp)).onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick), Alignment.Center) { Text(label.toString(), color = if (selected || focused) ShellText else ShellSecondary, fontSize = 9.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
}

@Composable
private fun SettingsButton(title: String, subtitle: String, accent: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.White.copy(.045f), RoundedCornerShape(13.dp)).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text(title, color = ShellText, fontSize = 13.sp); Spacer(Modifier.weight(1f)); Text(subtitle, color = accent, fontSize = 10.sp) }
}

@Composable
private fun PlayerPicker(apps: List<android.content.pm.ResolveInfo>, accent: Color, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(.72f)).focusable().onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key == Key.Back) { onDismiss(); true } else false }, Alignment.Center) {
        Column(Modifier.width(520.dp).background(Color(0xF0161922), RoundedCornerShape(22.dp)).border(1.dp, accent.copy(.5f), RoundedCornerShape(22.dp)).padding(20.dp)) {
            Text("Externen Player auswählen", color = ShellText, fontSize = 20.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp))
            apps.take(12).forEach { info -> SettingsButton(info.loadLabel(LocalContext.current.packageManager).toString(), info.activityInfo.packageName, accent) { onPick(info.activityInfo.packageName) } }
            if (apps.isEmpty()) Text("Keine passende Video-App gefunden.", color = ShellSecondary, modifier = Modifier.padding(12.dp))
            Spacer(Modifier.height(8.dp)); Text("Zurück = schließen", color = ShellSecondary, fontSize = 10.sp)
        }
    }
}

private fun initials(name: String): String = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercaseChar().toString() }
