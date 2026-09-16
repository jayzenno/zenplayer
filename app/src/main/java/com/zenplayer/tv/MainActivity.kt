package com.zenplayer.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

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
                // V5 is the active TV shell. It owns navigation, source forms,
                // zap chrome and player presentation; V3 remains in the tree as a rollback reference.
                ZenPlayerShellV5(SettingsStore(this@MainActivity))
            }
        }
    }
}
