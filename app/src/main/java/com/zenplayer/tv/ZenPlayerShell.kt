package com.zenplayer.tv

import androidx.compose.foundation.focusable
import androidx.compose.ui.Modifier

// Compatibility shim kept intentionally tiny; the main TV shell is provided by the current ZenPlayer UI files.
@androidx.compose.runtime.Composable
fun ZenPlayerShellCompat(settings: SettingsStore) {
    ZenPlayerShell(settings)
}
