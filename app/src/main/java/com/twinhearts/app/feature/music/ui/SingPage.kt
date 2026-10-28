package com.twinhearts.app.feature.music.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.twinhearts.app.R
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.music.domain.MusicFormat
import com.twinhearts.app.feature.music.domain.model.Lyrics
import com.twinhearts.app.feature.music.domain.model.Song
import com.twinhearts.app.feature.music.player.PlayerState
import kotlinx.coroutines.delay

/**
 * Why this file exists: karaoke. The song plays, its timed lyrics scroll, and the microphone
 * records the person singing. When they finish, the take is saved to My Recordings.
 */
@Composable
internal fun SingPage(
    song: Song?,
    lyrics: UiState<Lyrics>,
    player: PlayerState,
    singing: Boolean,
    onRetry: () -> Unit,
    onStart: () -> Unit,
    onFinish: () -> Unit,
    onAbort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    // Leaving the page mid-take throws the take away instead of leaving the microphone on.
    val abort by rememberUpdatedState(onAbort)
    val stillSinging by rememberUpdatedState(singing)
    DisposableEffect(Unit) { onDispose { if (stillSinging) abort() } }

    var seconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(singing) {
        seconds = 0
        while (singing) {
            delay(1000)
            seconds++
        }
    }

    Column(modifier = modifier.padding(horizontal = TwinTheme.spacing.screenEdge)) {
        if (song != null) {
            Text(song.title, style = MaterialTheme.typography.titleLarge, color = colors.ink, maxLines = 1, modifier = Modifier.padding(top = TwinTheme.spacing.s))
            Text(song.artist, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted, maxLines = 1)
        }
        Box(modifier = Modifier.weight(1f)) {
            when (lyrics) {
                UiState.Loading -> LoadingSkeleton(lines = 6)
                is UiState.Error -> ErrorState(lyrics.error.asText(), onRetry)
                UiState.Empty -> EmptyState(
                    title = stringResource(R.string.music_sing_no_lyrics_title),
                    message = stringResource(R.string.music_sing_no_lyrics_message),
                    icon = Icons.Rounded.Mic,
                )
                is UiState.Content -> LyricsList(
                    lyrics = lyrics.data,
                    positionMs = if (player.current?.id == song?.id) player.positionMs else 0L,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = TwinTheme.spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        ) {
            if (singing) {
                Text(
                    stringResource(R.string.music_sing_recording, MusicFormat.clock(seconds)),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.accent,
                )
                GlassButton(
                    text = stringResource(R.string.music_sing_finish),
                    onClick = onFinish,
                    leadingIcon = Icons.Rounded.Stop,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    stringResource(R.string.music_sing_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted,
                    textAlign = TextAlign.Center,
                )
                GlassButton(
                    text = stringResource(R.string.music_sing_start),
                    onClick = onStart,
                    enabled = song != null,
                    leadingIcon = Icons.Rounded.Mic,
                    style = GlassButtonStyle.Primary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
