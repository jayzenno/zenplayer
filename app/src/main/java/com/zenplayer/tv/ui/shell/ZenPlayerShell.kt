package com.zenplayer.tv.ui.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenplayer.tv.*
import com.zenplayer.tv.core.navigation.*
import com.zenplayer.tv.core.theme.*

@Composable
fun ZenPlayerShell(settings: SettingsStore) {
    var state by remember { mutableStateOf(ZenNavigationState()) }
    val ui = settings.ui
    val tokens = when (ui.theme) {
        ZenTheme.AURORA -> ZenThemes.Aurora
        ZenTheme.OBSIDIAN -> ZenThemes.Obsidian
        ZenTheme.FROST -> ZenThemes.Frost
        ZenTheme.AMBER -> ZenThemes.Amber
    }
    BackHandler { state = reduceNavigation(state, ZenNavigationAction.Back) }
    ZenPlayerTheme(tokens) {
        val motion = ui.animations && !ui.reducedMotion
        val transition = rememberInfiniteTransition(label = "zen-bg")
        val drift by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(9000), RepeatMode.Reverse), label = "drift")
        Box(Modifier.fillMaxSize().background(tokens.background)) {
            if (motion) AnimatedBackground(tokens, drift) else Box(Modifier.fillMaxSize().background(tokens.background))
            Row(Modifier.fillMaxSize().padding(28.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                ZenSidebar(state, tokens) { page -> state = reduceNavigation(state, ZenNavigationAction.OpenPage, page) }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    AnimatedContent(state.page, transitionSpec = { if (motion) fadeIn(tween(180)) togetherWith fadeOut(tween(120)) else fadeIn() togetherWith fadeOut() }, label = "page") { page ->
                        when (page) {
                            ZenPage.Home -> HomePage(tokens) { state = reduceNavigation(state, ZenNavigationAction.OpenPreview) }
                            ZenPage.Live -> LivePage(tokens) { state = reduceNavigation(state, ZenNavigationAction.OpenPreview) }
                            ZenPage.Epg -> EpgPage(tokens) { state = reduceNavigation(state, ZenNavigationAction.OpenPreview) }
                            ZenPage.Search -> SimplePage(tokens, "Suche", "Sender, Programme und Quellen durchsuchen")
                            ZenPage.Settings -> SettingsPage(settings)
                        }
                    }
                }
            }
            if (state.playerOpen) PreviewPlayer(tokens, state.playerFullscreen, { state = if (state.playerPreview) reduceNavigation(state, ZenNavigationAction.PromotePreviewToFullscreen) else reduceNavigation(state, ZenNavigationAction.ClosePlayer) }) { state = reduceNavigation(state, ZenNavigationAction.ClosePlayer) }
        }
    }
}

@Composable private fun AnimatedBackground(tokens: ZenThemeTokens, drift: Float) {
    Box(Modifier.fillMaxSize().background(Color.Transparent))
}

@Composable private fun ZenSidebar(state: ZenNavigationState, tokens: ZenThemeTokens, onPage: (ZenPage) -> Unit) {
    val pages = listOf(ZenPage.Home to Icons.Default.Tv, ZenPage.Live to Icons.Default.LiveTv, ZenPage.Epg to Icons.Default.VideoLibrary, ZenPage.Search to Icons.Default.Search, ZenPage.Settings to Icons.Default.Settings)
    Column(Modifier.width(210.dp).fillMaxHeight().zenGlass(tokens, strong = true).padding(14.dp).focusGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("ZENPLAYER", fontSize = 20.sp, modifier = Modifier.padding(14.dp))
        pages.forEach { (page, icon) -> SidebarItem(page, state.page == page, tokens, icon) { onPage(page) } }
    }
}

@Composable private fun SidebarItem(page: ZenPage, selected: Boolean, tokens: ZenThemeTokens, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.045f else 1f, label = "sidebar-scale")
    Row(Modifier.fillMaxWidth().scale(scale).background(if (selected) tokens.glassStrong else Color.Transparent, RoundedCornerShape(16.dp)).border(if (focused) 2.dp else 1.dp, if (focused) tokens.focusGlow else Color.Transparent, RoundedCornerShape(16.dp)).focusable().onFocusChanged { focused = it.isFocused }.onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && (e.key == Key.Enter || e.key == Key.DirectionCenter)) { onClick(); true } else false }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (focused || selected) tokens.accent else tokens.textMuted)
        Spacer(Modifier.width(12.dp)); Text(page.name, color = if (focused || selected) tokens.text else tokens.textMuted, fontSize = 15.sp)
    }
}

@Composable private fun HomePage(tokens: ZenThemeTokens, onPreview: () -> Unit) = Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
    Text("Für dich", fontSize = 36.sp); Text("Dein persönlicher Live-TV-Startbildschirm", color = tokens.textMuted, fontSize = 17.sp)
    PreviewCard(tokens, "Weitersehen", "Live TV", onPreview); Text("Schnellzugriff", fontSize = 22.sp)
}

@Composable private fun LivePage(tokens: ZenThemeTokens, onPreview: () -> Unit) = Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
    Text("Live TV", fontSize = 34.sp); Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { repeat(4) { PreviewCard(tokens, "Sender ${it + 1}", "Jetzt läuft", onPreview, Modifier.weight(1f)) } }
}

@Composable private fun EpgPage(tokens: ZenThemeTokens, onPreview: () -> Unit) = Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Text("TV Guide", fontSize = 34.sp); Text("Horizontale EPG-Vorschau", color = tokens.textMuted)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.width(160.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { repeat(5) { Text("Sender ${it + 1}", Modifier.zenGlass(tokens).padding(14.dp)) } }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) { repeat(5) { PreviewCard(tokens, "${18 + it}:00", "Programm ${it + 1}", onPreview, Modifier.width(190.dp)) } }
    }
}

@Composable private fun PreviewCard(tokens: ZenThemeTokens, title: String, subtitle: String, onPreview: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.04f else 1f, label = "card-scale")
    Box(modifier.height(128.dp).scale(scale).zenGlass(tokens, strong = focused).border(1.dp, if (focused) tokens.focusGlow else Color.Transparent, RoundedCornerShape(24.dp)).focusable().onFocusChanged { focused = it.isFocused }.onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && (e.key == Key.Enter || e.key == Key.DirectionCenter)) { onPreview(); true } else false }.padding(20.dp), contentAlignment = Alignment.BottomStart) { Column { Text(title, fontSize = 18.sp); Text(subtitle, color = tokens.textMuted, fontSize = 13.sp) } }
}

@Composable private fun SimplePage(tokens: ZenThemeTokens, title: String, subtitle: String) = Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(title, fontSize = 34.sp); Text(subtitle, color = tokens.textMuted, fontSize = 17.sp) }

@Composable private fun SettingsPage(settings: SettingsStore) {
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Look & Feel", fontSize = 34.sp)
        Text("Ändere das Design und sieh jede Änderung direkt in der Vorschau.", color = ZenThemes.Aurora.textMuted)
        LookAndFeelStudio(settings, Modifier.weight(1f).fillMaxWidth())
    }
}

@Composable private fun PreviewPlayer(tokens: ZenThemeTokens, fullscreen: Boolean, onOk: () -> Unit, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (fullscreen) 1f else .78f)).focusable().onKeyEvent { e -> if (e.type != KeyEventType.KeyUp) false else when (e.key) { Key.Enter, Key.DirectionCenter -> { onOk(); true }; Key.Back -> { onBack(); true }; else -> false } }, contentAlignment = if (fullscreen) Alignment.Center else Alignment.BottomEnd) {
        Box(Modifier.then(if (fullscreen) Modifier.fillMaxSize() else Modifier.width(600.dp).height(340.dp)).background(Color(0xFF05070B), RoundedCornerShape(if (fullscreen) 0.dp else 24.dp)).border(1.dp, tokens.focusGlow.copy(alpha = .6f), RoundedCornerShape(if (fullscreen) 0.dp else 24.dp)), contentAlignment = Alignment.BottomStart) {
            Column(Modifier.padding(24.dp)) { Text("Preview Player", fontSize = 22.sp); Text(if (fullscreen) "Fullscreen · OK zum Schließen" else "Preview · OK erneut für Fullscreen", color = tokens.textMuted) }
        }
    }
}
