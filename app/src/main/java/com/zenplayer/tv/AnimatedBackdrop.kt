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

/** Subtle cinematic light field: slow, diffuse and intentionally free of obvious shapes. */
@Composable
fun ZenAnimatedBackdrop(theme: ZenTheme, mode: AnimatedBackdrop, enabled: Boolean = true) {
    if (!enabled || mode == AnimatedBackdrop.NONE) return
    val transition = rememberInfiniteTransition(label = "zen-light-field")
    val phase by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(60000), RepeatMode.Reverse), label = "phase")
    val colors = remember(theme) {
        when (theme) {
            ZenTheme.AURORA -> Triple(Color(0xFF42D8FF), Color(0xFF726BFF), Color(0xFF08131C))
            ZenTheme.OBSIDIAN -> Triple(Color(0xFF8D86FF), Color(0xFF476CFF), Color(0xFF090B14))
            ZenTheme.FROST -> Triple(Color(0xFF86E8FF), Color(0xFF6DABFF), Color(0xFF09141A))
            ZenTheme.AMBER -> Triple(Color(0xFFFFC16A), Color(0xFFFF7088), Color(0xFF1A0C0B))
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val t = phase * 6.2831855f
        drawRect(Brush.verticalGradient(listOf(Color(0xFF020307), colors.third.copy(alpha = .72f), Color(0xFF020307))))
        fun glow(x: Float, y: Float, r: Float, c: Color, a: Float) {
            drawCircle(Brush.radialGradient(listOf(c.copy(alpha = a), c.copy(alpha = a * .28f), Color.Transparent), Offset(x, y), r), r, Offset(x, y))
        }
        glow(w * (.16f + .045f * sin(t * .18f)), h * (.18f + .035f * cos(t * .15f)), minOf(w, h) * .85f, colors.first, .12f)
        glow(w * (.84f + .045f * cos(t * .16f)), h * (.64f + .045f * sin(t * .13f)), minOf(w, h) * .78f, colors.second, .10f)
        glow(w * (.52f + .08f * sin(t * .10f)), h * (.90f + .025f * cos(t * .12f)), minOf(w, h) * .55f, colors.first, .055f)
        if (mode == AnimatedBackdrop.ORBIT) glow(w * (.50f + .11f * cos(t * .07f)), h * (.45f + .07f * sin(t * .07f)), minOf(w, h) * .45f, colors.second, .045f)
        if (mode == AnimatedBackdrop.MESH) glow(w * (.32f + .08f * sin(t * .09f)), h * (.55f + .06f * cos(t * .08f)), minOf(w, h) * .48f, colors.first, .035f)
        drawRect(Color.Black.copy(alpha = .27f))
    }
}
