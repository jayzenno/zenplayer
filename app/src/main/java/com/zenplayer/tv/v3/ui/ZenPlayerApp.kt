package com.zenplayer.tv.v3.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class NavItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun ZenPlayerApp() {
    val nav = remember {
        listOf(
            NavItem("Home", Icons.Default.Home),
            NavItem("Live TV", Icons.Default.LiveTv),
            NavItem("EPG", Icons.Default.LiveTv),
            NavItem("Suche", Icons.Default.Search),
            NavItem("Settings", Icons.Default.Settings)
        )
    }
    var selected by remember { mutableIntStateOf(0) }
    var contentHasFocus by remember { mutableIntStateOf(0) }

    val sidebarRequester = remember { FocusRequester() }
    val contentRequester = remember { FocusRequester() }

    BackHandler(enabled = contentHasFocus == 1) {
        sidebarRequester.requestFocus()
        contentHasFocus = 0
    }

    MaterialTheme {
        Box(Modifier.fillMaxSize().background(Color(0xFF05070D))) {
            Row(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    Modifier
                        .width(92.dp)
                        .fillMaxHeight()
                        .focusGroup(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Z", color = Color(0xFF7DE7FF), fontSize = 32.sp)
                    nav.forEachIndexed { index, item ->
                        NavButton(
                            item = item,
                            active = index == selected,
                            modifier = Modifier
                                .then(if (index == selected) Modifier.focusRequester(sidebarRequester) else Modifier)
                                .focusProperties {
                                    if (index == selected) {
                                        right = contentRequester
                                    }
                                },
                            onFocus = { contentHasFocus = 0 },
                            onClick = {
                                selected = index
                                contentRequester.requestFocus()
                            }
                        )
                    }
                }

                ContentPane(
                    title = nav[selected].label,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .focusRequester(contentRequester)
                        .focusable()
                        .focusProperties {
                            left = androidx.compose.ui.focus.FocusRequester.Cancel
                        }
                        .onFocusChanged { contentHasFocus = if (it.hasFocus) 1 else 0 }
                )
            }
        }
    }
}

@Composable
private fun ContentPane(
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = Color.White, fontSize = 38.sp)
            Text(
                "ZenPlayer 3.0 · Clean TV-first foundation",
                color = Color(0xFF9CA6BA),
                fontSize = 16.sp
            )
            Spacer(Modifier.width(1.dp))
            Text(
                "Navigation, Datenquellen, EPG und Player werden jetzt einzeln aufgebaut.",
                color = Color(0xFF69748A),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun NavButton(
    item: NavItem,
    active: Boolean,
    modifier: Modifier = Modifier,
    onFocus: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier
            .width(76.dp)
            .background(
                if (active) Color(0x227DE7FF) else Color.Transparent,
                RoundedCornerShape(20.dp)
            )
            .focusable()
            .onFocusChanged { if (it.hasFocus) onFocus() }
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            item.icon,
            contentDescription = item.label,
            tint = if (active) Color.White else Color(0xFF8791A6)
        )
    }
}
