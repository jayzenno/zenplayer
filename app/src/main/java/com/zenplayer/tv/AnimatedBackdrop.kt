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

/** Cinematic animated light field. Motion is deliberately slow and TV-friendly. */
@Composable
fun ZenAnimatedBackdrop(theme: ZenTheme, mode: AnimatedBackdrop, enabled: Boolean = true) {
    if (!enabled || mode == AnimatedBackdrop.NONE) return
    val transition = rememberInfiniteTransition(label = "zen-light-field")
    val phase by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(36000), RepeatMode.Reverse), label = "phase")
    val shimmer by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(14000), RepeatMode.Reverse), label = "shimmer")
    val colors = remember(theme) {
        when (theme) {
            ZenTheme.AURORA -> Triple(Color(0xFF42D8FF), Color(0xFF726BFF), Color(0xFF08131C))
            ZenTheme.OBSIDIAN -> Triple(Color(0xFF9A8CFF), Color(0xFF4D74FF), Color(0xFF090B14))
            ZenTheme.FROST -> Triple(Color(0xFF9AF2FF), Color(0xFF6DABFF), Color(0xFF09141A))
            ZenTheme.AMBER -> Triple(Color(0xFFFFC16A), Color(0xFFFF7088), Color(0xFF1A0C0B))
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val t = phase * 6.2831855f
        val s = shimmer * 6.2831855f
        val glass = ZenGlass.intensity.coerceIn(1, 10) / 10f
        drawRect(Brush.verticalGradient(listOf(Color(0xFF020307), colors.third.copy(alpha = .78f), Color(0xFF020307))))

        fun glow(x: Float, y: Float, r: Float, c: Color, a: Float) {
            drawCircle(
                Brush.radialGradient(
                    listOf(c.copy(alpha = a * (.60f + glass * .80f)), c.copy(alpha = a * .20f), Color.Transparent),
                    Offset(x, y), r
                ), r, Offset(x, y)
            )
        }

        // Large drifting light masses.
        glow(w * (.12f + .09f * sin(t * .22f)), h * (.20f + .06f * cos(t * .17f)), minOf(w, h) * .82f, colors.first, .15f)
        glow(w * (.86f + .07f * cos(t * .19f)), h * (.62f + .08f * sin(t * .15f)), minOf(w, h) * .74f, colors.second, .13f)
        glow(w * (.52f + .12f * sin(t * .11f)), h * (.92f + .04f * cos(t * .14f)), minOf(w, h) * .58f, colors.first, .065f)

        if (mode == AnimatedBackdrop.ORBIT) {
            val ox = w * .50f + w * .18f * cos(t * .12f)
            val oy = h * .48f + h * .13f * sin(t * .12f)
            glow(ox, oy, minOf(w, h) * .36f, colors.second, .075f)
            glow(w * .50f - w * .12f * cos(t * .12f), h * .48f - h * .10f * sin(t * .12f), minOf(w, h) * .22f, colors.first, .045f)
        }

        if (mode == AnimatedBackdrop.MESH) {
            for (i in 0..4) {
                val fx = i / 4f
                val x = w * (.15f + .70f * fx + .04f * sin(t * .10f + i))
                val y = h * (.28f + .38f * sin(t * .08f + i * .9f))
                glow(x, y, minOf(w, h) * .25f, if (i % 2 == 0) colors.first else colors.second, .035f)
            }
        }

        // A very soft moving sheen makes the setting preview visibly react.
        val sheenX = w * (.15f + .70f * ((sin(s) + 1f) / 2f))
        drawRect(
            Brush.radialGradient(
                listOf(colors.first.copy(alpha = .035f * glass), Color.Transparent),
                Offset(sheenX, h * .48f), minOf(w, h) * .70f
            )
        )
        drawRect(Color.Black.copy(alpha = .28f - glass * .07f))
    }
}
