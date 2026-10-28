package com.twinhearts.app.feature.music.data

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/**
 * Why this file exists: wraps Android's MediaRecorder for voice notes and karaoke takes
 * (AAC in an .m4a file). Only one recording can run at a time.
 */
class VoiceRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var startedAt = 0L

    val isRecording: Boolean get() = recorder != null

    /** Starts recording into a new cache file. Returns false if the microphone could not be opened. */
    fun start(): Boolean {
        if (recorder != null) return true
        val target = File(context.cacheDir, "music_take_${System.currentTimeMillis()}.m4a")
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        return try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(128_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(target.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            file = target
            startedAt = System.currentTimeMillis()
            true
        } catch (e: java.io.IOException) {
            r.release()
            target.delete()
            false
        } catch (e: RuntimeException) {
            r.release()
            target.delete()
            false
        }
    }

    /** Stops and returns the finished file with its length, or null if it was too short to keep. */
    fun stop(): Pair<File, Int>? {
        val r = recorder ?: return null
        val f = file
        recorder = null
        file = null
        val seconds = ((System.currentTimeMillis() - startedAt) / 1000L).toInt()
        return try {
            r.stop()
            r.release()
            if (f != null && f.length() > 0L) f to seconds.coerceAtLeast(1) else null
        } catch (e: RuntimeException) {
            // MediaRecorder throws when stopped right after start; there is nothing to keep.
            r.release()
            f?.delete()
            null
        }
    }

    /** Throws the take away. */
    fun cancel() {
        val r = recorder ?: return
        val f = file
        recorder = null
        file = null
        try {
            r.stop()
        } catch (ignored: RuntimeException) {
            // already unusable: just release below
        }
        r.release()
        f?.delete()
    }
}
