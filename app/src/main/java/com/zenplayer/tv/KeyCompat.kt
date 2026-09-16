package com.zenplayer.tv

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent as foundationOnKeyEvent

/** Keeps TV key handling available to compact UI files without repeated imports. */
fun Modifier.onKeyEvent(handler: (KeyEvent) -> Boolean): Modifier = this.foundationOnKeyEvent(handler)
