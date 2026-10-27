package com.twinhearts.app.feature.twinai.ui.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.twinhearts.app.R
import com.twinhearts.app.feature.twinai.domain.model.OrbState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Why this file exists: the Android version of the web "Twin voice mode". One tap opens a full
 * screen that listens (phone speech recognizer), sends what you said through the normal Twin chat,
 * reads the answer aloud (phone text-to-speech) and then closes itself, exactly like the web flow.
 * Free: it only uses what the phone already has.
 *
 * Everything that closes the screen goes through [exit], so the microphone and the voice can never
 * be left running (same single clean-up rule as the web code).
 */
@Stable
class TwinVoiceController internal constructor(
    private val context: Context,
    private val scope: CoroutineScope,
    /** Sends the spoken question to the Twin chat. Returns false when Twin is busy with another answer. */
    private val submit: (String) -> Boolean,
) {
    /** True while the voice screen is on screen. */
    var isOpen by mutableStateOf(false)
        private set
    var orb by mutableStateOf(OrbState.Idle)
        private set
    var muted by mutableStateOf(false)
        private set
    @get:StringRes var statusRes by mutableStateOf(R.string.twinai_voice_getting_ready)
        private set
    @get:StringRes var hintRes by mutableStateOf<Int?>(null)
        private set
    var finalText by mutableStateOf("")
        private set
    var interimText by mutableStateOf("")
        private set

    /** 0..1 loudness of your voice, smoothed. Drives the orb size while listening. */
    var level by mutableFloatStateOf(0f)
        private set

    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var manualStop = false
    private var awaitingReply = false
    private var token = 0
    private var closeJob: Job? = null
    private var watchdog: Job? = null

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    /** Opens the screen and starts listening. Call only after the microphone permission is granted. */
    fun open() {
        if (isOpen) return
        isOpen = true
        muted = false
        awaitingReply = false
        finalText = ""
        interimText = ""
        show(OrbState.Idle, R.string.twinai_voice_getting_ready, null)
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            show(OrbState.Error, R.string.twinai_voice_unsupported, R.string.twinai_voice_unsupported_hint)
            closeLater(1_600)
            return
        }
        startListening()
    }

    /** Shown when the microphone permission was refused: same message and auto-close as the web. */
    fun openDenied() {
        if (isOpen) return
        isOpen = true
        show(OrbState.Error, R.string.twinai_voice_mic_needed, R.string.twinai_voice_type_hint)
        closeLater(1_600)
    }

    /** The one clean-up path: every way out of voice mode ends here. */
    fun exit() {
        if (!isOpen) return
        isOpen = false
        token++
        manualStop = true
        awaitingReply = false
        closeJob?.cancel()
        watchdog?.cancel()
        stopListening()
        tts?.stop()
        level = 0f
        orb = OrbState.Idle
    }

    /** The mic button on the voice screen: interrupts Twin while it speaks, otherwise mutes / unmutes. */
    fun toggleMute() {
        closeJob?.cancel()
        if (orb == OrbState.Speaking) {
            token++
            watchdog?.cancel()
            tts?.stop()
            startListening()
            return
        }
        muted = !muted
        if (muted) {
            stopListening()
            tts?.stop()
            show(OrbState.Idle, R.string.twinai_voice_muted, R.string.twinai_voice_muted_hint)
        } else if (isOpen) {
            startListening()
        }
    }

    /** The screen calls this when Twin's answer is finished (or failed) after a spoken question. */
    fun onReplyFinished(failed: Boolean, reply: String?) {
        if (!awaitingReply) return
        awaitingReply = false
        if (!isOpen) return
        if (failed || reply.isNullOrBlank()) {
            show(OrbState.Error, R.string.twinai_voice_failed, R.string.twinai_voice_closing)
            closeLater(1_600)
            return
        }
        speak(reply)
    }

    internal fun shutdown() {
        exit()
        recognizer?.destroy()
        recognizer = null
        tts?.shutdown()
        tts = null
    }

    //  listening 

    private fun startListening() {
        if (!isOpen || muted || listening) return
        closeJob?.cancel()
        manualStop = false
        finalText = ""
        interimText = ""
        val engine = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
        engine.setRecognitionListener(listener)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        listening = true
        runCatching { engine.startListening(intent) }.onFailure {
            listening = false
            show(OrbState.Error, R.string.twinai_voice_cant_start, R.string.twinai_voice_try_again)
        }
    }

    private fun stopListening() {
        manualStop = true
        if (listening) runCatching { recognizer?.cancel() }
        listening = false
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = show(OrbState.Listening, R.string.twinai_voice_listening, null)
        override fun onBeginningOfSpeech() = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        override fun onRmsChanged(rmsdB: Float) {
            // rmsdB is roughly -2..10; smoothed like the web analyser so the orb does not flicker.
            val raw = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            level = level * 0.75f + raw * 0.25f
        }

        override fun onPartialResults(partialResults: Bundle?) {
            interimText = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
        }

        override fun onResults(results: Bundle?) {
            listening = false
            level = 0f
            if (manualStop || !isOpen) return
            val heard = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim().orEmpty()
            interimText = ""
            if (heard.isEmpty()) {
                show(OrbState.Idle, R.string.twinai_voice_ready, R.string.twinai_voice_closing)
                closeLater(1_200)
            } else {
                send(heard)
            }
        }

        override fun onError(error: Int) {
            listening = false
            level = 0f
            if (manualStop || !isOpen) return
            if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                show(OrbState.Error, R.string.twinai_voice_mic_needed, R.string.twinai_voice_type_hint)
            } else {
                // No speech, nothing matched, busy or network trouble: say so and close, never sit open.
                show(OrbState.Error, R.string.twinai_voice_no_hear, R.string.twinai_voice_closing)
            }
            closeLater(1_200)
        }
    }

    private fun send(question: String) {
        show(OrbState.Thinking, R.string.twinai_voice_thinking, null)
        finalText = question
        token++
        tts?.stop()
        if (submit(question)) {
            awaitingReply = true
        } else {
            show(OrbState.Error, R.string.twinai_voice_busy, R.string.twinai_voice_closing)
            closeLater(1_600)
        }
    }

    //  speaking 

    private fun speak(text: String) {
        val engine = tts ?: TextToSpeech(context.applicationContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }.also { tts = it; wire(it) }
        val mine = ++token
        val plain = text.replace(Regex("[*_`#>]"), "").replace(Regex("\\n{2,}"), ". ").replace(Regex("\\s+"), " ").trim()
        scope.launch {
            // A fresh engine needs a moment to start; wait for it, but never forever.
            var waited = 0
            while (!ttsReady && waited < 2_000) { delay(100); waited += 100 }
            if (mine != token || !isOpen) return@launch
            if (!ttsReady) { exit(); return@launch }
            engine.language = Locale.getDefault()
            finalText = plain
            interimText = ""
            show(OrbState.Speaking, R.string.twinai_voice_speaking, R.string.twinai_voice_speaking_hint)
            if (engine.speak(plain, TextToSpeech.QUEUE_FLUSH, null, "voice-$mine") == TextToSpeech.ERROR) {
                exit()
                return@launch
            }
            // Safety net only: some phones never report the end of speech.
            val words = plain.split(' ').count { it.isNotBlank() }
            val estimate = (words / 150.0 * 60_000 / 0.98).toLong()
            watchdog?.cancel()
            watchdog = scope.launch {
                delay((estimate + 3_000).coerceIn(4_000, 30_000))
                if (mine == token && isOpen) exit()
            }
        }
    }

    private fun wire(engine: TextToSpeech) {
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = finished(utteranceId)
            @Deprecated("Required by the abstract class", ReplaceWith("onError(utteranceId, -1)"))
            override fun onError(utteranceId: String?) = finished(utteranceId)
        })
    }

    /** Speech ended: close the voice screen, unless a newer turn (an interruption) already took over. */
    private fun finished(utteranceId: String?) {
        scope.launch {
            val id = utteranceId?.removePrefix("voice-")?.toIntOrNull()
            if (id == token && isOpen) exit()
        }
    }

    //  small helpers 

    private fun show(state: OrbState, @StringRes status: Int, @StringRes hint: Int?) {
        orb = state
        statusRes = status
        hintRes = hint
    }

    private fun closeLater(ms: Long) {
        closeJob?.cancel()
        closeJob = scope.launch {
            delay(ms)
            if (isOpen) exit()
        }
    }
}

@Composable
fun rememberTwinVoice(submit: (String) -> Boolean): TwinVoiceController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestSubmit = rememberUpdatedState(submit)
    val controller = remember { TwinVoiceController(context, scope) { latestSubmit.value(it) } }
    DisposableEffect(controller) { onDispose { controller.shutdown() } }
    return controller
}
