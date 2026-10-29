package com.twinhearts.app.feature.globe.ui.components

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
 * Why this file exists: records and plays voice notes with the Android framework (no new libraries),
 * the same way the Chat feature does. Voice notes are saved as AAC in .m4a files.
 */

/** A finished recording: the file and how many seconds it lasts. */
data class GlobeRecording(val file: File, val seconds: Int)

class GlobeVoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var startedAt = 0L

    /** `false` when the microphone could not be opened. */
    fun start(): Boolean {
        stopQuietly()
        val target = File(context.cacheDir, "globe-voice-${System.currentTimeMillis()}.m4a")
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
    fun finish(): GlobeRecording? {
        val media = recorder ?: return null
        val out = file
        recorder = null
        file = null
        val seconds = ((System.currentTimeMillis() - startedAt) / MS_PER_SECOND).toInt()
        return try {
            media.stop()
            media.release()
            out?.let { GlobeRecording(it, seconds) }
        } catch (e: RuntimeException) {
            media.release()
            out?.delete()
            null
        }
    }

    /** Stops and throws the recording away (the page was closed while recording). */
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

/** Plays one voice note at a time. [playing] is the url that plays now, or null. */
class GlobeVoicePlayer {
    var playing by mutableStateOf<String?>(null)
        private set
    private var player: MediaPlayer? = null

    /** Starts [url], or stops it when it is already playing. [onError] runs when it cannot be played. */
    fun toggle(url: String, onError: () -> Unit) {
        if (playing == url) {
            release()
            return
        }
        release()
        val media = MediaPlayer()
        try {
            media.setDataSource(url)
            media.setOnPreparedListener { it.start() }
            media.setOnCompletionListener { release() }
            media.setOnErrorListener { _, _, _ ->
                release()
                onError()
                true
            }
            player = media
            playing = url
            media.prepareAsync()
        } catch (e: Exception) {
            media.release()
            player = null
            playing = null
            onError()
        }
    }

    fun release() {
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
        playing = null
    }
}

@Composable
fun rememberGlobeVoicePlayer(): GlobeVoicePlayer {
    val player = remember { GlobeVoicePlayer() }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}

@Composable
fun rememberGlobeVoiceRecorder(context: Context): GlobeVoiceRecorder {
    val recorder = remember { GlobeVoiceRecorder(context) }
    DisposableEffect(recorder) { onDispose { recorder.cancel() } }
    return recorder
}
