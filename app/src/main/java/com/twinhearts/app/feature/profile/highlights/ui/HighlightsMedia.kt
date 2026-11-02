package com.twinhearts.app.feature.profile.highlights.ui

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.profile.highlights.domain.ClipKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Why this file exists: Coil shows photos but cannot draw a still from a video, and adding a video
 * decoder library would need a new dependency. So a video's first frame is read with Android's own
 * MediaMetadataRetriever, off the main thread, and remembered so scrolling stays smooth.
 */

/** A square-cropped picture of a clip: the photo itself, or the first frame of a video with a small play mark. */
@Composable
fun ClipThumb(source: String, kind: ClipKind, modifier: Modifier = Modifier) {
    if (kind == ClipKind.Image) {
        AsyncImage(model = source, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        VideoFrame(source, modifier)
    }
}

@Composable
private fun VideoFrame(source: String, modifier: Modifier) {
    val context = LocalContext.current
    val frame by produceState(initialValue = frameCache.get(source), source) {
        if (value == null) {
            value = withContext(Dispatchers.IO) { readFrame(context, source)?.also { frameCache.put(source, it) } }
        }
    }
    Box(modifier.background(TwinTheme.colors.card), contentAlignment = Alignment.Center) {
        frame?.let { Image(it.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = TwinTheme.colors.ink, modifier = Modifier.size(PLAY_MARK))
    }
}

private val PLAY_MARK = 20.dp
private const val FRAME_AT_US = 100_000L
private const val MAX_SIDE = 256
private const val CACHE_FRAMES = 32
private val frameCache = LruCache<String, Bitmap>(CACHE_FRAMES)

/** [source] is a web address (saved clip) or a phone address (chosen clip). Null when it cannot be read. */
private fun readFrame(context: android.content.Context, source: String): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        if (source.startsWith("http")) retriever.setDataSource(source, HashMap()) else retriever.setDataSource(context, Uri.parse(source))
        val raw = retriever.getFrameAtTime(FRAME_AT_US, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return null
        val scale = MAX_SIDE.toFloat() / maxOf(raw.width, raw.height)
        if (scale >= 1f) raw else Bitmap.createScaledBitmap(raw, (raw.width * scale).toInt(), (raw.height * scale).toInt(), true)
    } catch (e: RuntimeException) {
        null
    } finally {
        retriever.release()
    }
}
