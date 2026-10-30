package com.twinhearts.app.feature.movie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.twinhearts.app.core.design.TwinTheme

/*
 * Why this file exists: the glass pieces of the Movie page in one place, using exactly the same
 * recipe as the Music page (MusicGlass.kt) so both pages feel like one app. Glass is a light-to-clear
 * gradient fill plus a bright-to-faint 1 dp edge; there is no real blur because that made scrolling
 * slow (R-55).
 */

private val GlassFill = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.04f)))
private val GlassEdge = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.06f)))
private val WineFill = Brush.linearGradient(listOf(Color(0xFF9A1156), Color(0xFF4F0629)))
private val WineEdge = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.08f)))

/** Frosted grey glass. */
internal fun Modifier.movieGlass(shape: Shape): Modifier =
    this.clip(shape).background(GlassFill).border(1.dp, GlassEdge, shape)

/** Burgundy glass for the active / primary thing on a row. */
internal fun Modifier.movieWine(shape: Shape): Modifier =
    this.clip(shape).background(WineFill).border(1.dp, WineEdge, shape)

/** Pill chip / button: burgundy glass when [selected], grey glass otherwise. Optional leading icon. */
@Composable
internal fun MoviePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 40.dp,
) {
    val pill = TwinTheme.shapes.pill
    Row(
        modifier = modifier
            .heightIn(min = height)
            .then(if (selected) Modifier.movieWine(pill) else Modifier.movieGlass(pill))
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = TwinTheme.spacing.l),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, color = if (selected) Color.White else TwinTheme.colors.ink, maxLines = 1)
    }
}

/** Section title with a burgundy "See all" link, same as MusicSectionTitle. */
@Composable
internal fun MovieSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.padding(top = TwinTheme.spacing.s, bottom = TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = TwinTheme.colors.ink, modifier = Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            Box(
                Modifier.heightIn(min = TwinTheme.shapes.minTouch).clip(TwinTheme.shapes.small)
                    .clickable(onClick = onAction).padding(horizontal = TwinTheme.spacing.s),
                contentAlignment = Alignment.Center,
            ) { Text(actionLabel, style = MaterialTheme.typography.titleSmall, color = MovieStyle.Accent) }
        }
    }
}

/** Round glass button (back, history, favourite). [wine] makes it burgundy. */
@Composable
internal fun MovieGlassCircle(size: Dp, onClick: () -> Unit, modifier: Modifier = Modifier, wine: Boolean = false, content: @Composable () -> Unit) {
    Box(
        modifier = modifier.size(size)
            .then(if (wine) Modifier.movieWine(androidx.compose.foundation.shape.CircleShape) else Modifier.movieGlass(androidx.compose.foundation.shape.CircleShape))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** One stat in the Library row: big number over a small label, on glass. */
@Composable
internal fun MovieStat(value: String, label: String, modifier: Modifier = Modifier) {
    Box(modifier.heightIn(min = 64.dp).movieGlass(TwinTheme.shapes.card), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = TwinTheme.colors.ink, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, color = TwinTheme.colors.inkMuted, maxLines = 1)
        }
    }
}
