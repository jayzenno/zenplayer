package com.zenplayer.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.app.Activity

private val AppBg = Color(0xFF05070D)

/**
 * ZenPlayer's root composable. Owns the four-tile home gate (Live TV | VOD | Music | Settings)
 * and dispatches into each section. The gate's structure is fixed — see AGENTS.md.
 */
@Composable
fun ZenPlayerApp(settings: SettingsStore) {
    val context = LocalContext.current
    val ui = settings.ui
    val accent = when (ui.theme) {
        ZenTheme.AURORA -> Color(0xFF70E6FF)
        ZenTheme.OBSIDIAN -> Color(0xFFAAA8FF)
        ZenTheme.FROST -> Color(0xFF9FEAFF)
        ZenTheme.AMBER -> Color(0xFFFFC46E)
    }
    var tile by remember { mutableStateOf<HomeTile?>(null) }
    var gateFocusToken by remember { mutableStateOf(0) }
    var showExitDialog by remember { mutableStateOf(false) }

    fun returnToGate() {
        tile = null
        gateFocusToken++
    }

    BackHandler(enabled = true) {
        when {
            showExitDialog -> showExitDialog = false
            tile != null -> returnToGate()
            else -> showExitDialog = true
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("ZenPlayer beenden?") },
            text = { Text("Möchtest du ZenPlayer wirklich schließen?") },
            confirmButton = { TextButton(onClick = { (context as? Activity)?.finish() }) { Text("Beenden") } },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("Abbrechen") } }
        )
    }

    Box(Modifier.fillMaxSize().background(AppBg)) {
        ZenAnimatedBackdrop(ui.theme, ui.animatedBackdrop, !ui.reducedMotion)
        when (tile) {
            null -> Box(Modifier.fillMaxSize().padding(4.dp)) {
                ZenHomeGate(accent, gateFocusToken) { selected -> tile = selected }
            }
            HomeTile.LIVE_TV -> ZenPlayerShellV6(settings, startPage = "home", embedded = true, onExitToGate = ::returnToGate)
            HomeTile.SETTINGS -> ZenPlayerShellV6(settings, startPage = "settings", embedded = true, onExitToGate = ::returnToGate)
            HomeTile.VOD -> Box(Modifier.fillMaxSize().padding(20.dp)) { ZenVodScreen(accent) }
            HomeTile.MUSIC -> Box(Modifier.fillMaxSize().padding(20.dp)) { ZenMusicScreen(accent) }
        }
    }
}
