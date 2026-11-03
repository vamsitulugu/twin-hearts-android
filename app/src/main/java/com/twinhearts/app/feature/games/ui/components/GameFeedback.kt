package com.twinhearts.app.feature.games.ui.components

import android.media.AudioManager
import android.media.ToneGenerator
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.twinhearts.app.core.sound.TwinSound
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
            GameCue.Success, GameCue.LevelUp, GameCue.Coin, GameCue.Match, GameCue.Boom -> HapticFeedbackConstants.LONG_PRESS
            else -> HapticFeedbackConstants.KEYBOARD_TAP
        }
        view.performHapticFeedback(haptic)

        // The game-specific cues are the soft synthesized sounds (same as the web app). They obey the app's mute switch.
        when (cue) {
            GameCue.Dice -> TwinSound.dice()
            GameCue.Flip -> TwinSound.flip()
            GameCue.Match -> TwinSound.match()
            GameCue.Snap -> TwinSound.snap()
            GameCue.Boom -> TwinSound.boom()
            GameCue.Tick -> TwinSound.tick(0.6f)
            GameCue.SpinStart -> TwinSound.spinStart()
            GameCue.SpinEnd -> TwinSound.spinEnd()
            GameCue.Brush -> TwinSound.scratch(0.2f)
            else -> playBeep(cue)
        }
    }

    /** The original small beeps. They now stay quiet when the app's sound is muted. */
    private fun playBeep(cue: GameCue) {
        if (!TwinSound.isEnabled) return
        val (type, ms) = when (cue) {
            GameCue.Tap -> ToneGenerator.TONE_DTMF_1 to 40
            GameCue.Place -> ToneGenerator.TONE_DTMF_3 to 50
            GameCue.Success -> ToneGenerator.TONE_PROP_ACK to 150
            GameCue.Fail -> ToneGenerator.TONE_PROP_NACK to 150
            GameCue.Coin -> ToneGenerator.TONE_PROP_BEEP2 to 120
            GameCue.LevelUp -> ToneGenerator.TONE_PROP_ACK to 350
            else -> return // the soft cues are played in play()
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
