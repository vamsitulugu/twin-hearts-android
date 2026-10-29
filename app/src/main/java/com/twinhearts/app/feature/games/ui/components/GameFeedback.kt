package com.twinhearts.app.feature.games.ui.components

import android.media.AudioManager
import android.media.ToneGenerator
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.twinhearts.app.feature.games.ui.play.GameCue

/** Small beeps and taps, like the web app's synthesized sounds. Never crashes if audio is missing. */
class GameFeedback(private val view: View) {

    private val tone: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, VOLUME)
    } catch (e: RuntimeException) {
        null
    }

    fun play(cue: GameCue) {
        val haptic = when (cue) {
            GameCue.Success, GameCue.LevelUp, GameCue.Coin -> HapticFeedbackConstants.LONG_PRESS
            else -> HapticFeedbackConstants.KEYBOARD_TAP
        }
        view.performHapticFeedback(haptic)
        val (type, ms) = when (cue) {
            GameCue.Tap -> ToneGenerator.TONE_DTMF_1 to 40
            GameCue.Place -> ToneGenerator.TONE_DTMF_3 to 50
            GameCue.Dice -> ToneGenerator.TONE_DTMF_5 to 60
            GameCue.Success -> ToneGenerator.TONE_PROP_ACK to 150
            GameCue.Fail -> ToneGenerator.TONE_PROP_NACK to 150
            GameCue.Coin -> ToneGenerator.TONE_PROP_BEEP2 to 120
            GameCue.LevelUp -> ToneGenerator.TONE_PROP_ACK to 350
        }
        tone?.startTone(type, ms)
    }

    fun release() {
        tone?.release()
    }

    private companion object {
        const val VOLUME = 40
    }
}

@Composable
fun rememberGameFeedback(): GameFeedback {
    val view = LocalView.current
    val feedback = remember(view) { GameFeedback(view) }
    DisposableEffect(feedback) { onDispose { feedback.release() } }
    return feedback
}
