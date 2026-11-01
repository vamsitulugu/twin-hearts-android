package com.twinhearts.app.feature.music.singtogether.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.music.domain.model.Song
import com.twinhearts.app.feature.music.ui.components.MusicSheet
import com.twinhearts.app.feature.music.ui.components.SongCover

/*
 * Why this file exists: the small cards of Sing Together: "choose a song", "X invited you to sing"
 * and "waiting for your partner". The room itself is in SingRoomScreen.kt.
 */

/** "Choose a song to sing together": one tap on a song sends the invite. */
@Composable
internal fun SingSongPickerSheet(
    songs: List<Song>,
    currentSongId: String?,
    partnerName: String,
    onPick: (Song) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = TwinTheme.colors
    MusicSheet(onDismiss) {
        Text(
            stringResource(R.string.music_together_pick_title),
            style = MaterialTheme.typography.titleLarge,
            color = colors.ink,
        )
        Text(
            stringResource(R.string.music_together_pick_subtitle, partnerName),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkMuted,
            modifier = Modifier.padding(top = TwinTheme.spacing.xs, bottom = TwinTheme.spacing.s),
        )
        if (songs.isEmpty()) {
            Text(stringResource(R.string.music_together_pick_none), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(songs, key = { it.id }) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(song) }
                            .padding(vertical = TwinTheme.spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
                    ) {
                        SongCover(url = song.coverUrl, size = 44.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(song.title, style = MaterialTheme.typography.titleSmall, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                if (song.id == currentSongId) stringResource(R.string.music_together_pick_playing) else song.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (song.id == currentSongId) colors.accent else colors.inkMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The beating heart of the web invite card (animates scale only, R-55). */
@Composable
private fun PulsingHeart() {
    val scale by rememberInfiniteTransition(label = "heart").animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(550), RepeatMode.Reverse),
        label = "heartScale",
    )
    Icon(
        Icons.Rounded.Favorite,
        contentDescription = null,
        tint = TwinTheme.colors.accent,
        modifier = Modifier.size(44.dp).graphicsLayer { scaleX = scale; scaleY = scale },
    )
}

/** The partner invited me. */
@Composable
internal fun SingInviteDialog(
    partnerName: String,
    songTitle: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val colors = TwinTheme.colors
    AlertDialog(
        onDismissRequest = onDecline,
        containerColor = colors.card,
        icon = { PulsingHeart() },
        title = {
            Text(
                stringResource(R.string.music_together_invited, partnerName),
                style = MaterialTheme.typography.titleLarge,
                color = colors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            if (songTitle.isNotBlank()) {
                Text(
                    stringResource(R.string.music_together_invited_song, songTitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = onAccept) { Text(stringResource(R.string.music_together_accept), color = colors.success) } },
        dismissButton = { TextButton(onClick = onDecline) { Text(stringResource(R.string.music_together_decline), color = colors.inkMuted) } },
    )
}

/** I invited my partner and wait. It closes by itself when they answer. */
@Composable
internal fun SingWaitingDialog(onCancel: () -> Unit) {
    val colors = TwinTheme.colors
    AlertDialog(
        onDismissRequest = {},
        containerColor = colors.card,
        icon = { PulsingHeart() },
        title = {
            Text(
                stringResource(R.string.music_together_waiting),
                style = MaterialTheme.typography.titleLarge,
                color = colors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Text(
                stringResource(R.string.music_together_waiting_note),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.music_together_cancel), color = colors.inkMuted) } },
    )
}
