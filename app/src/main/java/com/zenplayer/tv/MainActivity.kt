package com.zenplayer.tv

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.LocalFocusManager
import androidx.compose.ui.graphics.Color
import android.os.SystemClock

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ZenLogger.init(this)
        ZenLogger.info("APP", "MainActivity.onCreate")
        setContent {
            var exitDialog by mutableStateOf(false)
            var lastBack by mutableLongStateOf(0L)
            val focusManager = LocalFocusManager.current

            BackHandler {
                // First Back moves spatial focus back toward the main navigation rail.
                // A second Back within 2 seconds is the explicit exit request.
                val moved = focusManager.moveFocus(FocusDirection.Left)
                if (moved) {
                    lastBack = 0L
                } else {
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastBack <= 2000L) exitDialog = true else lastBack = now
                }
            }

            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF70E6FF),
                    onPrimary = Color(0xFF001018),
                    surface = Color(0xFF0B0F18),
                    onSurface = Color(0xFFF7F8FC),
                    onSurfaceVariant = Color(0xFFB8C0D0)
                )
            ) {
                ZenPlayerShellV3(SettingsStore(this@MainActivity))
                if (exitDialog) {
                    AlertDialog(
                        onDismissRequest = { exitDialog = false },
                        title = { Text("ZenPlayer schließen?") },
                        text = { Text("Möchtest du ZenPlayer wirklich beenden?") },
                        confirmButton = { TextButton(onClick = { finish() }) { Text("Beenden") } },
                        dismissButton = { TextButton(onClick = { exitDialog = false }) { Text("Abbrechen") } }
                    )
                }
            }
        }
    }
}
