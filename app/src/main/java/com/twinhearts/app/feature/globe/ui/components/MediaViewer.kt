package com.twinhearts.app.feature.globe.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassIconButton
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.globe.ui.ViewerItem

private const val MAX_PHOTO_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f

/** Full-screen gallery: swipe between photos and videos, pinch or double-tap to zoom a photo. */
@Composable
fun MediaViewer(items: List<ViewerItem>, startIndex: Int, onClose: () -> Unit) {
    val pager = rememberPagerState(initialPage = startIndex) { items.size }
    var zoomed by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(TwinTheme.colors.background)) {
            HorizontalPager(state = pager, userScrollEnabled = !zoomed, modifier = Modifier.fillMaxSize()) { page ->
                val item = items[page]
                if (item.isVideo) {
                    if (page == pager.currentPage) VideoPage(item.url)
                } else {
                    ZoomableImage(item.url, onZoomChange = { zoomed = it })
                }
            }
            Text(
                text = stringResource(R.string.globe_viewer_counter, pager.currentPage + 1, items.size),
                style = MaterialTheme.typography.labelLarge,
                color = TwinTheme.colors.ink,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(TwinTheme.spacing.l),
            )
            GlassIconButton(
                icon = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.globe_close),
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(TwinTheme.spacing.m),
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPage(url: String) {
    val context = LocalContext.current
    val exo = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(exo) { onDispose { exo.release() } }
    AndroidView(factory = { ctx -> PlayerView(ctx).apply { player = exo } }, modifier = Modifier.fillMaxSize())
}

@Composable
private fun ZoomableImage(url: String, onZoomChange: (Boolean) -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = if (scale > 1.01f) 1f else DOUBLE_TAP_ZOOM
                        offset = Offset.Zero
                        onZoomChange(scale > 1f)
                    },
                )
            }
            .pointerInput(Unit) {
                // Only two fingers, or one finger on a zoomed photo, is a gesture here;
                // a plain swipe at normal size is left to the pager.
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.size >= 2 || scale > 1f) {
                            scale = (scale * event.calculateZoom()).coerceIn(1f, MAX_PHOTO_ZOOM)
                            offset = if (scale <= 1f) Offset.Zero else offset + event.calculatePan()
                            onZoomChange(scale > 1f)
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
        )
    }
}
