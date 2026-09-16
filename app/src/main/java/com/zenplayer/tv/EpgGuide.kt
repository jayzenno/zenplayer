package com.zenplayer.tv

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenplayer.tv.domain.model.EpgProgramme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val EpgText = Color(0xFFF5F6FA)
private val EpgSecondary = Color(0xFF9EA3B3)

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
                    Text("OK = auswählen · OK halten = Kontextmenü · ← → = Zeit", color = EpgSecondary, fontSize = 13.sp)
                }
                GuideButton("‹ 6h", accent) { windowStart -= 6L * 60L * 60L * 1000L }
                Spacer(Modifier.width(8.dp))
                GuideButton("Jetzt", accent) { windowStart = now - 2L * 60L * 60L * 1000L }
                Spacer(Modifier.width(8.dp))
                GuideButton("6h ›", accent) { windowStart += 6L * 60L * 60L * 1000L }
            }
            Spacer(Modifier.height(18.dp))
            TimeRail(windowStart, accent)
            Spacer(Modifier.height(10.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(channels) { channel ->
                    Row(Modifier.fillMaxWidth().height(92.dp), verticalAlignment = Alignment.CenterVertically) {
                        ChannelHeader(channel, accent)
                        Spacer(Modifier.width(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(programmes.filter { it.channelId == channel }) { programme ->
                                ProgrammeCard(
                                    programme = programme,
                                    now = now,
                                    accent = accent,
                                    selected = programme == selected,
                                    onClick = { selected = programme },
                                    onLongPress = { contextProgramme = programme }
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
                onStartOver = {
                    // Playback wiring comes next; the action is intentionally exposed only here.
                    contextProgramme = null
                },
                onLive = { contextProgramme = null }
            )
        }
    }
}

@Composable
private fun ChannelHeader(channel: String, accent: Color) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier.width(125.dp).height(78.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (focused) accent.copy(.12f) else Color.White.copy(.08f))
            .border(if (focused) 2.dp else 1.dp, if (focused) accent.copy(.75f) else Color.White.copy(.08f), RoundedCornerShape(18.dp))
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .padding(14.dp),
        Alignment.CenterStart
    ) {
        Text(channel, color = EpgText, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TimeRail(start: Long, accent: Color) {
    val fmt = SimpleDateFormat("HH:mm", Locale.GERMANY)
    Row(
        Modifier.fillMaxWidth().background(Color(0x16FFFFFF), RoundedCornerShape(14.dp)).padding(horizontal = 135.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        (0..6).forEach { i ->
            Text(
                fmt.format(Date(start + i * 60L * 60L * 1000L)),
                color = if (i == 2) accent else EpgSecondary,
                fontSize = 12.sp,
                fontWeight = if (i == 2) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ProgrammeCard(
    programme: EpgProgramme,
    now: Long,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val current = now >= programme.start && now < programme.end
    val duration = ((programme.end - programme.start) / 60000L).coerceAtLeast(1L)
    val width = (duration * 3L).coerceIn(150L, 360L).toInt().dp
    var focused by remember { mutableStateOf(false) }
    var pressedAt by remember { mutableLongStateOf(0L) }
    val transition = rememberInfiniteTransition(label = "epg-glow")
    val pulse by transition.animateFloat(
        initialValue = 0.62f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300), RepeatMode.Reverse),
        label = "glow"
    )
    val highlighted = focused || selected

    Box(
        Modifier.width(width).height(78.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (highlighted) accent.copy(if (selected) .18f else .11f) else Color.White.copy(.08f))
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) accent.copy(alpha = pulse) else if (selected) accent.copy(.7f) else Color.White.copy(.08f),
                RoundedCornerShape(18.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .onKeyEvent { event ->
                when {
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionCenter -> {
                        if (pressedAt == 0L) pressedAt = System.currentTimeMillis()
                        true
                    }
                    event.type == KeyEventType.KeyUp && event.key == Key.DirectionCenter -> {
                        val held = if (pressedAt == 0L) 0L else System.currentTimeMillis() - pressedAt
                        pressedAt = 0L
                        if (held >= 550L) onLongPress() else onClick()
                        true
                    }
                    else -> false
                }
            }
            .padding(12.dp)
    ) {
        Column {
            Text(programme.title, color = EpgText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (current) "● JETZT  ·  ${programme.durationSeconds / 60} min"
                else "${SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(programme.start))}  ·  ${programme.category ?: "TV"}",
                color = if (current) accent else EpgSecondary,
                fontSize = 11.sp
            )
            if (current) {
                val progress = ((now - programme.start).toFloat() / (programme.end - programme.start).toFloat()).coerceIn(0f, 1f)
                Spacer(Modifier.height(7.dp))
                Box(Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(.12f), RoundedCornerShape(2.dp))) {
                    Box(Modifier.fillMaxWidth(progress).height(3.dp).background(accent, RoundedCornerShape(2.dp)))
                }
            }
        }
    }
}

@Composable
private fun EpgContextMenu(
    programme: EpgProgramme,
    accent: Color,
    onDismiss: () -> Unit,
    onStartOver: () -> Unit,
    onLive: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(.58f)).clickable(onClick = onDismiss),
        Alignment.Center
    ) {
        Column(
            Modifier.width(520.dp).clip(RoundedCornerShape(28.dp))
                .background(Color(0xE8151720))
                .border(1.dp, accent.copy(.45f), RoundedCornerShape(28.dp))
                .padding(26.dp)
                .clickable { },
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(programme.title, color = EpgText, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text("Sendungsmenü", color = EpgSecondary, fontSize = 13.sp)
                }
                Icon(Icons.Default.Close, "Schließen", tint = EpgSecondary)
            }
            Spacer(Modifier.height(8.dp))
            ContextAction(Icons.Default.PlayArrow, "Live abspielen", "Zum laufenden Sender", accent, onLive)
            if (programme.isCatchupAvailable) {
                ContextAction(Icons.Default.SkipPrevious, "Von vorne starten", "Sendung ab Anfang wiedergeben", accent, onStartOver)
            }
            ContextAction(Icons.Default.Info, "Sendungsinfo", "Beschreibung, Zeit und EPG-Daten", accent) { }
            ContextAction(Icons.Default.Settings, "Sender-Einstellungen", "Audio, Untertitel, Bild und weitere Optionen", accent) { }
        }
    }
}

@Composable
private fun ContextAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(62.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(if (focused) accent.copy(.18f) else Color.White.copy(.06f))
            .border(if (focused) 2.dp else 1.dp, if (focused) accent.copy(.8f) else Color.White.copy(.07f), RoundedCornerShape(17.dp))
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (focused) EpgText else accent)
        Spacer(Modifier.width(13.dp))
        Column {
            Text(title, color = EpgText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = EpgSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun GuideButton(label: String, accent: Color, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier.background(if (focused) accent.copy(.15f) else Color.White.copy(.08f), RoundedCornerShape(14.dp))
            .border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(.08f), RoundedCornerShape(14.dp))
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(label, color = if (focused) EpgText else accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun demoProgrammes(now: Long): List<EpgProgramme> {
    val base = now - 2L * 60L * 60L * 1000L
    val names = listOf("Das Erste", "ZDF", "RTL", "ProSieben", "VOX")
    return names.flatMap { channel ->
        listOf("Nachrichten", "Magazin", "Film & Serie", "Late Night").mapIndexed { index, title ->
            val start = base + index * 90L * 60L * 1000L
            EpgProgramme(
                id = "$channel-$index",
                channelId = channel,
                title = title,
                start = start,
                end = start + 90L * 60L * 1000L,
                category = "TV",
                isCatchupAvailable = index < 2
            )
        }
    }
}
