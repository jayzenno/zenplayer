package com.zenplayer.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZenPlayerApp() }
    }
}

private val Background = Color(0xFF07080D)
private val Glass = Color(0x22FFFFFF)
private val GlassStrong = Color(0x36FFFFFF)
private val TextPrimary = Color(0xFFF5F6FA)
private val TextSecondary = Color(0xFF9EA3B3)

@Composable
fun ZenPlayerApp() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF25204A), Background, Background),
                    radius = 1100f
                )
            )
    ) {
        Row(Modifier.fillMaxSize().padding(26.dp)) {
            GlassNavigation()
            Spacer(Modifier.width(24.dp))
            HomeContent()
        }
    }
}

@Composable
private fun GlassNavigation() {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(78.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Glass)
            .border(1.dp, Color.White.copy(alpha = .09f), RoundedCornerShape(26.dp))
            .padding(vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        NavIcon(Icons.Default.Home, "Home", true)
        NavIcon(Icons.Default.LiveTv, "Live", false)
        NavIcon(Icons.Default.Search, "Search", false)
        Spacer(Modifier.weight(1f))
        NavIcon(Icons.Default.Settings, "Settings", false)
    }
}

@Composable
private fun NavIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean) {
    Box(
        modifier = Modifier
            .width(54.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) GlassStrong else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) TextPrimary else TextSecondary)
    }
}

@Composable
private fun HomeContent() {
    Column(Modifier.fillMaxSize()) {
        Text("ZENPLAYER", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(12.dp))
        HeroCard()
        Spacer(Modifier.height(22.dp))
        Section("Live jetzt", listOf("Das Erste", "ZDF", "RTL", "ProSieben", "VOX"))
        Spacer(Modifier.height(20.dp))
        Section("Für dich", listOf("ARD", "ZDFneo", "3sat", "arte", "ONE"))
    }
}

@Composable
private fun HeroCard() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF302B62), Color(0xFF11131D), Color(0x99111420))
                )
            )
            .border(1.dp, Color.White.copy(alpha = .12f), RoundedCornerShape(30.dp))
            .padding(30.dp)
    ) {
        Column(Modifier.align(Alignment.BottomStart)) {
            Text("LIVE · DAS ERSTE", color = Color.White.copy(alpha = .72f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Tagesschau", color = TextPrimary, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text("20:00 – 20:15  ·  Nachrichten", color = TextSecondary, fontSize = 15.sp)
        }
    }
}

@Composable
private fun Section(title: String, channels: List<String>) {
    Column {
        Text(title, color = TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(channels) { ChannelCard(it) }
        }
    }
}

@Composable
private fun ChannelCard(name: String) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.055f else 1f, label = "focus")
    Box(
        Modifier
            .scale(scale)
            .width(190.dp)
            .height(108.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (focused) GlassStrong else Glass)
            .border(1.5.dp, if (focused) Color.White.copy(.65f) else Color.White.copy(.08f), RoundedCornerShape(22.dp))
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .padding(18.dp)
    ) {
        Column(Modifier.align(Alignment.BottomStart)) {
            Text(name, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text("LIVE", color = TextSecondary, fontSize = 11.sp)
        }
    }
}
