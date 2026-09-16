package com.zenplayer.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.android.awaitFrame

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ZenLogger.init(this)
        ZenLogger.info("APP", "MainActivity.onCreate")
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF70E6FF),
                    onPrimary = Color(0xFF001018),
                    surface = Color(0xFF0B0F18),
                    onSurface = Color(0xFFF7F8FC),
                    onSurfaceVariant = Color(0xFFB8C0D0)
                )
            ) {
                val initialFocus = remember { FocusRequester() }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // A FocusRequester on a non-focusable parent resolves to the
                        // first focusable descendant. This is the same Compose focus
                        // rule used by Android's TV samples for deterministic entry.
                        .focusRequester(initialFocus)
                ) {
                    ZenPlayerShellV6(SettingsStore(this@MainActivity))
                }

                LaunchedEffect(initialFocus) {
                    awaitFrame()
                    initialFocus.requestFocus()
                }
            }
        }
    }
}
