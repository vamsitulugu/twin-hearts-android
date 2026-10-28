package com.twinhearts.app.feature.calls.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.calls.domain.CallPhase
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Why this file exists: lets the movie player start a voice or video call and shows the small
 * draggable call window over the movie (the web page's mini call bar and mini video window).
 * It reuses the app-wide [com.twinhearts.app.feature.calls.data.CallEngine]; no second call system.
 */

/** Returns a function that asks for the microphone (and camera for video), then starts the call. */
@Composable
fun rememberCallStarter(viewModel: CallViewModel = hiltViewModel()): (video: Boolean) -> Unit {
    val engine = viewModel.engine
    var wantVideo by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.all { it }) engine.startCall(wantVideo) else engine.permissionDenied()
    }
    return remember(engine, launcher) {
        { video: Boolean ->
            if (!engine.state.value.inCall) {
                wantVideo = video
                launcher.launch(
                    if (video) arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
                    else arrayOf(Manifest.permission.RECORD_AUDIO),
                )
            }
        }
    }
}

/** The small call window. Draws nothing when there is no call. Drag it anywhere over the movie. */
@Composable
fun CallBubble(modifier: Modifier = Modifier, viewModel: CallViewModel = hiltViewModel()) {
    val engine = viewModel.engine
    val state by engine.state.collectAsStateWithLifecycle()
    // The call page normally closes the "ended" state; here we do it ourselves after a short moment.
    LaunchedEffect(state.phase) {
        if (state.phase == CallPhase.Ended) {
            delay(ENDED_MS)
            engine.acknowledgeEnded()
        }
    }
    if (!state.inCall) return

    val colors = TwinTheme.colors
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val showVideo = state.video && state.remoteVideo != null && state.phase == CallPhase.Connected
    val shape = TwinTheme.shapes.small
    Column(
        modifier.offset { IntOffset(dragX.roundToInt(), dragY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    dragX += drag.x
                    dragY += drag.y
                }
            }
            .width(if (showVideo) VIDEO_W.dp else VOICE_W.dp)
            .clip(shape).background(colors.scrim).border(1.dp, colors.lineSoft, shape),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showVideo) {
            RtcVideoView(state.remoteVideo, state.egl, mirror = false, overlay = true, modifier = Modifier.size(VIDEO_W.dp, VIDEO_H.dp))
        } else {
            Box(Modifier.padding(top = 10.dp).size(40.dp).clip(CircleShape).background(colors.primaryBrush), contentAlignment = Alignment.Center) {
                Text(state.partnerName.take(1).uppercase(), color = colors.ink, style = MaterialTheme.typography.titleMedium)
            }
        }
        Text(
            when (state.phase) {
                CallPhase.Connected -> "%d:%02d".format(state.seconds / 60, state.seconds % 60)
                CallPhase.Calling -> stringResource(R.string.calls_calling)
                else -> stringResource(R.string.calls_connecting)
            },
            color = colors.ink, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp),
        )
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniButton(if (state.muted) Icons.Rounded.MicOff else Icons.Rounded.Mic, stringResource(R.string.calls_mute), colors.glass) { engine.toggleMute() }
            MiniButton(Icons.Rounded.CallEnd, stringResource(R.string.calls_end), colors.error) { engine.hangUp() }
        }
    }
}

@Composable
private fun MiniButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Box(Modifier.size(34.dp).clip(CircleShape).background(tint).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = TwinTheme.colors.ink, modifier = Modifier.size(18.dp))
    }
}

private const val ENDED_MS = 1_200L
private const val VIDEO_W = 120
private const val VIDEO_H = 160
private const val VOICE_W = 120
