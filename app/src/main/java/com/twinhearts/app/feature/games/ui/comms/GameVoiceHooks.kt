package com.twinhearts.app.feature.games.ui.comms

import androidx.compose.runtime.Composable

/**
 * Why this file exists: the in-game voice call as Games sees it, without importing the Calls feature
 * (R-83). GamesGraph turns the app's voice engine into these plain types; GameHost and the overlay
 * only read them.
 */
enum class GameVoiceUiPhase { Idle, Ringing, Connecting, Active }

enum class GameVoiceUiNotice { Ended, Declined, NoAnswer, Lost, Failed, NeedsMic, Busy, NotPaired }

data class GameVoiceUi(
    val phase: GameVoiceUiPhase = GameVoiceUiPhase.Idle,
    val partnerName: String = "",
    val muted: Boolean = false,
    val speaker: Boolean = true,
    val notice: GameVoiceUiNotice? = null,
) {
    /** True while the call is being set up or running (the little pill is on screen). */
    val running: Boolean get() = phase == GameVoiceUiPhase.Connecting || phase == GameVoiceUiPhase.Active
}

class GameVoiceHooks(
    /** Read inside a composable so the header button and the pill update. */
    val state: @Composable () -> GameVoiceUi,
    /** Calls my partner (asks for the microphone first). */
    val start: () -> Unit,
    /** Answers the ringing call (asks for the microphone first). */
    val accept: () -> Unit,
    val decline: () -> Unit,
    /** Ends the call for both people. */
    val hangUp: () -> Unit,
    val toggleMute: () -> Unit,
    val toggleSpeaker: () -> Unit,
    val clearNotice: () -> Unit,
    /** Runs while a game is on screen and the app is visible; listens for my partner's call. */
    val watch: suspend (gameKey: String) -> Unit,
    /** The game closed: ends the call and any ringing. */
    val leave: () -> Unit,
)
