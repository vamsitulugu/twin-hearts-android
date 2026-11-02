package com.twinhearts.app.feature.profile.highlights.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.profile.highlights.domain.ClipKind
import com.twinhearts.app.feature.profile.highlights.domain.Highlight
import com.twinhearts.app.feature.profile.highlights.domain.HighlightCategory
import com.twinhearts.app.feature.profile.highlights.domain.HighlightClip
import com.twinhearts.app.feature.profile.ui.screens.ConfirmDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val IMAGE_MS = 5000
private const val HOLD_MS = 220L
private const val PREV_ZONE = 0.33f
private const val POLL_MS = 50L
private val SEGMENT_HEIGHT = 3.dp
private val MINI = 34.dp

private enum class ViewerDialog { None, Menu, Edit, DeleteClip, DeleteHighlight }

/**
 * Full-screen story viewer (same behaviour as the web one): progress bars on top, tap the left third for
 * the previous clip and the rest for the next, hold to pause, photos stay 5 seconds, videos play to the end.
 * My own highlights get a ⋮ menu: edit name/category, add more clips, delete this clip, delete the highlight.
 */
@Composable
fun HighlightViewer(
    highlight: Highlight,
    ownerName: String,
    busy: Boolean,
    onClose: () -> Unit,
    onEdit: (title: String, category: HighlightCategory, done: () -> Unit) -> Unit,
    onAddMore: () -> Unit,
    onDeleteClip: (clip: HighlightClip, done: () -> Unit) -> Unit,
    onDeleteHighlight: (done: () -> Unit) -> Unit,
) {
    val colors = TwinTheme.colors
    var index by rememberSaveable { mutableIntStateOf(0) }
    var dialog by rememberSaveable { mutableStateOf(ViewerDialog.None) }
    var held by remember { mutableStateOf(false) }
    // A deleted clip shrinks the list, so the position is clamped instead of trusted.
    val position = index.coerceIn(0, highlight.clips.lastIndex)
    val clip = highlight.clips[position]
    val paused = held || dialog != ViewerDialog.None

    val goNext: () -> Unit = { if (position + 1 < highlight.clips.size) index = position + 1 else onClose() }
    val goPrevious: () -> Unit = { index = (position - 1).coerceAtLeast(0) }
    // The tap handler below is created once, so it reads the newest lambdas through these.
    val next by rememberUpdatedState(goNext)
    val previous by rememberUpdatedState(goPrevious)

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            key(clip.id) {
                val progress = remember { Animatable(0f) }
                var failed by remember { mutableStateOf(false) }
                var buffering by remember { mutableStateOf(true) }

                Box(
                    Modifier.fillMaxSize().pointerInput(Unit) {
                        val width = size.width
                        detectTapGestures(onPress = { offset ->
                            // Released before HOLD_MS = a tap (go back/forward); still down = hold to pause.
                            val released = withTimeoutOrNull(HOLD_MS) { tryAwaitRelease() }
                            if (released == null) {
                                held = true
                                tryAwaitRelease()
                                held = false
                            } else if (released) {
                                if (offset.x < width * PREV_ZONE) previous() else next()
                            }
                        })
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        failed -> Text(
                            stringResource(R.string.profile_hl_clip_failed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.inkMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(TwinTheme.spacing.xl),
                        )
                        clip.kind == ClipKind.Image -> {
                            AsyncImage(
                                model = clip.url,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                                onSuccess = { buffering = false },
                                onError = { failed = true },
                            )
                            // The 5 seconds start after the picture is on screen and stop while paused.
                            LaunchedEffect(buffering, paused) {
                                if (!buffering && !paused) {
                                    val left = ((1f - progress.value) * IMAGE_MS).toInt().coerceAtLeast(1)
                                    progress.animateTo(1f, tween(left, easing = LinearEasing))
                                    next()
                                }
                            }
                        }
                        else -> VideoClipPlayer(
                            url = clip.url,
                            paused = paused,
                            progress = progress,
                            onBuffering = { buffering = it },
                            onEnded = { next() },
                            onFailed = { failed = true },
                        )
                    }
                    if (buffering && !failed) CircularProgressIndicator(color = colors.ink, strokeWidth = 3.dp, modifier = Modifier.size(34.dp))
                }

                ViewerTop(
                    highlight = highlight,
                    ownerName = ownerName,
                    position = position,
                    clip = clip,
                    progress = { progress.value },
                    onMenu = { dialog = ViewerDialog.Menu },
                    onClose = onClose,
                )
            }
        }
    }

    when (dialog) {
        ViewerDialog.None -> Unit
        ViewerDialog.Menu -> OwnerMenu(
            title = highlight.title,
            onDismiss = { dialog = ViewerDialog.None },
            onEdit = { dialog = ViewerDialog.Edit },
            onAddMore = { dialog = ViewerDialog.None; onAddMore() },
            onDeleteClip = { dialog = ViewerDialog.DeleteClip },
            onDeleteHighlight = { dialog = ViewerDialog.DeleteHighlight },
        )
        ViewerDialog.Edit -> EditHighlightSheet(highlight, busy, onDismiss = { dialog = ViewerDialog.None }) { title, category ->
            onEdit(title, category) { dialog = ViewerDialog.None }
        }
        ViewerDialog.DeleteClip -> ConfirmDialog(
            title = stringResource(R.string.profile_hl_delete_clip_q),
            message = stringResource(R.string.profile_hl_delete_clip_msg, highlight.title),
            confirmLabel = stringResource(R.string.profile_hl_delete_clip),
            onConfirm = { onDeleteClip(clip) { dialog = ViewerDialog.None } },
            onDismiss = { dialog = ViewerDialog.None },
        )
        ViewerDialog.DeleteHighlight -> ConfirmDialog(
            title = stringResource(R.string.profile_hl_delete_all_q),
            message = stringResource(R.string.profile_hl_delete_all_msg, highlight.title),
            confirmLabel = stringResource(R.string.profile_hl_delete_all),
            onConfirm = { onDeleteHighlight { dialog = ViewerDialog.None } },
            onDismiss = { dialog = ViewerDialog.None },
        )
    }
}

/** Segments, title line and the ⋮ / ✕ buttons, laid over the picture. */
@Composable
private fun ViewerTop(
    highlight: Highlight,
    ownerName: String,
    position: Int,
    clip: HighlightClip,
    progress: () -> Float,
    onMenu: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = TwinTheme.colors
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.s)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
            highlight.clips.indices.forEach { n ->
                Box(Modifier.weight(1f).height(SEGMENT_HEIGHT).clip(RoundedCornerShape(SEGMENT_HEIGHT)).background(colors.ink.copy(alpha = 0.3f))) {
                    val fill = when {
                        n < position -> 1f
                        n == position -> progress()
                        else -> 0f
                    }
                    Box(Modifier.fillMaxHeight().fillMaxWidth(fill.coerceIn(0f, 1f)).background(colors.ink))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = TwinTheme.spacing.s), verticalAlignment = Alignment.CenterVertically) {
            ClipThumb(highlight.cover.url, highlight.cover.kind, Modifier.size(MINI).clip(CircleShape))
            Column(Modifier.weight(1f).padding(horizontal = TwinTheme.spacing.m)) {
                Text(highlight.title, style = MaterialTheme.typography.titleSmall, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${highlight.category.emoji} $ownerName" + (clip.createdAtMs?.let { " · ${formatClipDate(it)}" } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (highlight.mine) {
                IconButton(onClick = onMenu) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.profile_hl_more), tint = colors.ink) }
            }
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.profile_hl_close), tint = colors.ink) }
        }
    }
}

/** One ExoPlayer per clip. It plays at once, pauses with [paused] and tells the viewer when it ends or fails. */
@OptIn(UnstableApi::class)
@Composable
private fun VideoClipPlayer(
    url: String,
    paused: Boolean,
    progress: Animatable<Float, *>,
    onBuffering: (Boolean) -> Unit,
    onEnded: () -> Unit,
    onFailed: () -> Unit,
) {
    val context = LocalContext.current
    val buffering by rememberUpdatedState(onBuffering)
    val ended by rememberUpdatedState(onEnded)
    val failed by rememberUpdatedState(onFailed)
    val exo = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(exo) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                buffering(state == Player.STATE_BUFFERING || state == Player.STATE_IDLE)
                if (state == Player.STATE_ENDED) ended()
            }

            override fun onPlayerError(error: PlaybackException) = failed()
        }
        exo.addListener(listener)
        onDispose {
            exo.removeListener(listener)
            exo.release()
        }
    }
    LaunchedEffect(paused) { exo.playWhenReady = !paused }
    LaunchedEffect(exo) {
        while (true) {
            val length = exo.duration
            if (length > 0) progress.snapTo((exo.currentPosition.toFloat() / length).coerceIn(0f, 1f))
            delay(POLL_MS)
        }
    }
    AndroidView(factory = { ctx -> PlayerView(ctx).apply { player = exo; useController = false } }, modifier = Modifier.fillMaxSize())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OwnerMenu(
    title: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onAddMore: () -> Unit,
    onDeleteClip: () -> Unit,
    onDeleteHighlight: () -> Unit,
) {
    val colors = TwinTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.card, contentColor = colors.ink) {
        Column(Modifier.fillMaxWidth().padding(horizontal = TwinTheme.spacing.screenEdge).padding(bottom = TwinTheme.spacing.xl)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            MenuRow(stringResource(R.string.profile_hl_edit), colors.ink, onEdit)
            MenuRow(stringResource(R.string.profile_hl_add_more), colors.ink, onAddMore)
            MenuRow(stringResource(R.string.profile_hl_delete_clip), colors.error, onDeleteClip)
            MenuRow(stringResource(R.string.profile_hl_delete_all), colors.error, onDeleteHighlight)
        }
    }
}

@Composable
private fun MenuRow(label: String, color: Color, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = color, modifier = Modifier.fillMaxWidth())
    }
}

private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

private fun formatClipDate(epochMs: Long): String =
    Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormat)
