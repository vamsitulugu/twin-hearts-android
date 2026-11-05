package com.twinhearts.app.feature.games

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import com.twinhearts.app.feature.calls.data.GameVoiceNotice
import com.twinhearts.app.feature.calls.data.GameVoicePhase
import com.twinhearts.app.feature.calls.data.GameVoiceState
import com.twinhearts.app.feature.calls.ui.CallViewModel
import com.twinhearts.app.feature.calls.ui.GameVoiceViewModel
import com.twinhearts.app.feature.calls.ui.WatchCallOverlay
import com.twinhearts.app.feature.calls.ui.rememberGameVideoStarter
import com.twinhearts.app.feature.calls.ui.rememberGameVoiceActions
import com.twinhearts.app.feature.games.ui.GameCallHooks
import com.twinhearts.app.feature.games.ui.GamesScreen
import com.twinhearts.app.feature.games.ui.LocalGameCallHooks
import com.twinhearts.app.feature.games.ui.comms.GameVoiceHooks
import com.twinhearts.app.feature.games.ui.comms.GameVoiceUi
import com.twinhearts.app.feature.games.ui.comms.GameVoiceUiNotice
import com.twinhearts.app.feature.games.ui.comms.GameVoiceUiPhase

/**
 * Adds the Games page to the navigation graph (opened from the Home "Games" chip).
 * Also plugs the app-wide call engine into every game: face-to-face video (the 📹 button) and the
 * small in-game voice call (the 📞 button). This is the only place that knows both Games and Calls (R-83).
 */
fun NavGraphBuilder.gamesGraph(navController: NavHostController) {
    composable<Routes.Games> {
        val startGameVideo = rememberGameVideoStarter()
        val engine = hiltViewModel<CallViewModel>().engine
        val voiceEngine = hiltViewModel<GameVoiceViewModel>().engine
        val voiceActions = rememberGameVoiceActions()

        val voice = remember(voiceEngine, voiceActions) {
            GameVoiceHooks(
                state = {
                    val s by voiceEngine.state.collectAsStateWithLifecycle()
                    s.toUi()
                },
                start = voiceActions.start,
                accept = voiceActions.accept,
                decline = voiceEngine::decline,
                hangUp = voiceEngine::stop,
                toggleMute = voiceEngine::toggleMute,
                toggleSpeaker = voiceEngine::toggleSpeaker,
                clearNotice = voiceEngine::clearNotice,
                watch = voiceEngine::watch,
                leave = voiceEngine::leave,
            )
        }
        val hooks = remember(engine, startGameVideo, voice, voiceEngine) {
            GameCallHooks(
                // Video already carries voice, so a running game voice call ends first.
                startVideo = { gameKey ->
                    if (voiceEngine.state.value.running) voiceEngine.stop()
                    startGameVideo(gameKey)
                },
                hangUp = { engine.hangUp() },
                inCall = {
                    val state by engine.state.collectAsStateWithLifecycle()
                    state.inCall
                },
                overlay = { WatchCallOverlay(Modifier.fillMaxSize()) },
                voice = voice,
            )
        }

        // A normal call (or the video call) takes the microphone: the game voice call steps aside.
        val callState by engine.state.collectAsStateWithLifecycle()
        LaunchedEffect(callState.inCall) {
            if (callState.inCall && voiceEngine.state.value.running) voiceEngine.stop()
        }

        CompositionLocalProvider(LocalGameCallHooks provides hooks) {
            TwinNeutralPage {
                GamesScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun GameVoiceState.toUi() = GameVoiceUi(
    phase = when (phase) {
        GameVoicePhase.Idle -> GameVoiceUiPhase.Idle
        GameVoicePhase.Ringing -> GameVoiceUiPhase.Ringing
        GameVoicePhase.Connecting -> GameVoiceUiPhase.Connecting
        GameVoicePhase.Active -> GameVoiceUiPhase.Active
    },
    partnerName = partnerName,
    muted = muted,
    speaker = speaker,
    notice = notice?.toUi(),
)

private fun GameVoiceNotice.toUi() = when (this) {
    GameVoiceNotice.Ended -> GameVoiceUiNotice.Ended
    GameVoiceNotice.Declined -> GameVoiceUiNotice.Declined
    GameVoiceNotice.NoAnswer -> GameVoiceUiNotice.NoAnswer
    GameVoiceNotice.Lost -> GameVoiceUiNotice.Lost
    GameVoiceNotice.Failed -> GameVoiceUiNotice.Failed
    GameVoiceNotice.NeedsMic -> GameVoiceUiNotice.NeedsMic
    GameVoiceNotice.Busy -> GameVoiceUiNotice.Busy
    GameVoiceNotice.NotPaired -> GameVoiceUiNotice.NotPaired
}
