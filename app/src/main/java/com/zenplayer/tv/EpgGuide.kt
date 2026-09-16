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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/** Cinematic 10-foot EPG. Artwork is always supplied by the user's playlist/EPG. */
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
                    Text("TV Guide", color = EpgText, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Text("Live-Programm", color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                GuideButton("‹ 6 Stunden", accent) { windowStart -= 6L * 60L * 60L * 1000L }
                Spacer(Modifier.width(8.dp))
                GuideButton("Jetzt", accent, emphasized = true) { windowStart = now - 2L * 60L * 60L * 1000L }
                Spacer(Modifier.width(8.dp))
                GuideButton("6 Stunden ›", accent) { windowStart += 6L * 60L * 60L * 1000L }
            }

            Spacer(Modifier.height(16.dp))
            TimeHeader(windowStart, now, accent)
            Spacer(Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(channels) { channel ->
                    val channelProgrammes = programmes.filter { it.channelId == channel }
                    val channelLogo = channelProgrammes.firstOrNull()?.imageUrl
                    Row(
                        Modifier.fillMaxWidth().height(94.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChannelHeader(channel, channelLogo, accent, selected?.channelId == channel)
                        Spacer(Modifier.width(12.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxHeight().focusGroup(),
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)
                        ) {
                            items(channelProgrammes) { programme ->
                                ProgrammeCard(
                                    programme = programme,
                                    now = now,
                                    accent = accent,
                                    selected = programme == selected,
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
    Row(
        Modifier.fillMaxWidth().height(46.dp)
            .background(EpgPanel, RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(.07f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(145.dp)) {
            Text("SENDER", color = EpgSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
            (0..6).forEach { i ->
                val time = start + i * 60L * 60L * 1000L
                val current = kotlin.math.abs(time - now) < 30L * 60L * 1000L
                Text(
                    fmt.format(Date(time)),
                    color = if (current) accent else EpgSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (current) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun ChannelHeader(channel: String, logoUrl: String?, accent: Color, active: Boolean) {
    var focused by remember { mutableStateOf(false) }
    val highlighted = focused || active
    Row(
        Modifier.width(145.dp).height(86.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (highlighted) accent.copy(.13f) else Color.White.copy(.055f))
            .border(if (focused) 2.dp else 1.dp, if (focused) accent.copy(.8f) else Color.White.copy(.07f), RoundedCornerShape(20.dp))
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(42.dp).height(42.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(.07f)), Alignment.Center) {
            if (!logoUrl.isNullOrBlank()) {
                AsyncImage(model = logoUrl, contentDescription = "$channel Logo", modifier = Modifier.fillMaxSize().padding(5.dp), contentScale = ContentScale.Fit)
            } else {
                Text(initials(channel), color = if (highlighted) accent else EpgText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(channel, color = EpgText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Text(if (active) "LIVE" else "TV", color = if (active) accent else EpgSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
    val width = (duration * 3L).coerceIn(175L, 410L).toInt().dp
    var focused by remember { mutableStateOf(false) }
    var pressedAt by remember { mutableLongStateOf(0L) }
    val transition = rememberInfiniteTransition(label = "epg-glow")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "glow"
    )
    val highlighted = focused || selected

    Box(Modifier.width(width).height(86.dp)) {
        if (focused) {
            Box(
                Modifier.fillMaxSize().padding(1.dp)
                    .background(accent.copy(alpha = .10f * pulse), RoundedCornerShape(21.dp))
            )
        }
        Row(
            Modifier.fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(if (highlighted) accent.copy(if (selected) .17f else .10f) else Color.White.copy(.055f))
                .border(
                    if (focused) 2.dp else if (selected) 1.5.dp else 1.dp,
                    when {
                        focused -> accent.copy(alpha = .65f + .25f * pulse)
                        selected -> accent.copy(.65f)
                        else -> Color.White.copy(.07f)
                    },
                    RoundedCornerShape(20.dp)
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
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(programme.title, color = EpgText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Spacer(Modifier.height(5.dp))
                Text(
                    if (current) "JETZT  ·  ${programme.durationSeconds / 60} min"
                    else "${SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(programme.start))}  ·  ${programme.category ?: "TV"}",
                    color = if (current) accent else EpgSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (current) FontWeight.Bold else FontWeight.Normal
                )
                if (current) {
                    val progress = ((now - programme.start).toFloat() / (programme.end - programme.start).toFloat()).coerceIn(0f, 1f)
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(.10f), RoundedCornerShape(2.dp))) {
                        Box(Modifier.fillMaxWidth(progress).height(3.dp).background(accent, RoundedCornerShape(2.dp)))
                    }
                }
            }
            if (programme.isCatchupAvailable) {
                Spacer(Modifier.width(12.dp))
                Text("REPLAY", color = if (highlighted) accent else EpgSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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
    val first = remember { FocusRequester() }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(.64f)), Alignment.Center) {
        Column(
            Modifier.width(540.dp).clip(RoundedCornerShape(30.dp))
                .background(Color(0xF0161922))
                .border(1.dp, accent.copy(.42f), RoundedCornerShape(30.dp))
                .padding(28.dp)
                .clickable { },
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(programme.title, color = EpgText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Sendungsmenü  ·  OK auswählen  ·  Zurück schließen", color = EpgSecondary, fontSize = 12.sp)
                }
                Icon(Icons.Default.Close, "Schließen", tint = EpgSecondary)
            }
            Spacer(Modifier.height(8.dp))
            ContextAction(Icons.Default.PlayArrow, "Live abspielen", "Zum Sender und aktuellem Live-Punkt", accent, first, onLive)
            if (programme.isCatchupAvailable) ContextAction(Icons.Default.SkipPrevious, "Von vorne starten", "Replay genau am Anfang der Sendung", accent, onClick = onStartOver)
            ContextAction(Icons.Default.Info, "Sendungsinfo", "Beschreibung, Zeiten und verfügbare EPG-Daten", accent) { }
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
    requester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(64.dp)
            .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
            .clip(RoundedCornerShape(18.dp))
            .background(if (focused) accent.copy(.18f) else Color.White.copy(.055f))
            .border(if (focused) 2.dp else 1.dp, if (focused) accent.copy(.82f) else Color.White.copy(.07f), RoundedCornerShape(18.dp))
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (focused) EpgText else accent)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, color = EpgText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = EpgSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun GuideButton(label: String, accent: Color, emphasized: Boolean = false, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier.clip(RoundedCornerShape(15.dp))
            .background(if (focused || emphasized) accent.copy(if (focused) .20f else .11f) else Color.White.copy(.055f))
            .border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(.08f), RoundedCornerShape(15.dp))
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(label, color = if (focused || emphasized) EpgText else EpgSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun initials(name: String): String =
    name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "TV" }

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
