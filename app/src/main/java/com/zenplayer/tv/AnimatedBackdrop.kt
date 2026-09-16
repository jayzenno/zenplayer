package com.zenplayer.tv

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

/** Very slow cinematic light fields. No hard geometry, grids or video assets. */
@Composable
fun ZenAnimatedBackdrop(theme: ZenTheme, mode: AnimatedBackdrop, enabled: Boolean = true) {
    if (!enabled || mode == AnimatedBackdrop.NONE) return
    val transition = rememberInfiniteTransition(label = "zen-premium-backdrop")
    val phase by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(42000), RepeatMode.Reverse), label = "phase")
    val colors = remember(theme) {
        when (theme) {
            ZenTheme.AURORA -> Triple(Color(0xFF56D7FF), Color(0xFF756BFF), Color(0xFF07151D))
            ZenTheme.OBSIDIAN -> Triple(Color(0xFF9A91FF), Color(0xFF4C76FF), Color(0xFF080A14))
            ZenTheme.FROST -> Triple(Color(0xFF8BE8FF), Color(0xFF6EA8FF), Color(0xFF07151B))
            ZenTheme.AMBER -> Triple(Color(0xFFFFC36B), Color(0xFFFF7891), Color(0xFF1B0D0B))
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val t = phase * 6.2831855f
        drawRect(Brush.verticalGradient(listOf(Color(0xFF03050A), colors.third, Color(0xFF03050A))))

        fun light(x: Float, y: Float, radius: Float, color: Color, alpha: Float) {
            drawCircle(Brush.radialGradient(listOf(color.copy(alpha), color.copy(alpha * .22f), Color.Transparent), center = Offset(x, y), radius = radius), radius, Offset(x, y))
        }
        light(w * (.18f + .07f * sin(t * .32f)), h * (.24f + .05f * cos(t * .27f)), minOf(w,h) * .72f, colors.first, .105f)
        light(w * (.80f + .06f * cos(t * .24f)), h * (.62f + .07f * sin(t * .21f)), minOf(w,h) * .62f, colors.second, .085f)
        light(w * (.48f + .12f * sin(t * .17f)), h * (.92f + .03f * cos(t * .2f)), minOf(w,h) * .48f, colors.first, .045f)

        if (mode == AnimatedBackdrop.ORBIT) {
            light(w * (.52f + .14f * cos(t * .12f)), h * (.43f + .09f * sin(t * .12f)), minOf(w,h) * .34f, colors.second, .045f)
        }
        if (mode == AnimatedBackdrop.MESH) {
            light(w * (.35f + .10f * sin(t * .16f)), h * (.56f + .08f * cos(t * .13f)), minOf(w,h) * .42f, colors.first, .055f)
        }
        // A faint veil gives the glass surfaces above it something to refract into.
        drawRect(Color.Black.copy(.34f))
    }
}
