package com.zenplayer.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.zenplayer.tv.ui.shell.ZenPlayerShell

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ZenLogger.init(this)
        ZenLogger.info("APP", "ZenPlayer 2 foundation")
        val settings = SettingsStore(this)
        setContent { ZenPlayerShell(settings) }
    }
}
