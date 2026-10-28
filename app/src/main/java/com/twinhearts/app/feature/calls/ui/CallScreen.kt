package com.twinhearts.app.feature.calls.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.calls.domain.CallPhase
import com.twinhearts.app.feature.calls.domain.EndReason
import kotlinx.coroutines.delay

/**
 * Why this file exists: the full-screen call page (voice or video, outgoing or incoming).
 * [incoming] true means the engine is already ringing; false means this screen starts the call.
 */
@Composable
fun CallScreen(video: Boolean, incoming: Boolean, onClose: () -> Unit, viewModel: CallViewModel = hiltViewModel()) {
    val engine = viewModel.engine
    val state by engine.state.collectAsStateWithLifecycle()
    val close by rememberUpdatedState(onClose)
    var started by rememberSaveable { mutableStateOf(false) }

    val permissions = remember(video) {
        if (video) arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA) else arrayOf(Manifest.permission.RECORD_AUDIO)
    }
    var pendingAccept by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.all { it }) {
            if (pendingAccept) engine.accept() else engine.startCall(video)
        } else if (pendingAccept) {
            engine.decline()
        } else {
            engine.permissionDenied()
        }
    }

    // Outgoing: ask for permission, then start once. Incoming: the watcher already rang.
    LaunchedEffect(Unit) {
        if (!incoming && !started && !state.inCall) {
            started = true
            pendingAccept = false
            launcher.launch(permissions)
        }
    }
    // When the call is over, show the reason for a moment, then leave.
    LaunchedEffect(state.phase) {
        if (state.phase == CallPhase.Ended) {
            delay(1_200)
            engine.acknowledgeEnded()
            close()
        }
    }

    val colors = TwinTheme.colors
    TwinBackground {
        Box(Modifier.fillMaxSize()) {
            if (state.video && state.remoteVideo != null && state.phase == CallPhase.Connected) {
                RtcVideoView(state.remoteVideo, state.egl, mirror = false, modifier = Modifier.fillMaxSize())
            }
            val showAvatar = !(state.video && state.remoteVideo != null && state.phase == CallPhase.Connected)
            Column(
                modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(TwinTheme.spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(TwinTheme.spacing.xxl))
                if (showAvatar) {
                    Box(
                        Modifier.size(112.dp).clip(CircleShape).background(colors.primaryBrush),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(state.partnerName.take(1).uppercase(), style = MaterialTheme.typography.displayMedium, color = colors.ink)
                    }
                    Spacer(Modifier.height(TwinTheme.spacing.l))
                }
                Text(state.partnerName, style = MaterialTheme.typography.headlineMedium, color = colors.ink, textAlign = TextAlign.Center)
                Text(statusLine(state.phase, state.video, state.seconds, state.endReason), style = MaterialTheme.typography.bodyLarge, color = colors.inkMuted)
                Spacer(Modifier.weight(1f))

                if (state.phase == CallPhase.Incoming) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        RoundButton(Icons.Rounded.CallEnd, stringResource(R.string.calls_decline), colors.error, onClick = { engine.decline() })
                        RoundButton(
                            if (state.video) Icons.Rounded.Videocam else Icons.Rounded.Call,
                            stringResource(R.string.calls_accept), colors.success,
                            onClick = { pendingAccept = true; launcher.launch(permissions) },
                        )
                    }
                } else if (state.phase != CallPhase.Ended) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ToggleButton(if (state.muted) Icons.Rounded.MicOff else Icons.Rounded.Mic, stringResource(R.string.calls_mute), state.muted) { engine.toggleMute() }
                        ToggleButton(Icons.Rounded.VolumeUp, stringResource(R.string.calls_speaker), state.speaker) { engine.toggleSpeaker() }
                        if (state.video) {
                            ToggleButton(if (state.cameraOff) Icons.Rounded.VideocamOff else Icons.Rounded.Videocam, stringResource(R.string.calls_camera), state.cameraOff) { engine.toggleCamera() }
                            ToggleButton(Icons.Rounded.Cameraswitch, stringResource(R.string.calls_flip), false) { engine.switchCamera() }
                        }
                    }
                    Spacer(Modifier.height(TwinTheme.spacing.xl))
                    RoundButton(Icons.Rounded.CallEnd, stringResource(R.string.calls_end), colors.error, onClick = { engine.hangUp() })
                }
            }
            if (state.video && state.localVideo != null && state.phase != CallPhase.Ended) {
                RtcVideoView(
                    state.localVideo, state.egl, mirror = true, overlay = true,
                    modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(TwinTheme.spacing.l)
                        .width(104.dp).aspectRatio(0.62f).clip(TwinTheme.shapes.small)
                        .border(1.dp, colors.lineSoft, TwinTheme.shapes.small),
                )
            }
        }
    }
}

@Composable
private fun statusLine(phase: CallPhase, video: Boolean, seconds: Int, reason: EndReason?): String = when (phase) {
    CallPhase.Calling -> stringResource(R.string.calls_calling)
    CallPhase.Incoming -> stringResource(if (video) R.string.calls_incoming_video else R.string.calls_incoming_voice)
    CallPhase.Connecting -> stringResource(R.string.calls_connecting)
    CallPhase.Connected -> "%d:%02d".format(seconds / 60, seconds % 60)
    CallPhase.Ended -> stringResource(
        when (reason) {
            EndReason.Declined -> R.string.calls_declined
            EndReason.NoAnswer -> R.string.calls_no_answer
            EndReason.Missed -> R.string.calls_missed
            EndReason.Failed -> R.string.calls_failed
            EndReason.PermissionDenied -> R.string.calls_permission
            else -> R.string.calls_ended
        },
    )
    CallPhase.Idle -> ""
}

@Composable
private fun RoundButton(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = spacedBy(TwinTheme.spacing.xs)) {
        Box(Modifier.size(68.dp).clip(CircleShape).background(tint).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = TwinTheme.colors.background, modifier = Modifier.size(30.dp))
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = TwinTheme.colors.inkMuted)
    }
}

@Composable
private fun ToggleButton(icon: ImageVector, label: String, on: Boolean, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = spacedBy(TwinTheme.spacing.xs)) {
        Box(
            Modifier.size(56.dp).clip(CircleShape)
                .background(if (on) colors.ink else colors.glass)
                .border(1.dp, colors.lineSoft, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = if (on) colors.background else colors.ink)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
    }
}
