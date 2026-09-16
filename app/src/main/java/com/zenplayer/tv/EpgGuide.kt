package com.zenplayer.tv

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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenplayer.tv.domain.model.EpgProgramme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EpgGuide(accent: Color) {
    val now = remember { System.currentTimeMillis() }
    var windowStart by remember { mutableStateOf(now - 2L * 60L * 60L * 1000L) }
    var selected by remember { mutableStateOf<EpgProgramme?>(null) }
    val channels = remember { listOf("Das Erste", "ZDF", "RTL", "ProSieben", "VOX") }
    val programmes = remember { demoProgrammes(now) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("TV Guide", color = Color(0xFFF5F6FA), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text("D-Pad-first · 6-Stunden-Fenster · Catch-up direkt aus dem Guide", color = Color(0xFF9EA3B3), fontSize = 13.sp)
            }
            GuideButton("‹ 6h") { windowStart -= 6L * 60L * 60L * 1000L }
            Spacer(Modifier.width(8.dp)); GuideButton("Jetzt", accent) { windowStart = now - 2L * 60L * 60L * 1000L }
            Spacer(Modifier.width(8.dp)); GuideButton("6h ›") { windowStart += 6L * 60L * 60L * 1000L }
        }
        Spacer(Modifier.height(18.dp)); TimeRail(windowStart, accent); Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            items(channels) { channel ->
                Row(Modifier.fillMaxWidth().height(92.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(125.dp).height(78.dp).background(Color(0x22FFFFFF), RoundedCornerShape(18.dp)).border(1.dp, Color.White.copy(.08f), RoundedCornerShape(18.dp)).padding(14.dp), Alignment.CenterStart) { Text(channel, color = Color(0xFFF5F6FA), fontWeight = FontWeight.SemiBold) }
                    Spacer(Modifier.width(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(programmes.filter { it.channelId == channel }) { programme -> ProgrammeCard(programme, now, accent, programme == selected) { selected = programme } } }
                }
            }
        }
        selected?.let { Spacer(Modifier.height(12.dp)); ProgrammeActionBar(it, accent) }
    }
}

@Composable
private fun TimeRail(start: Long, accent: Color) {
    val fmt = SimpleDateFormat("HH:mm", Locale.GERMANY)
    Row(Modifier.fillMaxWidth().background(Color(0x16FFFFFF), RoundedCornerShape(14.dp)).padding(horizontal = 135.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        (0..6).forEach { i -> Text(fmt.format(Date(start + i * 60L * 60L * 1000L)), color = if (i == 2) accent else Color(0xFF9EA3B3), fontSize = 12.sp, fontWeight = if (i == 2) FontWeight.Bold else FontWeight.Normal) }
    }
}

@Composable
private fun ProgrammeCard(programme: EpgProgramme, now: Long, accent: Color, selected: Boolean, onClick: () -> Unit) {
    val current = now >= programme.start && now < programme.end
    val duration = ((programme.end - programme.start) / 60000L).coerceAtLeast(1L)
    val width = (duration * 3L).coerceIn(150L, 360L).toInt().dp
    Box(Modifier.width(width).height(78.dp).background(if (selected) Color(0x44FFFFFF) else Color(0x1FFFFFFF), RoundedCornerShape(18.dp)).border(if (selected) 2.dp else 1.dp, if (selected) accent else Color.White.copy(.08f), RoundedCornerShape(18.dp)).focusable().clickable(onClick = onClick).padding(12.dp)) {
        Column {
            Text(programme.title, color = Color(0xFFF5F6FA), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(if (current) "● JETZT  ·  ${programme.durationSeconds / 60} min" else "${SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(programme.start))}  ·  ${programme.category ?: "TV"}", color = if (current) accent else Color(0xFF9EA3B3), fontSize = 11.sp)
            if (current) {
                val progress = ((now - programme.start).toFloat() / (programme.end - programme.start).toFloat()).coerceIn(0f, 1f)
                Spacer(Modifier.height(7.dp)); Box(Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(.12f), RoundedCornerShape(2.dp))) { Box(Modifier.fillMaxWidth(progress).height(3.dp).background(accent, RoundedCornerShape(2.dp))) }
            }
        }
    }
}

@Composable
private fun ProgrammeActionBar(programme: EpgProgramme, accent: Color) {
    Row(Modifier.fillMaxWidth().background(Color(0x2AFFFFFF), RoundedCornerShape(22.dp)).border(1.dp, Color.White.copy(.1f), RoundedCornerShape(22.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(programme.title, color = Color(0xFFF5F6FA), fontSize = 18.sp, fontWeight = FontWeight.Bold); Text(programme.description ?: "Programm auswählen und direkt loslegen.", color = Color(0xFF9EA3B3), fontSize = 12.sp) }
        if (programme.isCatchupAvailable) Row(Modifier.background(accent.copy(.18f), RoundedCornerShape(16.dp)).clickable { }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.SkipPrevious, null, tint = Color(0xFFF5F6FA)); Spacer(Modifier.width(7.dp)); Text("Von vorne starten", color = Color(0xFFF5F6FA), fontWeight = FontWeight.SemiBold) }
        else Row(Modifier.background(Color.White.copy(.08f), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.PlayArrow, null, tint = Color(0xFF9EA3B3)); Spacer(Modifier.width(7.dp)); Text("Live", color = Color(0xFF9EA3B3), fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun GuideButton(label: String, accent: Color = Color(0xFFF5F6FA), onClick: () -> Unit) {
    Box(Modifier.background(Color(0x1FFFFFFF), RoundedCornerShape(14.dp)).border(1.dp, Color.White.copy(.08f), RoundedCornerShape(14.dp)).focusable().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp)) { Text(label, color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
}

private fun demoProgrammes(now: Long): List<EpgProgramme> {
    val base = now - 2L * 60L * 60L * 1000L
    val names = listOf("Das Erste", "ZDF", "RTL", "ProSieben", "VOX")
    return names.flatMap { channel -> listOf("Nachrichten", "Magazin", "Film & Serie", "Late Night").mapIndexed { index, title -> val start = base + index * 90L * 60L * 1000L; EpgProgramme("$channel-$index", channel, title, start = start, end = start + 90L * 60L * 1000L, category = "TV", isCatchupAvailable = index < 2) } }
}
