package com.twinhearts.app.feature.music.ui

import com.twinhearts.app.feature.music.ui.components.MusicGlassCircle
import com.twinhearts.app.feature.music.ui.components.musicWine
import com.twinhearts.app.feature.music.ui.components.musicGlass
import com.twinhearts.app.feature.music.ui.components.SongCover
import com.twinhearts.app.feature.music.ui.components.MusicStyle
import com.twinhearts.app.feature.music.ui.components.MusicPill
import coil.compose.AsyncImage
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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

    // 1. Liked Songs: the one big card, as in the design.
    item(key = "liked") {
        LikedCard(count = liked.size, coverUrl = liked.firstOrNull { !it.coverUrl.isNullOrBlank() }?.coverUrl) {
            actions.onOpenScope(LibraryScope.Liked)
        }
    }

    // 2. The other libraries stay one tap away, in a single scrolling row instead of a 2x3 grid.
    item(key = "library") {
        LazyRow(
            modifier = Modifier.padding(top = TwinTheme.spacing.m),
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        ) {
            item { LibraryChip(Icons.Rounded.MusicNote, stringResource(R.string.music_tile_mine), mine.size.toString()) { actions.onOpenScope(LibraryScope.Mine) } }
            item { LibraryChip(Icons.Rounded.Favorite, stringResource(R.string.music_tile_partner), partner.size.toString()) { actions.onOpenScope(LibraryScope.Partner) } }
            item { LibraryChip(Icons.Rounded.Star, stringResource(R.string.music_tile_featured), featured.size.toString()) { actions.onOpenScope(LibraryScope.Featured) } }
            item { AddSongsButton(actions.onAddSongs) }
            item { LibraryChip(Icons.Rounded.Mic, stringResource(R.string.music_tile_karaoke), stringResource(R.string.music_tile_karaoke_ready, ready.size), actions.onKaraoke) }
        }
    }

    // 3. Recently Played: covers in a row, not a long list.
    if (recent.isNotEmpty()) {
        item(key = "recent_title") {
            MusicSectionTitle(
                title = stringResource(R.string.music_recent_title),
                actionLabel = stringResource(R.string.music_see_all),
                onAction = { actions.onOpenScope(LibraryScope.Recent) },
            )
        }
        item(key = "recent") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                items(recent, key = { "recent_${it.id}" }) { song ->
                    RecentCard(song = song, current = song.id == currentSongId, onClick = { actions.onPlay(song, recent) })
                }
            }
        }
    }

    // 4. Quick Actions: three equal buttons. Plain search is the magnifier at the top.
    item(key = "quick_title") { MusicSectionTitle(title = stringResource(R.string.music_quick_title)) }
    item(key = "quick") {
        Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            MusicPill(stringResource(R.string.music_quick_play_all), selected = true, onClick = { actions.onPlayAll(false) }, modifier = Modifier.weight(1f), icon = Icons.Rounded.PlayArrow)
            MusicPill(stringResource(R.string.music_quick_shuffle), selected = false, onClick = { actions.onPlayAll(true) }, modifier = Modifier.weight(1f), icon = Icons.Rounded.Shuffle)
            MusicPill(stringResource(R.string.music_quick_search_online), selected = false, onClick = actions.onSearchOnline, modifier = Modifier.weight(1f), icon = Icons.Rounded.Language)
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
            val coverById = remember(songs) { songs.associate { it.id to it.coverUrl } }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                item(key = "new_playlist") {
                    PlaylistCard(playlist = null, coverUrl = null, onClick = actions.onNewPlaylist)
                }
                items(library.playlists.playlists.reversed(), key = { it.id }) { playlist ->
                    val cover = playlist.songIds.firstNotNullOfOrNull { id -> coverById[id]?.takeIf { it.isNotBlank() } }
                    PlaylistCard(playlist, cover) { actions.onOpenPlaylist(playlist) }
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

/** "Liked Songs": wide burgundy-glass card with the heart, the count and a faint cover behind it. */
@Composable
private fun LikedCard(count: Int, coverUrl: String?, onClick: () -> Unit) {
    val shape = TwinTheme.shapes.card
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = TwinTheme.spacing.xs)
            .musicWine(shape)
            .clickable(onClick = onClick),
    ) {
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.35f,
                modifier = Modifier.matchParentSize(),
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(Brush.horizontalGradient(listOf(Color(0xFF4F0629), Color(0xFF4F0629).copy(alpha = 0.55f), Color.Transparent))),
            )
        }
        Row(
            modifier = Modifier.padding(TwinTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            Box(
                modifier = Modifier.size(56.dp).musicGlass(TwinTheme.shapes.field),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.music_tile_liked), style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text(stringResource(R.string.music_song_count, count), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.8f))
        }
    }
}

/** Small glass pill for a library: icon, name and count. */
@Composable
private fun LibraryChip(icon: ImageVector, label: String, count: String, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    val pill = TwinTheme.shapes.pill
    Row(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .musicGlass(pill)
            .clickable(onClick = onClick)
            .padding(horizontal = TwinTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        Icon(icon, contentDescription = null, tint = MusicStyle.Accent, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, color = colors.ink, maxLines = 1)
        Text(count, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 1)
    }
}

/** Small round + next to the library chips: add songs from the phone. */
@Composable
private fun AddSongsButton(onClick: () -> Unit) {
    MusicGlassCircle(size = 44.dp, onClick = onClick, wine = true) {
        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.music_add_songs), tint = Color.White)
    }
}

/** One recently played song: square cover with a play badge, title and artist underneath. */
@Composable
private fun RecentCard(song: Song, current: Boolean, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    Column(modifier = Modifier.width(112.dp).clip(TwinTheme.shapes.field).clickable(onClick = onClick)) {
        Box {
            SongCover(url = song.coverUrl, size = 112.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.music_action_play), tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        Text(
            song.title,
            style = MaterialTheme.typography.titleSmall,
            color = if (current) MusicStyle.Accent else colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = TwinTheme.spacing.xs),
        )
        Text(song.artist, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A playlist: cover of its first song (or a note), name and song count. The first card creates a new one. */
@Composable
private fun PlaylistCard(playlist: Playlist?, coverUrl: String?, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.field
    Column(modifier = Modifier.width(112.dp).clip(shape).clickable(onClick = onClick)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(96.dp).musicGlass(shape),
            contentAlignment = Alignment.Center,
        ) {
            when {
                playlist == null -> Icon(Icons.Rounded.Add, contentDescription = null, tint = colors.ink, modifier = Modifier.size(32.dp))
                coverUrl != null -> AsyncImage(model = coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else -> Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = MusicStyle.Accent, modifier = Modifier.size(32.dp))
            }
        }
        Text(
            text = playlist?.name ?: stringResource(R.string.music_new_playlist),
            style = MaterialTheme.typography.titleSmall,
            color = colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = TwinTheme.spacing.xs),
        )
        if (playlist != null) {
            Text(stringResource(R.string.music_song_count, playlist.songIds.size), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
        }
    }
}

private const val RECENT_ON_HOME = 10
