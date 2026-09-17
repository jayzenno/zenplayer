package com.zenplayer.tv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigationStateTest {
    @Test
    fun `OK selection changes page without changing remembered focus`() {
        val state = NavigationState(page = "home", lastSidebarId = "epg")

        val next = reduceNavigation(state, NavigationAction.OpenPage, "search")

        assertEquals("search", next.page)
        assertEquals("epg", next.lastSidebarId)
    }

    @Test
    fun `focus and selected page are independent`() {
        val state = NavigationState(page = "epg", lastSidebarId = "epg")

        val next = reduceNavigation(state, NavigationAction.FocusSidebar, "settings")

        assertEquals("epg", next.page)
        assertEquals("settings", next.lastSidebarId)
    }

    @Test
    fun `back from content returns home without opening exit dialog`() {
        val state = NavigationState(page = "search", lastSidebarId = "search")

        val next = reduceNavigation(state, NavigationAction.Back)

        assertEquals("home", next.page)
        assertFalse(next.showExitDialog)
    }

    @Test
    fun `first back on home arms second back instead of exiting`() {
        val next = reduceNavigation(NavigationState(), NavigationAction.Back)

        assertTrue(next.homeBackReset)
        assertFalse(next.showExitDialog)
    }

    @Test
    fun `second back on home opens exit dialog`() {
        val state = NavigationState(homeBackReset = true)

        val next = reduceNavigation(state, NavigationAction.Back)

        assertTrue(next.showExitDialog)
    }

    @Test
    fun `back dismisses exit dialog before doing anything else`() {
        val state = NavigationState(showExitDialog = true, homeBackReset = true)

        val next = reduceNavigation(state, NavigationAction.Back)

        assertFalse(next.showExitDialog)
        assertTrue(next.homeBackReset)
    }

    @Test
    fun `back closes player before navigating shell`() {
        val state = NavigationState(page = "epg", playerOpen = true)

        val next = reduceNavigation(state, NavigationAction.Back)

        assertFalse(next.playerOpen)
        assertEquals("epg", next.page)
    }

    @Test
    fun `back closes sources and restores source page`() {
        val state = NavigationState(page = "settings", sourcesOpen = true, sourceReturn = "settings")

        val next = reduceNavigation(state, NavigationAction.Back)

        assertFalse(next.sourcesOpen)
        assertEquals("settings", next.page)
    }
}
