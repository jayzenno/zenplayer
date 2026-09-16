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

/** Procedural cinematic backdrop: soft gradient fields and slow orbital light, no video assets. */
@Composable
fun ZenAnimatedBackdrop(theme: ZenTheme, mode: AnimatedBackdrop, enabled: Boolean = true) {
    if (!enabled || mode == AnimatedBackdrop.NONE) return
    val transition = rememberInfiniteTransition(label = "zen-cinematic-backdrop")
    val phase by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(24000), RepeatMode.Reverse), label = "backdrop-phase")
    val palette = remember(theme) {
        when (theme) {
            ZenTheme.AURORA -> Triple(Color(0xFF6E63FF), Color(0xFF21D4C4), Color(0xFF10152F))
            ZenTheme.OBSIDIAN -> Triple(Color(0xFF5B7CFF), Color(0xFF9B72FF), Color(0xFF080B15))
            ZenTheme.FROST -> Triple(Color(0xFF55D6CF), Color(0xFF6EA8FF), Color(0xFF0A2027))
            ZenTheme.AMBER -> Triple(Color(0xFFFFB65C), Color(0xFFFF6E8A), Color(0xFF28120E))
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val t = phase * (Math.PI * 2f).toFloat()
        drawRect(Brush.linearGradient(listOf(palette.third, Color(0xFF06080D)), Offset(0f, 0f), Offset(w, h)))

        if (mode == AnimatedBackdrop.MESH) {
            val step = 90f
            var x = -step
            while (x < w + step) {
                val wave = sin(x * .008f + t) * h * .045f
                drawLine(palette.first.copy(.035f), Offset(x, 0f), Offset(x + wave, h), 1f)
                x += step
            }
            var y = 0f
            while (y < h) {
                val wave = cos(y * .009f + t * .7f) * w * .025f
                drawLine(palette.second.copy(.025f), Offset(0f, y), Offset(w, y + wave), 1f)
                y += step
            }
        }

        val p1 = Offset(w * (.28f + .10f * sin(t)), h * (.32f + .08f * cos(t * .8f)))
        val p2 = Offset(w * (.78f + .08f * cos(t * .7f)), h * (.58f + .10f * sin(t * .65f)))
        val p3 = Offset(w * (.48f + .12f * cos(t * .45f)), h * (.92f + .05f * sin(t * .6f)))
        val r1 = minOf(w, h) * .52f
        val r2 = minOf(w, h) * .46f
        val r3 = minOf(w, h) * .38f
        drawCircle(Brush.radialGradient(listOf(palette.first.copy(.22f), palette.first.copy(.07f), Color.Transparent), center = p1, radius = r1), r1, p1)
        drawCircle(Brush.radialGradient(listOf(palette.second.copy(.17f), palette.second.copy(.05f), Color.Transparent), center = p2, radius = r2), r2, p2)
        drawCircle(Brush.radialGradient(listOf(palette.first.copy(.10f), Color.Transparent), center = p3, radius = r3), r3, p3)

        if (mode == AnimatedBackdrop.ORBIT) {
            val center = Offset(w * .58f, h * .45f)
            val orbit = minOf(w, h) * .30f
            repeat(2) { i ->
                val a = t * if (i == 0) 1f else -0.65f + i
                val point = Offset(center.x + cos(a) * orbit, center.y + sin(a) * orbit * .55f)
                drawCircle(palette.first.copy(.14f), minOf(w, h) * .08f, point)
                drawCircle(palette.second.copy(.06f), minOf(w, h) * .18f, point)
            }
        }
        drawRect(Color.Black.copy(.16f))
    }
}
