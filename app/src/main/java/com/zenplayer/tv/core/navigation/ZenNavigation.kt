package com.zenplayer.tv.core.navigation

enum class FocusZone { Sidebar, Content, Player, SourceManager, Dialog }
enum class ZenPage { Home, Live, Epg, Search, Settings }

data class ZenNavigationState(
    val page: ZenPage = ZenPage.Home,
    val focusZone: FocusZone = FocusZone.Sidebar,
    val lastSidebarItem: ZenPage = ZenPage.Home,
    val lastContentItem: String? = null,
    val playerOpen: Boolean = false,
    val playerFullscreen: Boolean = false,
    val playerPreview: Boolean = false,
    val sourceManagerOpen: Boolean = false,
    val sourceReturnPage: ZenPage = ZenPage.Home,
    val exitArmed: Boolean = false,
)

enum class ZenNavigationAction {
    MoveSidebar, FocusContent, FocusSidebar, OpenPreview, PromotePreviewToFullscreen,
    ClosePlayer, OpenSources, CloseSources, OpenPage, Back,
}

fun reduceNavigation(
    state: ZenNavigationState,
    action: ZenNavigationAction,
    page: ZenPage = state.page,
    contentItem: String? = state.lastContentItem,
): ZenNavigationState = when (action) {
    ZenNavigationAction.MoveSidebar -> state.copy(page = page, lastSidebarItem = page, focusZone = FocusZone.Sidebar, exitArmed = false)
    ZenNavigationAction.FocusContent -> state.copy(focusZone = FocusZone.Content, lastContentItem = contentItem ?: state.lastContentItem, exitArmed = false)
    ZenNavigationAction.FocusSidebar -> state.copy(focusZone = FocusZone.Sidebar, exitArmed = false)
    ZenNavigationAction.OpenPreview -> state.copy(focusZone = FocusZone.Player, playerOpen = true, playerPreview = true, playerFullscreen = false, exitArmed = false)
    ZenNavigationAction.PromotePreviewToFullscreen -> state.copy(focusZone = FocusZone.Player, playerOpen = true, playerPreview = false, playerFullscreen = true, exitArmed = false)
    ZenNavigationAction.ClosePlayer -> state.copy(focusZone = FocusZone.Content, playerOpen = false, playerPreview = false, playerFullscreen = false, exitArmed = false)
    ZenNavigationAction.OpenSources -> state.copy(focusZone = FocusZone.SourceManager, sourceManagerOpen = true, sourceReturnPage = state.page, exitArmed = false)
    ZenNavigationAction.CloseSources -> state.copy(page = state.sourceReturnPage, focusZone = FocusZone.Content, sourceManagerOpen = false, exitArmed = false)
    ZenNavigationAction.OpenPage -> state.copy(page = page, lastSidebarItem = page, focusZone = FocusZone.Content, exitArmed = false)
    ZenNavigationAction.Back -> when {
        state.playerOpen -> reduceNavigation(state, ZenNavigationAction.ClosePlayer)
        state.sourceManagerOpen -> reduceNavigation(state, ZenNavigationAction.CloseSources)
        state.focusZone == FocusZone.Content -> state.copy(focusZone = FocusZone.Sidebar, exitArmed = false)
        state.page != ZenPage.Home -> state.copy(page = ZenPage.Home, lastSidebarItem = ZenPage.Home, focusZone = FocusZone.Sidebar, exitArmed = false)
        !state.exitArmed -> state.copy(exitArmed = true)
        else -> state.copy(exitArmed = false)
    }
}
