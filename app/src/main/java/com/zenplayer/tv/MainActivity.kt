package com.zenplayer.tv

import android.app.KeyguardManager
import android.os.Bundle
import android.view.KeyEvent
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
                ZenPlayerShellV6(SettingsStore(this@MainActivity))
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        ZenLogger.event(
            "KEY",
            "action=${KeyEvent.actionToString(event.action)} key=${KeyEvent.keyCode} name=${KeyEvent.keyCodeName()} repeat=${event.repeatCount} device=${event.deviceId}"
        )
        return super.dispatchKeyEvent(event)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        ZenLogger.event("WINDOW", "focus=$hasFocus")
    }

    override fun onResume() {
        super.onResume()
        ZenLogger.event("LIFECYCLE", "resume")
    }

    override fun onPause() {
        ZenLogger.event("LIFECYCLE", "pause")
        super.onPause()
    }

    private fun KeyEvent.keyCodeName(): String =
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> "DPAD_UP"
            KeyEvent.KEYCODE_DPAD_DOWN -> "DPAD_DOWN"
            KeyEvent.KEYCODE_DPAD_LEFT -> "DPAD_LEFT"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "DPAD_RIGHT"
            KeyEvent.KEYCODE_DPAD_CENTER -> "DPAD_CENTER"
            KeyEvent.KEYCODE_ENTER -> "ENTER"
            KeyEvent.KEYCODE_BACK -> "BACK"
            KeyEvent.KEYCODE_ESCAPE -> "ESCAPE"
            KeyEvent.KEYCODE_MENU -> "MENU"
            KeyEvent.KEYCODE_HOME -> "HOME"
            KeyEvent.KEYCODE_VOLUME_UP -> "VOLUME_UP"
            KeyEvent.KEYCODE_VOLUME_DOWN -> "VOLUME_DOWN"
            else -> KeyEvent.keyCodeToString(keyCode)
        }
}
