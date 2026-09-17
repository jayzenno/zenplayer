package com.zenplayer.tv

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Small, isolated Compose smoke test. It is deliberately not a CI gate yet. */
@RunWith(AndroidJUnit4::class)
class NavigationUiSmokeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun navigationSurfaceRenders() {
        composeRule.setContent { NavigationTestSurface() }
        composeRule.onNodeWithText("Home").assertIsDisplayed()
        composeRule.onNodeWithText("EPG").assertIsDisplayed()
        composeRule.onNodeWithText("Suche").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }
}

@androidx.compose.runtime.Composable
private fun NavigationTestSurface() {
    androidx.compose.foundation.layout.Row {
        androidx.compose.material3.Text("Home")
        androidx.compose.material3.Text("EPG")
        androidx.compose.material3.Text("Suche")
        androidx.compose.material3.Text("Settings")
    }
}
