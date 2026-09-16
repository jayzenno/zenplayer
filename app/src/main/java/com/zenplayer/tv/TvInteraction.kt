package com.zenplayer.tv

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type

/** Remote-first activation. Callers own the focus node; OK/Enter fires once on key-up. */
fun Modifier.tvAction(onAction: () -> Unit): Modifier =
    onKeyEvent { event ->
        val center = event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter
        if (!center) return@onKeyEvent false
        if (event.type == KeyEventType.KeyUp) onAction()
        true
    }
