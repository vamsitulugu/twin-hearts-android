package com.twinhearts.app.feature.music.ui

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.common.rememberReduceMotion
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.music.domain.LrcParser
import com.twinhearts.app.feature.music.domain.model.Lyrics
import com.twinhearts.app.feature.music.domain.model.Song
import com.twinhearts.app.feature.music.player.PlayerState

/*
 * Why this file exists: the lyrics page (read, paste, import a .lrc file) and the scrolling
 * lyrics list that the karaoke "Sing" page reuses.
 */

/** Timed lyrics highlight the current line and scroll with the song; plain lyrics just scroll. */
@Composable
internal fun LyricsList(
    lyrics: Lyrics,
    positionMs: Long,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    durationMs: Long = 0L,
) {
    val colors = TwinTheme.colors
    val active = if (lyrics.synced) LrcParser.activeIndex(lyrics.lines, positionMs) else -1
    val reduceMotion = rememberReduceMotion()

    // Follow the song: timed lyrics follow the sung line; plain lyrics follow how far into the song we are.
    // If the person scrolls by hand, the page leaves them alone for a few seconds, then follows again.
    var autoScrolling by remember { androidx.compose.runtime.mutableStateOf(false) }
    var pausedUntil by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    LaunchedEffect(state.isScrollInProgress) {
        if (state.isScrollInProgress && !autoScrolling) pausedUntil = System.currentTimeMillis() + 4_000L
    }
    val target = when {
        lyrics.lines.isEmpty() -> -1
        lyrics.synced -> active
        durationMs > 0L && positionMs > 0L ->
            ((positionMs.toFloat() / durationMs) * lyrics.lines.size).toInt().coerceIn(0, lyrics.lines.lastIndex)
        else -> -1
    }
    LaunchedEffect(target) {
        if (target < 0 || System.currentTimeMillis() < pausedUntil) return@LaunchedEffect
        autoScrolling = true
        try {
            // Keep the current line about a third of the way down, so the next lines are visible below it.
            val offset = -(state.layoutInfo.viewportSize.height / 3)
            if (reduceMotion) state.scrollToItem(target, offset) else state.animateScrollToItem(target, offset)
        } finally {
            autoScrolling = false
        }
    }
    LazyColumn(
        state = state,
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = TwinTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        itemsIndexed(lyrics.lines) { index, line ->
            val isActive = index == active
            Text(
                text = line.text.ifEmpty { " " },
                style = if (isActive) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                color = when {
                    !lyrics.synced -> colors.ink
                    isActive -> colors.ink
                    else -> colors.inkFaint
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun LyricsPage(
    song: Song?,
    lyrics: UiState<Lyrics>,
    saving: Boolean,
    player: PlayerState,
    onRetry: () -> Unit,
    onSave: (String) -> Unit,
    onPlay: () -> Unit,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var pasteOpen by rememberSaveable { mutableStateOf(false) }
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) readLyricsFile(context, uri)?.let(onSave)
    }
    val colors = TwinTheme.colors
    val playingThis = song != null && player.current?.id == song.id

    Column(modifier = modifier.padding(horizontal = TwinTheme.spacing.screenEdge)) {
        if (song != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = TwinTheme.spacing.s)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(song.title, style = MaterialTheme.typography.titleMedium, color = colors.ink, maxLines = 1)
                    Text(song.artist, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 1)
                }
                IconButton(onClick = if (playingThis) onTogglePlay else onPlay) {
                    Icon(
                        if (playingThis && player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(if (playingThis && player.isPlaying) R.string.music_pause else R.string.music_action_play),
                        tint = colors.ink,
                    )
                }
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            when (lyrics) {
                UiState.Loading -> LoadingSkeleton(lines = 6)
                is UiState.Error -> ErrorState(lyrics.error.asText(), onRetry)
                UiState.Empty -> EmptyState(
                    title = stringResource(R.string.music_lyrics_empty_title),
                    message = stringResource(R.string.music_lyrics_empty_message),
                    icon = Icons.Rounded.Lyrics,
                )
                is UiState.Content -> LyricsList(
                    lyrics = lyrics.data,
                    positionMs = if (playingThis) player.positionMs else 0L,
                    durationMs = if (playingThis) player.durationMs else 0L,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = TwinTheme.spacing.m),
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            GlassButton(
                text = stringResource(R.string.music_lyrics_paste),
                onClick = { pasteOpen = true },
                style = GlassButtonStyle.Secondary,
                loading = saving,
                modifier = Modifier.weight(1f),
            )
            GlassButton(
                text = stringResource(R.string.music_lyrics_import),
                onClick = { importLauncher.launch(arrayOf("text/*", "application/octet-stream")) },
                style = GlassButtonStyle.Ghost,
                modifier = Modifier.weight(1f),
            )
        }
    }
    if (pasteOpen && song != null) {
        var text by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pasteOpen = false },
            containerColor = colors.card,
            title = { Text(stringResource(R.string.music_lyrics_paste), style = MaterialTheme.typography.titleLarge, color = colors.ink) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(MAX_LYRICS_CHARS) },
                    placeholder = { Text(stringResource(R.string.music_lyrics_paste_hint)) },
                    minLines = 6,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = { onSave(text); pasteOpen = false }) { Text(stringResource(R.string.music_save), color = colors.accent) }
            },
            dismissButton = { TextButton(onClick = { pasteOpen = false }) { Text(stringResource(R.string.music_cancel), color = colors.inkMuted) } },
        )
    }
}

private const val MAX_LYRICS_CHARS = 200 * 1024

/** Reads a small text/.lrc file. Returns null when it cannot be read or is far too big for lyrics. */
private fun readLyricsFile(context: Context, uri: Uri): String? = try {
    context.contentResolver.openInputStream(uri)?.use { input ->
        val buffer = ByteArray(MAX_LYRICS_CHARS + 1)
        var filled = 0
        while (filled < buffer.size) {
            val n = input.read(buffer, filled, buffer.size - filled)
            if (n < 0) break
            filled += n
        }
        if (filled > MAX_LYRICS_CHARS) null else String(buffer, 0, filled, Charsets.UTF_8)
    }
} catch (e: java.io.IOException) {
    null
}
