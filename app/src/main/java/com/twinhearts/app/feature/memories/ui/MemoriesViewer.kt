package com.twinhearts.app.feature.memories.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.memories.domain.model.Memory
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val MAX_ZOOM = 4f
private const val DOUBLE_TAP_ZOOM = 2.5f

/**
 * Full-screen viewer (web `memViewer`): swipe between photos and videos, pinch or double-tap to
 * zoom, tap to hide the bars, favourite / share / delete, and a menu with Download and Details.
 * Delete only asks [onDelete]; the page shows the confirmation and removes the item.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MemoriesViewer(
    items: List<Memory>,
    startIndex: Int,
    onDismiss: () -> Unit,
    onFavorite: (Memory) -> Unit,
    onShare: (Memory) -> Unit,
    onDownload: (Memory) -> Unit,
    onDelete: (Memory) -> Unit,
) {
    if (items.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }
    val pager = rememberPagerState(initialPage = startIndex.coerceIn(0, items.lastIndex)) { items.size }
    var barsVisible by rememberSaveable { mutableStateOf(true) }
    var menuOpen by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf<Memory?>(null) }
    val current = items[pager.currentPage.coerceIn(0, items.lastIndex)]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 0) { page ->
                ViewerPage(
                    item = items[page],
                    isCurrent = page == pager.currentPage,
                    onTap = { barsVisible = !barsVisible },
                )
            }
            if (barsVisible) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .statusBarsPadding()
                        .padding(horizontal = TwinTheme.spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.memories_close), tint = Color.White)
                    }
                    Text(
                        text = "${pager.currentPage + 1} / ${items.size}",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Rounded.MoreVert, stringResource(R.string.memories_more), tint = Color.White)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.memories_download)) },
                                onClick = {
                                    menuOpen = false
                                    onDownload(current)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.memories_details)) },
                                onClick = {
                                    menuOpen = false
                                    details = current
                                },
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .navigationBarsPadding()
                        .padding(vertical = TwinTheme.spacing.xs),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
                ) {
                    IconButton(onClick = { onFavorite(current) }) {
                        Icon(
                            if (current.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            stringResource(R.string.memories_favorite),
                            tint = if (current.favorite) Color(0xFFFFC857) else Color.White,
                        )
                    }
                    IconButton(onClick = { onShare(current) }) {
                        Icon(Icons.Rounded.Share, stringResource(R.string.memories_share), tint = Color.White)
                    }
                    IconButton(onClick = { onDelete(current) }) {
                        Icon(Icons.Rounded.Delete, stringResource(R.string.memories_delete), tint = Color.White)
                    }
                }
            }
        }
    }

    details?.let { memory ->
        AlertDialog(
            onDismissRequest = { details = null },
            containerColor = TwinTheme.colors.card,
            title = { Text(stringResource(R.string.memories_details), color = TwinTheme.colors.ink) },
            text = {
                Text(
                    text = buildDetails(memory),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TwinTheme.colors.inkMuted,
                )
            },
            confirmButton = { TextButton(onClick = { details = null }) { Text(stringResource(R.string.memories_ok)) } },
        )
    }
}

@Composable
private fun buildDetails(memory: Memory): String {
    val date = memory.date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: "—"
    val type = stringResource(if (memory.isVideo) R.string.memories_type_video else R.string.memories_type_photo)
    return buildList {
        add(stringResource(R.string.memories_detail_date, date))
        add(stringResource(R.string.memories_detail_type, type))
        if (memory.name.isNotBlank()) add(stringResource(R.string.memories_detail_name, memory.name))
    }.joinToString("\n")
}

@Composable
private fun ViewerPage(item: Memory, isCurrent: Boolean, onTap: () -> Unit) {
    if (item.isVideo) {
        if (isCurrent) {
            VideoPage(item.url, onTap)
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.PlayArrow, null, tint = Color.White.copy(alpha = 0.6f))
            }
        }
    } else {
        ZoomableImage(item, onTap)
    }
}

/** A picture you can pinch and double-tap to zoom. Swiping still changes page until you zoom in. */
@Composable
private fun ZoomableImage(item: Memory, onTap: () -> Unit) {
    var scale by remember(item.id) { mutableFloatStateOf(1f) }
    var offset by remember(item.id) { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    fun clamp(o: Offset, s: Float): Offset {
        val maxX = size.width * (s - 1f) / 2f
        val maxY = size.height * (s - 1f) / 2f
        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(item.id) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else DOUBLE_TAP_ZOOM
                        offset = Offset.Zero
                    },
                )
            }
            .pointerInput(item.id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        // One finger on an un-zoomed picture is a swipe for the pager; leave it alone.
                        if (event.changes.size > 1 || scale > 1f) {
                            val newScale = (scale * event.calculateZoom()).coerceIn(1f, MAX_ZOOM)
                            scale = newScale
                            offset = if (newScale > 1f) clamp(offset + event.calculatePan(), newScale) else Offset.Zero
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = item.url,
            contentDescription = stringResource(R.string.memories_photo_cd),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPage(url: String, onTap: () -> Unit) {
    val context = LocalContext.current
    val exo = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(exo) { onDispose { exo.release() } }
    AndroidView(
        factory = { ctx -> PlayerView(ctx).apply { player = exo; setOnClickListener { onTap() } } },
        modifier = Modifier.fillMaxSize(),
    )
}
