package com.zenplayer.tv.ui.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenplayer.tv.core.navigation.FocusZone
import com.zenplayer.tv.core.navigation.ZenNavigationAction
import com.zenplayer.tv.core.navigation.ZenNavigationState
import com.zenplayer.tv.core.navigation.ZenPage
import com.zenplayer.tv.core.navigation.reduceNavigation
import com.zenplayer.tv.core.theme.ZenPlayerTheme
import com.zenplayer.tv.core.theme.ZenThemeTokens
import com.zenplayer.tv.core.theme.ZenThemes
import com.zenplayer.tv.core.theme.zenGlass

@Composable
fun ZenPlayerShell() {
    var state by remember { mutableStateOf(ZenNavigationState()) }
    var tokens by remember { mutableStateOf(ZenThemes.Aurora) }

    BackHandler {
        state = reduceNavigation(state, ZenNavigationAction.Back)
    }

    ZenPlayerTheme(tokens) {
        val infinite = rememberInfiniteTransition(label = "zen-background")
        val drift by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(9000), RepeatMode.Reverse),
            label = "background-drift",
        )

        Box(Modifier.fillMaxSize().background(tokens.background)) {
            AnimatedBackground(tokens, drift)
            Row(Modifier.fillMaxSize().padding(28.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                ZenSidebar(
                    state = state,
                    tokens = tokens,
                    onPage = { page ->
                        state = reduceNavigation(state, ZenNavigationAction.OpenPage, page)
                    },
                    onSources = {
                        state = reduceNavigation(state, ZenNavigationAction.OpenSources)
                    },
                )
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    AnimatedContent(
                        targetState = state.page,
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                        label = "zen-page",
                    ) { page ->
                        when (page) {
                            ZenPage.Home -> HomePage(tokens, state) { state = reduceNavigation(state, ZenNavigationAction.OpenPreview) }
                            ZenPage.Live -> LivePage(tokens, state) { state = reduceNavigation(state, ZenNavigationAction.OpenPreview) }
                            ZenPage.Epg -> EpgPage(tokens, state) { state = reduceNavigation(state, ZenNavigationAction.OpenPreview) }
                            ZenPage.Search -> PlaceholderPage(tokens, "Search", "Search across channels, programmes and sources")
                            ZenPage.Settings -> SettingsPage(tokens) { tokens = it }
                        }
                    }
                }
            }

            if (state.playerOpen) {
                PreviewPlayer(
                    tokens = tokens,
                    fullscreen = state.playerFullscreen,
                    onOk = {
                        state = if (state.playerPreview) {
                            reduceNavigation(state, ZenNavigationAction.PromotePreviewToFullscreen)
                        } else {
                            reduceNavigation(state, ZenNavigationAction.ClosePlayer)
                        }
                    },
                    onBack = { state = reduceNavigation(state, ZenNavigationAction.ClosePlayer) },
                )
            }
        }
    }
}

@Composable
private fun AnimatedBackground(tokens: ZenThemeTokens, drift: Float) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(tokens.backgroundSecondary.copy(alpha = 0.92f), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(0.18f + drift * 0.25f, 0.2f),
                radius = 950f,
            )
        )
    )
}

@Composable
private fun ZenSidebar(
    state: ZenNavigationState,
    tokens: ZenThemeTokens,
    onPage: (ZenPage) -> Unit,
    onSources: () -> Unit,
) {
    val items = listOf(
        ZenPage.Home to Icons.Default.Tv,
        ZenPage.Live to Icons.Default.LiveTv,
        ZenPage.Epg to Icons.Default.VideoLibrary,
        ZenPage.Search to Icons.Default.Search,
        ZenPage.Settings to Icons.Default.Settings,
    )
    Column(
        Modifier.width(210.dp).fillMaxHeight().zenGlass(tokens, strong = true).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("ZENPLAYER", fontSize = 20.sp, letterSpacing = 1.5.sp, modifier = Modifier.padding(14.dp))
        items.forEach { (page, icon) ->
            SidebarItem(
                page = page,
                selected = state.page == page,
                tokens = tokens,
                icon = icon,
                onClick = { onPage(page) },
            )
        }
        Spacer(Modifier.weight(1f))
        SidebarItem(ZenPage.Live, false, tokens, Icons.Default.VideoLibrary, "Sources", onSources)
    }
}

@Composable
private fun SidebarItem(
    page: ZenPage,
    selected: Boolean,
    tokens: ZenThemeTokens,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String = page.name,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by androidx.compose.animation.core.animateFloatAsState(if (focused) 1.045f else 1f, label = "sidebar-scale")
    val borderColor by animateColorAsState(if (focused) tokens.focusGlow.copy(alpha = 0.9f) else Color.Transparent, label = "sidebar-border")
    Row(
        Modifier
            .fillMaxWidth()
            .scale(scale)
            .background(if (selected) tokens.glassStrong else Color.Transparent, RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .onKeyEvent {
                if (it.type == KeyEventType.KeyUp && it.key == Key.Enter) {
                    onClick(); true
                } else false
            }
            .focusable()
            .padding(horizontal = 14.dp, vertical = 13.dp)
            .then(Modifier)
            .onFocusChangedCompat { focused = it },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (focused || selected) tokens.accent else tokens.textMuted)
        Spacer(Modifier.width(12.dp))
        Text(label, color = if (focused || selected) tokens.text else tokens.textMuted, fontSize = 15.sp)
    }
}

@Composable
private fun HomePage(tokens: ZenThemeTokens, state: ZenNavigationState, onPreview: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Text("Good evening", fontSize = 36.sp)
        Text("Your TV, your way.", color = tokens.textMuted, fontSize = 18.sp)
        PreviewCard(tokens, "Continue watching", "Live TV", onPreview)
        Text("Quick access", fontSize = 22.sp)
    }
}

@Composable
private fun LivePage(tokens: ZenThemeTokens, state: ZenNavigationState, onPreview: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Live TV", fontSize = 34.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { index -> PreviewCard(tokens, "Channel ${index + 1}", "Now playing", onPreview, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun EpgPage(tokens: ZenThemeTokens, state: ZenNavigationState, onPreview: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("TV Guide", fontSize = 34.sp)
        Text("Horizontal guide foundation — preview on first OK, fullscreen on second OK.", color = tokens.textMuted)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.width(160.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(5) { Text("Channel ${it + 1}", Modifier.zenGlass(tokens).padding(14.dp)) }
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(5) { index ->
                    PreviewCard(tokens, "${18 + index}:00", "Programme ${index + 1}", onPreview, Modifier.width(190.dp))
                }
            }
        }
    }
}

@Composable
private fun PreviewCard(
    tokens: ZenThemeTokens,
    title: String,
    subtitle: String,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by androidx.compose.animation.core.animateFloatAsState(if (focused) 1.04f else 1f, label = "card-scale")
    Box(
        modifier
            .height(128.dp)
            .scale(scale)
            .zenGlass(tokens, strong = focused)
            .border(1.dp, if (focused) tokens.focusGlow else Color.Transparent, RoundedCornerShape(24.dp))
            .onKeyEvent {
                if (it.type == KeyEventType.KeyUp && it.key == Key.Enter) { onPreview(); true } else false
            }
            .focusable()
            .onFocusChangedCompat { focused = it }
            .padding(20.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column {
            Text(title, fontSize = 18.sp)
            Text(subtitle, color = tokens.textMuted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun PlaceholderPage(tokens: ZenThemeTokens, title: String, subtitle: String) {
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, fontSize = 34.sp)
        Text(subtitle, color = tokens.textMuted, fontSize = 17.sp)
    }
}

@Composable
private fun SettingsPage(tokens: ZenThemeTokens, onTheme: (ZenThemeTokens) -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Themes", fontSize = 34.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ThemeButton("Aurora", ZenThemes.Aurora, onTheme)
            ThemeButton("Obsidian", ZenThemes.Obsidian, onTheme)
            ThemeButton("Frost", ZenThemes.Frost, onTheme)
            ThemeButton("Amber", ZenThemes.Amber, onTheme)
        }
        Text("Shared themes, receiver skins, wallpapers and reduced-motion controls will plug into this token system.", color = tokens.textMuted)
    }
}

@Composable
private fun ThemeButton(label: String, theme: ZenThemeTokens, onClick: (ZenThemeTokens) -> Unit) {
    Box(
        Modifier.width(150.dp).height(90.dp).zenGlass(theme).onKeyEvent {
            if (it.type == KeyEventType.KeyUp && it.key == Key.Enter) { onClick(theme); true } else false
        }.focusable().padding(16.dp),
        contentAlignment = Alignment.BottomStart,
    ) { Text(label) }
}

@Composable
private fun PreviewPlayer(
    tokens: ZenThemeTokens,
    fullscreen: Boolean,
    onOk: () -> Unit,
    onBack: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (fullscreen) 1f else 0.78f))
            .onKeyEvent {
                when {
                    it.type != KeyEventType.KeyUp -> false
                    it.key == Key.Enter -> { onOk(); true }
                    it.key == Key.Back -> { onBack(); true }
                    else -> false
                }
            }.focusable(),
        contentAlignment = if (fullscreen) Alignment.Center else Alignment.BottomEnd,
    ) {
        Box(
            Modifier
                .then(if (fullscreen) Modifier.fillMaxSize() else Modifier.width(600.dp).height(340.dp))
                .background(Color(0xFF05070B), RoundedCornerShape(if (fullscreen) 0.dp else 24.dp))
                .border(1.dp, tokens.focusGlow.copy(alpha = 0.6f), RoundedCornerShape(if (fullscreen) 0.dp else 24.dp))
        ) {
            Column(Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                Text("Preview Player", fontSize = 22.sp)
                Text(if (fullscreen) "Fullscreen • OK to close" else "Preview • OK again for fullscreen", color = tokens.textMuted)
            }
        }
    }
}

private fun Modifier.onFocusChangedCompat(onChanged: (Boolean) -> Unit): Modifier =
    androidx.compose.ui.focus.onFocusChanged { onChanged(it.isFocused) }
