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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

/** GPU-friendly procedural backdrop: no video, no MP4, no continuously decoded bitmap. */
@Composable
fun ZenAnimatedBackdrop(theme: ZenTheme, mode: AnimatedBackdrop, enabled: Boolean = true) {
    if (!enabled || mode == AnimatedBackdrop.NONE) return
    val transition = rememberInfiniteTransition(label = "zen-backdrop")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(18000), RepeatMode.Restart),
        label = "backdrop-phase"
    )
    val colors = remember(theme) {
        when (theme) {
            ZenTheme.AURORA -> listOf(Color(0xFF7C5CFF), Color(0xFF29D9C2), Color(0xFF16204F))
            ZenTheme.OBSIDIAN -> listOf(Color(0xFF3D8DFF), Color(0xFF6957FF), Color(0xFF0A1830))
            ZenTheme.FROST -> listOf(Color(0xFF56D8D0), Color(0xFF6DA9FF), Color(0xFF0B3135))
            ZenTheme.AMBER -> listOf(Color(0xFFFFB45E), Color(0xFFFF6F91), Color(0xFF351A12))
        }
    }

    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * .52f
        val cy = h * .44f
        val radius = minOf(w, h) * .22f

        when (mode) {
            AnimatedBackdrop.AURORA_FLOW -> {
                val x1 = cx + cos(phase) * w * .22f
                val y1 = cy + sin(phase * .73f) * h * .22f
                val x2 = cx + cos(phase + 2.1f) * w * .30f
                val y2 = cy + sin(phase * .61f + 1.4f) * h * .28f
                drawCircle(colors[0].copy(.11f), radius * 1.55f, Offset(x1, y1), blendMode = BlendMode.Screen)
                drawCircle(colors[1].copy(.075f), radius * 1.75f, Offset(x2, y2), blendMode = BlendMode.Screen)
                drawCircle(colors[2].copy(.055f), radius * 2.1f, Offset(cx, cy), blendMode = BlendMode.Screen)
            }
            AnimatedBackdrop.ORBIT -> {
                val orbit = radius * 1.8f
                repeat(3) { index ->
                    val a = phase * (if (index == 1) -1f else 1f) + index * 2.1f
                    val p = Offset(cx + cos(a) * orbit, cy + sin(a) * orbit * .58f)
                    drawCircle(colors[index].copy(.10f), radius * .62f, p, blendMode = BlendMode.Screen)
                    drawCircle(colors[index].copy(.20f), radius * .9f, p, style = Stroke(width = 1.2f), blendMode = BlendMode.Screen)
                }
            }
            AnimatedBackdrop.MESH -> {
                val spacing = (minOf(w, h) * .13f).coerceAtLeast(70f)
                for (x in -spacing..w step spacing) {
                    val wave = sin(x * .008f + phase) * h * .035f
                    drawLine(colors[0].copy(.045f), Offset(x, 0f + wave), Offset(x + w * .10f, h), strokeWidth = 1.1f)
                }
                for (y in -spacing..h step spacing) {
                    val wave = cos(y * .009f + phase * .8f) * w * .025f
                    drawLine(colors[1].copy(.035f), Offset(0f + wave, y), Offset(w, y + h * .08f), strokeWidth = 1.1f)
                }
            }
            AnimatedBackdrop.NONE -> Unit
        }
    }
}
