package com.zenplayer.tv

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
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
 * regressed: D-pad focus, OK selection, blocked Left navigation from content,
 * Back to the sidebar/home and the two-step exit dialog.
 */
@RunWith(AndroidJUnit4::class)
class NavigationUiSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun dpadSelectionBackAndExitFlow() {
        val home = composeRule.onNodeWithContentDescription("Home").onParent()
        val epg = composeRule.onNodeWithContentDescription("EPG").onParent()
        val search = composeRule.onNodeWithContentDescription("Suche").onParent()

        composeRule.waitForIdle()
        home.assertIsDisplayed()
        home.assertIsFocused()

        home.performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.waitForIdle()
        epg.assertIsFocused()

        epg.performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("OK startet den gewählten Sender").assertIsDisplayed()

        // Left from content is deliberately blocked; focus must stay out of
        // the sidebar. Back is the explicit way back to the sidebar/home.
        epg.performKeyInput { pressKey(Key.DirectionLeft) }
        composeRule.waitForIdle()
        home.assertIsNotFocused()
        composeRule.onNodeWithText("OK startet den gewählten Sender").assertIsDisplayed()

        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitForIdle()
        home.assertIsFocused()

        home.performKeyInput {
            pressKey(Key.DirectionDown)
            pressKey(Key.DirectionDown)
        }
        composeRule.waitForIdle()
        search.assertIsFocused()
        search.performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Fokus öffnet nichts · erst OK startet die Eingabe").assertIsDisplayed()

        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitForIdle()
        home.assertIsFocused()

        // First Back on Home arms the two-step exit flow.
        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitForIdle()

        // Second Back on Home opens the exit dialog.
        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("ZenPlayer beenden?").assertIsDisplayed()
    }
}
