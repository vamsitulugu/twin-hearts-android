package com.twinhearts.app.feature.music.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.music.domain.MusicFormat
import com.twinhearts.app.feature.music.domain.model.OnlineTrack
import com.twinhearts.app.feature.music.domain.model.Playlist
import com.twinhearts.app.feature.music.domain.model.Song
import com.twinhearts.app.feature.music.ui.components.MusicSectionTitle
import com.twinhearts.app.feature.music.ui.components.SongCover
import com.twinhearts.app.feature.music.ui.components.SongRow

/*
 * Why this file exists: the screens pushed on top of the Music home: song lists (My Playlist,
 * Partner, Featured, Liked, Recent), one playlist, library search, Search Online and the
 * Karaoke list. Each is a plain function of the data it is given.
 */

private val listPadding: PaddingValues @Composable get() = PaddingValues(
    start = TwinTheme.spacing.screenEdge,
    end = TwinTheme.spacing.screenEdge,
    bottom = 24.dp,
)

/** A list of songs with Play and Shuffle on top. Used for scopes and for one playlist. */
@Composable
internal fun SongListPage(
    songs: List<Song>,
    currentSongId: String?,
    emptyTitle: String,
    emptyMessage: String,
    onPlay: (Song, List<Song>) -> Unit,
    onPlayAll: (shuffle: Boolean) -> Unit,
    onSongMenu: (Song) -> Unit,
    modifier: Modifier = Modifier,
    headerExtra: (@Composable () -> Unit)? = null,
    ownerLabel: ((Song) -> String?)? = null,
) {
    LazyColumn(modifier = modifier, contentPadding = listPadding) {
        if (headerExtra != null) item(key = "extra") { headerExtra() }
        if (songs.isEmpty()) {
            item(key = "empty") {
                EmptyState(title = emptyTitle, message = emptyMessage, icon = Icons.Rounded.MusicNote, modifier = Modifier.padding(top = TwinTheme.spacing.xl))
            }
        } else {
            item(key = "buttons") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = TwinTheme.spacing.m),
                    horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
                ) {
                    GlassButton(
                        text = stringResource(R.string.music_action_play),
                        onClick = { onPlayAll(false) },
                        leadingIcon = Icons.Rounded.PlayArrow,
                        modifier = Modifier.weight(1f),
                    )
                    GlassButton(
                        text = stringResource(R.string.music_quick_shuffle),
                        onClick = { onPlayAll(true) },
                        leadingIcon = Icons.Rounded.Shuffle,
                        style = GlassButtonStyle.Secondary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            items(songs, key = { it.id }) { song ->
                SongRow(
                    song = song,
                    current = song.id == currentSongId,
                    onClick = { onPlay(song, songs) },
                    onMore = { onSongMenu(song) },
                    ownerLabel = ownerLabel?.invoke(song),
                )
            }
        }
    }
}

/** Library search: songs and playlists that match; with no text, the recently played songs. */
@Composable
internal fun SearchPage(
    query: String,
    onQuery: (String) -> Unit,
    songs: List<Song>,
    recent: List<Song>,
    playlists: List<Playlist>,
    currentSongId: String?,
    onPlay: (Song, List<Song>) -> Unit,
    onSongMenu: (Song) -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    modifier: Modifier = Modifier,
) {
    val matchedSongs = if (query.isBlank()) emptyList() else songs.filter { MusicFormat.matches(it, query) }
    val matchedLists = if (query.isBlank()) emptyList() else playlists.filter { MusicFormat.normalise(it.name).contains(MusicFormat.normalise(query)) }
    LazyColumn(modifier = modifier, contentPadding = listPadding) {
        item(key = "field") {
            TwinTextField(
                value = query,
                onValueChange = onQuery,
                label = stringResource(R.string.music_search_label),
                placeholder = stringResource(R.string.music_search_hint),
                leadingIcon = Icons.Rounded.Search,
                imeAction = ImeAction.Search,
                modifier = Modifier.padding(top = TwinTheme.spacing.m),
            )
        }
        if (query.isBlank()) {
            if (recent.isNotEmpty()) {
                item(key = "recent_title") { MusicSectionTitle(stringResource(R.string.music_search_recent)) }
                items(recent, key = { "r_${it.id}" }) { song ->
                    SongRow(song, song.id == currentSongId, { onPlay(song, recent) }, { onSongMenu(song) })
                }
            }
        } else if (matchedSongs.isEmpty() && matchedLists.isEmpty()) {
            item(key = "none") {
                EmptyState(
                    title = stringResource(R.string.music_search_none_title),
                    message = stringResource(R.string.music_search_none_message),
                    icon = Icons.Rounded.Search,
                    modifier = Modifier.padding(top = TwinTheme.spacing.xl),
                )
            }
        } else {
            if (matchedSongs.isNotEmpty()) {
                item(key = "songs_title") { MusicSectionTitle(stringResource(R.string.music_search_songs)) }
                items(matchedSongs, key = { "s_${it.id}" }) { song ->
                    SongRow(song, song.id == currentSongId, { onPlay(song, matchedSongs) }, { onSongMenu(song) })
                }
            }
            if (matchedLists.isNotEmpty()) {
                item(key = "lists_title") { MusicSectionTitle(stringResource(R.string.music_playlists_title)) }
                items(matchedLists, key = { "p_${it.id}" }) { playlist ->
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TwinTheme.colors.ink,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = TwinTheme.spacing.m)
                            .clickable { onOpenPlaylist(playlist) },
                    )
                }
            }
        }
    }
}

/** "Search Online": Deezer through the server. Results can be previewed (30 s) and added to Featured. */
@Composable
internal fun OnlineSearchPage(
    state: OnlineUiState,
    onQuery: (String) -> Unit,
    onSearch: () -> Unit,
    onPreview: (OnlineTrack) -> Unit,
    onAdd: (OnlineTrack) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    LazyColumn(modifier = modifier, contentPadding = listPadding) {
        item(key = "field") {
            Column(modifier = Modifier.padding(top = TwinTheme.spacing.m), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                TwinTextField(
                    value = state.query,
                    onValueChange = onQuery,
                    label = stringResource(R.string.music_online_label),
                    placeholder = stringResource(R.string.music_online_hint),
                    leadingIcon = Icons.Rounded.Language,
                    imeAction = ImeAction.Search,
                    onImeAction = onSearch,
                )
                GlassButton(
                    text = stringResource(R.string.music_online_search),
                    onClick = onSearch,
                    enabled = state.query.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        when (val results = state.results) {
            null -> item(key = "intro") {
                EmptyState(
                    title = stringResource(R.string.music_online_intro_title),
                    message = stringResource(R.string.music_online_intro_message),
                    icon = Icons.Rounded.Language,
                    modifier = Modifier.padding(top = TwinTheme.spacing.xl),
                )
            }
            UiState.Loading -> item(key = "loading") { LoadingSkeleton(lines = 5, modifier = Modifier.padding(top = TwinTheme.spacing.l)) }
            UiState.Empty -> item(key = "none") {
                EmptyState(
                    title = stringResource(R.string.music_search_none_title),
                    message = stringResource(R.string.music_online_none_message),
                    icon = Icons.Rounded.Search,
                    modifier = Modifier.padding(top = TwinTheme.spacing.xl),
                )
            }
            is UiState.Error -> item(key = "error") { ErrorState(results.error.asText(), onSearch) }
            is UiState.Content -> items(results.data, key = { it.id }) { track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = TwinTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
                ) {
                    SongCover(url = track.coverUrl, size = 52.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(track.title, style = MaterialTheme.typography.titleMedium, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.artist, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (track.previewUrl != null) {
                        IconButton(onClick = { onPreview(track) }) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.music_online_preview), tint = colors.ink)
                        }
                        when {
                            state.addingId == track.id -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = colors.accent)
                            track.id in state.addedIds -> Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.music_online_added), tint = colors.success)
                            else -> IconButton(onClick = { onAdd(track) }) {
                                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.music_online_add), tint = colors.accent)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Songs that have lyrics, ready for karaoke. */
@Composable
internal fun KaraokeListPage(
    songs: List<Song>,
    onSing: (Song) -> Unit,
    onSongMenu: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, contentPadding = listPadding) {
        if (songs.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    title = stringResource(R.string.music_karaoke_empty_title),
                    message = stringResource(R.string.music_karaoke_empty_message),
                    icon = Icons.Rounded.MusicNote,
                    modifier = Modifier.padding(top = TwinTheme.spacing.xl),
                )
            }
        } else {
            items(songs, key = { it.id }) { song ->
                SongRow(song = song, current = false, onClick = { onSing(song) }, onMore = { onSongMenu(song) })
            }
        }
    }
}
