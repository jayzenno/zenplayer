package com.zenplayer.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenplayer.tv.domain.model.Channel
import com.zenplayer.tv.domain.model.EpgProgramme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val V7Text = Color(0xFFF7F8FC)
private val V7Muted = Color(0xFF8D96AA)
private val V7Bg = Color(0xFF05070D)
private val V7Keys = setOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)

@Composable
fun ZenPlayerShellV7(settings: SettingsStore) {
    var page by remember { mutableStateOf("epg") }
    var replay by remember { mutableStateOf<Channel?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { PlaylistStore(context) }
    val accent = when (settings.ui.theme) { ZenTheme.AURORA -> Color(0xFF70E6FF); ZenTheme.OBSIDIAN -> Color(0xFFAAA8FF); ZenTheme.FROST -> Color(0xFF9FEAFF); ZenTheme.AMBER -> Color(0xFFFFC46E) }
    BackHandler { if (replay != null) replay = null else if (page != "epg") page = "epg" else Unit }
    if (replay != null) {
        ZenPlayerScreen(replay!!, settings, { replay = null }, { false }, true)
    } else if (page == "home") {
        ZenPlayerShellV6(settings)
        Box(Modifier.fillMaxSize().padding(22.dp), Alignment.TopEnd) { V7Button("EPG V7", accent) { page = "epg" } }
    } else {
        Box(Modifier.fillMaxSize().background(V7Bg)) {
            ZenAnimatedBackdrop(settings.ui.theme, settings.ui.animatedBackdrop, !settings.ui.reducedMotion)
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("EPG", color = V7Text, fontSize = 34.sp, fontWeight = FontWeight.Black); Text("V7 · Live / Replay / Von vorne starten", color = V7Muted, fontSize = 13.sp) }
                    V7Button("Live TV", accent) { page = "home" }
                }
                Spacer(Modifier.height(14.dp))
                V7Epg(store, accent) { replay = it }
            }
        }
    }
}

@Composable private fun V7Epg(store: PlaylistStore, accent: Color, onPlay: (Channel) -> Unit) {
    val now = System.currentTimeMillis(); var day by remember { mutableIntStateOf(0) }; var details by remember { mutableStateOf<Pair<Channel, EpgProgramme>?>(null) }
    val center = now + day * 86_400_000L; val from = center - 6 * 3_600_000L; val to = center + 18 * 3_600_000L
    Box(Modifier.fillMaxSize()) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 30.dp)) {
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V7Button("‹ Gestern", accent) { day = (day - 1).coerceAtLeast(-1) }
                V7Button("Heute", accent) { day = 0 }
                V7Button("Morgen ›", accent) { day = (day + 1).coerceAtMost(1) }
            } }
            store.channels.forEach { c ->
                val ps = store.programmes.filter { it.channelId == c.id && it.end > from && it.start < to }.sortedBy { it.start }
                if (ps.isNotEmpty()) {
                    item { Text(c.name, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
                    items(ps, key = { it.id }) { p ->
                        val current = p.start <= now && p.end > now; val past = p.end <= now
                        V7Programme(p, accent, current, past) { details = c to p }
                    }
                }
            }
        }
        details?.let { (c, p) ->
            V7Details(c, p, accent, onClose = { details = null }) {
                val current = p.start <= System.currentTimeMillis() && p.end > System.currentTimeMillis()
                val resolved = if (!current && p.end <= System.currentTimeMillis() && p.isCatchupAvailable) CatchupResolver.resolve(c, p, store.channels, true) else null
                if (resolved != null) onPlay(resolved) else if (current) onPlay(c)
                details = null
            }
        }
    }
}

@Composable private fun V7Programme(p: EpgProgramme, accent: Color, current: Boolean, past: Boolean, onOpen: () -> Unit) {
    var focused by remember(p.id) { mutableStateOf(false) }
    val status = when { current -> "● LIVE"; past && p.isCatchupAvailable -> "REPLAY"; past -> "VERGANGEN"; else -> "SPÄTER" }
    Row(Modifier.fillMaxWidth().height(68.dp).focusable().onFocusChanged { focused = it.isFocused }.onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key in V7Keys) { onOpen(); true } else false }.background(if (focused || current) accent.copy(if (focused) .17f else .09f) else Color.White.copy(.05f), RoundedCornerShape(15.dp)).border(if (focused || current) 2.dp else 1.dp, if (focused || current) accent else Color.Transparent, RoundedCornerShape(15.dp)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(p.start)), color = accent, fontSize = 11.sp, modifier = Modifier.width(52.dp)); Column(Modifier.weight(1f)) { Text(p.title, color = V7Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Text(p.subtitle ?: p.category ?: "TV", color = V7Muted, fontSize = 9.sp) }; Text(status, color = if (current || p.isCatchupAvailable) accent else V7Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun V7Details(channel: Channel, p: EpgProgramme, accent: Color, onClose: () -> Unit, onPlay: () -> Unit) {
    val now = System.currentTimeMillis(); val past = p.end <= now; val current = p.start <= now && !past; val replayAvailable = past && p.isCatchupAvailable
    Box(Modifier.fillMaxSize().background(Color(0xEE05070D)), Alignment.Center) { Column(Modifier.width(760.dp).background(Color(0xF0181D29), RoundedCornerShape(28.dp)).border(1.dp, Color.White.copy(.14f), RoundedCornerShape(28.dp)).padding(28.dp)) {
        Text("${channel.name} · SENDUNGSDETAILS", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text(p.title, color = V7Text, fontSize = 28.sp, fontWeight = FontWeight.Black); Text("${fmtV7(p.start)} – ${fmtV7(p.end)}", color = V7Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)); p.subtitle?.takeIf { it.isNotBlank() }?.let { Text(it, color = V7Text, fontSize = 15.sp, modifier = Modifier.padding(top = 12.dp)) }; p.description?.takeIf { it.isNotBlank() }?.let { Text(it, color = V7Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp), maxLines = 6) }; Spacer(Modifier.height(18.dp)); Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            when { replayAvailable -> V7Button("▶ Von vorne starten", accent, onPlay); current -> V7Button("▶ Live ansehen", accent, onPlay) }
            V7Button("Zurück", accent, onClose)
        }
    } }
}

@Composable private fun V7Button(title: String, accent: Color, action: () -> Unit) { var focused by remember { mutableStateOf(false) }; Box(Modifier.focusable().onFocusChanged { focused = it.isFocused }.onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key in V7Keys) { action(); true } else false }.background(if (focused) accent.copy(.18f) else Color.White.copy(.07f), RoundedCornerShape(12.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(.1f), RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 10.dp)) { Text(title, color = V7Text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) } }
private fun fmtV7(ms: Long) = SimpleDateFormat("dd.MM. HH:mm", Locale.GERMANY).format(Date(ms))
