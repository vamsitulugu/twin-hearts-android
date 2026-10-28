package com.twinhearts.app.feature.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.twinhearts.app.feature.music.domain.MixKind
import com.twinhearts.app.feature.music.domain.SmartMixes
import java.time.Instant
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.feature.music.domain.LibraryQueries
import com.twinhearts.app.feature.music.domain.model.LibraryScope
import com.twinhearts.app.feature.music.domain.model.MusicLibrary
import com.twinhearts.app.feature.music.domain.model.Playlist
import com.twinhearts.app.feature.music.domain.model.Song
import com.twinhearts.app.feature.music.ui.components.MusicSectionTitle
import com.twinhearts.app.feature.music.ui.components.SongRow

/*
 * Why this file exists: the Music tab home. It follows `homeMusic` in the web app's
 * public/music/library.js: hero, tiles, Recently Played, Quick Actions, Our Playlists.
 */

/** Everything the home needs to tell the screen about. */
internal data class MusicHomeActions(
    val onRetry: () -> Unit,
    val onAddSongs: () -> Unit,
    val onOpenScope: (LibraryScope) -> Unit,
    val onKaraoke: () -> Unit,
    val onPlayAll: (shuffle: Boolean) -> Unit,
    val onSearch: () -> Unit,
    val onSearchOnline: () -> Unit,
    val onPlay: (Song, List<Song>) -> Unit,
    val onSongMenu: (Song) -> Unit,
    val onOpenPlaylist: (Playlist) -> Unit,
    val onNewPlaylist: () -> Unit,
    val onOpenMix: (MixKind) -> Unit,
)

@Composable
internal fun MusicHome(
    state: MusicUiState,
    currentSongId: String?,
    partnerName: String,
    actions: MusicHomeActions,
    modifier: Modifier = Modifier,
    bottomPadding: androidx.compose.ui.unit.Dp = 24.dp,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = TwinTheme.spacing.screenEdge,
            end = TwinTheme.spacing.screenEdge,
            bottom = bottomPadding,
        ),
    ) {
        state.adding?.let { progress ->
            item(key = "adding") { AddingCard(progress) }
        }
        when (val library = state.library) {
            UiState.Loading -> item(key = "loading") { LoadingSkeleton(lines = 6, modifier = Modifier.padding(top = TwinTheme.spacing.l)) }
            is UiState.Error -> item(key = "error") { ErrorState(message = library.error.asText(), onRetry = actions.onRetry) }
            UiState.Empty -> {
                item(key = "hero") { Hero(partnerName = partnerName, hasSongs = false, onShuffle = {}) }
                item(key = "empty") {
                    EmptyState(
                        title = stringResource(R.string.music_empty_title),
                        message = stringResource(R.string.music_empty_message),
                        icon = Icons.Rounded.MusicNote,
                        actionLabel = stringResource(R.string.music_add_songs),
                        onAction = actions.onAddSongs,
                        modifier = Modifier.padding(top = TwinTheme.spacing.xl),
                    )
                }
            }
            is UiState.Content -> content(library.data, state, currentSongId, partnerName, actions)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.content(
    library: MusicLibrary,
    state: MusicUiState,
    currentSongId: String?,
    partnerName: String,
    actions: MusicHomeActions,
) {
    val role = state.who?.myRole.orEmpty()
    val songs = library.songs
    val mine = LibraryQueries.forScope(LibraryScope.Mine, songs, role)
    val partner = LibraryQueries.forScope(LibraryScope.Partner, songs, role)
    val featured = LibraryQueries.forScope(LibraryScope.Featured, songs, role)
    val liked = LibraryQueries.forScope(LibraryScope.Liked, songs, role)
    val ready = LibraryQueries.singable(songs)
    val recent = LibraryQueries.forScope(LibraryScope.Recent, songs, role).take(RECENT_ON_HOME)

    item(key = "hero") {
        // The picks only change when the set of songs with artwork changes, so the hero stays put while you listen.
        val poolKey = songs.filter { !it.isPreview && !it.coverUrl.isNullOrBlank() }.map { it.id }.sorted().joinToString(",")
        val picks = remember(poolKey) { SmartMixes.heroPicks(songs) }
        if (picks.isEmpty()) {
            Hero(partnerName = partnerName, hasSongs = true, onShuffle = { actions.onPlayAll(true) })
        } else {
            HeroSlider(picks = picks, onPlay = { actions.onPlay(it, LibraryQueries.full(songs)) })
        }
    }

    item(key = "tiles") {
        Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m), modifier = Modifier.padding(top = TwinTheme.spacing.l)) {
            TileRow(
                Tile(Icons.Rounded.MusicNote, stringResource(R.string.music_tile_mine), mine.size.toString()) { actions.onOpenScope(LibraryScope.Mine) },
                Tile(Icons.Rounded.Favorite, stringResource(R.string.music_tile_partner), partner.size.toString()) { actions.onOpenScope(LibraryScope.Partner) },
            )
            TileRow(
                Tile(Icons.Rounded.Star, stringResource(R.string.music_tile_featured), featured.size.toString()) { actions.onOpenScope(LibraryScope.Featured) },
                Tile(Icons.Rounded.Mic, stringResource(R.string.music_tile_karaoke), stringResource(R.string.music_tile_karaoke_ready, ready.size), actions.onKaraoke),
            )
            TileRow(
                Tile(Icons.Rounded.Favorite, stringResource(R.string.music_tile_liked), liked.size.toString()) { actions.onOpenScope(LibraryScope.Liked) },
                null,
            )
        }
    }

    if (recent.isNotEmpty()) {
        item(key = "recent_title") {
            MusicSectionTitle(
                title = stringResource(R.string.music_recent_title),
                actionLabel = stringResource(R.string.music_see_all),
                onAction = { actions.onOpenScope(LibraryScope.Recent) },
            )
        }
        items(recent, key = { "recent_${it.id}" }) { song ->
            SongRow(
                song = song,
                current = song.id == currentSongId,
                onClick = { actions.onPlay(song, recent) },
                onMore = { actions.onSongMenu(song) },
            )
        }
    }

    item(key = "quick_title") { MusicSectionTitle(title = stringResource(R.string.music_quick_title)) }
    item(key = "quick") {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        ) {
            QuickChip(Icons.Rounded.PlayArrow, stringResource(R.string.music_quick_play_all), highlighted = true) { actions.onPlayAll(false) }
            QuickChip(Icons.Rounded.Shuffle, stringResource(R.string.music_quick_shuffle)) { actions.onPlayAll(true) }
            QuickChip(Icons.Rounded.Search, stringResource(R.string.music_quick_search), onClick = actions.onSearch)
            QuickChip(Icons.Rounded.Language, stringResource(R.string.music_quick_search_online), highlighted = true, onClick = actions.onSearchOnline)
        }
    }

    val mixes = SmartMixes.build(songs, Instant.now())
    if (mixes.isNotEmpty()) {
        item(key = "mixes_title") { MusicSectionTitle(title = stringResource(R.string.music_made_for_us)) }
        item(key = "mixes") { MadeForUsRow(mixes, partnerName, actions.onOpenMix) }
    }

    item(key = "playlists_title") {
        MusicSectionTitle(
            title = stringResource(R.string.music_playlists_title),
            actionLabel = if (library.playlists.available) stringResource(R.string.music_new_short) else null,
            onAction = if (library.playlists.available) actions.onNewPlaylist else null,
        )
    }
    item(key = "playlists") {
        if (!library.playlists.available) {
            GlassCard(contentPadding = TwinTheme.spacing.m) {
                Text(
                    stringResource(R.string.music_playlists_need_update),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TwinTheme.colors.inkMuted,
                )
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                items(library.playlists.playlists.reversed(), key = { it.id }) { playlist ->
                    PlaylistCard(playlist) { actions.onOpenPlaylist(playlist) }
                }
                item(key = "new_playlist") {
                    PlaylistCard(playlist = null, onClick = actions.onNewPlaylist)
                }
            }
        }
    }
}

@Composable
private fun Hero(partnerName: String, hasSongs: Boolean, onShuffle: () -> Unit) {
    val colors = TwinTheme.colors
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.xl) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.music_hero_title), style = MaterialTheme.typography.headlineMedium, color = colors.ink)
                Text(
                    text = if (hasSongs) {
                        stringResource(R.string.music_hero_subtitle)
                    } else {
                        stringResource(R.string.music_hero_first_song, partnerName)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(top = TwinTheme.spacing.s),
                )
            }
            if (hasSongs) {
                Box(
                    modifier = Modifier
                        .padding(start = TwinTheme.spacing.m)
                        .size(56.dp)
                        .clip(TwinTheme.shapes.circle)
                        .background(colors.primaryBrush)
                        .clickable(onClick = onShuffle),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.music_action_play), tint = colors.ink, modifier = Modifier.size(30.dp))
                }
            }
        }
    }
}

@Composable
private fun AddingCard(progress: AddProgress) {
    val colors = TwinTheme.colors
    GlassCard(modifier = Modifier.fillMaxWidth().padding(top = TwinTheme.spacing.m), contentPadding = TwinTheme.spacing.m) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = colors.accent)
            Column {
                Text(
                    stringResource(R.string.music_adding_progress, progress.done + 1, progress.total),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.ink,
                )
                Text(progress.currentName, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 1)
            }
        }
    }
}

private data class Tile(val icon: ImageVector, val label: String, val count: String, val onClick: () -> Unit)

@Composable
private fun TileRow(first: Tile, second: Tile?) {
    Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
        TileCard(first, Modifier.weight(1f))
        if (second != null) TileCard(second, Modifier.weight(1f)) else Box(Modifier.weight(1f))
    }
}

@Composable
private fun TileCard(tile: Tile, modifier: Modifier) {
    val colors = TwinTheme.colors
    GlassCard(modifier = modifier, onClick = tile.onClick, contentPadding = TwinTheme.spacing.m) {
        Icon(tile.icon, contentDescription = null, tint = colors.accent)
        Text(tile.label, style = MaterialTheme.typography.titleMedium, color = colors.ink, modifier = Modifier.padding(top = TwinTheme.spacing.s))
        Text(tile.count, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
    }
}

@Composable
private fun QuickChip(icon: ImageVector, label: String, highlighted: Boolean = false, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    val pill = TwinTheme.shapes.pill
    Row(
        modifier = Modifier
            .heightIn(min = TwinTheme.shapes.minTouch)
            .clip(pill)
            .background(if (highlighted) colors.primaryBrush else androidx.compose.ui.graphics.SolidColor(colors.glass))
            .border(1.dp, colors.line, pill)
            .clickable(onClick = onClick)
            .padding(horizontal = TwinTheme.spacing.l),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        Icon(icon, contentDescription = null, tint = colors.ink, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, color = colors.ink)
    }
}

@Composable
private fun PlaylistCard(playlist: Playlist?, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    GlassCard(modifier = Modifier.width(140.dp), onClick = onClick, contentPadding = TwinTheme.spacing.m) {
        Icon(
            if (playlist == null) Icons.Rounded.Add else Icons.Rounded.QueueMusic,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = playlist?.name ?: stringResource(R.string.music_new_playlist),
            style = MaterialTheme.typography.titleSmall,
            color = colors.ink,
            maxLines = 2,
            modifier = Modifier.padding(top = TwinTheme.spacing.s),
        )
        if (playlist != null) {
            Text(
                stringResource(R.string.music_song_count, playlist.songIds.size),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted,
            )
        }
    }
}

private const val RECENT_ON_HOME = 4
