package com.zenplayer.tv

import androidx.compose.ui.graphics.Color

object ZenGlass {
    var intensity: Int = 5
    fun surface(): Color = Color.White.copy(alpha = (0.025f + intensity.coerceIn(1, 10) * 0.0085f))
    fun border(): Color = Color.White.copy(alpha = (0.035f + intensity.coerceIn(1, 10) * 0.009f))
}
