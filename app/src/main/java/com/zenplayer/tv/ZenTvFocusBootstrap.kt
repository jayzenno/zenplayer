package com.zenplayer.tv

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.yield

/**
 * Native TV focus bootstrap used only for the initial focus hand-off.
 *
 * Android/Compose recommends explicit FocusRequester/focusProperties for TV
 * navigation. This tiny bootstrap exists because ZenPlayer's real targets are
 * created several layers below the Activity and can otherwise start with no
 * Android focus owner at all on some TV launch paths.
 */
@Composable
fun ZenTvFocusBootstrap(content: @Composable () -> Unit) {
    val requester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var bootstrapFocused by remember { mutableStateOf(false) }

    Box {
        Box(
            Modifier
                .size(1.dp)
                .focusRequester(requester)
                .onFocusChanged { bootstrapFocused = it.isFocused }
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (!bootstrapFocused || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionRight, Key.DirectionDown, Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            focusManager.moveFocus(FocusDirection.Enter)
                            true
                        }
                        else -> false
                    }
                }
        )
        content()
    }

    LaunchedEffect(requester) {
        yield()
        requester.requestFocus()
    }
}
