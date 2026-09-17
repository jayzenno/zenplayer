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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun ZenBackground(state: ZenThemeState, modifier: Modifier = Modifier) {
    val background = state.receiver?.background ?: state.preset.background
    val accent = state.receiver?.accent ?: state.preset.accent
    val secondary = state.receiver?.secondary ?: state.preset.secondary
    val duration = if (state.reducedMotion || state.animationSpeed == ZenAnimationSpeed.Off) 0 else state.animationSpeed.durationMs
    val transition = rememberInfiniteTransition(label = "zenBackground")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration.coerceAtLeast(1), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "backgroundPhase"
    )
    val motionPhase = if (duration == 0) .5f else phase
    val intensity = state.backgroundIntensity.coerceIn(.15f, 1f)

    Box(modifier.fillMaxSize()) {
        when (state.background) {
            ZenBackgroundPreset.Static -> StaticBackground(background, accent, intensity)
            ZenBackgroundPreset.AuroraFlow -> AuroraFlowBackground(background, accent, secondary, motionPhase, intensity)
            ZenBackgroundPreset.DeepSpace -> DeepSpaceBackground(background, accent, secondary, motionPhase, intensity)
            ZenBackgroundPreset.LiquidGlass -> LiquidGlassBackground(background, accent, secondary, motionPhase, intensity)
            ZenBackgroundPreset.Eclipse -> EclipseBackground(background, accent, secondary, motionPhase, intensity)
            ZenBackgroundPreset.OceanDepth -> OceanDepthBackground(background, accent, secondary, motionPhase, intensity)
            ZenBackgroundPreset.ParticleDrift -> ParticleDriftBackground(background, accent, secondary, motionPhase, intensity)
            ZenBackgroundPreset.CloudedLight -> CloudedLightBackground(background, accent, secondary, motionPhase, intensity)
            ZenBackgroundPreset.GradientMesh -> GradientMeshBackground(background, accent, secondary, motionPhase, intensity)
        }
        if (state.glowStrength > 0f) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        listOf(accent.copy(alpha = .12f * state.glowStrength), Color.Transparent),
                        center = Offset(.25f * 1920f, .22f * 1080f),
                        radius = 1000f
                    )
                )
            )
        }
    }
}

@Composable
private fun StaticBackground(background: Color, accent: Color, intensity: Float) {
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(background, accent.copy(alpha = .10f * intensity), background))))
}

@Composable
private fun AuroraFlowBackground(background: Color, accent: Color, secondary: Color, phase: Float, intensity: Float) {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(background)
        val p = phase * 2f - 1f
        drawCircle(accent.copy(alpha = .28f * intensity), size.minDimension * .58f, Offset(size.width * (.16f + .18f * p), size.height * .20f))
        drawCircle(secondary.copy(alpha = .22f * intensity), size.minDimension * .62f, Offset(size.width * (.84f - .16f * p), size.height * .78f))
        drawCircle(accent.copy(alpha = .10f * intensity), size.minDimension * .42f, Offset(size.width * (.55f + .10f * p), size.height * .50f))
        drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = .025f), Color.Transparent, Color.Black.copy(alpha = .16f))))
    }
}

@Composable
private fun DeepSpaceBackground(background: Color, accent: Color, secondary: Color, phase: Float, intensity: Float) {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(background)
        val drift = size.width * (phase - .5f) * .05f
        val stars = listOf(.08f to .18f, .17f to .72f, .31f to .31f, .46f to .82f, .62f to .22f, .76f to .61f, .90f to .38f, .55f to .49f, .26f to .55f, .84f to .15f)
        stars.forEachIndexed { index, point ->
            val twinkle = .18f + (index % 3) * .07f
            drawCircle((if (index % 2 == 0) accent else secondary).copy(alpha = twinkle * intensity), (1.5f + index % 3).dp.toPx(), Offset(size.width * point.first + drift, size.height * point.second))
        }
        drawCircle(secondary.copy(alpha = .10f * intensity), size.minDimension * .44f, Offset(size.width * (.52f - drift / size.width), size.height * .47f))
    }
}

@Composable
private fun LiquidGlassBackground(background: Color, accent: Color, secondary: Color, phase: Float, intensity: Float) {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(background)
        val p = phase * 2f - 1f
        drawCircle(accent.copy(alpha = .25f * intensity), size.minDimension * .55f, Offset(size.width * (.18f + .20f * p), size.height * .26f))
        drawCircle(secondary.copy(alpha = .18f * intensity), size.minDimension * .48f, Offset(size.width * (.82f - .18f * p), size.height * .72f))
        drawOval(Brush.linearGradient(listOf(Color.White.copy(alpha = .09f), Color.Transparent)), Offset(-size.width * .10f, size.height * (.12f + .04f * p)), Size(size.width * 1.20f, size.height * .38f))
        drawOval(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = .045f))), Offset(-size.width * .10f, size.height * .55f), Size(size.width * 1.20f, size.height * .30f))
    }
}

@Composable
private fun EclipseBackground(background: Color, accent: Color, secondary: Color, phase: Float, intensity: Float) {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(background)
        val center = Offset(size.width * (.42f + phase * .16f), size.height * .46f)
        drawCircle(secondary.copy(alpha = .08f * intensity), size.minDimension * .40f, center)
        drawCircle(Color.Black.copy(alpha = .86f), size.minDimension * .29f, center)
        drawCircle(accent.copy(alpha = .34f * intensity), size.minDimension * .31f, center, style = Stroke(width = size.minDimension * .018f))
    }
}

@Composable
private fun OceanDepthBackground(background: Color, accent: Color, secondary: Color, phase: Float, intensity: Float) {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(background)
        repeat(5) { index ->
            val y = size.height * (.18f + index * .18f) + size.height * .035f * (phase - .5f)
            drawOval(Brush.horizontalGradient(listOf(Color.Transparent, accent.copy(alpha = (.14f - index * .018f) * intensity), Color.Transparent)), Offset(-size.width * .12f, y), Size(size.width * 1.24f, size.height * .13f))
        }
        drawCircle(secondary.copy(alpha = .11f * intensity), size.minDimension * .34f, Offset(size.width * .78f, size.height * .20f))
    }
}

@Composable
private fun ParticleDriftBackground(background: Color, accent: Color, secondary: Color, phase: Float, intensity: Float) {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(background)
        repeat(28) { index ->
            val x = ((index * .071f + phase * .08f) % 1f) * size.width
            val y = ((index * .137f + phase * .035f) % 1f) * size.height
            val color = if (index % 3 == 0) secondary else accent
            drawCircle(color.copy(alpha = (.07f + (index % 4) * .022f) * intensity), (1.5f + index % 3).dp.toPx(), Offset(x, y))
        }
    }
}

@Composable
private fun CloudedLightBackground(background: Color, accent: Color, secondary: Color, phase: Float, intensity: Float) {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(background)
        val p = phase * 2f - 1f
        drawCircle(accent.copy(alpha = .22f * intensity), size.minDimension * .56f, Offset(size.width * (.20f + .14f * p), size.height * .34f))
        drawCircle(secondary.copy(alpha = .18f * intensity), size.minDimension * .50f, Offset(size.width * (.80f - .12f * p), size.height * .66f))
        drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = .03f), Color.Transparent, Color.Black.copy(alpha = .12f))))
    }
}

@Composable
private fun GradientMeshBackground(background: Color, accent: Color, secondary: Color, phase: Float, intensity: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val p = phase * 2f - 1f
        drawRect(background)
        drawRect(Brush.linearGradient(listOf(background, accent.copy(alpha = .18f * intensity), secondary.copy(alpha = .16f * intensity), background), start = Offset(size.width * (.15f + .18f * p), 0f), end = Offset(size.width * (.85f - .18f * p), size.height)))
        drawCircle(accent.copy(alpha = .08f * intensity), size.minDimension * .50f, Offset(size.width * (.25f + .08f * p), size.height * .30f))
        drawCircle(secondary.copy(alpha = .08f * intensity), size.minDimension * .46f, Offset(size.width * (.75f - .08f * p), size.height * .70f))
    }
}
