package com.twinhearts.app.feature.cinema.ui.invite

import androidx.compose.material3.AlertDialog
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
import com.twinhearts.app.feature.cinema.domain.model.CinemaInvite
import kotlinx.coroutines.delay

/** How often the app asks for a waiting invite while it is open. Same rhythm as the web app. */
private const val POLL_MS = 6_000L

/**
 * Why this file exists: one watcher in TwinApp (like IncomingCallWatcher) so a movie-night invite shows on
 * ANY screen while the app is open. Polling stops when the app is in the background; the push notification
 * covers that case and, when tapped, the first poll after opening shows the dialog.
 * [enabled] is false while someone is signed out or already inside the cinema.
 */
@Composable
fun CinemaInviteWatcher(
    enabled: Boolean,
    onAccept: (sessionId: String) -> Unit,
    viewModel: CinemaInviteViewModel = hiltViewModel(),
) {
    val invite by viewModel.invite.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(enabled, lifecycleOwner) {
        if (!enabled) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                viewModel.check()
                delay(POLL_MS)
            }
        }
    }

    invite?.let { current ->
        CinemaInviteDialog(
            invite = current,
            onAccept = { viewModel.accept()?.let(onAccept) },
            onNotNow = viewModel::notNow,
        )
    }
}

/** The invite card. It only closes through its two buttons, like the web card. */
@Composable
internal fun CinemaInviteDialog(invite: CinemaInvite, onAccept: () -> Unit, onNotNow: () -> Unit) {
    val colors = TwinTheme.colors
    val message = if (invite.movie.isNullOrBlank()) {
        stringResource(R.string.cinema_invite_message)
    } else {
        stringResource(R.string.cinema_invite_message_movie, invite.movie)
    }
    AlertDialog(
        onDismissRequest = {},
        containerColor = colors.card,
        title = { Text(stringResource(R.string.cinema_invite_title, invite.hostName), color = colors.ink) },
        text = { Text(message, color = colors.inkMuted) },
        confirmButton = {
            TextButton(onClick = onAccept) { Text(stringResource(R.string.cinema_invite_accept), color = colors.accent) }
        },
        dismissButton = {
            TextButton(onClick = onNotNow) { Text(stringResource(R.string.cinema_invite_not_now)) }
        },
    )
}
