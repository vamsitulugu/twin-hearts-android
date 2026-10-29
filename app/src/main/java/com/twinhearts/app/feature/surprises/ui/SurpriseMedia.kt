package com.twinhearts.app.feature.surprises.ui

import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
import com.twinhearts.app.feature.surprises.domain.model.MediaKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Why this file exists: shows the file attached to an unlocked surprise. Works for links this app
 * saves (https) and for the `data:` links the web app saves. A photo opens full screen on tap; a
 * video or voice note only creates its player once the person presses play, so a long list never
 * holds many players in memory at once.
 */
@Composable
fun SurpriseMedia(url: String, kind: MediaKind, modifier: Modifier = Modifier) {
    when (kind) {
        MediaKind.Image -> SurpriseImage(url, modifier)
        MediaKind.Video -> PlayableMedia(url, video = true, modifier = modifier)
        MediaKind.Audio -> PlayableMedia(url, video = false, modifier = modifier)
    }
}

/** Bytes of a `data:` link, or null when it is damaged. */
private fun decodeDataUrl(url: String): ByteArray? {
    val comma = url.indexOf(',')
    if (comma < 0) return null
    return try {
        Base64.decode(url.substring(comma + 1), Base64.DEFAULT)
    } catch (_: IllegalArgumentException) {
        null
    }
}

@Composable
private fun SurpriseImage(url: String, modifier: Modifier) {
    val colors = TwinTheme.colors
    val isData = url.startsWith("data:", ignoreCase = true)
    // A normal link is handed to Coil as is. A data link is decoded off the main thread first.
    val model by produceState<Any?>(if (isData) null else url, url) {
        if (isData) value = withContext(Dispatchers.Default) { decodeDataUrl(url) }
    }
    var enlarged by rememberSaveable(url) { mutableStateOf(false) }
    val description = stringResource(R.string.surprises_media_photo)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp, max = 320.dp)
            .clip(TwinTheme.shapes.small)
            .background(colors.glass)
            .clickable(role = Role.Image) { enlarged = true }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (enlarged && model != null) {
        Dialog(
            onDismissRequest = { enlarged = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .clickable { enlarged = false },
            ) {
                AsyncImage(
                    model = model,
                    contentDescription = description,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                IconButton(
                    onClick = { enlarged = false },
                    modifier = Modifier.align(Alignment.TopEnd).padding(TwinTheme.spacing.m),
                ) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.surprises_media_close_photo), tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun PlayableMedia(url: String, video: Boolean, modifier: Modifier) {
    var started by rememberSaveable(url) { mutableStateOf(false) }
    if (started) {
        MediaPlayer(url, video, modifier)
    } else {
        PlayPlaceholder(video, modifier) { started = true }
    }
}

@Composable
private fun PlayPlaceholder(video: Boolean, modifier: Modifier, onPlay: () -> Unit) {
    val colors = TwinTheme.colors
    val label = stringResource(if (video) R.string.surprises_media_play_video else R.string.surprises_media_play_voice)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(if (video) 160.dp else 64.dp)
            .clip(TwinTheme.shapes.small)
            .background(colors.glass)
            .clickable(role = Role.Button, onClick = onPlay)
            .semantics { contentDescription = label }
            .padding(horizontal = TwinTheme.spacing.l),
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(colors.primaryBrush),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (video) Icons.Rounded.PlayArrow else Icons.Rounded.Mic, contentDescription = null, tint = colors.ink)
        }
        Text(label, style = MaterialTheme.typography.titleSmall, color = colors.ink)
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun MediaPlayer(url: String, video: Boolean, modifier: Modifier) {
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
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exo
                useController = true
                // A voice note has no picture to tap, so its controls stay on screen.
                if (!video) controllerShowTimeoutMs = 0
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(if (video) 220.dp else 120.dp)
            .clip(TwinTheme.shapes.small),
    )
}
