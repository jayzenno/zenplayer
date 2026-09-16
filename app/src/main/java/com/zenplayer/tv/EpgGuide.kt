package com.zenplayer.tv

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zenplayer.tv.domain.model.EpgProgramme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val EpgText = Color(0xFFF5F6FA)
private val EpgSecondary = Color(0xFF9EA3B3)
private val EpgPanel = Color(0xCC11141D)

@Composable
fun EpgGuide(accent: Color) {
    val now = remember { System.currentTimeMillis() }
    var windowStart by remember { mutableLongStateOf(now - 2L * 60L * 60L * 1000L) }
    var selected by remember { mutableStateOf<EpgProgramme?>(null) }
    var contextProgramme by remember { mutableStateOf<EpgProgramme?>(null) }
    val channels = remember { listOf("Das Erste", "ZDF", "RTL", "ProSieben", "VOX") }
    val programmes = remember { demoProgrammes(now) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("TV Guide", color = EpgText, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text("Live-Programm", color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                GuideButton("‹ 6 Stunden", accent) { windowStart -= 6L * 60L * 60L * 1000L }
                Spacer(Modifier.width(8.dp))
                GuideButton("Jetzt", accent, emphasized = true) { windowStart = now - 2L * 60L * 60L * 1000L }
                Spacer(Modifier.width(8.dp))
                GuideButton("6 Stunden ›", accent) { windowStart += 6L * 60L * 60L * 1000L }
            }
            Spacer(Modifier.height(12.dp))
            TimeHeader(windowStart, now, accent)
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.weight(1f)) {
                items(channels) { channel ->
                    val channelProgrammes = programmes.filter { it.channelId == channel }
                    val channelLogo = channelProgrammes.firstOrNull()?.imageUrl
                    Row(Modifier.fillMaxWidth().height(84.dp), verticalAlignment = Alignment.CenterVertically) {
                        ChannelHeader(channel, channelLogo, accent, selected?.channelId == channel)
                        Spacer(Modifier.width(10.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxHeight().focusGroup(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 3.dp)
                        ) {
                            items(channelProgrammes) { programme ->
                                ProgrammeCard(
                                    programme, now, accent, programme == selected,
                                    onClick = { selected = programme },
                                    onLongPress = { selected = programme; contextProgramme = programme }
                                )
                            }
                        }
                    }
                }
            }
        }

        contextProgramme?.let { programme ->
            EpgContextMenu(
                programme = programme,
                accent = accent,
                onDismiss = { contextProgramme = null },
                onStartOver = { contextProgramme = null },
                onLive = { contextProgramme = null }
            )
        }
    }
}

@Composable
private fun TimeHeader(start: Long, now: Long, accent: Color) {
    val fmt = SimpleDateFormat("HH:mm", Locale.GERMANY)
    Row(Modifier.fillMaxWidth().height(42.dp).background(EpgPanel, RoundedCornerShape(14.dp)).border(1.dp, Color.White.copy(.07f), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(132.dp)) { Text("SENDER", color = EpgSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp) }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
            (0..6).forEach { i ->
                val time = start + i * 60L * 60L * 1000L
                val current = kotlin.math.abs(time - now) < 30L * 60L * 1000L
                Text(fmt.format(Date(time)), color = if (current) accent else EpgSecondary, fontSize = 10.sp, fontWeight = if (current) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun ChannelHeader(channel: String, logoUrl: String?, accent: Color, active: Boolean) {
    var focused by remember { mutableStateOf(false) }
    val highlighted = focused || active
    Column(
        Modifier.width(132.dp).height(78.dp).clip(RoundedCornerShape(17.dp))
            .background(if (highlighted) accent.copy(.13f) else Color.White.copy(.055f))
            .border(if (focused) 2.dp else 1.dp, if (focused) accent.copy(.82f) else Color.White.copy(.07f), RoundedCornerShape(17.dp))
            .onFocusChanged { focused = it.isFocused }.focusable().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(Color.White.copy(.045f)), Alignment.Center) {
            if (!logoUrl.isNullOrBlank()) AsyncImage(model = logoUrl, contentDescription = "$channel Logo", modifier = Modifier.fillMaxSize().padding(7.dp), contentScale = ContentScale.Fit)
            else Text(initials(channel), color = if (highlighted) accent else EpgText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(5.dp))
        Text(channel, color = EpgText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun ProgrammeCard(programme: EpgProgramme, now: Long, accent: Color, selected: Boolean, onClick: () -> Unit, onLongPress: () -> Unit) {
    val current = now >= programme.start && now < programme.end
    val duration = ((programme.end - programme.start) / 60000L).coerceAtLeast(1L)
    val width = (duration * 2.55f).coerceIn(155f, 350f).toInt().dp
    var focused by remember { mutableStateOf(false) }
    var pressedAt by remember { mutableLongStateOf(0L) }
    val transition = rememberInfiniteTransition(label = "epg-glow")
    val pulse by transition.animateFloat(.35f, .9f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "glow")
    val highlighted = focused || selected
    Box(Modifier.width(width).height(78.dp)) {
        if (focused) Box(Modifier.fillMaxSize().padding(1.dp).background(accent.copy(.08f * pulse), RoundedCornerShape(18.dp)))
        Row(Modifier.fillMaxSize().clip(RoundedCornerShape(17.dp))
            .background(if (highlighted) accent.copy(if (selected) .16f else .09f) else Color.White.copy(.055f))
            .border(if (focused) 2.dp else if (selected) 1.5.dp else 1.dp,
                when { focused -> accent.copy(.65f + .25f * pulse); selected -> accent.copy(.65f); else -> Color.White.copy(.07f) }, RoundedCornerShape(17.dp))
            .onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick)
            .onKeyEvent { event ->
                when {
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionCenter -> { if (pressedAt == 0L) pressedAt = System.currentTimeMillis(); true }
                    event.type == KeyEventType.KeyUp && event.key == Key.DirectionCenter -> {
                        val held = if (pressedAt == 0L) 0L else System.currentTimeMillis() - pressedAt
                        pressedAt = 0L; if (held >= 550L) onLongPress() else onClick(); true
                    }
                    else -> false
                }
            }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(programme.title, color = EpgText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Text(if (current) "JETZT · ${programme.durationSeconds / 60} min" else "${SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(programme.start))} · ${programme.category ?: "TV"}", color = if (current) accent else EpgSecondary, fontSize = 10.sp, fontWeight = if (current) FontWeight.Bold else FontWeight.Normal)
                if (current) {
                    val progress = ((now - programme.start).toFloat() / (programme.end - programme.start).toFloat()).coerceIn(0f, 1f)
                    Spacer(Modifier.height(6.dp)); Box(Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(.10f), RoundedCornerShape(2.dp))) { Box(Modifier.fillMaxWidth(progress).height(3.dp).background(accent, RoundedCornerShape(2.dp))) }
                }
            }
            if (programme.isCatchupAvailable) { Spacer(Modifier.width(9.dp)); Text("REPLAY", color = if (highlighted) accent else EpgSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
        }
    }
}

@Composable
private fun EpgContextMenu(programme: EpgProgramme, accent: Color, onDismiss: () -> Unit, onStartOver: () -> Unit, onLive: () -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { first.requestFocus() }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(.68f)).focusGroup()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && event.key == Key.Back) { onDismiss(); true } else true
            },
        Alignment.Center
    ) {
        Column(Modifier.width(500.dp).clip(RoundedCornerShape(26.dp)).background(Color(0xF0161922)).border(1.dp, accent.copy(.42f), RoundedCornerShape(26.dp)).padding(24.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(programme.title, color = EpgText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Sendungsmenü · OK auswählen · Zurück schließen", color = EpgSecondary, fontSize = 11.sp)
                }
                Icon(Icons.Default.Close, "Schließen", tint = EpgSecondary)
            }
            Spacer(Modifier.height(5.dp))
            ContextAction(Icons.Default.PlayArrow, "Live abspielen", "Zum Sender und aktuellen Live-Punkt", accent, first, onLive)
            if (programme.isCatchupAvailable) ContextAction(Icons.Default.SkipPrevious, "Von vorne starten", "Replay genau am Anfang der Sendung", accent, onClick = onStartOver)
            ContextAction(Icons.Default.Info, "Sendungsinfo", "Beschreibung, Zeiten und verfügbare EPG-Daten", accent) { }
            ContextAction(Icons.Default.Settings, "Sender-Einstellungen", "Audio, Untertitel, Bild und weitere Optionen", accent) { }
        }
    }
}

@Composable
private fun ContextAction(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, accent: Color, requester: FocusRequester? = null, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(58.dp).then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
        .clip(RoundedCornerShape(16.dp)).background(if (focused) accent.copy(.18f) else Color.White.copy(.055f))
        .border(if (focused) 2.dp else 1.dp, if (focused) accent.copy(.82f) else Color.White.copy(.07f), RoundedCornerShape(16.dp))
        .onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (focused) EpgText else accent); Spacer(Modifier.width(12.dp)); Column {
            Text(title, color = EpgText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = EpgSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun GuideButton(label: String, accent: Color, emphasized: Boolean = false, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.clip(RoundedCornerShape(14.dp)).background(if (focused || emphasized) accent.copy(if (focused) .20f else .11f) else Color.White.copy(.055f))
        .border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(.08f), RoundedCornerShape(14.dp))
        .onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 9.dp)) {
        Text(label, color = EpgText, fontSize = 11.sp, fontWeight = if (focused || emphasized) FontWeight.SemiBold else FontWeight.Normal)
    }
}

private fun initials(name: String): String = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercaseChar().toString() }

private fun demoProgrammes(now: Long): List<EpgProgramme> {
    val base = now - 3L * 60L * 60L * 1000L
    val channels = listOf("Das Erste", "ZDF", "RTL", "ProSieben", "VOX")
    val titles = listOf("Tagesschau", "Abendmagazin", "Prime Time", "Die große Show", "Spielfilm")
    return channels.flatMapIndexed { channelIndex, channel ->
        (0..9).map { i ->
            val start = base + (i * 45L + channelIndex * 7L) * 60L * 1000L
            EpgProgramme("demo-$channelIndex-$i", channel, titles[(i + channelIndex) % titles.size], start = start, end = start + 45L * 60L * 1000L, category = "TV", isCatchupAvailable = i < 6)
        }
    }
}