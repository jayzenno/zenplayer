package com.zenplayer.tv

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent as foundationOnPreviewKeyEvent

fun Modifier.onKeyEvent(handler: (KeyEvent) -> Boolean): Modifier = this.foundationOnPreviewKeyEvent(handler)

val KeyEvent.type: KeyEventType
    get() = when (nativeKeyEvent.action) {
        AndroidKeyEvent.ACTION_DOWN -> KeyEventType.KeyDown
        AndroidKeyEvent.ACTION_UP -> KeyEventType.KeyUp
        else -> KeyEventType.Unknown
    }

val KeyEvent.key: Key
    get() = Key(nativeKeyEvent.keyCode.toLong())
