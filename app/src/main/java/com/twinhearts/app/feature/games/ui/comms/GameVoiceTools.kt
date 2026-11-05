package com.twinhearts.app.feature.games.ui.comms

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
 * Why this file exists: records and plays the voice messages of the in-game chat with the Android
 * framework (no new libraries). Games keeps its own small copy instead of importing Chat (R-83).
 */

/** A finished recording: the file and how long it lasted in milliseconds. */
data class GameRecording(val file: File, val millis: Long)

/** Records one voice message at a time as AAC in an .m4a file (the server stores "audio/mp4" as m4a). */
class GameVoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var startedAt = 0L

    /** `false` when the microphone could not be opened (for example a call has it). */
    fun start(): Boolean {
        stopQuietly()
        val target = File(context.cacheDir, "game-voice-${System.currentTimeMillis()}.m4a")
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
    fun finish(): GameRecording? {
        val media = recorder ?: return null
        val out = file
        recorder = null
        file = null
        val millis = System.currentTimeMillis() - startedAt
        return try {
            media.stop()
            media.release()
            out?.let { GameRecording(it, millis) }
        } catch (e: RuntimeException) {
            // MediaRecorder throws when it was stopped too soon to have any sound.
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
    }
}

/** Plays one voice message at a time. [playing] is the address (or file) that plays now, or null. */
class GameVoicePlayer {
    var playing by mutableStateOf<String?>(null)
        private set
    private var player: MediaPlayer? = null

    fun toggle(source: String, onFailed: () -> Unit) {
        if (playing == source) {
            stop()
            return
        }
        stop()
        val media = MediaPlayer()
        try {
            media.setDataSource(source)
            media.setOnCompletionListener { stop() }
            media.setOnErrorListener { _, _, _ -> stop(); onFailed(); true }
            media.setOnPreparedListener { it.start() }
            media.prepareAsync()
            player = media
            playing = source
        } catch (e: Exception) {
            media.release()
            onFailed()
        }
    }

    fun stop() {
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
        playing = null
    }
}

@Composable
fun rememberGameVoicePlayer(): GameVoicePlayer {
    val state = remember { GameVoicePlayer() }
    DisposableEffect(Unit) { onDispose { state.stop() } }
    return state
}
