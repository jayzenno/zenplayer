package com.zenplayer.tv

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Diagnostic, emulator-backed navigation test for the real MainActivity.
 *
 * Every transition is logged before and after the input. On failure we also
 * dump the Compose semantics tree so the CI log contains enough evidence to
 * distinguish input, focus, state, and rendering failures.
 */
@RunWith(AndroidJUnit4::class)
class NavigationUiDiagnosticsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun traceDpadNavigationContract() {
        val home = composeRule.onNodeWithContentDescription("Home").onParent()
        val epg = composeRule.onNodeWithContentDescription("EPG").onParent()
        val search = composeRule.onNodeWithContentDescription("Suche").onParent()

        trace("BOOT") {
            composeRule.waitForIdle()
            home.assertIsDisplayed()
            home.assertIsFocused()
        }

        trace("HOME --DOWN--> EPG") {
            home.performKeyInput { pressKey(Key.DirectionDown) }
            composeRule.waitForIdle()
            epg.assertIsFocused()
        }

        trace("EPG --CENTER--> EPG CONTENT") {
            epg.performKeyInput { pressKey(Key.DirectionCenter) }
            composeRule.waitForIdle()
            composeRule.onNodeWithText("OK startet den gewählten Sender").assertIsDisplayed()
        }

        trace("EPG CONTENT --BACK--> SIDEBAR") {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
            composeRule.waitForIdle()
            home.assertIsFocused()
        }

        trace("HOME --DOWN,DOWN--> SEARCH") {
            home.performKeyInput { pressKey(Key.DirectionDown) }
            composeRule.waitForIdle()
            epg.assertIsFocused()
            epg.performKeyInput { pressKey(Key.DirectionDown) }
            composeRule.waitForIdle()
            search.assertIsFocused()
        }

        trace("SEARCH --CENTER--> SEARCH CONTENT") {
            search.performKeyInput { pressKey(Key.DirectionCenter) }
            composeRule.waitForIdle()
            composeRule.onNodeWithText("Fokus öffnet nichts · erst OK startet die Eingabe").assertIsDisplayed()
        }

        trace("SEARCH CONTENT --BACK--> SIDEBAR") {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
            composeRule.waitForIdle()
            search.assertIsFocused()
        }

        trace("SEARCH SIDEBAR --BACK--> HOME") {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
            composeRule.waitForIdle()
            home.assertIsFocused()
        }

        trace("HOME --BACK--> EXIT ARM") {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
            composeRule.waitForIdle()
            home.assertIsFocused()
            composeRule.onAllNodesWithText("ZenPlayer beenden?").assertCountEquals(0)
        }

        trace("HOME --BACK--> EXIT DIALOG") {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
            composeRule.waitForIdle()
            composeRule.onNodeWithText("ZenPlayer beenden?").assertIsDisplayed()
        }
    }

    private fun trace(step: String, action: () -> Unit) {
        println("\n===== ZENPLAYER UI DIAGNOSTIC: $step =====")
        println("BEFORE: ${describeFocus()}")
        try {
            action()
            println("AFTER:  ${describeFocus()}")
            println("RESULT: PASS")
        } catch (failure: Throwable) {
            println("AFTER:  ${describeFocus()}")
            println("RESULT: FAIL")
            println("FAILURE TYPE: ${failure::class.qualifiedName}")
            println("FAILURE MESSAGE: ${failure.message}")
            println("--- COMPOSE SEMANTICS TREE ---")
            try {
                composeRule.onRoot(useUnmergedTree = true).printToLog("ZENPLAYER_DIAGNOSTIC")
            } catch (dumpFailure: Throwable) {
                println("SEMANTICS DUMP FAILED: ${dumpFailure.message}")
            }
            throw failure
        }
    }

    private fun describeFocus(): String {
        val states = listOf(
            "Home" to composeRule.onNodeWithContentDescription("Home").onParent(),
            "EPG" to composeRule.onNodeWithContentDescription("EPG").onParent(),
            "Suche" to composeRule.onNodeWithContentDescription("Suche").onParent(),
        )
        return states.joinToString(" | ") { (name, node) ->
            try {
                node.assertIsFocused()
                "$name=FOCUSED"
            } catch (_: Throwable) {
                "$name=not-focused"
            }
        }
    }
}
