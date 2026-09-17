package com.zenplayer.tv

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent as foundationOnKeyEvent

fun Modifier.onKeyEvent(handler: (KeyEvent) -> Boolean): Modifier = this.foundationOnKeyEvent(handler)

val KeyEvent.type: KeyEventType
    get() = when (nativeKeyEvent.action) {
        AndroidKeyEvent.ACTION_DOWN -> KeyEventType.KeyDown
        AndroidKeyEvent.ACTION_UP -> KeyEventType.KeyUp
        else -> KeyEventType.Unknown
    }

val KeyEvent.key: Key
    get() = Key(nativeKeyEvent.keyCode)
