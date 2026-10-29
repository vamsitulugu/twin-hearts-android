package com.twinhearts.app.feature.vault.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.vault.domain.model.VaultItem
import com.twinhearts.app.feature.vault.domain.model.VaultKind

/**
 * Keeps screenshots and the recent-apps preview from showing private photos while this is on
 * screen. Works for the page itself and, when called inside a [Dialog], for that window too.
 */
@Composable
internal fun SecureWindowEffect() {
    val view = LocalView.current
    val context = LocalContext.current
    DisposableEffect(view) {
        val window = (view.parent as? DialogWindowProvider)?.window ?: (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            // Only the dialog window is cleared here; the page clears its own flag when it is left.
            if (view.parent is DialogWindowProvider) window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

/** Page-level secure flag: set while the vault page is shown, cleared when it is left. */
@Composable
internal fun SecurePageEffect() {
    val context = LocalContext.current
    DisposableEffect(context) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}

/** Full-screen, swipeable viewer over every vault item, starting at the tapped one. */
@Composable
internal fun VaultViewer(items: List<VaultItem>, startIndex: Int, onClose: () -> Unit) {
    val pager = rememberPagerState(initialPage = startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))) { items.size }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        SecureWindowEffect()
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(pager, modifier = Modifier.fillMaxSize()) { page ->
                val item = items[page]
                when (item.kind) {
                    VaultKind.Photo -> AsyncImage(
                        model = item.url,
                        contentDescription = stringResource(R.string.vault_photo),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                    VaultKind.Video -> VaultVideoPage(url = item.url, active = pager.currentPage == page)
                }
            }
            Row(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(TwinTheme.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.vault_close), tint = Color.White)
                }
                Text(
                    text = if (items.size > 1) "${pager.currentPage + 1} / ${items.size}" else "",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                // Balances the close button so the counter stays centred.
                Box(Modifier.padding(horizontal = TwinTheme.spacing.xxl))
            }
        }
    }
}

/** One video page: plays only while it is the page on screen, and lets go of the player when it is not. */
@OptIn(UnstableApi::class)
@Composable
private fun VaultVideoPage(url: String, active: Boolean) {
    val context = LocalContext.current
    val exo = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
        }
    }
    DisposableEffect(exo) { onDispose { exo.release() } }
    LaunchedEffect(active, exo) { exo.playWhenReady = active }
    AndroidView(
        factory = { ctx -> PlayerView(ctx).apply { player = exo } },
        update = { it.player = exo },
        modifier = Modifier.fillMaxSize(),
    )
}
