package com.twinhearts.app.feature.globe.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.globe.domain.model.MediaKind
import com.twinhearts.app.feature.globe.domain.model.Memory
import com.twinhearts.app.feature.globe.ui.ViewerItem

/** Photos and videos of a memory, in the order the full-screen gallery swipes through them. */
fun viewerItemsOf(memory: Memory): List<ViewerItem> =
    memory.media
        .filter { it.kind == MediaKind.Photo || it.kind == MediaKind.Video }
        .map { ViewerItem(it.url, it.kind == MediaKind.Video) }

/** "Are you sure?" dialog used for deleting a memory or a single photo / video / voice note. */
@Composable
fun ConfirmDeleteDialog(text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = TwinTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        text = { Text(text, color = colors.ink) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.globe_delete), color = colors.error) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.globe_cancel), color = colors.inkMuted) }
        },
    )
}

/** A square thumbnail. Videos show a play badge because a video frame cannot be loaded as an image. */
@Composable
fun MediaThumb(url: String, isVideo: Boolean, size: Dp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(colors.glass)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isVideo) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.globe_play_video), tint = colors.ink)
        } else {
            AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        }
    }
}
