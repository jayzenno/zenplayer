package com.zenplayer.tv

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent

/**
 * Android TV key handling shim.
 *
 * ZenPlayer's TV controls are leaf focus targets. Preview handling makes the
 * focused target see DPAD/ENTER events before a descendant can consume them,
 * while keeping the existing callback contract unchanged.
 */
fun Modifier.onKeyEvent(handler: (KeyEvent) -> Boolean): Modifier =
    onPreviewKeyEvent(handler)
