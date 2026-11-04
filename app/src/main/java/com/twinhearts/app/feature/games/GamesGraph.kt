package com.twinhearts.app.feature.games

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.design.TwinNeutralPage
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.calls.ui.CallViewModel
import com.twinhearts.app.feature.calls.ui.WatchCallOverlay
import com.twinhearts.app.feature.calls.ui.rememberCallStarter
import com.twinhearts.app.feature.games.ui.GameCallHooks
import com.twinhearts.app.feature.games.ui.GamesScreen
import com.twinhearts.app.feature.games.ui.LocalGameCallHooks

/**
 * Adds the Games page to the navigation graph (opened from the Home "Games" chip).
 * Also plugs the app-wide call engine into every game (face-to-face video, like the web 📹 button).
 */
fun NavGraphBuilder.gamesGraph(navController: NavHostController) {
    composable<Routes.Games> {
        val startCall = rememberCallStarter()
        val engine = hiltViewModel<CallViewModel>().engine
        val hooks = remember(engine, startCall) {
            GameCallHooks(
                startVideo = { startCall(true) },
                hangUp = { engine.hangUp() },
                inCall = {
                    val state by engine.state.collectAsStateWithLifecycle()
                    state.inCall
                },
                overlay = { WatchCallOverlay(Modifier.fillMaxSize()) },
            )
        }
        CompositionLocalProvider(LocalGameCallHooks provides hooks) {
            TwinNeutralPage {
                GamesScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
