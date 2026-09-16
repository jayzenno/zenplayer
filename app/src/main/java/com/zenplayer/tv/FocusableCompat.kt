package com.zenplayer.tv

import androidx.compose.ui.Modifier

/**
 * Compatibility bridge for TV UI code that uses .focusable() without importing
 * the foundation extension in every source file.
 */
fun Modifier.focusable(): Modifier =
    androidx.compose.foundation.focusable(this)
