package com.zenplayer.tv

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
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

        composeRule.waitForIdle()
        home.assertIsDisplayed()
        home.onParent().assertIsFocused()

        home.performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.waitForIdle()
        epg.onParent().assertIsFocused()

        epg.performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("EPG").assertIsDisplayed()

        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitForIdle()
        home.onParent().assertIsFocused()

        home.performKeyInput {
            pressKey(Key.DirectionDown)
            pressKey(Key.DirectionDown)
        }
        composeRule.waitForIdle()
        search.onParent().assertIsFocused()
        search.performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Suche").assertIsDisplayed()

        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitForIdle()
        home.onParent().assertIsFocused()

        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("ZenPlayer beenden?").assertIsDisplayed()
    }
}
