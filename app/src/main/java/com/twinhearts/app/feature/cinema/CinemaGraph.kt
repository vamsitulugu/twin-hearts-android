package com.twinhearts.app.feature.cinema

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.twinhearts.app.feature.calls.domain.CallPhase
import com.twinhearts.app.feature.calls.ui.CallViewModel
import com.twinhearts.app.feature.calls.ui.CinemaCallOverlay
import com.twinhearts.app.feature.calls.ui.rememberCallStarter
import com.twinhearts.app.feature.cinema.ui.CinemaCallHooks
import androidx.compose.ui.Modifier
import com.twinhearts.app.feature.cinema.ui.CinemaScreen

/**
 * Plugs the Cinema into the app (R-43). Opened from the Home "Cinema" chip (I am the host) or from the
 * invite dialog (I am the partner, `sessionId` set). Leaving the cinema simply goes back one screen.
 */
fun NavGraphBuilder.cinemaGraph(navController: NavHostController) {
    composable<Routes.Cinema> {
        // Voice / Video buttons inside the theatre use the app's own call engine (the web cinema borrows its parent page's one).
        val engine = hiltViewModel<CallViewModel>().engine
        val start = rememberCallStarter()
        val hooks = remember(engine, start) {
            CinemaCallHooks(
                start = start,
                end = { if (engine.state.value.phase != CallPhase.Idle) engine.hangUp() },
                toggleMute = { engine.toggleMute() },
                toggleSpeaker = { engine.toggleSpeaker() },
                flipCamera = { engine.switchCamera() },
                overlay = { CinemaCallOverlay(Modifier.fillMaxSize()) },
            )
        }
        CinemaScreen(onClose = { navController.popBackStack() }, callHooks = hooks)
    }
}
