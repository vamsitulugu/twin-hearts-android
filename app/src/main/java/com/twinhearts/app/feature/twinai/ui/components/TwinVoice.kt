package com.twinhearts.app.feature.twinai.ui.components

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme

enum class VoicePhase { Idle, Listening, Thinking, Speaking, Muted }

/**
 * Why this file exists: runs Twin's voice mode. It listens (phone speech recognition), hands the
 * words to Twin, reads the answer aloud, then listens again, until the user ends it or mutes.
 * Everything stops when the page leaves the screen (see TwinAiScreen).
 */
class VoiceController(
    context: Context,
    private val speaker: TwinSpeaker,
    private val onHeard: (String) -> Unit,
    private val onUnavailable: () -> Unit,
) {
    var phase by mutableStateOf(VoicePhase.Idle)
        private set
    var transcript by mutableStateOf("")
        private set

    private var recognizer: SpeechRecognizer? = null
    private val appContext = context.applicationContext
    private var misses = 0
    private var open = false

    init {
        speaker.onFinished = { if (open && phase == VoicePhase.Speaking) listen() }
    }

    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            onUnavailable()
            return
        }
        open = true
        misses = 0
        listen()
    }

    fun listen() {
        if (!open) return
        speaker.stop()
        transcript = ""
        phase = VoicePhase.Listening
        recognizer?.destroy()
        val r = SpeechRecognizer.createSpeechRecognizer(appContext)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (text.isBlank()) { retryListening(); return }
                misses = 0
                transcript = text
                phase = VoicePhase.Thinking
                onHeard(text)
            }
            override fun onPartialResults(partial: Bundle?) {
                partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { transcript = it }
            }
            override fun onError(error: Int) {
                if (phase == VoicePhase.Listening) retryListening()
            }
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        r.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true),
        )
    }

    /** Silence or no match: try again a few times, then wait for a tap instead of looping forever. */
    private fun retryListening() {
        misses++
        if (misses >= 3) {
            recognizer?.destroy(); recognizer = null
            phase = VoicePhase.Idle
        } else {
            listen()
        }
    }

    /** Twin's answer arrived: read it, then [speaker] calls back and we listen again. */
    fun speak(id: String, text: String) {
        if (!open || phase == VoicePhase.Muted) return
        recognizer?.destroy(); recognizer = null
        phase = VoicePhase.Speaking
        speaker.toggle(id, text)
    }

    /** The answer failed: stop waiting and let the user tap to try again. */
    fun failed() {
        if (open && phase == VoicePhase.Thinking) phase = VoicePhase.Idle
    }

    fun onMicTap() {
        when (phase) {
            VoicePhase.Speaking -> listen() // interrupt Twin and talk
            VoicePhase.Muted, VoicePhase.Idle -> { misses = 0; listen() }
            VoicePhase.Listening -> mute()
            VoicePhase.Thinking -> Unit
        }
    }

    fun mute() {
        recognizer?.destroy(); recognizer = null
        speaker.stop()
        phase = VoicePhase.Muted
    }

    fun end() {
        open = false
        recognizer?.destroy(); recognizer = null
        speaker.stop()
        speaker.onFinished = null
        phase = VoicePhase.Idle
    }
}

@Composable
fun rememberVoiceController(
    speaker: TwinSpeaker,
    onHeard: (String) -> Unit,
    onUnavailable: () -> Unit,
): VoiceController {
    val context = LocalContext.current
    val controller = remember { VoiceController(context, speaker, onHeard, onUnavailable) }
    DisposableEffect(controller) { onDispose { controller.end() } }
    return controller
}

/** Full-screen voice mode: big orb, status line, what you said, mute and end buttons. */
@Composable
fun TwinVoiceOverlay(controller: VoiceController, onEnd: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val status = when (controller.phase) {
        VoicePhase.Listening -> R.string.twinai_voice_listening
        VoicePhase.Thinking -> R.string.twinai_voice_thinking
        VoicePhase.Speaking -> R.string.twinai_voice_speaking
        VoicePhase.Muted -> R.string.twinai_voice_muted
        VoicePhase.Idle -> R.string.twinai_voice_hint_speak
    }
    val hint = when (controller.phase) {
        VoicePhase.Muted -> R.string.twinai_voice_hint_resume
        VoicePhase.Speaking -> R.string.twinai_voice_hint_interrupt
        else -> R.string.twinai_voice_hint_speak
    }
    Box(modifier.fillMaxSize().background(colors.scrim)) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(TwinTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.l, Alignment.CenterVertically),
        ) {
            TwinOrb(size = 160.dp)
            Text(stringResource(status), style = MaterialTheme.typography.headlineSmall, color = colors.ink)
            Text(
                controller.transcript,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.inkMuted,
                textAlign = TextAlign.Center,
                maxLines = 4,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xl)) {
                VoiceButton(
                    if (controller.phase == VoicePhase.Muted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                    stringResource(R.string.twinai_voice_mute),
                    controller::onMicTap,
                )
                VoiceButton(Icons.Rounded.Close, stringResource(R.string.twinai_voice_end), onEnd)
            }
            Text(stringResource(hint), style = MaterialTheme.typography.bodySmall, color = colors.inkFaint)
        }
    }
}

@Composable
private fun VoiceButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    Box(
        Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(colors.glass)
            .border(1.dp, colors.line, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = colors.ink, modifier = Modifier.size(28.dp))
    }
}
