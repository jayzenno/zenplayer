package com.zenplayer.tv.v3.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.first

private data class NavItem(val label: String, val icon: ImageVector)

@Composable
fun ZenPlayerApp() {
    val context = LocalContext.current
    val themeStore = remember(context) { ZenThemeStore(context.applicationContext) }
    var themeState by remember { mutableStateOf(ZenThemeState()) }
    var themeHydrated by remember { mutableStateOf(false) }
    var selected by remember { mutableIntStateOf(0) }
    var contentHasFocus by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(themeStore) {
        themeState = themeStore.state.first()
        themeHydrated = true
    }
    LaunchedEffect(themeState, themeHydrated) {
        if (themeHydrated) themeStore.save(themeState)
    }

    val nav = remember {
        listOf(
            NavItem("Home", Icons.Default.Home),
            NavItem("Live TV", Icons.Default.LiveTv),
            NavItem("EPG", Icons.Default.LiveTv),
            NavItem("Suche", Icons.Default.Search),
            NavItem("Settings", Icons.Default.Settings)
        )
    }
    val sidebarRequesters = remember { List(nav.size) { FocusRequester() } }
    val contentRequester = remember { FocusRequester() }
    val studioRequester = remember { FocusRequester() }

    BackHandler {
        if (showExitDialog) {
            showExitDialog = false
        } else if (contentHasFocus) {
            contentHasFocus = false
            sidebarRequesters[selected].requestFocus()
        } else {
            showExitDialog = true
        }
    }

    LaunchedEffect(Unit) {
        sidebarRequesters[0].requestFocus()
    }

    ZenTheme(themeState) {
        Box(Modifier.fillMaxSize()) {
            ZenBackground(themeState)
            Box(
                Modifier.fillMaxSize().background(
                    MaterialTheme.colorScheme.background.copy(alpha = .38f)
                )
            )
            Row(
                Modifier.fillMaxSize().padding(22.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Sidebar(
                    nav = nav,
                    selected = selected,
                    requesters = sidebarRequesters,
                    onSelect = { index ->
                        selected = index
                        contentHasFocus = false
                    },
                    onEnterContent = {
                        contentHasFocus = true
                        if (selected == nav.lastIndex) studioRequester.requestFocus() else contentRequester.requestFocus()
                    },
                    contentRequester = contentRequester,
                    studioRequester = studioRequester
                )

                Box(Modifier.weight(1f).fillMaxHeight()) {
                    if (selected == nav.lastIndex) {
                        ThemeStudio(
                            state = themeState,
                            onStateChange = { themeState = it },
                            firstFocusRequester = studioRequester,
                            sidebarRequester = sidebarRequesters[selected],
                            onChildFocus = { contentHasFocus = true },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        ContentPane(
                            title = nav[selected].label,
                            modifier = Modifier.fillMaxSize()
                                .focusRequester(contentRequester)
                                .focusable()
                                .focusProperties { left = androidx.compose.ui.focus.FocusRequester.Cancel }
                                .onFocusChanged { contentHasFocus = it.hasFocus }
                        )
                    }
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("ZenPlayer schließen?") },
            text = { Text("Möchtest du die App wirklich beenden?") },
            confirmButton = {
                Button(onClick = { (context as? Activity)?.finish() }) { Text("Beenden") }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("Abbrechen") }
            }
        )
    }
}

@Composable
private fun Sidebar(
    nav: List<NavItem>,
    selected: Int,
    requesters: List<FocusRequester>,
    onSelect: (Int) -> Unit,
    onEnterContent: () -> Unit,
    contentRequester: FocusRequester,
    studioRequester: FocusRequester
) {
    Column(
        Modifier.width(232.dp).fillMaxHeight()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = .68f))
            .focusGroup()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(6.dp).fillMaxHeight().background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("ZENPLAYER", fontSize = 20.sp)
                Text("TV", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.width(1.dp))
        nav.forEachIndexed { index, item ->
            SidebarItem(
                item = item,
                selected = index == selected,
                requester = requesters[index],
                onFocus = {},
                onClick = { onSelect(index) },
                focusRight = if (index == nav.lastIndex) studioRequester else contentRequester
            )
        }
        Spacer(Modifier.weight(1f))
        Text("OK auswählen · ←/→ navigieren", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(10.dp))
    }
}

@Composable
private fun SidebarItem(
    item: NavItem,
    selected: Boolean,
    requester: FocusRequester,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    focusRight: FocusRequester
) {
    var focused by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary
    Box(
        Modifier.fillMaxWidth().focusRequester(requester)
            .focusProperties { right = focusRight }
            .onFocusChanged {
                focused = it.hasFocus
                if (it.hasFocus) onFocus()
            }
            .focusable()
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(18.dp))
            .background(
                when {
                    focused -> accent.copy(alpha = .24f)
                    selected -> accent.copy(alpha = .13f)
                    else -> Color.Transparent
                },
                RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(item.icon, item.label, tint = if (focused || selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(item.label, color = if (focused || selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
        }
    }
}

@Composable
private fun ContentPane(title: String, modifier: Modifier = Modifier) {
    Box(modifier.padding(32.dp), contentAlignment = Alignment.CenterStart) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 42.sp)
            Text("ZenPlayer 3.0", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
            Text("Die TV-first Oberfläche wird jetzt Schritt für Schritt aufgebaut.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
        }
    }
}
