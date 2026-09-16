package com.zenplayer.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class NavItem(val id: String, val label: String, val page: Int, val icon: ImageVector)

@Composable
fun ZenSidebar(page: Int, accent: Color, settings: SettingsStore, onPage: (Int) -> Unit) {
    val all = listOf(
        NavItem("home", "Home", 0, Icons.Default.Home),
        NavItem("epg", "EPG", 1, Icons.Default.PlayArrow),
        NavItem("playlist", "Playlist", 2, Icons.Default.List),
        NavItem("search", "Suche", 3, Icons.Default.Search),
        NavItem("settings", "Settings", 4, Icons.Default.Settings),
    )
    val ordered = settings.ui.sidebarOrder.mapNotNull { id -> all.firstOrNull { it.id == id } }
        .filterNot { it.id != "settings" && it.id in settings.ui.sidebarHidden }
        .let { current -> current + all.filter { item -> current.none { it.id == item.id } && (item.id == "settings" || item.id !in settings.ui.sidebarHidden) } }
    val firstRequester = remember { FocusRequester() }
    LaunchedEffect(page, ordered.size) { runCatching { firstRequester.requestFocus() } }

    Column(
        Modifier.width(64.dp)
            .background(Color.White.copy(.035f), RoundedCornerShape(18.dp))
            .border(1.dp, Color.White.copy(.08f), RoundedCornerShape(18.dp))
            .padding(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.width(46.dp).height(46.dp)
                .background(accent.copy(.12f), RoundedCornerShape(14.dp))
                .border(1.dp, accent.copy(.28f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) { Text("Z", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }

        ordered.forEachIndexed { index, item ->
            var focused by remember(item.id) { mutableStateOf(false) }
            val selected = page == item.page
            Box(
                Modifier.width(46.dp).height(46.dp)
                    .background(if (selected || focused) accent.copy(if (focused) .22f else .16f) else Color.Transparent, RoundedCornerShape(14.dp))
                    .border(if (focused) 2.dp else 1.dp, if (focused || selected) accent.copy(.9f) else Color.Transparent, RoundedCornerShape(14.dp))
                    .onFocusChanged { focused = it.isFocused }
                    .then(if (index == 0) Modifier.focusRequester(firstRequester) else Modifier)
                    .focusable()
                    .onKeyEvent { event ->
                        val center = event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter
                        if (center && event.type == KeyEventType.KeyUp) { onPage(item.page); true }
                        else center && event.type == KeyEventType.KeyDown
                    },
                contentAlignment = Alignment.Center
            ) { Icon(item.icon, item.label, tint = if (selected || focused) Color.White else Color(0xFF8E95A8)) }
        }
    }
}
