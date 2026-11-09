package com.twinhearts.app.feature.home.ui.signal

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.notifications.NotificationEvents
import com.twinhearts.app.feature.home.domain.model.LoveKind
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope

private const val POLL_MS = 5_000L

/**
 * Why this file exists: one watcher in TwinApp so a Hug / Touch / Miss You from my partner opens an
 * Accept card on ANY screen while the app is open. It checks every few seconds and also at once when
 * the push arrives. [enabled] is false while signed out; [onHome] hides only the hug card (Home shows its own).
 */
@Composable
fun LoveSignalWatcher(
    enabled: Boolean,
    onHome: Boolean,
    viewModel: LoveSignalViewModel = hiltViewModel(),
) {
    val popup by viewModel.popup.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(enabled, onHome, lifecycleOwner) {
        if (!enabled) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            coroutineScope {
                launch { NotificationEvents.love.collect { viewModel.check(onHome) } }
                while (true) {
                    viewModel.check(onHome)
                    delay(POLL_MS)
                }
            }
        }
    }

    popup?.let { LoveSignalDialog(it, onAccept = viewModel::accept, onLater = viewModel::later) }
}

/** The card. It closes only through its two buttons, so a signal is never lost by a stray tap. */
@Composable
internal fun LoveSignalDialog(popup: LoveSignalPopup, onAccept: () -> Unit, onLater: () -> Unit) {
    val colors = TwinTheme.colors
    val (emoji, title, accept) = when (popup.kind) {
        LoveKind.Hug -> Triple("🤗", stringResource(R.string.lovesignal_hug, popup.partnerName), R.string.lovesignal_accept_hug)
        LoveKind.Touch -> Triple("💓", stringResource(R.string.lovesignal_touch, popup.partnerName), R.string.lovesignal_accept)
        LoveKind.MissYou -> Triple("💔", stringResource(R.string.lovesignal_miss, popup.partnerName), R.string.lovesignal_accept)
    }
    AlertDialog(
        onDismissRequest = {},
        containerColor = colors.card,
        icon = { Text(emoji, style = MaterialTheme.typography.displayMedium) },
        title = { Text(title, color = colors.ink) },
        confirmButton = { TextButton(onClick = onAccept) { Text(stringResource(accept), color = colors.accent) } },
        dismissButton = { TextButton(onClick = onLater) { Text(stringResource(R.string.lovesignal_later)) } },
    )
}
