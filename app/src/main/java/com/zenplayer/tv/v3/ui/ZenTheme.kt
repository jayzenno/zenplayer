package com.zenplayer.tv.v3.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ZenThemePreset(
    val label: String,
    val accent: Color,
    val background: Color,
    val surface: Color
) {
    Aurora("Aurora", Color(0xFF64E8FF), Color(0xFF050A16), Color(0xFF101A2D)),
    Obsidian("Obsidian", Color(0xFFC58CFF), Color(0xFF08060E), Color(0xFF181323)),
    Frost("Frost", Color(0xFF66B8FF), Color(0xFFEAF4FF), Color(0xFFD7E8F7)),
    Amber("Amber", Color(0xFFFFB84D), Color(0xFF100A04), Color(0xFF24170A))
}

data class ZenThemeState(
    val preset: ZenThemePreset = ZenThemePreset.Aurora,
    val reducedMotion: Boolean = false,
    val wallpaperEnabled: Boolean = false
)

private val AuroraDark = darkColorScheme(
    primary = ZenThemePreset.Aurora.accent,
    background = ZenThemePreset.Aurora.background,
    surface = ZenThemePreset.Aurora.surface,
    onBackground = Color.White,
    onSurface = Color.White
)
private val ObsidianDark = darkColorScheme(
    primary = ZenThemePreset.Obsidian.accent,
    background = ZenThemePreset.Obsidian.background,
    surface = ZenThemePreset.Obsidian.surface,
    onBackground = Color.White,
    onSurface = Color.White
)
private val FrostLight = lightColorScheme(
    primary = ZenThemePreset.Frost.accent,
    background = ZenThemePreset.Frost.background,
    surface = ZenThemePreset.Frost.surface,
    onBackground = Color(0xFF10151D),
    onSurface = Color(0xFF10151D)
)
private val AmberDark = darkColorScheme(
    primary = ZenThemePreset.Amber.accent,
    background = ZenThemePreset.Amber.background,
    surface = ZenThemePreset.Amber.surface,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun ZenTheme(state: ZenThemeState, content: @Composable () -> Unit) {
    val scheme = when (state.preset) {
        ZenThemePreset.Aurora -> AuroraDark
        ZenThemePreset.Obsidian -> ObsidianDark
        ZenThemePreset.Frost -> FrostLight
        ZenThemePreset.Amber -> AmberDark
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun ThemeStudio(
    state: ZenThemeState,
    onStateChange: (ZenThemeState) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxSize().padding(36.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Text("Look & Feel", fontSize = 36.sp)
        Text(
            "Alles hier wird direkt auf die gesamte ZenPlayer-Oberfläche angewendet.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ThemePreview(state)
        Text("Themes", fontSize = 22.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ZenThemePreset.entries.forEach { preset ->
                ThemePresetCard(
                    preset = preset,
                    selected = state.preset == preset,
                    reducedMotion = state.reducedMotion
                ) {
                    onStateChange(state.copy(preset = preset))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ThemeOptionCard(
                title = "Reduced Motion",
                value = if (state.reducedMotion) "An" else "Aus",
                selected = state.reducedMotion,
                accent = state.preset.accent,
                onClick = { onStateChange(state.copy(reducedMotion = !state.reducedMotion)) }
            )
            ThemeOptionCard(
                title = "Wallpaper",
                value = if (state.wallpaperEnabled) "An" else "Aus",
                selected = state.wallpaperEnabled,
                accent = state.preset.accent,
                onClick = { onStateChange(state.copy(wallpaperEnabled = !state.wallpaperEnabled)) }
            )
        }
    }
}

@Composable
private fun ThemePreview(state: ZenThemeState) {
    val accent by animateColorAsState(
        state.preset.accent,
        tween(if (state.reducedMotion) 0 else 280),
        label = "themeAccent"
    )
    val background by animateColorAsState(
        state.preset.background,
        tween(if (state.reducedMotion) 0 else 280),
        label = "themeBackground"
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(
                Brush.linearGradient(listOf(background, state.preset.surface)),
                RoundedCornerShape(28.dp)
            )
            .border(1.dp, accent.copy(alpha = .35f), RoundedCornerShape(28.dp))
            .padding(28.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ZenPlayer", fontSize = 30.sp)
            Text("Live Theme Preview", color = accent, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text("${state.preset.label} · Focus Glow · Glass surfaces")
        }
    }
}

@Composable
private fun ThemePresetCard(
    preset: ZenThemePreset,
    selected: Boolean,
    reducedMotion: Boolean,
    onClick: () -> Unit
) {
    var focused by rememberSaveable { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (focused) 1.06f else 1f,
        tween(if (reducedMotion) 0 else 140),
        label = "themeFocusScale"
    )
    Box(
        Modifier
            .size(width = 170.dp, height = 104.dp)
            .scale(scale)
            .alpha(if (selected) 1f else .72f)
            .background(preset.surface, RoundedCornerShape(20.dp))
            .border(
                if (focused || selected) 2.dp else 1.dp,
                if (focused || selected) preset.accent else Color.White.copy(alpha = .08f),
                RoundedCornerShape(20.dp)
            )
            .onFocusChanged { focused = it.hasFocus }
            .focusable()
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(16.dp).background(preset.accent, RoundedCornerShape(8.dp)))
            Text(preset.label, fontSize = 18.sp)
            Text(
                if (selected) "Aktiv" else "Auswählen",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ThemeOptionCard(
    title: String,
    value: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    var focused by rememberSaveable { mutableStateOf(false) }
    Box(
        Modifier
            .size(width = 240.dp, height = 84.dp)
            .background(
                if (selected) accent.copy(alpha = .12f) else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(18.dp)
            )
            .border(
                if (focused || selected) 2.dp else 1.dp,
                if (focused || selected) accent else MaterialTheme.colorScheme.outline.copy(alpha = .35f),
                RoundedCornerShape(18.dp)
            )
            .onFocusChanged { focused = it.hasFocus }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 17.sp)
            Text(value, fontSize = 13.sp, color = accent)
        }
    }
}
