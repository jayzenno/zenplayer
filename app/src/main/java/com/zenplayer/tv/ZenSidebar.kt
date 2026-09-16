package com.zenplayer.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp

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
    LaunchedEffect(page, ordered.size) { firstRequester.requestFocus() }

    Column(
        Modifier.width(64.dp)
            .background(Color.White.copy(.035f), RoundedCornerShape(18.dp))
            .border(1.dp, Color.White.copy(.08f), RoundedCornerShape(18.dp))
            .padding(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ordered.forEachIndexed { index, item ->
            var focused by remember(item.id) { mutableStateOf(false) }
            Box(
                Modifier.width(46.dp).height(46.dp)
                    .background(if (page == item.page || focused) accent.copy(.18f) else Color.Transparent, RoundedCornerShape(14.dp))
                    .border(if (focused) 2.dp else 1.dp, if (focused || page == item.page) accent.copy(.9f) else Color.Transparent, RoundedCornerShape(14.dp))
                    .onFocusChanged { focused = it.isFocused }
                    .then(if (index == 0) Modifier.focusRequester(firstRequester) else Modifier)
                    .focusable()
                    .clickable { onPage(item.page) },
                Alignment.Center
            ) { Icon(item.icon, item.label, tint = if (page == item.page || focused) Color.White else Color(0xFF8E95A8)) }
        }
    }
}
