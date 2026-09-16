package com.zenplayer.tv

import androidx.compose.foundation.focusable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type

/** Remote-first activation: one OK/Enter key-up produces exactly one action. */
fun Modifier.tvAction(onAction: () -> Unit): Modifier =
    focusable().onKeyEvent { event ->
        val center = event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter
        if (!center) return@onKeyEvent false
        if (event.type == KeyEventType.KeyUp) onAction()
        true
    }
