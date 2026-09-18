package com.zenplayer.tv

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The four primary destinations of ZenPlayer. This structure is intentional and permanent — see AGENTS.md. */
enum class HomeTile(val label: String, val subtitle: String, val icon: ImageVector, val tint: Color) {
    LIVE_TV("Live TV", "IPTV · Sender · EPG", Icons.Default.LiveTv, Color(0xFF52E1FF)),
    VOD("VOD", "Filme & Serien", Icons.Default.Movie, Color(0xFFB784FF)),
    MUSIC("Music", "Spotify · Apple Music · EclipseMusic", Icons.Default.MusicNote, Color(0xFF6CFFB0)),
    SETTINGS("Settings", "Theme · Player · Quellen", Icons.Default.Settings, Color(0xFFFFC46E)),
}

/**
 * The ZenPlayer home gate: four large horizontal tiles (Live TV | VOD | Music | Settings).
 * This is the primary navigation of the whole app and must not be replaced by a sidebar,
 * bottom nav, or vertical row layout — see AGENTS.md "Home Screen Rule".
 */
@Composable
fun ZenHomeGate(accent: Color, requestFocusToken: Any?, onSelect: (HomeTile) -> Unit) {
    val requesters = remember { HomeTile.entries.associateWith { FocusRequester() } }

    LaunchedEffect(requestFocusToken) {
        withFrameNanos { }
        runCatching { requesters.getValue(HomeTile.LIVE_TV).requestFocus() }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 40.dp)) {
        Spacer(Modifier.height(24.dp))
        Text("ZENPLAYER", color = Color.White.copy(.55f), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 6.sp)
        Spacer(Modifier.height(10.dp))
        Text("Willkommen zurück", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black)
        Text("Wähle einen Bereich", color = Color(0xFF9EA6B8), fontSize = 15.sp)
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.fillMaxWidth().height(340.dp).focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            HomeTile.entries.forEach { tile ->
                GateTileCard(
                    tile = tile,
                    accent = accent,
                    requester = requesters.getValue(tile),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onSelect = { onSelect(tile) }
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            "D-Pad zum Navigieren · OK zum Öffnen",
            color = Color(0xFF6B7484),
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GateTileCard(tile: HomeTile, accent: Color, requester: FocusRequester, modifier: Modifier, onSelect: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.045f else 1f, tween(220), label = "gateScale")
    val borderWidth by animateDpAsState(if (focused) 3.dp else 1.dp, tween(220), label = "gateBorder")
    val glowAlpha by animateFloatAsState(if (focused) 1f else 0f, tween(260), label = "gateGlow")

    Box(
        modifier
            .aspectRatio(0.86f, matchHeightConstraintsFirst = false)
            .focusRequester(requester)
            .onFocusChanged { focused = it.isFocused }
            .tvAction(onSelect)
            .focusable()
            .scale(scale)
            .background(
                Brush.verticalGradient(
                    listOf(
                        tile.tint.copy(alpha = if (focused) 0.22f else 0.10f),
                        Color.White.copy(alpha = ZenGlass.surface().alpha)
                    )
                ),
                RoundedCornerShape(32.dp)
            )
            .border(borderWidth, if (focused) tile.tint else Color.White.copy(.10f), RoundedCornerShape(32.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (glowAlpha > 0f) {
            Box(
                Modifier.fillMaxSize().padding(2.dp).background(
                    Brush.radialGradient(listOf(tile.tint.copy(alpha = .16f * glowAlpha), Color.Transparent)),
                    RoundedCornerShape(30.dp)
                )
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(84.dp).background(tile.tint.copy(alpha = if (focused) .28f else .16f), CircleShape),
                Alignment.Center
            ) {
                Icon(tile.icon, tile.label, tint = tile.tint, modifier = Modifier.size(38.dp))
            }
            Spacer(Modifier.height(22.dp))
            Text(tile.label, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text(
                tile.subtitle,
                color = Color(0xFF9EA6B8),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }
    }
}

