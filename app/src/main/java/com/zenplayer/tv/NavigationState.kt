package com.zenplayer.tv

/**
 * Pure navigation state used by the TV shell. Kept free of Compose/Android so
 * navigation regressions can be covered by fast JVM tests.
 */
data class NavigationState(
    val page: String = "home",
    val homeBackReset: Boolean = false,
    val showExitDialog: Boolean = false,
    val playerOpen: Boolean = false,
    val sourcesOpen: Boolean = false,
    val sourceReturn: String = "home",
    val lastSidebarId: String = "home",
)

enum class NavigationAction {
    Back,
    DismissExitDialog,
    OpenPage,
    OpenSources,
    CloseSources,
    OpenPlayer,
    ClosePlayer,
    FocusSidebar,
}

fun reduceNavigation(state: NavigationState, action: NavigationAction, page: String? = null): NavigationState = when (action) {
    NavigationAction.DismissExitDialog -> state.copy(showExitDialog = false)
    NavigationAction.OpenPage -> state.copy(
        page = page ?: state.page,
        homeBackReset = if ((page ?: state.page) == state.page) state.homeBackReset else false,
    )
    NavigationAction.FocusSidebar -> state.copy(lastSidebarId = page ?: state.lastSidebarId)
    NavigationAction.OpenSources -> state.copy(sourcesOpen = true, sourceReturn = state.page, homeBackReset = false)
    NavigationAction.CloseSources -> state.copy(sourcesOpen = false, page = state.sourceReturn, homeBackReset = false)
    NavigationAction.OpenPlayer -> state.copy(playerOpen = true)
    NavigationAction.ClosePlayer -> state.copy(playerOpen = false)
    NavigationAction.Back -> when {
        state.showExitDialog -> state.copy(showExitDialog = false)
        state.playerOpen -> state.copy(playerOpen = false)
        state.sourcesOpen -> state.copy(sourcesOpen = false, page = state.sourceReturn, homeBackReset = false)
        state.page != "home" -> state.copy(page = "home", homeBackReset = false)
        !state.homeBackReset -> state.copy(homeBackReset = true)
        else -> state.copy(showExitDialog = true)
    }
}
