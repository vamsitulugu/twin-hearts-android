package com.twinhearts.app.feature.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme

/*
 * Why this file exists: the glass pieces of the Music page in one place, so Home, Recordings, the
 * mini player and Now Playing all look the same. Glass is a light-to-clear gradient fill plus a
 * bright-to-faint 1 dp edge; there is no real blur because that is what made scrolling slow (R-55).
 */

private val GlassFill = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.04f)))
private val GlassEdge = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.06f)))
private val WineFill = Brush.linearGradient(listOf(Color(0xFF9A1156), Color(0xFF4F0629)))
private val WineEdge = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.08f)))

/** Frosted grey glass. */
internal fun Modifier.musicGlass(shape: Shape): Modifier =
    this.clip(shape).background(GlassFill).border(1.dp, GlassEdge, shape)

/** Burgundy glass for the active / primary thing on a row. */
internal fun Modifier.musicWine(shape: Shape): Modifier =
    this.clip(shape).background(WineFill).border(1.dp, WineEdge, shape)

/** Round glass button with [content] centred, used for the Now Playing shortcuts and play circles. */
@Composable
internal fun MusicGlassCircle(size: androidx.compose.ui.unit.Dp, onClick: () -> Unit, modifier: Modifier = Modifier, wine: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .size(size)
            .then(if (wine) Modifier.musicWine(CircleShape) else Modifier.musicGlass(CircleShape))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** Top bar of the Music home: side-menu button, title, search and the ⋮ menu (its dropdown goes in [menu]). */
@Composable
internal fun MusicHeader(
    title: String,
    onMenu: () -> Unit,
    onSearch: () -> Unit,
    onMore: () -> Unit,
    menu: @Composable () -> Unit,
) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = TwinTheme.spacing.s, vertical = TwinTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMenu) {
            Icon(Icons.Rounded.Menu, contentDescription = stringResource(R.string.core_menu), tint = colors.ink)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = colors.ink,
            maxLines = 1,
            modifier = Modifier.weight(1f).padding(horizontal = TwinTheme.spacing.s),
        )
        IconButton(onClick = onSearch) {
            Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.music_page_search), tint = colors.ink)
        }
        Box {
            IconButton(onClick = onMore) {
                Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.music_more), tint = colors.ink)
            }
            menu()
        }
    }
}

/** Two big tabs on a glass track; the chosen one is burgundy glass. */
@Composable
internal fun MusicTabs(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val pill = TwinTheme.shapes.pill
    Row(modifier = modifier.fillMaxWidth().musicGlass(pill).padding(4.dp)) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .then(if (selected) Modifier.musicWine(pill) else Modifier.clip(pill))
                    .clickable { onSelect(index) }
                    .semantics { role = Role.Tab; this.selected = selected },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = MaterialTheme.typography.titleSmall, color = if (selected) Color.White else colors.inkMuted)
            }
        }
    }
}

/** Pill button / chip: burgundy glass when [selected], grey glass otherwise. Optional leading icon. */
@Composable
internal fun MusicPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: androidx.compose.ui.unit.Dp = 44.dp,
) {
    val colors = TwinTheme.colors
    val pill = TwinTheme.shapes.pill
    Row(
        modifier = modifier
            .heightIn(min = height)
            .then(if (selected) Modifier.musicWine(pill) else Modifier.musicGlass(pill))
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = TwinTheme.spacing.l),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, color = if (selected) Color.White else colors.ink, maxLines = 1)
    }
}
