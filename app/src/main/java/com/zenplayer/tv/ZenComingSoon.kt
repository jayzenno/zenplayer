package com.zenplayer.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenplayer.tv.domain.provider.MusicProvider

/** A source/plugin/provider row that is architecturally wired up but not connected yet. */
private data class ComingSoonEntry(val title: String, val subtitle: String, val icon: ImageVector)

@Composable
fun ZenVodScreen(accent: Color) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { }; runCatching { first.requestFocus() } }
    ZenComingSoonLayout(
        title = "VOD",
        tagline = "Filme & Serien",
        body = "Der VOD-Bereich wird auf der Nuvio-Plugin- und Collection-Architektur aufgebaut. " +
            "Sobald eine offene, autorisierte Schnittstelle verfügbar ist, erscheinen hier Kategorien, Suche und Watchlist.",
        accent = accent,
        first = first,
        entries = listOf(
            ComingSoonEntry("Nuvio Collections", "Wartet auf Plugin-Anbindung", Icons.Default.CollectionsBookmark),
            ComingSoonEntry("Filme & Serien", "Metadaten, Poster, Backdrops", Icons.Default.Movie),
            ComingSoonEntry("Watchlist & Fortschritt", "Continue Watching", Icons.Default.Bookmark),
        )
    )
}

@Composable
fun ZenMusicScreen(accent: Color) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { }; runCatching { first.requestFocus() } }
    val providers = remember { listOf(com.zenplayer.tv.domain.provider.EclipseMusicProvider(), com.zenplayer.tv.domain.provider.SpotifyProvider(), com.zenplayer.tv.domain.provider.AppleMusicProvider()) }
    ZenComingSoonLayout(
        title = "Music",
        tagline = "Spotify · Apple Music · EclipseMusic",
        body = "EclipseMusic hat oberste Priorität für eine native Integration. Spotify und Apple Music folgen über offizielle APIs.",
        accent = accent,
        first = first,
        entries = providers.map { musicProviderEntry(it) }
    )
}

private fun musicProviderEntry(provider: MusicProvider): ComingSoonEntry = ComingSoonEntry(
    provider.displayName,
    "Nicht verbunden",
    Icons.Default.MusicNote
)

@Composable
private fun ZenComingSoonLayout(title: String, tagline: String, body: String, accent: Color, first: FocusRequester, entries: List<ComingSoonEntry>) {
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        Text(title, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
        Text(tagline, color = Color(0xFF9EA6B8), fontSize = 14.sp)
        Spacer(Modifier.height(14.dp))
        Text(body, color = Color(0xFFB6BECF), fontSize = 13.sp, modifier = Modifier.fillMaxWidth(.7f))
        Spacer(Modifier.height(24.dp))
        Column(Modifier.focusGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            entries.forEachIndexed { index, entry ->
                ComingSoonRow(entry, accent, if (index == 0) first else null)
            }
        }
    }
}

@Composable
private fun ComingSoonRow(entry: ComingSoonEntry, accent: Color, requester: FocusRequester?) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth(.65f).height(72.dp)
            .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .background(if (focused) accent.copy(.14f) else Color.White.copy(.045f), RoundedCornerShape(18.dp))
            .border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(.08f), RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).background(accent.copy(.16f), RoundedCornerShape(14.dp)), Alignment.Center) {
            Icon(entry.icon, null, tint = accent)
        }
        Column(Modifier.padding(start = 16.dp)) {
            Text(entry.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(entry.subtitle, color = Color(0xFF9EA6B8), fontSize = 11.sp)
        }
    }
}
