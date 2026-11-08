package com.twinhearts.app.feature.cinema.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.TwinTheme

/** The Cinema page: finds the link, then shows the cinema full screen. Leaving always goes back through [onClose]. */
@Composable
fun CinemaScreen(onClose: () -> Unit, viewModel: CinemaViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CinemaContent(state = state, onRetry = viewModel::load, onClose = onClose)
}

/** Stateless part (R-42): one branch per state (R-41). */
@Composable
internal fun CinemaContent(state: UiState<String>, onRetry: () -> Unit, onClose: () -> Unit) {
    when (state) {
        is UiState.Content -> CinemaPlayer(url = state.data, onClose = onClose)
        is UiState.Error -> ErrorState(
            message = state.error.asText(),
            onRetry = onRetry,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
        )
        // There is no "empty" cinema: a link either exists or the call failed.
        UiState.Loading, UiState.Empty -> LoadingSkeleton(Modifier.fillMaxSize().statusBarsPadding().padding(TwinTheme.spacing.xl))
    }
}

/** The WebView plus the "Leave cinema?" question when the phone's Back button is pressed. */
@Composable
private fun CinemaPlayer(url: String, onClose: () -> Unit) {
    var askLeave by rememberSaveable { mutableStateOf(false) }
    BackHandler { askLeave = true }
    // The cinema's own Leave button closes straight away: the person already chose to leave.
    CinemaWebView(url = url, onLeave = onClose)
    if (askLeave) {
        AlertDialog(
            onDismissRequest = { askLeave = false },
            containerColor = TwinTheme.colors.card,
            title = { Text(stringResource(R.string.cinema_leave_title), color = TwinTheme.colors.ink) },
            text = { Text(stringResource(R.string.cinema_leave_message), color = TwinTheme.colors.inkMuted) },
            confirmButton = {
                TextButton(onClick = { askLeave = false; onClose() }) {
                    Text(stringResource(R.string.cinema_leave_confirm), color = TwinTheme.colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { askLeave = false }) { Text(stringResource(R.string.cinema_leave_stay)) }
            },
        )
    }
}
