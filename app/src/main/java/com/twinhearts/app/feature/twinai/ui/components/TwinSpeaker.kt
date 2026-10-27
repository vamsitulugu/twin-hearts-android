package com.twinhearts.app.feature.twinai.ui.components

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Why this file exists: the "🔊 Listen" button reads Twin's answer aloud with the phone's own
 * text-to-speech voice (free, no key). Only one answer speaks at a time.
 */
class TwinSpeaker internal constructor(context: Context, private val onError: () -> Unit) {
    /** Id of the message being read now, or null. The UI turns "Listen" into "Stop" for it. */
    var speakingId by mutableStateOf<String?>(null)
        private set

    /** Called on the main thread when an answer has been read to the end (voice mode then listens again). */
    var onFinished: (() -> Unit)? = null

    private val main = Handler(Looper.getMainLooper())
    private var ready = false
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) tts?.language = Locale.getDefault()
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) { speakingId = null; main.post { onFinished?.invoke() } }
            @Deprecated("Required by the abstract class", ReplaceWith("onError(utteranceId, -1)"))
            override fun onError(utteranceId: String?) { speakingId = null; main.post { onFinished?.invoke() } }
        })
    }

    fun toggle(id: String, text: String) {
        if (speakingId == id) {
            stop()
            return
        }
        val engine = tts
        if (!ready || engine == null) {
            onError()
            return
        }
        engine.stop()
        speakingId = id
        val plain = text.replace(Regex("[*#`>]"), "").replace(Regex("\\s+"), " ").trim()
        if (engine.speak(plain, TextToSpeech.QUEUE_FLUSH, null, id) == TextToSpeech.ERROR) {
            speakingId = null
            onError()
        }
    }

    fun stop() {
        tts?.stop()
        speakingId = null
    }

    internal fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

@Composable
fun rememberSpeaker(onError: () -> Unit): TwinSpeaker {
    val context = LocalContext.current
    val speaker = remember { TwinSpeaker(context, onError) }
    DisposableEffect(speaker) { onDispose { speaker.shutdown() } }
    return speaker
}
