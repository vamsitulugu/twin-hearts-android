package com.twinhearts.app.feature.collection.ui

import android.media.AudioAttributes
import android.media.MediaPlayer

/** One-at-a-time voice playback with the platform MediaPlayer (no extra dependency). */
class VoicePlayer(
    private val onStateChange: (playingId: String?) -> Unit,
    private val onError: () -> Unit,
) {
    private var player: MediaPlayer? = null
    private var currentId: String? = null

    /** Plays [url] for [id]; tapping the item that is playing stops it. */
    fun toggle(id: String, url: String) {
        if (currentId == id) {
            stop()
            return
        }
        stop()
        val mp = MediaPlayer()
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build(),
            )
            mp.setDataSource(url)
            mp.setOnPreparedListener { it.start() }
            mp.setOnCompletionListener { stop() }
            mp.setOnErrorListener { _, _, _ ->
                stop()
                onError()
                true
            }
            player = mp
            currentId = id
            onStateChange(id)
            mp.prepareAsync()
        } catch (e: Exception) {
            mp.release()
            player = null
            currentId = null
            onStateChange(null)
            onError()
        }
    }

    fun stop() {
        player?.let {
            try { it.stop() } catch (_: IllegalStateException) { }
            it.release()
        }
        player = null
        currentId = null
        onStateChange(null)
    }
}
