package com.twinhearts.app.feature.globe.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.twinhearts.app.R
import kotlinx.coroutines.delay
import java.io.File

/*
 * Why this file exists: the photo picker, the video picker and the voice recorder are used by both
 * the Pin a Memory form and the memory detail sheet. These helpers hold the permission and picker
 * plumbing once. The system photo picker needs no storage permission.
 */

private const val MAX_PHOTO_PICKS = 20

class MediaPickers(val pickPhotos: () -> Unit, val pickVideo: () -> Unit)

@Composable
fun rememberMediaPickers(onPhotos: (List<String>) -> Unit, onVideo: (String) -> Unit): MediaPickers {
    val photosDone by rememberUpdatedState(onPhotos)
    val videoDone by rememberUpdatedState(onVideo)
    val photos = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTO_PICKS)) { uris ->
        if (uris.isNotEmpty()) photosDone(uris.map { it.toString() })
    }
    val video = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) videoDone(uri.toString())
    }
    return remember(photos, video) {
        MediaPickers(
            pickPhotos = { photos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            pickVideo = { video.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
        )
    }
}

class VoiceControl(val recording: Boolean, val toggle: () -> Unit)

/**
 * Tap once to record, tap again to stop. [maxSeconds] stops a recording by itself (30 s for notes on
 * an existing memory, like the web app); null means no limit. [onMessage] gets a string resource to show.
 */
@Composable
fun rememberVoiceControl(
    maxSeconds: Int?,
    onFinished: (File, Int) -> Unit,
    onMessage: (Int) -> Unit,
): VoiceControl {
    val context = LocalContext.current
    val recorder = rememberGlobeVoiceRecorder(context)
    var recording by remember { mutableStateOf(false) }
    val finishedCallback by rememberUpdatedState(onFinished)
    val messageCallback by rememberUpdatedState(onMessage)

    fun stop() {
        recording = false
        recorder.finish()?.let { finishedCallback(it.file, it.seconds) }
    }

    fun begin() {
        if (recorder.start()) {
            recording = true
            messageCallback(R.string.globe_toast_recording)
        } else {
            messageCallback(R.string.globe_toast_no_mic)
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) begin() else messageCallback(R.string.globe_toast_no_mic)
    }

    LaunchedEffect(recording) {
        if (!recording || maxSeconds == null) return@LaunchedEffect
        delay(maxSeconds * MS_PER_SECOND)
        if (recording) stop()
    }

    return VoiceControl(recording) {
        when {
            recording -> stop()
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> begin()
            else -> permission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

private const val MS_PER_SECOND = 1000L
