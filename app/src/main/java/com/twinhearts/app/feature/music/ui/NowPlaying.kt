package com.twinhearts.app.feature.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.music.domain.MusicFormat
import com.twinhearts.app.feature.music.domain.model.Song
import com.twinhearts.app.feature.music.player.PlayerState
import com.twinhearts.app.feature.music.player.RepeatKind
import com.twinhearts.app.feature.music.ui.components.MusicSheet
import com.twinhearts.app.feature.music.ui.components.SongCover

/*
 * Why this file exists: the mini player (bottom of the Music page), the full "Now Playing"
 * screen, and the queue sheet. They only read [PlayerState] and call back; the player lives in
 * MusicPlayerController.
 */

internal data class PlayerActions(
    val onTogglePlay: () -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onShuffle: () -> Unit,
    val onRepeat: () -> Unit,
    val onLike: (Song) -> Unit,
    val onLyrics: (Song) -> Unit,
    val onQueue: () -> Unit,
    val onSleep: () -> Unit,
    val onClose: () -> Unit,
)

@Composable
internal fun MiniPlayer(state: PlayerState, onOpen: () -> Unit, onTogglePlay: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier) {
    val song = state.current ?: return
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.card
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.s)
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.line, shape)
            .clickable(onClick = onOpen),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            SongCover(url = song.coverUrl, size = 44.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.titleSmall, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (state.isBuffering && !state.isPlaying) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = colors.accent)
            }
            IconButton(onClick = onTogglePlay) {
                Icon(
                    if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(if (state.isPlaying) R.string.music_pause else R.string.music_action_play),
                    tint = colors.ink,
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Rounded.SkipNext, contentDescription = stringResource(R.string.music_next), tint = colors.ink)
            }
        }
        val fraction = if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = colors.accent,
            trackColor = colors.lineSoft,
        )
    }
}

@Composable
internal fun NowPlayingScreen(state: PlayerState, actions: PlayerActions, modifier: Modifier = Modifier) {
    val song = state.current ?: return
    val colors = TwinTheme.colors
    TwinBackground {
        Column(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = TwinTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = actions.onClose) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.core_back), tint = colors.ink)
                }
                Text(
                    stringResource(R.string.music_now_playing),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.inkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                Box(Modifier.size(48.dp))
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                SongCover(url = song.coverUrl, size = 280.dp)
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(song.title, style = MaterialTheme.typography.headlineSmall, color = colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(song.artist, style = MaterialTheme.typography.bodyLarge, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { actions.onLike(song) }) {
                    Icon(
                        if (song.liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = stringResource(if (song.liked) R.string.music_action_unlike else R.string.music_action_like),
                        tint = if (song.liked) colors.accent else colors.ink,
                    )
                }
            }
            SeekBar(state = state, onSeek = actions.onSeek)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = actions.onShuffle) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = stringResource(R.string.music_quick_shuffle), tint = if (state.shuffle) colors.accent else colors.inkMuted)
                }
                IconButton(onClick = actions.onPrevious, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = stringResource(R.string.music_previous), tint = colors.ink, modifier = Modifier.size(36.dp))
                }
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(TwinTheme.shapes.circle)
                        .background(colors.primaryBrush)
                        .clickable(onClick = actions.onTogglePlay),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isBuffering && !state.isPlaying) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp, color = colors.ink)
                    } else {
                        Icon(
                            if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(if (state.isPlaying) R.string.music_pause else R.string.music_action_play),
                            tint = colors.ink,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
                IconButton(onClick = actions.onNext, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = stringResource(R.string.music_next), tint = colors.ink, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = actions.onRepeat) {
                    Icon(
                        if (state.repeat == RepeatKind.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = stringResource(R.string.music_repeat),
                        tint = if (state.repeat == RepeatKind.Off) colors.inkMuted else colors.accent,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = TwinTheme.spacing.l),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                IconButton(onClick = { actions.onLyrics(song) }) {
                    Icon(Icons.Rounded.Lyrics, contentDescription = stringResource(R.string.music_action_lyrics), tint = colors.ink)
                }
                IconButton(onClick = actions.onQueue) {
                    Icon(Icons.Rounded.QueueMusic, contentDescription = stringResource(R.string.music_queue_title), tint = colors.ink)
                }
                IconButton(onClick = actions.onSleep) {
                    Icon(Icons.Rounded.Bedtime, contentDescription = stringResource(R.string.music_sleep_title), tint = if (state.sleepAtMs != null) colors.accent else colors.ink)
                }
            }
        }
    }
}

@Composable
private fun SeekBar(state: PlayerState, onSeek: (Long) -> Unit) {
    val colors = TwinTheme.colors
    // While the finger is down we show the finger's position, not the playing position.
    var dragging by remember { mutableStateOf<Float?>(null) }
    val duration = state.durationMs.coerceAtLeast(1L)
    val shown = dragging ?: (state.positionMs.toFloat() / duration).coerceIn(0f, 1f)
    Slider(
        value = shown,
        onValueChange = { dragging = it },
        onValueChangeFinished = {
            dragging?.let { onSeek((it * duration).toLong()) }
            dragging = null
        },
        colors = SliderDefaults.colors(
            thumbColor = colors.accent,
            activeTrackColor = colors.accent,
            inactiveTrackColor = colors.lineSoft,
        ),
    )
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(MusicFormat.clockMs((shown * duration).toLong()), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, modifier = Modifier.weight(1f))
        Text(MusicFormat.clockMs(state.durationMs), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
    }
}

@Composable
internal fun QueueSheet(state: PlayerState, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val colors = TwinTheme.colors
    MusicSheet(onDismiss) {
        Text(stringResource(R.string.music_queue_title), style = MaterialTheme.typography.titleLarge, color = colors.ink, modifier = Modifier.padding(bottom = TwinTheme.spacing.s))
        LazyColumn(modifier = Modifier.height(380.dp)) {
            itemsIndexed(state.queue, key = { index, song -> "$index-${song.id}" }) { index, song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(index) }
                        .padding(vertical = TwinTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
                ) {
                    SongCover(url = song.coverUrl, size = 44.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        val isCurrent = song.id == state.current?.id
                        Text(song.title, style = MaterialTheme.typography.titleSmall, color = if (isCurrent) colors.accent else colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artist, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
