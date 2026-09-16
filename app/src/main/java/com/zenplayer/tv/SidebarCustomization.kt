package com.zenplayer.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SidebarCustomizer(settings: SettingsStore, accent: Color) {
    val names = mapOf("home" to "Home", "epg" to "EPG", "playlist" to "Playlist", "search" to "Suche", "settings" to "Settings")
    val order = settings.ui.sidebarOrder.filter { it in names.keys }
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("Seitenleiste", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Reihenfolge ändern oder Bereiche ausblenden. Settings bleibt als Sicherheitsanker sichtbar.", color = Color(0xFF9EA6B8), fontSize = 11.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(order) { id ->
                SidebarEditRow(id, names.getValue(id), id in settings.ui.sidebarHidden, accent,
                    canMoveUp = order.indexOf(id) > 0,
                    canMoveDown = order.indexOf(id) < order.lastIndex,
                    onToggle = {
                        if (id == "settings") return@SidebarEditRow
                        val hidden = settings.ui.sidebarHidden.toMutableSet()
                        if (!hidden.add(id)) hidden.remove(id)
                        settings.updateUi(settings.ui.copy(sidebarHidden = hidden))
                    },
                    onMove = { direction ->
                        val mutable = settings.ui.sidebarOrder.toMutableList()
                        val from = mutable.indexOf(id)
                        val to = (from + direction).coerceIn(0, mutable.lastIndex)
                        if (from != to) {
                            val value = mutable.removeAt(from)
                            mutable.add(to, value)
                            settings.updateUi(settings.ui.copy(sidebarOrder = mutable))
                        }
                    })
            }
        }
        SmallFocusButton("Standardreihenfolge wiederherstellen", accent) {
            settings.updateUi(settings.ui.copy(sidebarOrder = listOf("home", "epg", "playlist", "search", "settings"), sidebarHidden = emptySet()))
        }
    }
}

@Composable
private fun SidebarEditRow(id: String, title: String, hidden: Boolean, accent: Color, canMoveUp: Boolean, canMoveDown: Boolean, onToggle: () -> Unit, onMove: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().height(46.dp).background(Color.White.copy(.045f), RoundedCornerShape(13.dp)).border(1.dp, Color.White.copy(.07f), RoundedCornerShape(13.dp)).padding(horizontal = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = if (hidden) Color(0xFF666D7C) else Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        EditButton("↑", accent, canMoveUp) { onMove(-1) }
        EditButton("↓", accent, canMoveDown) { onMove(1) }
        EditButton(if (hidden) "AN" else "AUS", accent, id != "settings") { onToggle() }
    }
}

@Composable
private fun EditButton(label: String, accent: Color, enabled: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.padding(start = 5.dp).background(if (focused && enabled) accent.copy(.18f) else Color.Transparent, RoundedCornerShape(8.dp)).border(if (focused && enabled) 1.dp else 0.dp, if (focused && enabled) accent else Color.Transparent, RoundedCornerShape(8.dp)).onFocusChanged { focused = it.isFocused }.focusable(enabled).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 8.dp, vertical = 5.dp), contentAlignment = Alignment.Center) {
        Text(label, color = if (enabled) Color.White else Color(0xFF4F5562), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SmallFocusButton(title: String, accent: Color, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.background(if (focused) accent.copy(.16f) else Color.White.copy(.04f), RoundedCornerShape(10.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(.07f), RoundedCornerShape(10.dp)).onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 7.dp)) {
        Text(title, color = Color.White, fontSize = 10.sp)
    }
}
