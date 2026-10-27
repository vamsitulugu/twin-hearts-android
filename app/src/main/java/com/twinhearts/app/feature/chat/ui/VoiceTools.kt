package com.twinhearts.app.feature.chat.ui

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.io.File

/*
 * Why this file exists: records and plays voice messages with the Android framework (no new libraries).
 */

/** A finished recording: the file and how many seconds it lasts. */
data class Recording(val file: File, val seconds: Int)

/**
 * Why this file exists: records a voice message as AAC in an .m4a file (the server stores
 * "audio/mp4" as m4a). One recorder = one recording at a time.
 */
class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var startedAt = 0L

    /** `false` when the microphone could not be opened. */
    fun start(): Boolean {
        stopQuietly()
        val target = File(context.cacheDir, "voice-${System.currentTimeMillis()}.m4a")
        val media = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        return try {
            media.setAudioSource(MediaRecorder.AudioSource.MIC)
            media.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            media.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            media.setAudioEncodingBitRate(BIT_RATE)
            media.setAudioSamplingRate(SAMPLE_RATE)
            media.setOutputFile(target.absolutePath)
            media.prepare()
            media.start()
            recorder = media
            file = target
            startedAt = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            media.release()
            target.delete()
            false
        }
    }

    /** Stops and returns the recording, or `null` when it failed. */
    fun finish(): Recording? {
        val media = recorder ?: return null
        val out = file
        recorder = null
        file = null
        val seconds = ((System.currentTimeMillis() - startedAt) / MS_PER_SECOND).toInt()
        return try {
            media.stop()
            media.release()
            out?.let { Recording(it, seconds) }
        } catch (e: RuntimeException) {
            media.release()
            out?.delete()
            null
        }
    }

    /** Stops and throws the recording away. */
    fun cancel() {
        val out = file
        stopQuietly()
        out?.delete()
    }

    private fun stopQuietly() {
        recorder?.let { runCatching { it.stop() }; it.release() }
        recorder = null
        file = null
    }

    private companion object {
        const val BIT_RATE = 64_000
        const val SAMPLE_RATE = 44_100
        const val MS_PER_SECOND = 1000L
    }
}

/** Plays one voice or audio message at a time. [playing] is the url that is playing now (or null). */
class VoicePlayerState {
    var playing by mutableStateOf<String?>(null)
        private set
    private var player: MediaPlayer? = null

    /** Playback speed the person picked (1x, 1.5x, 2x); it stays for the next voice message too. */
    var speed by mutableStateOf(SPEEDS.first())
        private set

    /** "1", "1.5" or "2": the number shown next to the multiplication sign. */
    val speedLabel: String get() = if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()

    fun cycleSpeed() {
        speed = SPEEDS[(SPEEDS.indexOf(speed) + 1) % SPEEDS.size]
        player?.let { applySpeed(it) }
    }

    private fun applySpeed(media: MediaPlayer) {
        runCatching { media.playbackParams = media.playbackParams.setSpeed(speed) }
    }

    fun toggle(url: String, onFailed: () -> Unit) {
        if (playing == url) {
            stop()
            return
        }
        stop()
        val media = MediaPlayer()
        try {
            media.setDataSource(url)
            media.setOnCompletionListener { stop() }
            media.setOnErrorListener { _, _, _ -> stop(); onFailed(); true }
            media.setOnPreparedListener { it.start(); applySpeed(it) }
            media.prepareAsync()
            player = media
            playing = url
        } catch (e: Exception) {
            media.release()
            onFailed()
        }
    }

    private companion object {
        val SPEEDS = listOf(1f, 1.5f, 2f)
    }

    fun stop() {
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
        playing = null
    }
}

@Composable
fun rememberVoicePlayer(): VoicePlayerState {
    val state = remember { VoicePlayerState() }
    DisposableEffect(Unit) { onDispose { state.stop() } }
    return state
}
