package com.zenplayer.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SidebarCustomizer(settings: SettingsStore, accent: Color) {
    val names = linkedMapOf("home" to "Home", "epg" to "EPG", "search" to "Suche", "settings" to "Settings")
    val order = settings.ui.sidebarOrder.filter { it in names.keys }.ifEmpty { names.keys.toList() }
    val first = remember { FocusRequester() }
    LaunchedEffect(order) { runCatching { first.requestFocus() } }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Seitenleiste", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text("↑/↓ Sender verschieben · OK Sichtbarkeit · ←/→ Navigation", color = Color(0xFF9EA6B8), fontSize = 11.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            itemsIndexed(order, key = { _, id -> id }) { index, id ->
                SidebarEditRow(id, names.getValue(id), id in settings.ui.sidebarHidden, accent, if (index == 0) first else null, index + 1,
                    canMoveUp = index > 0, canMoveDown = index < order.lastIndex,
                    onToggle = {
                        if (id == "settings") return@SidebarEditRow
                        val hidden = settings.ui.sidebarHidden.toMutableSet()
                        if (!hidden.add(id)) hidden.remove(id)
                        settings.updateUi(settings.ui.copy(sidebarHidden = hidden))
                    },
                    onMove = { direction ->
                        val mutable = settings.ui.sidebarOrder.filter { it in names.keys }.toMutableList()
                        val from = mutable.indexOf(id)
                        val to = (from + direction).coerceIn(0, mutable.lastIndex)
                        if (from >= 0 && from != to) {
                            val value = mutable.removeAt(from)
                            mutable.add(to, value)
                            settings.updateUi(settings.ui.copy(sidebarOrder = mutable))
                        }
                    })
            }
        }
        FocusButton("Standardreihenfolge", accent) {
            settings.updateUi(settings.ui.copy(sidebarOrder = listOf("home", "epg", "search", "settings"), sidebarHidden = emptySet()))
        }
    }
}

@Composable
private fun SidebarEditRow(id: String, title: String, hidden: Boolean, accent: Color, requester: FocusRequester?, position: Int, canMoveUp: Boolean, canMoveDown: Boolean, onToggle: () -> Unit, onMove: (Int) -> Unit) {
    var rowFocused by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(56.dp).then(if (requester != null) Modifier.focusRequester(requester) else Modifier).onFocusChanged { rowFocused = it.hasFocus }.background(if (rowFocused) accent.copy(.12f) else Color.White.copy(.05f), RoundedCornerShape(14.dp)).border(if (rowFocused) 2.dp else 1.dp, if (rowFocused) accent.copy(.85f) else Color.White.copy(.09f), RoundedCornerShape(14.dp)).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$position", color = if (rowFocused) accent else Color(0xFF70788A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(title, color = if (hidden) Color(0xFF666D7C) else Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(start = 10.dp))
        Text(if (hidden) "VERSTECKT" else "AKTIV", color = if (hidden) Color(0xFF666D7C) else accent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        FocusButton("↑", accent, canMoveUp) { onMove(-1) }
        FocusButton("↓", accent, canMoveDown) { onMove(1) }
        if (id != "settings") FocusButton(if (hidden) "AN" else "AUS", accent) { onToggle() }
    }
}

@Composable
private fun FocusButton(label: String, accent: Color, enabled: Boolean = true, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.padding(start = 6.dp).focusable(enabled).onFocusChanged { focused = it.isFocused }.background(if (focused && enabled) accent.copy(.18f) else Color.Transparent, RoundedCornerShape(9.dp)).border(if (focused && enabled) 2.dp else 1.dp, if (focused && enabled) accent else Color.White.copy(.07f), RoundedCornerShape(9.dp)).tvAction(enabled, onClick).padding(horizontal = 9.dp, vertical = 7.dp), contentAlignment = Alignment.Center) {
        Text(label, color = if (enabled) Color.White else Color(0xFF4F5562), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

private fun Modifier.tvAction(enabled: Boolean, action: () -> Unit): Modifier = this.then(if (enabled) Modifier.tvAction(action) else Modifier)
