package com.zenplayer.tv

import androidx.compose.ui.Modifier
import androidx.compose.foundation.focusable as foundationFocusable

/**
 * Compatibility bridge for TV UI code that uses .focusable() without importing
 * the foundation extension in every source file.
 */
fun Modifier.focusable(): Modifier =
    foundationFocusable(this)
