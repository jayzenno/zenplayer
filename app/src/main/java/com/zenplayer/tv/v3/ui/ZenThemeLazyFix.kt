package com.zenplayer.tv.v3.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * LazyListScope adapter for the standalone Reduced Motion option in ThemeStudio.
 * LazyRow's content block is a LazyListScope DSL, so this wrapper emits the
 * composable card through item {} instead of invoking a composable directly
 * from the DSL scope.
 */
fun LazyListScope.CompactOptionCard(
    label: String,
    selected: Boolean,
    accent: Color,
    onFocus: () -> Unit,
    onClick: () -> Unit
) {
    item(key = "theme-option-$label") {
        Box(
            Modifier
                .size(width = 150.dp, height = 58.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) accent else Color.White.copy(alpha = .08f),
                    shape = RoundedCornerShape(18.dp)
                )
                .focusable()
                .clickable(onClick = onClick)
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(13.dp).background(accent, RoundedCornerShape(7.dp)))
                Text(label, fontSize = 16.sp)
                if (selected) Text("Aktiv", fontSize = 12.sp, color = accent)
            }
        }
    }
}
