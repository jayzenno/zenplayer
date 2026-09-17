package com.zenplayer.tv.v3.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ZenThemePreset(val label: String, val accent: Color, val background: Color, val surface: Color, val secondary: Color, val glassOpacity: Float, val glowStrength: Float) {
    Aurora("Aurora", Color(0xFF64E8FF), Color(0xFF050A16), Color(0xFF101A2D), Color(0xFF6D7BFF), .62f, .72f),
    Nebula("Nebula", Color(0xFFB784FF), Color(0xFF090615), Color(0xFF19122A), Color(0xFF5B6DFF), .64f, .78f),
    Obsidian("Obsidian", Color(0xFFC58CFF), Color(0xFF08060E), Color(0xFF181323), Color(0xFF6550A8), .70f, .60f),
    Frost("Frost", Color(0xFF66B8FF), Color(0xFFEAF4FF), Color(0xFFD7E8F7), Color(0xFF9EDBFF), .48f, .45f),
    Midnight("Midnight", Color(0xFF718CFF), Color(0xFF050916), Color(0xFF10182C), Color(0xFF3EC8FF), .64f, .62f),
    Ember("Ember", Color(0xFFFF5C45), Color(0xFF120604), Color(0xFF25100D), Color(0xFFFFA14A), .62f, .70f),
    Amber("Amber", Color(0xFFFFB84D), Color(0xFF100A04), Color(0xFF24170A), Color(0xFFFFD27A), .58f, .65f),
    Emerald("Emerald", Color(0xFF4FE0A0), Color(0xFF04100C), Color(0xFF0E2119), Color(0xFF35B8C7), .62f, .68f),
    Ocean("Ocean", Color(0xFF42CFFF), Color(0xFF031019), Color(0xFF0B202C), Color(0xFF4B7CFF), .64f, .70f),
    Rose("Rose", Color(0xFFFF72B6), Color(0xFF12050D), Color(0xFF25101D), Color(0xFFB66BFF), .62f, .70f),
    Mono("Mono", Color(0xFFE8E8E8), Color(0xFF090909), Color(0xFF1B1B1B), Color(0xFF8E8E8E), .72f, .28f),
    Cyber("Cyber", Color(0xFF5CFFF1), Color(0xFF06070D), Color(0xFF111522), Color(0xFFFF4FD8), .58f, .88f),
    Solar("Solar", Color(0xFFFFD166), Color(0xFF130B03), Color(0xFF281A08), Color(0xFFFF8C42), .58f, .70f),
    Void("Void", Color(0xFF9B8CFF), Color(0xFF020203), Color(0xFF0C0B10), Color(0xFF3C3655), .78f, .35f)
}

enum class ZenReceiverTheme(val label: String, val accent: Color, val background: Color, val surface: Color, val secondary: Color) {
    Heaven("Heaven", Color(0xFF4EA4FF), Color(0xFF040B18), Color(0xFF10213A), Color(0xFF8AB8FF)),
    Magenta("Magenta", Color(0xFFFF4FA3), Color(0xFF12030D), Color(0xFF281021), Color(0xFFB85CFF)),
    Arena("Arena", Color(0xFFFF4D42), Color(0xFF0B0707), Color(0xFF211313), Color(0xFFFFB14D)),
    Cinema("Cinema", Color(0xFFFFC857), Color(0xFF0D0A06), Color(0xFF211A0E), Color(0xFFFF7D5C)),
    Stream("Stream", Color(0xFF5CB8FF), Color(0xFF050A12), Color(0xFF111D2B), Color(0xFF8A7CFF)),
    Sports("Sports", Color(0xFF62E36B), Color(0xFF040D06), Color(0xFF102217), Color(0xFF3AC7FF))
}

enum class ZenBackgroundPreset(val label: String) { Static("Static"), AuroraFlow("Aurora Flow"), DeepSpace("Deep Space"), LiquidGlass("Liquid Glass"), Eclipse("Eclipse"), OceanDepth("Ocean Depth"), ParticleDrift("Particle Drift"), CloudedLight("Clouded Light"), GradientMesh("Gradient Mesh") }
enum class ZenAnimationSpeed(val label: String, val durationMs: Int) { Off("Off", 1), Slow("Slow", 18000), Normal("Normal", 9000), Fast("Fast", 4500) }

data class ZenThemeState(
    val preset: ZenThemePreset = ZenThemePreset.Aurora,
    val receiver: ZenReceiverTheme? = null,
    val background: ZenBackgroundPreset = ZenBackgroundPreset.AuroraFlow,
    val backgroundBlur: Int = 24,
    val backgroundIntensity: Float = .62f,
    val animationSpeed: ZenAnimationSpeed = ZenAnimationSpeed.Slow,
    val reducedMotion: Boolean = false,
    val glassOpacity: Float = ZenThemePreset.Aurora.glassOpacity,
    val glowStrength: Float = ZenThemePreset.Aurora.glowStrength
)

private fun receiverScheme(receiver: ZenReceiverTheme) = darkColorScheme(primary = receiver.accent, secondary = receiver.secondary, background = receiver.background, surface = receiver.surface, onBackground = Color.White, onSurface = Color.White)
private val FrostLight = lightColorScheme(primary = ZenThemePreset.Frost.accent, secondary = ZenThemePreset.Frost.secondary, background = ZenThemePreset.Frost.background, surface = ZenThemePreset.Frost.surface, onBackground = Color(0xFF10151D), onSurface = Color(0xFF10151D))

@Composable
fun ZenTheme(state: ZenThemeState, content: @Composable () -> Unit) {
    val scheme = when {
        state.receiver != null -> receiverScheme(state.receiver)
        state.preset == ZenThemePreset.Frost -> FrostLight
        else -> darkColorScheme(primary = state.preset.accent, secondary = state.preset.secondary, background = state.preset.background, surface = state.preset.surface, onBackground = Color.White, onSurface = Color.White)
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun ThemeStudio(state: ZenThemeState, onStateChange: (ZenThemeState) -> Unit, firstFocusRequester: FocusRequester, sidebarRequester: FocusRequester, onChildFocus: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { firstFocusRequester.requestFocus() }
    LazyColumn(modifier = modifier.fillMaxSize().focusProperties { left = sidebarRequester }, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { ThemeHeader() }
        item { ThemePreview(state) }
        item { ThemeSection("Zen Themes") { ThemeRow(Modifier.focusGroup()) { items(ZenThemePreset.entries, key = { it.name }) { p -> ThemePresetCard(p, state.receiver == null && state.preset == p, state.reducedMotion, if (p == ZenThemePreset.Aurora) Modifier.focusRequester(firstFocusRequester) else Modifier, onChildFocus) { onStateChange(state.copy(preset = p, receiver = null, glassOpacity = p.glassOpacity, glowStrength = p.glowStrength)) } } } } }
        item { ThemeSection("Receiver") { ThemeRow(Modifier.focusGroup()) { items(ZenReceiverTheme.entries, key = { it.name }) { r -> ReceiverCard(r, state.receiver == r, state.reducedMotion, onChildFocus) { onStateChange(state.copy(receiver = r, glassOpacity = .60f, glowStrength = .68f)) } } } } }
        item { ThemeSection("Background") { ThemeRow(Modifier.focusGroup()) { items(ZenBackgroundPreset.entries, key = { it.name }) { b -> CompactOptionCard(b.label, state.background == b, state.preset.accent, onChildFocus) { onStateChange(state.copy(background = b)) } } } } }
        item { ThemeSection("Animation") { ThemeRow(Modifier.focusGroup()) { items(ZenAnimationSpeed.entries, key = { it.name }) { s -> CompactOptionCard(s.label, !state.reducedMotion && state.animationSpeed == s, state.preset.accent, onChildFocus) { onStateChange(state.copy(animationSpeed = s, reducedMotion = s == ZenAnimationSpeed.Off)) } }; item(key = "reduced-motion") { CompactOptionCard("Reduced Motion", state.reducedMotion, state.preset.accent, onChildFocus) { onStateChange(state.copy(reducedMotion = !state.reducedMotion)) } } } } }
        item { Row(Modifier.fillMaxWidth().focusGroup(), horizontalArrangement = Arrangement.spacedBy(18.dp)) { ControlColumn("Blur", "${state.backgroundBlur}%", state.backgroundBlur / 100f, onChildFocus, Modifier.width(220.dp)) { onStateChange(state.copy(backgroundBlur = (it * 100).toInt().coerceIn(0, 60))) }; ControlColumn("Intensity", "${(state.backgroundIntensity * 100).toInt()}%", state.backgroundIntensity, onChildFocus, Modifier.width(220.dp)) { onStateChange(state.copy(backgroundIntensity = it.coerceIn(.15f, 1f))) }; ControlColumn("Glass", "${(state.glassOpacity * 100).toInt()}%", state.glassOpacity, onChildFocus, Modifier.width(220.dp)) { onStateChange(state.copy(glassOpacity = it.coerceIn(.25f, .90f))) }; ControlColumn("Glow", "${(state.glowStrength * 100).toInt()}%", state.glowStrength, onChildFocus, Modifier.width(220.dp)) { onStateChange(state.copy(glowStrength = it.coerceIn(0f, 1f))) } } }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable private fun ThemeHeader() { Column(Modifier.fillMaxWidth().padding(top = 28.dp, start = 26.dp, end = 26.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("Look & Feel", fontSize = 36.sp); Text("Alles wird direkt auf die TV-Oberfläche angewendet. Mit OK auswählen, mit ←/→ innerhalb der Zeile navigieren.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp) } }
@Composable private fun ThemeSection(title: String, content: @Composable () -> Unit) { Column(Modifier.fillMaxWidth().padding(horizontal = 26.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) { Text(title, fontSize = 21.sp); content() } }
@Composable private fun ThemeRow(modifier: Modifier = Modifier, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) { LazyRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), content = content) }
@Composable private fun ThemePreview(state: ZenThemeState) { val accent by animateColorAsState(state.receiver?.accent ?: state.preset.accent, tween(if (state.reducedMotion) 0 else 220), label = "previewAccent"); Box(Modifier.fillMaxWidth().padding(horizontal = 26.dp).height(205.dp).clip(RoundedCornerShape(28.dp)).border(1.dp, accent.copy(alpha = .52f), RoundedCornerShape(28.dp))) { ZenBackground(state); Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .22f)).padding(22.dp)) { Column(verticalArrangement = Arrangement.spacedBy(9.dp)) { Text("ZENPLAYER", fontSize = 13.sp, color = accent); Text("Live Theme Preview", fontSize = 30.sp); Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { PreviewTile("LIVE TV", accent); PreviewTile("EPG", accent); PreviewTile("NOW PLAYING", accent) }; Text("${state.receiver?.label ?: state.preset.label} · ${state.background.label} · Blur ${state.backgroundBlur}%", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) } } } }
@Composable private fun PreviewTile(label: String, accent: Color) { Box(Modifier.size(width = 145.dp, height = 58.dp).background(Color.White.copy(alpha = .08f), RoundedCornerShape(14.dp)).border(1.dp, accent.copy(alpha = .28f), RoundedCornerShape(14.dp)).padding(11.dp)) { Text(label, fontSize = 11.sp) } }
@Composable private fun ThemePresetCard(p: ZenThemePreset, selected: Boolean, reducedMotion: Boolean, modifier: Modifier, onFocus: () -> Unit, onClick: () -> Unit) { FocusCard(modifier, selected, p.accent, reducedMotion, p.label, p.surface, onFocus, onClick) }
@Composable private fun ReceiverCard(r: ZenReceiverTheme, selected: Boolean, reducedMotion: Boolean, onFocus: () -> Unit, onClick: () -> Unit) { FocusCard(Modifier, selected, r.accent, reducedMotion, r.label, r.surface, onFocus, onClick) }
@Composable private fun CompactOptionCard(label: String, selected: Boolean, accent: Color, onFocus: () -> Unit, onClick: () -> Unit) { FocusCard(Modifier, selected, accent, false, label, MaterialTheme.colorScheme.surface, onFocus, onClick, 150.dp, 62.dp) }
@Composable private fun FocusCard(modifier: Modifier, selected: Boolean, accent: Color, reducedMotion: Boolean, label: String, surface: Color, onFocus: () -> Unit, onClick: () -> Unit, width: Dp = 170.dp, height: Dp = 92.dp) { var focused by remember { mutableStateOf(false) }; val scale by animateFloatAsState(if (focused) 1.045f else 1f, tween(if (reducedMotion) 0 else 130), label = "cardFocusScale"); Box(modifier.size(width, height).scale(scale).alpha(if (selected) 1f else .74f).clip(RoundedCornerShape(18.dp)).background(surface.copy(alpha = if (selected) .92f else .72f), RoundedCornerShape(18.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent else accent.copy(alpha = if (selected) .42f else .12f), RoundedCornerShape(18.dp)).onFocusChanged { focused = it.hasFocus; if (it.hasFocus) onFocus() }.focusable().clickable(onClick = onClick).padding(14.dp)) { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Box(Modifier.size(13.dp).background(accent, RoundedCornerShape(7.dp))); Text(label, fontSize = 16.sp); if (selected) Text("Aktiv", fontSize = 11.sp, color = accent) } } }
@Composable private fun ControlColumn(title: String, value: String, sliderValue: Float, onFocus: () -> Unit, modifier: Modifier, onChange: (Float) -> Unit) { Column(modifier.height(84.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Text(title, fontSize = 15.sp); Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary) }; Slider(value = sliderValue, onValueChange = onChange, modifier = Modifier.onFocusChanged { if (it.hasFocus) onFocus() }) } }
