package com.twinhearts.app.feature.calls.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.twinhearts.app.feature.calls.domain.CallPhase

/**
 * Why this file exists: listens for my partner's calls while the app is open, on any screen, and
 * asks the app to open the call page when one comes in. Draws nothing.
 */
@Composable
fun IncomingCallWatcher(onIncoming: (video: Boolean) -> Unit, viewModel: CallViewModel = hiltViewModel()) {
    val engine = viewModel.engine
    val state by engine.state.collectAsStateWithLifecycle()
    val open by rememberUpdatedState(onIncoming)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { engine.watch() }
    }
    LaunchedEffect(state.phase) {
        if (state.phase == CallPhase.Incoming) open(state.video)
    }
}
