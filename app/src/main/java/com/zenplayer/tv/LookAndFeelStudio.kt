package com.zenplayer.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenplayer.tv.core.theme.ZenPlayerTheme
import com.zenplayer.tv.core.theme.ZenThemeTokens
import com.zenplayer.tv.core.theme.ZenThemes

@Composable
fun LookAndFeelStudio(settings: SettingsStore, modifier: Modifier = Modifier) {
    val ui = settings.ui
    val tokens = ui.theme.toThemeTokens()

    Row(modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(
            Modifier.width(350.dp).fillMaxHeight().focusGroup(),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text("Look & Feel", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Text("Änderungen werden sofort angewendet", color = Color.White.copy(.62f), fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))

            StudioChoice("Theme", ui.theme.label, ZenTheme.entries.map { it.label }, tokens.accent) { value ->
                settings.updateUi(ui.copy(theme = ZenTheme.entries.first { it.label == value }))
            }
            StudioChoice("Hintergrund", ui.animatedBackdrop.label, AnimatedBackdrop.entries.map { it.label }, tokens.accent) { value ->
                settings.updateUi(ui.copy(animatedBackdrop = AnimatedBackdrop.entries.first { it.label == value }))
            }
            StudioChoice("Glasstärke", "${ui.glassIntensity}/10", (1..10).map { "$it/10" }, tokens.accent) { value ->
                settings.updateUi(ui.copy(glassIntensity = value.substringBefore('/').toInt()))
            }
            StudioChoice("UI-Größe", "${ui.uiScale}%", listOf(75, 85, 95, 100, 110, 120, 125).map { "$it%" }, tokens.accent) { value ->
                settings.updateUi(ui.copy(uiScale = value.removeSuffix("%").toInt()))
            }
            StudioToggle("Animationen", ui.animations, tokens.accent) { checked ->
                settings.updateUi(ui.copy(animations = checked))
            }
            StudioToggle("Reduced Motion", ui.reducedMotion, tokens.accent) { checked ->
                settings.updateUi(ui.copy(reducedMotion = checked))
            }
        }

        Column(Modifier.weight(1f).fillMaxHeight()) {
            Text("Live Vorschau", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("So sieht deine aktuelle Konfiguration in ZenPlayer aus.", color = Color.White.copy(.58f), fontSize = 10.sp)
            Spacer(Modifier.height(7.dp))
            ThemePreview(ui, tokens, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun ThemePreview(ui: UiSettings, tokens: ZenThemeTokens, modifier: Modifier) {
    val baseDensity = LocalDensity.current
    val scale = ui.uiScale.coerceIn(75, 125) / 100f
    CompositionLocalProvider(LocalDensity provides Density(baseDensity.density * scale, baseDensity.fontScale)) {
        ZenPlayerTheme(tokens) {
            Box(modifier.clipGlass(tokens)) {
                ZenAnimatedBackdrop(ui.theme, ui.animatedBackdrop, ui.animations && !ui.reducedMotion)
                Column(Modifier.fillMaxSize().padding(18.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(54.dp).height(54.dp).background(tokens.accent.copy(.16f), RoundedCornerShape(17.dp)).border(1.dp, tokens.accent.copy(.55f), RoundedCornerShape(17.dp)), Alignment.Center) {
                            Text("Z", color = tokens.text, fontSize = 23.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("ZenPlayer", color = tokens.text, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            Text("Look & Feel Preview", color = tokens.textMuted, fontSize = 9.sp)
                        }
                        PreviewIcon("⌕", tokens)
                        Spacer(Modifier.width(7.dp))
                        PreviewIcon("⚙", tokens)
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PreviewSidebar(tokens)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Für dich", color = tokens.text, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Text("Live TV · deine aktuelle Oberfläche", color = tokens.textMuted, fontSize = 9.sp)
                            PreviewChannel("Das Erste HD", "20:15 · Tatort", true, tokens)
                            PreviewChannel("ZDF HD", "20:15 · heute journal", false, tokens)
                            PreviewEpg(tokens)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewSidebar(tokens: ZenThemeTokens) {
    Column(
        Modifier.width(58.dp).fillMaxHeight().zenStudioGlass(tokens, true),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PreviewIcon("⌂", tokens, true)
        PreviewIcon("▶", tokens)
        PreviewIcon("⌕", tokens)
        PreviewIcon("⚙", tokens)
    }
}

@Composable
private fun PreviewIcon(label: String, tokens: ZenThemeTokens, selected: Boolean = false) {
    Box(
        Modifier.width(40.dp).height(40.dp)
            .background(if (selected) tokens.accent.copy(.20f) else Color.Transparent, RoundedCornerShape(12.dp))
            .border(if (selected) 1.dp else 0.dp, tokens.focusGlow, RoundedCornerShape(12.dp)),
        Alignment.Center
    ) { Text(label, color = if (selected) tokens.text else tokens.textMuted, fontSize = 14.sp) }
}

@Composable
private fun PreviewChannel(title: String, subtitle: String, focused: Boolean, tokens: ZenThemeTokens) {
    Row(
        Modifier.fillMaxWidth().height(62.dp)
            .zenStudioGlass(tokens, focused)
            .border(if (focused) 2.dp else 1.dp, if (focused) tokens.focusGlow else tokens.glassBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(38.dp).height(38.dp).background(tokens.accent.copy(.14f), RoundedCornerShape(11.dp)), Alignment.Center) {
            Text(title.take(1), color = tokens.accent, fontWeight = FontWeight.Black)
        }
        Column(Modifier.padding(start = 11.dp).weight(1f)) {
            Text(title, color = tokens.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = tokens.textMuted, fontSize = 9.sp)
        }
        Text(if (focused) "FOCUS" else "LIVE", color = if (focused) tokens.accent else tokens.textMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PreviewEpg(tokens: ZenThemeTokens) {
    Row(Modifier.fillMaxWidth().height(70.dp).zenStudioGlass(tokens), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.width(70.dp).fillMaxHeight(), Alignment.Center) { Text("20:15", color = tokens.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        Column(Modifier.weight(1f).padding(vertical = 9.dp), verticalArrangement = Arrangement.Center) {
            Text("Tatort", color = tokens.text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("20:15 — 21:45", color = tokens.textMuted, fontSize = 8.sp)
        }
        Column(Modifier.weight(1f).padding(vertical = 9.dp), verticalArrangement = Arrangement.Center) {
            Text("Tagesthemen", color = tokens.textMuted, fontSize = 10.sp)
            Text("21:45 — 22:15", color = tokens.textMuted.copy(.70f), fontSize = 8.sp)
        }
    }
}

@Composable
private fun StudioChoice(label: String, value: String, options: List<String>, accent: Color, onChange: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    var index by remember(value) { mutableStateOf(options.indexOf(value).coerceAtLeast(0)) }
    Row(
        Modifier.fillMaxWidth().height(48.dp)
            .background(if (focused) accent.copy(.12f) else Color.White.copy(.045f), RoundedCornerShape(13.dp))
            .border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(.08f), RoundedCornerShape(13.dp))
            .focusable().onFocusChanged { focused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyUp) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> { index = (index - 1 + options.size) % options.size; onChange(options[index]); true }
                    Key.DirectionRight, Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { index = (index + 1) % options.size; onChange(options[index]); true }
                    else -> false
                }
            }.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White.copy(.62f), fontSize = 10.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("  ${index + 1}/${options.size}", color = accent, fontSize = 8.sp)
    }
}

@Composable
private fun StudioToggle(label: String, checked: Boolean, accent: Color, onChange: (Boolean) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(44.dp).focusable().onFocusChanged { focused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && event.key in setOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)) {
                    onChange(!checked); true
                } else false
            }
            .background(if (focused) accent.copy(.10f) else Color.Transparent, RoundedCornerShape(11.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
        Text(if (checked) "AN" else "AUS", color = if (checked) accent else Color.White.copy(.45f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

private fun ZenTheme.toThemeTokens(): ZenThemeTokens = when (this) {
    ZenTheme.AURORA -> ZenThemes.Aurora
    ZenTheme.OBSIDIAN -> ZenThemes.Obsidian
    ZenTheme.FROST -> ZenThemes.Frost
    ZenTheme.AMBER -> ZenThemes.Amber
}

private fun Modifier.zenStudioGlass(tokens: ZenThemeTokens, strong: Boolean = false): Modifier =
    background(if (strong) tokens.glassStrong else tokens.glass, RoundedCornerShape(18.dp))
        .border(1.dp, tokens.glassBorder, RoundedCornerShape(18.dp))

private fun Modifier.clipGlass(tokens: ZenThemeTokens): Modifier =
    background(tokens.background, RoundedCornerShape(24.dp))
        .border(1.dp, tokens.glassBorder, RoundedCornerShape(24.dp))
