package com.twinhearts.app.feature.music.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.music.domain.MusicFormat
import com.twinhearts.app.feature.music.domain.model.Playlist
import com.twinhearts.app.feature.music.domain.model.Song
import com.twinhearts.app.feature.music.domain.model.SongEdit
import com.twinhearts.app.feature.music.domain.model.SongVisibility
import java.text.DateFormat
import java.util.Date

/*
 * Why this file exists: every bottom sheet and dialog of the Music page (song menu, details,
 * edit, playlist pickers, sleep timer, confirm, text prompt). All are stateless except for the
 * text the person is typing.
 */

/** What the song menu can do. The screen decides what each one means. */
internal data class SongMenuActions(
    val onPlay: () -> Unit,
    val onPlayNext: () -> Unit,
    val onAddToQueue: () -> Unit,
    val onToggleLike: () -> Unit,
    val onAddToPlaylist: () -> Unit,
    val onSing: () -> Unit,
    val onLyrics: () -> Unit,
    val onDetails: () -> Unit,
    val onShare: () -> Unit,
    val onEdit: () -> Unit,
    val onDelete: () -> Unit,
    /** Only set when the menu was opened inside a playlist. */
    val onRemoveFromPlaylist: (() -> Unit)? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MusicSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = TwinTheme.colors.card,
        contentColor = TwinTheme.colors.ink,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = TwinTheme.spacing.l)
                .padding(bottom = TwinTheme.spacing.xl),
        ) { content() }
    }
}

@Composable
private fun SheetHeader(song: Song) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = TwinTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        SongCover(url = song.coverUrl, size = 56.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.titleMedium, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun SongMenuSheet(song: Song, mine: Boolean, actions: SongMenuActions, onDismiss: () -> Unit) {
    MusicSheet(onDismiss) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            SheetHeader(song)
            SheetAction(Icons.Rounded.PlayArrow, stringResource(R.string.music_action_play), actions.onPlay)
            SheetAction(Icons.Rounded.SkipNext, stringResource(R.string.music_action_play_next), actions.onPlayNext)
            SheetAction(Icons.Rounded.QueueMusic, stringResource(R.string.music_action_add_queue), actions.onAddToQueue)
            SheetAction(
                icon = if (song.liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                label = stringResource(if (song.liked) R.string.music_action_unlike else R.string.music_action_like),
                onClick = actions.onToggleLike,
                highlighted = song.liked,
            )
            SheetAction(Icons.Rounded.PlaylistAdd, stringResource(R.string.music_action_add_playlist), actions.onAddToPlaylist)
            SheetAction(Icons.Rounded.Mic, stringResource(R.string.music_action_sing), actions.onSing)
            SheetAction(Icons.Rounded.Lyrics, stringResource(R.string.music_action_lyrics), actions.onLyrics)
            SheetAction(Icons.Rounded.Info, stringResource(R.string.music_action_details), actions.onDetails)
            SheetAction(Icons.Rounded.Share, stringResource(R.string.music_action_share), actions.onShare)
            actions.onRemoveFromPlaylist?.let { remove ->
                SheetAction(Icons.Rounded.Delete, stringResource(R.string.music_action_remove_playlist), remove)
            }
            if (mine) {
                SheetAction(Icons.Rounded.Edit, stringResource(R.string.music_action_edit), actions.onEdit)
                SheetAction(Icons.Rounded.Delete, stringResource(R.string.music_action_delete), actions.onDelete, danger = true)
            }
        }
    }
}

@Composable
internal fun SongDetailsDialog(song: Song, addedBy: String, onDismiss: () -> Unit) {
    val colors = TwinTheme.colors
    val visibility = stringResource(
        when (song.visibility) {
            SongVisibility.OnlyMe -> R.string.music_visibility_only_me
            SongVisibility.Partner -> R.string.music_visibility_partner
            SongVisibility.Both -> R.string.music_visibility_both
        },
    )
    val added = song.createdAt?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date.from(it)) }
    val rows = listOf(
        R.string.music_detail_title to song.title,
        R.string.music_detail_artist to song.artist,
        R.string.music_detail_album to song.album,
        R.string.music_detail_genre to song.genre,
        R.string.music_detail_year to song.year,
        R.string.music_detail_duration to MusicFormat.clock(song.durationSec),
        R.string.music_detail_added_by to addedBy,
        R.string.music_detail_added to added.orEmpty(),
        R.string.music_detail_plays to song.myPlayCount.toString(),
        R.string.music_detail_visible_to to visibility,
    ).filter { it.second.isNotBlank() }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        title = { Text(song.title, style = MaterialTheme.typography.titleLarge, color = colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                rows.forEach { (label, value) ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(label), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted, modifier = Modifier.weight(0.4f))
                        Text(value, style = MaterialTheme.typography.bodyMedium, color = colors.ink, modifier = Modifier.weight(0.6f))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.music_close), color = colors.accent) } },
    )
}

@Composable
internal fun EditSongDialog(song: Song, onSave: (SongEdit) -> Unit, onDismiss: () -> Unit) {
    val colors = TwinTheme.colors
    var title by rememberSaveable { mutableStateOf(song.title) }
    var artist by rememberSaveable { mutableStateOf(if (song.artist == "Unknown Artist") "" else song.artist) }
    var album by rememberSaveable { mutableStateOf(song.album) }
    var genre by rememberSaveable { mutableStateOf(song.genre) }
    var visibility by rememberSaveable { mutableStateOf(song.visibility) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        title = { Text(stringResource(R.string.music_edit_title), style = MaterialTheme.typography.titleLarge, color = colors.ink) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                TwinTextField(title, { title = it }, stringResource(R.string.music_field_title), maxLength = 200)
                TwinTextField(artist, { artist = it }, stringResource(R.string.music_field_artist), maxLength = 200)
                TwinTextField(album, { album = it }, stringResource(R.string.music_field_album), maxLength = 200)
                TwinTextField(genre, { genre = it }, stringResource(R.string.music_field_genre), maxLength = 80)
                Text(stringResource(R.string.music_detail_visible_to), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
                Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    TwinFilterChip(stringResource(R.string.music_visibility_both), visibility == SongVisibility.Both, { visibility = SongVisibility.Both })
                    TwinFilterChip(stringResource(R.string.music_visibility_only_me), visibility == SongVisibility.OnlyMe, { visibility = SongVisibility.OnlyMe })
                    TwinFilterChip(stringResource(R.string.music_visibility_partner), visibility == SongVisibility.Partner, { visibility = SongVisibility.Partner })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(SongEdit(title, artist, album, genre, visibility)) }) {
                Text(stringResource(R.string.music_save), color = colors.accent)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.music_cancel), color = colors.inkMuted) } },
    )
}

/** One text field and two buttons: new playlist, rename playlist, rename recording. */
@Composable
internal fun TextPromptDialog(
    title: String,
    label: String,
    initial: String,
    confirmLabel: String,
    maxLength: Int,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = TwinTheme.colors
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        title = { Text(title, style = MaterialTheme.typography.titleLarge, color = colors.ink) },
        text = { TwinTextField(text, { text = it }, label, maxLength = maxLength) },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) {
                Text(confirmLabel, color = if (text.isNotBlank()) colors.accent else colors.inkFaint)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.music_cancel), color = colors.inkMuted) } },
    )
}

@Composable
internal fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = TwinTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        title = { Text(title, style = MaterialTheme.typography.titleLarge, color = colors.ink) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = colors.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.music_cancel), color = colors.inkMuted) } },
    )
}

@Composable
internal fun AddToPlaylistSheet(
    song: Song,
    playlists: List<Playlist>,
    available: Boolean,
    onPick: (Playlist) -> Unit,
    onNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = TwinTheme.colors
    MusicSheet(onDismiss) {
        SheetHeader(song)
        Text(stringResource(R.string.music_action_add_playlist), style = MaterialTheme.typography.titleLarge, color = colors.ink)
        if (!available) {
            Text(
                stringResource(R.string.music_playlists_need_update),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkMuted,
                modifier = Modifier.padding(vertical = TwinTheme.spacing.m),
            )
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                items(playlists, key = { it.id }) { playlist ->
                    val already = song.id in playlist.songIds
                    SheetAction(
                        icon = if (already) Icons.Rounded.Check else Icons.Rounded.QueueMusic,
                        label = playlist.name,
                        onClick = { onPick(playlist) },
                        highlighted = already,
                    )
                }
            }
            SheetAction(Icons.Rounded.Add, stringResource(R.string.music_new_playlist), onNew)
        }
    }
}

/** Tick songs, then "Add". Used inside a playlist to add songs from the library. */
@Composable
internal fun PickSongsSheet(
    title: String,
    songs: List<Song>,
    onDone: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = TwinTheme.colors
    val picked = remember { mutableStateListOf<String>() }
    MusicSheet(onDismiss) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = colors.ink, modifier = Modifier.padding(bottom = TwinTheme.spacing.s))
        if (songs.isEmpty()) {
            Text(stringResource(R.string.music_pick_none), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                items(songs, key = { it.id }) { song ->
                    val on = song.id in picked
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (on) picked.remove(song.id) else picked.add(song.id) }
                            .padding(vertical = TwinTheme.spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
                    ) {
                        SongCover(url = song.coverUrl, size = 44.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(song.title, style = MaterialTheme.typography.titleSmall, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(
                            if (on) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                            contentDescription = null,
                            tint = if (on) colors.accent else colors.inkMuted,
                        )
                    }
                }
            }
            GlassButton(
                text = stringResource(R.string.music_pick_add, picked.size),
                onClick = { onDone(picked.toList()) },
                enabled = picked.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TwinTheme.spacing.m),
            )
        }
    }
}

@Composable
internal fun SleepTimerSheet(sleepAtMs: Long?, onSet: (Int?) -> Unit, onDismiss: () -> Unit) {
    val colors = TwinTheme.colors
    MusicSheet(onDismiss) {
        Text(stringResource(R.string.music_sleep_title), style = MaterialTheme.typography.titleLarge, color = colors.ink)
        Text(
            stringResource(if (sleepAtMs == null) R.string.music_sleep_off else R.string.music_sleep_on),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkMuted,
            modifier = Modifier.padding(vertical = TwinTheme.spacing.s),
        )
        listOf(15, 30, 45, 60).forEach { minutes ->
            SheetAction(Icons.Rounded.Bedtime, stringResource(R.string.music_sleep_minutes, minutes), { onSet(minutes) })
        }
        if (sleepAtMs != null) {
            SheetAction(Icons.Rounded.Delete, stringResource(R.string.music_sleep_cancel), { onSet(null) }, danger = true)
        }
        GlassButton(
            text = stringResource(R.string.music_close),
            onClick = onDismiss,
            style = GlassButtonStyle.Ghost,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TwinTheme.spacing.m),
        )
    }
}
