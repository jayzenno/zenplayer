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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenplayer.tv.domain.model.Channel

@Composable
fun ZenHomeScreen(store: PlaylistStore, accent: Color, refresh: Int, onPlay: (Channel) -> Unit, onOpenPlaylist: () -> Unit) {
    val channels = store.channels
    val first = remember { FocusRequester() }
    LaunchedEffect(refresh, channels.size) { first.requestFocus() }
    Box(Modifier.fillMaxSize().padding(8.dp)) {
        if (channels.isEmpty()) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ZENPLAYER", color = Color.White.copy(.9f), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                Spacer(Modifier.height(10.dp))
                Text("Dein TV. Deine Oberfläche.", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                Text("Importiere eine M3U-Playlist, dann baut ZenPlayer deine Startseite daraus.", color = Color(0xFF9EA6B8), fontSize = 13.sp)
                Spacer(Modifier.height(24.dp))
                ActionCard("Playlist importieren", "M3U / M3U8 · Sender · Logos · Catch-up", Icons.Default.List, accent, first, onOpenPlaylist)
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Text("Für dich", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("${channels.size} Sender · bereit zum Abspielen", color = Color(0xFF9EA6B8), fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    channels.take(12).forEachIndexed { index, channel ->
                        ChannelCard(channel, accent, if (index == 0) first else null) { onPlay(channel) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, requester: FocusRequester, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(.65f).height(72.dp).background(Brush.linearGradient(listOf(accent.copy(.18f), Color.White.copy(.055f))), RoundedCornerShape(20.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(.09f), RoundedCornerShape(20.dp)).focusRequester(requester).onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = accent)
        Column(Modifier.padding(start = 14.dp)) { Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = Color(0xFF9EA6B8), fontSize = 10.sp) }
    }
}

@Composable
private fun ChannelCard(channel: Channel, accent: Color, requester: FocusRequester?, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(58.dp).background(if (focused) accent.copy(.14f) else Color.White.copy(.045f), RoundedCornerShape(16.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(.07f), RoundedCornerShape(16.dp)).then(if (requester != null) Modifier.focusRequester(requester) else Modifier).onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(initials(channel.name), color = if (focused) accent else Color(0xFF9EA6B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 16.dp))
        Column(Modifier.weight(1f)) { Text(channel.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold); Text(channel.group ?: "Live TV", color = Color(0xFF9EA6B8), fontSize = 10.sp) }
        Icon(Icons.Default.PlayArrow, null, tint = if (focused) Color.White else Color(0xFF747C8E))
    }
}
