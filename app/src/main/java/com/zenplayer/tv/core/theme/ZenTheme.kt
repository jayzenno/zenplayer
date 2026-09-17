package com.zenplayer.tv.core.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class ZenThemeTokens(
    val background: Color,
    val backgroundSecondary: Color,
    val glass: Color,
    val glassStrong: Color,
    val glassBorder: Color,
    val text: Color,
    val textMuted: Color,
    val accent: Color,
    val focusGlow: Color,
)

object ZenThemes {
    val Aurora = ZenThemeTokens(
        background = Color(0xFF050914),
        backgroundSecondary = Color(0xFF0A1630),
        glass = Color(0x66152238),
        glassStrong = Color(0xB3142035),
        glassBorder = Color(0x4DFFFFFF),
        text = Color(0xFFF6F9FF),
        textMuted = Color(0xFF9EABC2),
        accent = Color(0xFF6DEBFF),
        focusGlow = Color(0xFF52DFFF),
    )

    val Obsidian = Aurora.copy(
        background = Color(0xFF08070D),
        backgroundSecondary = Color(0xFF17101F),
        accent = Color(0xFFB78CFF),
        focusGlow = Color(0xFFA96EFF),
    )

    val Frost = Aurora.copy(
        background = Color(0xFF09131B),
        backgroundSecondary = Color(0xFF10232E),
        glass = Color(0x668DA9B8),
        accent = Color(0xFFB8F3FF),
        focusGlow = Color(0xFF9AEAFF),
    )

    val Amber = Aurora.copy(
        background = Color(0xFF120B05),
        backgroundSecondary = Color(0xFF241508),
        accent = Color(0xFFFFB65A),
        focusGlow = Color(0xFFFF9E38),
    )
}

object ZenMotion {
    const val FocusMs = 170
    const val PageMs = 280
    const val GlowMs = 1200
    val easing = FastOutSlowInEasing
    fun <T> tweenFocus() = tween<T>(FocusMs, easing = easing)
}

@Composable
fun ZenPlayerTheme(
    tokens: ZenThemeTokens = ZenThemes.Aurora,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = tokens.accent,
            onPrimary = Color(0xFF001018),
            background = tokens.background,
            surface = tokens.glassStrong,
            onSurface = tokens.text,
            onSurfaceVariant = tokens.textMuted,
        ),
        content = content,
    )
}

fun Modifier.zenGlass(tokens: ZenThemeTokens, strong: Boolean = false): Modifier =
    clip(RoundedCornerShape(24.dp))
        .background(
            Brush.linearGradient(
                listOf(
                    if (strong) tokens.glassStrong else tokens.glass,
                    tokens.backgroundSecondary.copy(alpha = 0.46f),
                )
            )
        )
        .border(1.dp, tokens.glassBorder, RoundedCornerShape(24.dp))
