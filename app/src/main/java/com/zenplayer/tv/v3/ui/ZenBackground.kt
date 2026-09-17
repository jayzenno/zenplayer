package com.zenplayer.tv.v3.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun ZenBackground(state: ZenThemeState, modifier: Modifier = Modifier) {
    val background = state.receiver?.background ?: state.preset.background
    val accent = state.receiver?.accent ?: state.preset.accent
    val secondary = state.receiver?.secondary ?: state.preset.secondary
    val motion = if (state.reducedMotion || state.animationSpeed == ZenAnimationSpeed.Off) 0 else state.animationSpeed.durationMs
    val transition = rememberInfiniteTransition(label = "zenBackground")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(motion.coerceAtLeast(1), easing = LinearEasing), RepeatMode.Reverse),
        label = "backgroundPhase"
    )
    val animatedPhase = if (motion == 0) 0.5f else phase
    val intensity = state.backgroundIntensity

    Box(modifier.fillMaxSize().blur(state.backgroundBlur.dp)) {
        when (state.background) {
            ZenBackgroundPreset.Static -> StaticBackground(background, accent, intensity)
            ZenBackgroundPreset.AuroraFlow -> AuroraFlowBackground(background, accent, secondary, animatedPhase, intensity)
            ZenBackgroundPreset.DeepSpace -> DeepSpaceBackground(background, accent, animatedPhase, intensity)
            ZenBackgroundPreset.LiquidGlass -> LiquidGlassBackground(background, accent, secondary, animatedPhase, intensity)
            ZenBackgroundPreset.Eclipse -> EclipseBackground(background, accent, animatedPhase, intensity)
            ZenBackgroundPreset.OceanDepth -> OceanDepthBackground(background, accent, secondary, animatedPhase, intensity)
            ZenBackgroundPreset.ParticleDrift -> ParticleDriftBackground(background, accent, animatedPhase, intensity)
            ZenBackgroundPreset.CloudedLight -> CloudedLightBackground(background, accent, secondary, animatedPhase, intensity)
            ZenBackgroundPreset.GradientMesh -> GradientMeshBackground(background, accent, secondary, animatedPhase, intensity)
        }
    }
    if (state.glowStrength > 0f) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = .10f * state.glowStrength), Color.Transparent),
                    radius = 900f
                )
            )
        )
    }
}

@Composable
private fun StaticBackground(background: Color, accent: Color, intensity: Float) {
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(accent.copy(alpha = 0.18f * intensity), background, background))))
}

@Composable
private fun AuroraFlowBackground(background: Color, accent: Color, secondary: Color, phase: Float, intensity: Float) {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(background)
        val offset = size.width * (phase - 0.5f)
        drawCircle(accent.copy(alpha = 0.30f * intensity), size.width * 0.46f, androidx.compose.ui.geometry.Offset(size.width * 0.22f + offset, size.height * 0.28f))
        drawCircle(secondary.copy(alpha = 0.22f * intensity), size.width * 0.55f, androidx.compose.ui.geometry.Offset(size.width * 0.78f - offset, size.height * 0.72f))
    }
}

@Composable
private fun DeepSpaceBackground(background: Color, accent: Color, phase: Float, intensity: Float) {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(background)
        val drift = size.width * (phase - 0.5f) * 0.18f
        val stars = listOf(0.08f to 0.18f, 0.19f to 0.72f, 0.34f to 0.31f, 0.51f to 0.82f, 0.68f to 0.22f, 0.79f to 0.61f, 0.91f to 0.38f, 0.58f to 0.48f)
        stars.forEachIndexed { index, point ->
            val twinkle = 0.18f + ((index % 3) * 0.06f)
            drawCircle(accent.copy(alpha = twinkle * intensity), (2 + index % 3).dp.toPx(), androidx.compose.ui.geometry.Offset(size.width * point.first + drift, size.height * point.second))
        }
        drawCircle(accent.copy(alpha = 0.16f * intensity), size.width * 0.34f, androidx.compose.ui.geometry.Offset(size.width * 0.52f - drift, size.height * 0.46f))
    }
}

@Composable
private fun LiquidGlassBackground(background: Color, accent: Color, secondary: Color, phase: Float, intensity: Float) {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(background)
        val x = size.width * (0.18f + phase * 0.64f)
        val y = size.height * (0.25f + (1f - phase) * 0.42f)
        drawCircle(accent.copy(alpha = 0.26f * intensity), size.width * 0.42f, androidx.compose.ui.geometry.Offset(x, y))
        drawCircle(secondary.copy(alpha = 0.18f * intensity), size.width * 0.34f, androidx.compose.ui.geometry.Offset(size.width - x, size.height - y))
        drawOval(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.06f * intensity), Color.Transparent)), androidx.compose.ui.geometry.Offset(0f, size.height * 0.18f), androidx.compose.ui.geometry.Size(size.width, size.height * 0.34f))
    }
}

@Composable
private fun EclipseBackground(background: Color, accent: Color, phase: Float, intensity: Float) {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(background)
        val center = androidx.compose.ui.geometry.Offset(size.width * (0.38f + phase * 0.24f), size.height * 0.46f)
        drawCircle(Color.Black.copy(alpha = 0.82f), size.width * 0.30f, center)
        drawCircle(accent.copy(alpha = 0.30f * intensity), size.width * 0.32f, center, style = Stroke(width = size.width * 0.018f))
    }
}

@Composable
private fun OceanDepthBackground(background: Color, accent: Color, secondary: Color, phase: Float, intensity: Float) {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(background)
        repeat(4) { index ->
            val y = size.height * (0.28f + index * 0.17f) + size.height * 0.025f * phase
            drawOval(Brush.horizontalGradient(listOf(Color.Transparent, accent.copy(alpha = (0.12f - index * 0.018f) * intensity), Color.Transparent)), androidx.compose.ui.geometry.Offset(-size.width * 0.12f, y), androidx.compose.ui.geometry.Size(size.width * 1.24f, size.height * 0.14f))
        }
        drawCircle(secondary.copy(alpha = 0.10f * intensity), size.width * 0.38f, androidx.compose.ui.geometry.Offset(size.width * 0.78f, size.height * 0.18f))
    }
}

@Composable
private fun ParticleDriftBackground(background: Color, accent: Color, phase: Float, intensity: Float) {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(background)
        repeat(18) { index ->
            val x = ((index * 0.071f + phase * 0.10f) % 1f) * size.width
            val y = ((index * 0.137f + phase * 0.04f) % 1f) * size.height
            drawCircle(accent.copy(alpha = (0.08f + (index % 4) * 0.025f) * intensity), (1.5f + index % 3).dp.toPx(), androidx.compose.ui.geometry.Offset(x, y))
        }
    }
}

@Composable
private fun CloudedLightBackground(background: Color, accent: Color, secondary: Color, phase: Float, intensity: Float) {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(background)
        val shift = size.width * (phase - 0.5f) * 0.24f
        drawCircle(accent.copy(alpha = 0.24f * intensity), size.width * 0.52f, androidx.compose.ui.geometry.Offset(size.width * 0.18f + shift, size.height * 0.38f))
        drawCircle(secondary.copy(alpha = 0.20f * intensity), size.width * 0.46f, androidx.compose.ui.geometry.Offset(size.width * 0.82f - shift, size.height * 0.66f))
    }
}

@Composable
private fun GradientMeshBackground(background: Color, accent: Color, secondary: Color, phase: Float, intensity: Float) {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.linearGradient(listOf(background, accent.copy(alpha = 0.16f * intensity), secondary.copy(alpha = 0.13f * intensity), background), start = androidx.compose.ui.geometry.Offset(size.width * phase, 0f), end = androidx.compose.ui.geometry.Offset(size.width * (1f - phase), size.height)))
    }
}
