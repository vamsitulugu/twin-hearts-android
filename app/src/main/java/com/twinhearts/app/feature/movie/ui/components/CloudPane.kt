package com.twinhearts.app.feature.movie.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.SerifFamily
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.movie.data.upload.UploadProblem
import com.twinhearts.app.feature.movie.data.upload.UploadState
import com.twinhearts.app.feature.movie.domain.formatBytes
import com.twinhearts.app.feature.movie.domain.formatRuntime
import com.twinhearts.app.feature.movie.domain.model.CloudLibrary
import com.twinhearts.app.feature.movie.domain.model.SharedMovie
import com.twinhearts.app.feature.movie.ui.LocalVideo
import com.twinhearts.app.feature.movie.ui.readLocalVideo
import kotlinx.coroutines.launch

/**
 * Why this file exists: the "Cloud Storage" half of the Watch tab. It shows the shared storage bar,
 * uploads a movie once for both phones, lists the shared movies and deletes them (all like the web page).
 */
@Composable
fun CloudPane(
    cloud: UiState<CloudLibrary>,
    upload: UploadState,
    onRetry: () -> Unit,
    onPick: (SharedMovie) -> Unit,
    onUpload: (LocalVideo) -> Unit,
    onCancelUpload: () -> Unit,
    onDismissUpload: () -> Unit,
    onDelete: (SharedMovie) -> Unit,
) {
    val colors = TwinTheme.colors
    val running = upload is UploadState.Running
    var pendingDelete by remember { mutableStateOf<SharedMovie?>(null) }
    KeepAwakeWhile(running)

    Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.l)) {
        if (cloud is UiState.Content) UsageCard(cloud.data)
        Text(stringResource(R.string.movie_cloud_hint), color = colors.inkMuted, style = MaterialTheme.typography.bodySmall)

        when (upload) {
            is UploadState.Running -> ProgressCard(upload, onCancelUpload)
            is UploadState.Failed -> FailureNote(upload, onDismissUpload)
            else -> Unit
        }
        if (!running) UploadButton(onUpload = { onDismissUpload(); onUpload(it) })

        Text(
            stringResource(R.string.movie_cloud_your_movies).uppercase(), color = colors.inkMuted,
            style = MaterialTheme.typography.labelSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        )
        when (cloud) {
            UiState.Loading -> LoadingSkeleton()
            UiState.Empty -> EmptyState(
                stringResource(R.string.movie_cloud_empty_title),
                message = stringResource(R.string.movie_cloud_empty_message), icon = Icons.Rounded.Movie,
            )
            is UiState.Error -> ErrorState(message = cloud.error.asText(), onRetry = onRetry)
            is UiState.Content -> {
                // While uploading, the half-sent file is on the server list too; hide it like the web page does.
                cloud.data.movies.filter { it.ready || !running }.forEach { movie ->
                    MovieRow(movie, onPick = { onPick(movie) }, onDelete = { pendingDelete = movie })
                }
            }
        }
    }

    pendingDelete?.let { movie ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = colors.card,
            text = { Text(stringResource(R.string.movie_cloud_delete_confirm, movie.title), color = colors.ink, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton({ pendingDelete = null; onDelete(movie) }) { Text(stringResource(R.string.movie_history_delete), color = colors.error) }
            },
            dismissButton = {
                TextButton({ pendingDelete = null }) { Text(stringResource(R.string.movie_cancel), color = colors.inkMuted) }
            },
        )
    }
}

@Composable
private fun UsageCard(lib: CloudLibrary) {
    val colors = TwinTheme.colors
    val pct = if (lib.limitBytes > 0) (lib.usedBytes * PERCENT / lib.limitBytes).toInt().coerceIn(0, PERCENT.toInt()) else 0
    GlassCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.movie_cloud_usage), color = colors.inkMuted, style = MaterialTheme.typography.bodySmall)
            Text("$pct%", color = colors.ink, style = MaterialTheme.typography.bodySmall)
        }
        Bar(pct / PERCENT.toFloat())
        if (lib.limitBytes > 0) {
            Text(
                stringResource(R.string.movie_cloud_used_of, formatBytes(lib.usedBytes), formatBytes(lib.limitBytes)),
                color = colors.inkMuted, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = TwinTheme.spacing.s),
            )
        }
    }
}

@Composable
private fun Bar(fraction: Float) {
    Box(
        Modifier.padding(top = TwinTheme.spacing.s).fillMaxWidth().height(6.dp)
            .clip(RoundedCornerShape(3.dp)).background(TwinTheme.colors.glass),
    ) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().background(TwinTheme.colors.primaryBrush))
    }
}

@Composable
private fun ProgressCard(state: UploadState.Running, onCancel: () -> Unit) {
    val colors = TwinTheme.colors
    val percent = if (state.totalBytes > 0) (state.sentBytes * PERCENT / state.totalBytes).toInt().coerceIn(0, PERCENT.toInt()) else 0
    val label = when {
        state.finishing -> stringResource(R.string.movie_upload_finishing)
        state.sentBytes == 0L -> stringResource(R.string.movie_upload_starting)
        else -> stringResource(R.string.movie_upload_progress, percent, formatBytes(state.sentBytes), formatBytes(state.totalBytes)) +
            if (state.bytesPerSec > 0) " • " + stringResource(R.string.movie_upload_speed, state.bytesPerSec / BYTES_PER_MB) else ""
    }
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CloudUpload, null, tint = colors.accent)
            Text(label, color = colors.ink, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = TwinTheme.spacing.s))
        }
        Bar(percent / PERCENT.toFloat())
        Text(
            stringResource(R.string.movie_upload_keep_open), color = colors.inkMuted, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = TwinTheme.spacing.s),
        )
        if (!state.finishing) {
            GlassButton(
                stringResource(R.string.movie_upload_cancel), onCancel,
                Modifier.fillMaxWidth().padding(top = TwinTheme.spacing.m), style = GlassButtonStyle.Secondary,
            )
        }
    }
}

@Composable
private fun FailureNote(state: UploadState.Failed, onDismiss: () -> Unit) {
    val colors = TwinTheme.colors
    val text = when (state.problem) {
        UploadProblem.BadType -> stringResource(R.string.movie_toast_bad_type)
        UploadProblem.Network -> stringResource(R.string.movie_upload_failed_network)
        UploadProblem.Server -> state.message ?: stringResource(R.string.movie_upload_failed)
    }
    GlassCard(Modifier.fillMaxWidth(), onClick = onDismiss) {
        Text(text, color = colors.error, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun UploadButton(onUpload: (LocalVideo) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val video = readLocalVideo(context, uri)
            if (video == null) Toast.makeText(context, R.string.movie_toast_bad_video, Toast.LENGTH_SHORT).show() else onUpload(video)
        }
    }
    GlassButton(
        stringResource(R.string.movie_cloud_upload), { picker.launch(UPLOAD_TYPES) },
        Modifier.fillMaxWidth(), leadingIcon = Icons.Rounded.CloudUpload,
    )
}

@Composable
private fun MovieRow(movie: SharedMovie, onPick: () -> Unit, onDelete: () -> Unit) {
    val colors = TwinTheme.colors
    GlassCard(Modifier.fillMaxWidth(), onClick = if (movie.ready) onPick else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    movie.title, color = colors.ink, style = MaterialTheme.typography.titleMedium, fontFamily = SerifFamily,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (movie.ready) "${formatRuntime(movie.durationSec)} • ${formatBytes(movie.sizeBytes)}" else stringResource(R.string.movie_cloud_incomplete),
                    color = colors.inkMuted, style = MaterialTheme.typography.bodySmall,
                )
            }
            if (movie.ready) {
                IconButton(onPick) { Icon(Icons.Rounded.PlayArrow, stringResource(R.string.movie_cloud_watch), tint = colors.accent) }
            }
            IconButton(onDelete) { Icon(Icons.Rounded.Delete, stringResource(R.string.movie_cloud_delete), tint = colors.inkMuted) }
        }
    }
}

/** Keeps the screen on while a big upload runs, so the phone does not sleep and slow it down. */
@Composable
private fun KeepAwakeWhile(on: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, on) {
        view.keepScreenOn = on
        onDispose { view.keepScreenOn = false }
    }
}

private val UPLOAD_TYPES = arrayOf("video/mp4", "video/webm", "video/x-m4v", "video/quicktime")
private const val PERCENT = 100L
private const val BYTES_PER_MB = 1_000_000.0
