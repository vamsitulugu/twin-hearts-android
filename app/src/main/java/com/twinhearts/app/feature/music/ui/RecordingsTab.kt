package com.twinhearts.app.feature.music.ui

import androidx.compose.material.icons.rounded.Favorite
import com.twinhearts.app.feature.music.ui.components.musicGlass
import com.twinhearts.app.feature.music.ui.components.MusicGlassCircle
import com.twinhearts.app.feature.music.ui.components.MusicStyle
import com.twinhearts.app.feature.music.ui.components.MusicPill
import com.twinhearts.app.core.common.rememberCameraGate
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.twinhearts.app.core.design.TwinTextField
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.twinhearts.app.R
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.music.domain.MusicFormat
import com.twinhearts.app.feature.music.domain.model.Recording
import com.twinhearts.app.feature.music.domain.model.RecordingFilter
import com.twinhearts.app.feature.music.domain.model.RecordingKind
import com.twinhearts.app.feature.music.domain.model.RecordingMedia
import com.twinhearts.app.feature.music.domain.model.RecordingOwner
import com.twinhearts.app.feature.music.ui.components.TextPromptDialog
import com.twinhearts.app.feature.music.ui.components.ConfirmDialog
import kotlinx.coroutines.delay
import java.io.File
import java.text.DateFormat
import java.util.Date

/*
 * Why this file exists: the "My Recordings" tab (Audio / Video / Favorites, Everyone / Mine /
 * partner's), plus the pieces to record: microphone permission, the recording dialogs and the
 * video player. Follows public/music/recordings.js in the web app.
 */

internal data class RecordingsActions(
    val onRetry: () -> Unit,
    val onRecordAudio: () -> Unit,
    val onRecordVideo: () -> Unit,
    val onFilter: (RecordingFilter) -> Unit,
    val onOwner: (RecordingOwner) -> Unit,
    val onPlay: (Recording) -> Unit,
    val onFavorite: (Recording) -> Unit,
    val onRename: (Recording, String) -> Unit,
    val onDelete: (Recording) -> Unit,
    val onQuery: (String) -> Unit = {},
)

@Composable
internal fun RecordingsTab(
    state: RecordingsUiState,
    partnerName: String,
    currentSongId: String?,
    actions: RecordingsActions,
    modifier: Modifier = Modifier,
    bottomPadding: androidx.compose.ui.unit.Dp = 24.dp,
) {
    val colors = TwinTheme.colors
    var renaming by remember { mutableStateOf<Recording?>(null) }
    var deleting by remember { mutableStateOf<Recording?>(null) }
    val role = state.who?.myRole.orEmpty()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = TwinTheme.spacing.screenEdge, end = TwinTheme.spacing.screenEdge, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        item(key = "filters") {
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.padding(top = TwinTheme.spacing.xs)) {
                MusicPill(stringResource(R.string.music_rec_audio), state.filter == RecordingFilter.Audio, { actions.onFilter(RecordingFilter.Audio) }, Modifier.weight(1f), Icons.Rounded.Mic)
                MusicPill(stringResource(R.string.music_rec_video), state.filter == RecordingFilter.Video, { actions.onFilter(RecordingFilter.Video) }, Modifier.weight(1f), Icons.Rounded.Videocam)
                MusicPill(stringResource(R.string.music_rec_favorites), state.filter == RecordingFilter.Favorites, { actions.onFilter(RecordingFilter.Favorites) }, Modifier.weight(1f), Icons.Rounded.Favorite)
            }
        }
        item(key = "record") {
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.padding(top = TwinTheme.spacing.xs)) {
                MusicPill(stringResource(R.string.music_rec_record_audio), true, actions.onRecordAudio, Modifier.weight(1f), Icons.Rounded.Mic, height = 56.dp)
                MusicPill(stringResource(R.string.music_rec_record_video), false, actions.onRecordVideo, Modifier.weight(1f), Icons.Rounded.Videocam, height = 56.dp)
            }
        }
        item(key = "owners") {
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.padding(bottom = TwinTheme.spacing.xs)) {
                MusicPill(stringResource(R.string.music_rec_everyone), state.owner == RecordingOwner.Everyone, { actions.onOwner(RecordingOwner.Everyone) }, height = 40.dp)
                MusicPill(stringResource(R.string.music_rec_mine), state.owner == RecordingOwner.Mine, { actions.onOwner(RecordingOwner.Mine) }, height = 40.dp)
                MusicPill(stringResource(R.string.music_rec_partners, partnerName), state.owner == RecordingOwner.Partner, { actions.onOwner(RecordingOwner.Partner) }, height = 40.dp)
            }
        }
        if (state.all is UiState.Content) {
            item(key = "search") {
                TwinTextField(
                    value = state.query,
                    onValueChange = actions.onQuery,
                    label = stringResource(R.string.music_rec_search_label),
                    placeholder = stringResource(R.string.music_rec_search_hint),
                    leadingIcon = Icons.Rounded.Search,
                    imeAction = ImeAction.Search,
                )
            }
        }
        when (val all = state.all) {
            UiState.Loading -> item(key = "loading") { LoadingSkeleton(lines = 4, modifier = Modifier.padding(top = TwinTheme.spacing.l)) }
            is UiState.Error -> item(key = "error") { ErrorState(all.error.asText(), actions.onRetry) }
            else -> {
                val rows = state.visible
                if (rows.isEmpty()) {
                    item(key = "empty") {
                        EmptyState(
                            title = stringResource(R.string.music_rec_empty_title),
                            message = stringResource(R.string.music_rec_empty_message),
                            icon = Icons.Rounded.Mic,
                            modifier = Modifier.padding(top = TwinTheme.spacing.xl),
                        )
                    }
                } else {
                    items(rows, key = { it.id }) { rec ->
                        RecordingRow(
                            rec = rec,
                            mine = rec.userRole == role,
                            ownerName = if (rec.userRole == role) stringResource(R.string.music_rec_you) else partnerName,
                            playing = state.playingId == rec.id && currentSongId?.endsWith(rec.id) == true,
                            onPlay = { actions.onPlay(rec) },
                            onFavorite = { actions.onFavorite(rec) },
                            onRename = { renaming = rec },
                            onDelete = { deleting = rec },
                        )
                    }
                }
            }
        }
    }
    renaming?.let { rec ->
        TextPromptDialog(
            title = stringResource(R.string.music_rec_rename),
            label = stringResource(R.string.music_field_title),
            initial = rec.title,
            confirmLabel = stringResource(R.string.music_save),
            maxLength = 200,
            onConfirm = { actions.onRename(rec, it); renaming = null },
            onDismiss = { renaming = null },
        )
    }
    deleting?.let { rec ->
        ConfirmDialog(
            title = stringResource(R.string.music_rec_delete_title),
            message = stringResource(R.string.music_rec_delete_message, rec.title),
            confirmLabel = stringResource(R.string.music_action_delete),
            onConfirm = { actions.onDelete(rec); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun RecordingRow(
    rec: Recording,
    mine: Boolean,
    ownerName: String,
    playing: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = TwinTheme.colors
    var menu by remember { mutableStateOf(false) }
    val date = rec.createdAt?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date.from(it)) }
    val meta = listOfNotNull(ownerName, MusicFormat.clock(rec.durationSec), date).joinToString(" · ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .musicGlass(TwinTheme.shapes.field)
            .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .musicGlass(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = when {
                    rec.media == RecordingMedia.Video -> Icons.Rounded.Videocam
                    rec.kind == RecordingKind.Karaoke -> Icons.Rounded.MusicNote
                    else -> Icons.Rounded.Mic
                },
                contentDescription = null,
                tint = MusicStyle.Accent,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(rec.title, style = MaterialTheme.typography.titleMedium, color = if (playing) MusicStyle.Accent else colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(meta, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onFavorite) {
            Icon(
                if (rec.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = stringResource(R.string.music_rec_favorites),
                tint = if (rec.favorite) MusicStyle.Accent else colors.inkMuted,
            )
        }
        MusicGlassCircle(size = 40.dp, onClick = onPlay, wine = true) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.music_action_play), tint = Color.White)
        }
        if (mine) {
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.music_more), tint = colors.inkMuted)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, modifier = Modifier.background(colors.card)) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.music_rec_rename), color = colors.ink) },
                        leadingIcon = { Icon(Icons.Rounded.Edit, null, tint = colors.ink) },
                        onClick = { menu = false; onRename() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.music_action_delete), color = colors.error) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = colors.error) },
                        onClick = { menu = false; onDelete() },
                    )
                }
            }
        }
    }
}

// ── microphone permission ──

/** Asks for the microphone with a short reason first, then runs [ask]'s callback once allowed. */
internal class MicAccess(val ask: (onGranted: () -> Unit) -> Unit)

@Composable
internal fun rememberMicAccess(): MicAccess {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    var rationale by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pending?.invoke() else Toast.makeText(context, R.string.music_mic_denied, Toast.LENGTH_LONG).show()
        pending = null
    }
    if (rationale) {
        val colors = TwinTheme.colors
        AlertDialog(
            onDismissRequest = { rationale = false; pending = null },
            containerColor = colors.card,
            title = { Text(stringResource(R.string.music_mic_title), style = MaterialTheme.typography.titleLarge, color = colors.ink) },
            text = { Text(stringResource(R.string.music_mic_message), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted) },
            confirmButton = {
                TextButton(onClick = { rationale = false; launcher.launch(Manifest.permission.RECORD_AUDIO) }) {
                    Text(stringResource(R.string.music_mic_allow), color = colors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { rationale = false; pending = null }) { Text(stringResource(R.string.music_cancel), color = colors.inkMuted) }
            },
        )
    }
    return remember {
        MicAccess { onGranted ->
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                onGranted()
            } else {
                pending = onGranted
                rationale = true
            }
        }
    }
}

// ── recording flow ──

/** Where a recording is: not started, recording, or finished and waiting for a name. */
internal sealed interface Take {
    data object Idle : Take
    data class Recording(val karaokeSongId: String?) : Take
    data class Naming(
        val file: File,
        val seconds: Int,
        val media: RecordingMedia,
        val kind: RecordingKind,
        val songId: String?,
        val defaultTitle: String,
    ) : Take
}

/** The "Recording…" dialog with a running timer, and the "Save recording" name dialog. */
@Composable
internal fun TakeDialogs(
    take: Take,
    onStop: () -> Unit,
    onCancel: () -> Unit,
    onSave: (Take.Naming, String) -> Unit,
    onDiscard: (Take.Naming) -> Unit,
) {
    val colors = TwinTheme.colors
    when (take) {
        Take.Idle -> Unit
        is Take.Recording -> {
            // Karaoke is recorded on its own page, which shows its own timer and buttons.
            if (take.karaokeSongId != null) return
            var seconds by remember { mutableIntStateOf(0) }
            LaunchedEffect(Unit) {
                while (true) {
                    delay(1000)
                    seconds++
                }
            }
            AlertDialog(
                onDismissRequest = {},
                containerColor = colors.card,
                title = { Text(stringResource(R.string.music_rec_recording), style = MaterialTheme.typography.titleLarge, color = colors.ink) },
                text = {
                    Text(MusicFormat.clock(seconds), style = MaterialTheme.typography.displaySmall, color = colors.accent, modifier = Modifier.fillMaxWidth())
                },
                confirmButton = { TextButton(onClick = onStop) { Text(stringResource(R.string.music_rec_stop), color = colors.accent) } },
                dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.music_cancel), color = colors.inkMuted) } },
            )
        }
        is Take.Naming -> TextPromptDialog(
            title = stringResource(R.string.music_rec_save_title),
            label = stringResource(R.string.music_field_title),
            initial = take.defaultTitle,
            confirmLabel = stringResource(R.string.music_save),
            maxLength = 200,
            onConfirm = { onSave(take, it) },
            onDismiss = { onDiscard(take) },
        )
    }
}

/** Opens the phone's camera app to film a short video message; reports the saved file. */
@Composable
internal fun rememberVideoCapture(onCaptured: (File, Int) -> Unit): () -> Unit {
    val context = LocalContext.current
    var target by remember { mutableStateOf<File?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { ok ->
        val file = target
        target = null
        if (ok && file != null && file.length() > 0L) {
            onCaptured(file, videoSeconds(context, file))
        } else {
            file?.delete()
        }
    }
    val withCamera = rememberCameraGate()
    return {
        val dir = File(context.cacheDir, "music_capture").apply { mkdirs() }
        val file = File(dir, "video_${System.currentTimeMillis()}.mp4")
        target = file
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.chatfiles", file)
        withCamera { launcher.launch(uri) }
    }
}

private fun videoSeconds(context: Context, file: File): Int {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, Uri.fromFile(file))
        ((retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L) / 1000L).toInt().coerceAtLeast(1)
    } catch (e: RuntimeException) {
        1
    } finally {
        retriever.release()
    }
}

/** Full-screen video with Media3's standard controls. Closes with back or the Close button. */
@OptIn(UnstableApi::class)
@Composable
internal fun VideoDialog(url: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val exo = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(exo) { onDispose { exo.release() } }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { ctx -> PlayerView(ctx).apply { player = exo } },
                modifier = Modifier.fillMaxSize(),
            )
            GlassButton(
                text = stringResource(R.string.music_close),
                onClick = onDismiss,
                style = GlassButtonStyle.Secondary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(TwinTheme.spacing.l),
            )
        }
    }
}
