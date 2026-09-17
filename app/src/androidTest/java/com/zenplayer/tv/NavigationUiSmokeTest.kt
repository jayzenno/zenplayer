package com.zenplayer.tv

import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Emulator-backed regression test for the real ZenPlayer shell.
 *
 * This deliberately drives the production MainActivity instead of a test-only
 * copy of the navigation UI. It verifies the TV contract that previously
 * regressed: D-pad focus, OK selection, Back to home, focus restoration and
 * the two-step exit dialog.
 */
@RunWith(AndroidJUnit4::class)
class NavigationUiSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun dpadSelectionBackAndExitFlow() {
        val home = composeRule.onNodeWithContentDescription("Home")
        val epg = composeRule.onNodeWithContentDescription("EPG")
        val search = composeRule.onNodeWithContentDescription("Suche")

        home.assertIsDisplayed()
        home.assertIsFocused()

        home.performKeyInput { pressKey(Key.DirectionDown) }
        epg.assertIsFocused()

        epg.performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.onNodeWithText("EPG").assertIsDisplayed()

        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        home.assertIsFocused()

        home.performKeyInput {
            pressKey(Key.DirectionDown)
            pressKey(Key.DirectionDown)
        }
        search.assertIsFocused()
        search.performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.onNodeWithText("Suche").assertIsDisplayed()

        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        home.assertIsFocused()

        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.onNodeWithText("ZenPlayer beenden?").assertIsDisplayed()
    }
}
