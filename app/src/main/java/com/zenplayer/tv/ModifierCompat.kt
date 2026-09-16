package com.zenplayer.tv

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip as drawClip
import androidx.compose.ui.graphics.Shape

/** Compatibility extension for the legacy EPG screen, which intentionally keeps its imports minimal. */
fun Modifier.clip(shape: Shape): Modifier = drawClip(shape)
