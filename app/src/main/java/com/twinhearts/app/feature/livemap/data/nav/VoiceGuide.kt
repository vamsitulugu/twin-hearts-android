package com.twinhearts.app.feature.livemap.data.nav

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads directions aloud with Android's built-in text-to-speech (no key, works offline once a
 * voice is installed). The music lowers while the sentence is spoken, like Google Maps.
 * Created on first use and released when the trip ends.
 */
@Singleton
class VoiceGuide @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var tts: TextToSpeech? = null
    private var ready = false
    private val pending = ArrayDeque<String>()
    private var focus: AudioFocusRequest? = null
    private val audio get() = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private fun ensure() {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            val engine = tts
            if (status == TextToSpeech.SUCCESS && engine != null) {
                val result = engine.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.setLanguage(Locale.ENGLISH)
                }
                engine.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) = abandonFocus()
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) = abandonFocus()
                })
                ready = true
                while (pending.isNotEmpty()) say(pending.removeFirst())
            }
        }
    }

    /** Says [text] now, replacing anything still being spoken: an old instruction is useless. */
    fun speak(text: String) {
        ensure()
        if (!ready) {
            pending.clear()
            pending.addLast(text)
            return
        }
        say(text)
    }

    private fun say(text: String) {
        requestFocus()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nav-${System.nanoTime()}")
    }

    private fun requestFocus() {
        val manager = audio ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                ).build()
            focus = request
            manager.requestAudioFocus(request)
        }
    }

    private fun abandonFocus() {
        val manager = audio ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focus?.let { manager.abandonAudioFocusRequest(it) }
            focus = null
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        abandonFocus()
    }

    fun release() {
        pending.clear()
        abandonFocus()
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
