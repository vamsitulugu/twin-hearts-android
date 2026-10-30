package com.twinhearts.app.feature.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.music.domain.MusicFormat
import com.twinhearts.app.feature.music.domain.model.Song

/*
 * Why this file exists: small pieces every Music screen reuses (cover art, song row, section
 * title, sheet action row). They are stateless: everything comes in as parameters (R-42).
 */

/** Album art, or a music-note tile when the song has none. */
@Composable
internal fun SongCover(url: String?, size: Dp, modifier: Modifier = Modifier, icon: ImageVector = Icons.Rounded.MusicNote) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.small
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(colors.glass)
            .border(1.dp, colors.line, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        } else {
            Icon(icon, contentDescription = null, tint = colors.pinkSoft, modifier = Modifier.size(size / 2))
        }
    }
}

/** One song in a list. The current song's title is drawn in the accent colour. */
@Composable
internal fun SongRow(
    song: Song,
    current: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    ownerLabel: String? = null,
) {
    val colors = TwinTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .musicGlass(TwinTheme.shapes.field)
            .clickable(onClick = onClick)
            .padding(start = TwinTheme.spacing.xs, top = TwinTheme.spacing.xs, bottom = TwinTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        SongCover(url = song.coverUrl, size = 52.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (current) MusicStyle.Accent else colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = if (ownerLabel != null) "${song.artist} · $ownerLabel" else song.artist
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (song.liked) {
            Icon(
                Icons.Rounded.Favorite,
                contentDescription = stringResource(R.string.music_liked),
                tint = MusicStyle.Accent,
                modifier = Modifier.size(16.dp),
            )
        }
        if (song.durationSec > 0) {
            Text(MusicFormat.clock(song.durationSec), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
        }
        IconButton(onClick = onMore) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.music_more), tint = colors.inkMuted)
        }
    }
}

/** "Recently Played" style heading with an optional link on the right. */
@Composable
internal fun MusicSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = TwinTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = TwinTheme.spacing.xl, bottom = TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = colors.ink, modifier = Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            Box(
                modifier = Modifier
                    .heightIn(min = TwinTheme.shapes.minTouch)
                    .clip(TwinTheme.shapes.small)
                    .clickable(onClick = onAction)
                    .padding(horizontal = TwinTheme.spacing.s),
                contentAlignment = Alignment.Center,
            ) {
                Text(actionLabel, style = MaterialTheme.typography.titleSmall, color = MusicStyle.Accent)
            }
        }
    }
}

/** One line in a bottom sheet: icon, label, optional "on" state, red when [danger]. */
@Composable
internal fun SheetAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    highlighted: Boolean = false,
) {
    val colors = TwinTheme.colors
    val tint = when {
        danger -> colors.error
        highlighted -> colors.accent
        else -> colors.ink
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = TwinTheme.shapes.minTouch)
            .clip(TwinTheme.shapes.field)
            .clickable(onClick = onClick)
            .padding(horizontal = TwinTheme.spacing.s, vertical = TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.l),
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Text(label, style = MaterialTheme.typography.titleMedium, color = tint)
    }
}
