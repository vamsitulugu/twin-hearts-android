package com.twinhearts.app.feature.calls.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.rememberReduceMotion
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.calls.domain.CallPhase
import com.twinhearts.app.feature.calls.data.UpgradeNotice
import com.twinhearts.app.feature.calls.domain.EndReason
import com.twinhearts.app.feature.calls.domain.VideoUpgrade
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

    // Voice call -> video: the camera permission is asked first, then the request goes out (or the answer).
    var upgradeAction by remember { mutableStateOf(UpgradeAction.Request) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            if (upgradeAction == UpgradeAction.Accept) engine.declineVideoUpgrade()
        } else if (upgradeAction == UpgradeAction.Accept) {
            engine.acceptVideoUpgrade()
        } else {
            engine.requestVideoUpgrade()
        }
    }
    val context = LocalContext.current
    LaunchedEffect(state.upgradeNotice) {
        val notice = state.upgradeNotice ?: return@LaunchedEffect
        val text = when (notice) {
            UpgradeNotice.Declined -> context.getString(R.string.calls_upgrade_declined, state.partnerName)
            UpgradeNotice.Failed -> context.getString(R.string.calls_upgrade_failed)
        }
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        engine.clearUpgradeNotice()
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

    val videoLive = state.video && state.remoteVideo != null && state.phase == CallPhase.Connected
    val ringing = state.phase == CallPhase.Calling || state.phase == CallPhase.Incoming || state.phase == CallPhase.Connecting
    Box(Modifier.fillMaxSize().background(CallBackground)) {
        if (videoLive) {
            RtcVideoView(state.remoteVideo, state.egl, mirror = false, modifier = Modifier.fillMaxSize())
            // Dark fades top and bottom so the name, timer and buttons stay readable over any picture.
            Box(Modifier.fillMaxWidth().height(180.dp).align(Alignment.TopCenter).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.60f), Color.Transparent))))
            Box(Modifier.fillMaxWidth().height(280.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)))))
        }
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = TwinTheme.spacing.xl, vertical = TwinTheme.spacing.l),
            horizontalAlignment = if (videoLive) Alignment.Start else Alignment.CenterHorizontally,
        ) {
            if (videoLive) {
                // Video call: name + timer sit in the top-left corner, like WhatsApp.
                Text(state.partnerName, style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text(statusLine(state.phase, state.video, state.seconds, state.endReason), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.78f))
                if (state.reconnecting) Text(stringResource(R.string.calls_reconnecting), style = MaterialTheme.typography.labelMedium, color = CallAmber)
            } else {
                Spacer(Modifier.height(TwinTheme.spacing.xxl))
                PulsingAvatar(name = state.partnerName, pulsing = ringing)
                Spacer(Modifier.height(TwinTheme.spacing.xl))
                Text(state.partnerName, style = MaterialTheme.typography.headlineLarge, color = Color.White, textAlign = TextAlign.Center)
                Spacer(Modifier.height(TwinTheme.spacing.xs))
                Text(statusLine(state.phase, state.video, state.seconds, state.endReason), style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.70f))
                if (state.reconnecting) Text(stringResource(R.string.calls_reconnecting), style = MaterialTheme.typography.labelLarge, color = CallAmber)
                if (state.upgrade == VideoUpgrade.Requested) Text(stringResource(R.string.calls_upgrade_asking, state.partnerName), style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.78f))
            }
            Spacer(Modifier.weight(1f))
            if (state.upgrade == VideoUpgrade.Asked) {
                UpgradeBanner(
                    text = stringResource(R.string.calls_upgrade_request, state.partnerName),
                    onNotNow = { engine.declineVideoUpgrade() },
                    onTurnOn = { upgradeAction = UpgradeAction.Accept; cameraLauncher.launch(Manifest.permission.CAMERA) },
                )
                Spacer(Modifier.height(TwinTheme.spacing.m))
            }

            if (state.phase == CallPhase.Incoming) {
                Row(Modifier.fillMaxWidth().padding(bottom = TwinTheme.spacing.xl), horizontalArrangement = Arrangement.SpaceEvenly) {
                    RoundButton(Icons.Rounded.CallEnd, stringResource(R.string.calls_decline), CallRed, onClick = { engine.decline() })
                    RoundButton(
                        if (state.video) Icons.Rounded.Videocam else Icons.Rounded.Call,
                        stringResource(R.string.calls_accept), CallGreen,
                        onClick = { pendingAccept = true; launcher.launch(permissions) },
                    )
                }
            } else if (state.phase != CallPhase.Ended) {
                // One glass panel for the in-call controls, then a big red End button under it.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(PanelShape)
                        .background(Color.White.copy(alpha = if (videoLive) 0.14f else 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), PanelShape)
                        .padding(vertical = TwinTheme.spacing.l, horizontal = TwinTheme.spacing.m),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ToggleButton(if (state.muted) Icons.Rounded.MicOff else Icons.Rounded.Mic, stringResource(R.string.calls_mute), state.muted) { engine.toggleMute() }
                    ToggleButton(Icons.Rounded.VolumeUp, stringResource(R.string.calls_speaker), state.speaker) { engine.toggleSpeaker() }
                    if (!state.video && state.phase == CallPhase.Connected) {
                        ToggleButton(Icons.Rounded.Videocam, stringResource(R.string.calls_video), state.upgrade == VideoUpgrade.Requested) {
                            upgradeAction = UpgradeAction.Request
                            cameraLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                    if (state.video) {
                        ToggleButton(if (state.cameraOff) Icons.Rounded.VideocamOff else Icons.Rounded.Videocam, stringResource(R.string.calls_camera), state.cameraOff) { engine.toggleCamera() }
                        ToggleButton(Icons.Rounded.Cameraswitch, stringResource(R.string.calls_flip), false) { engine.switchCamera() }
                    }
                }
                Spacer(Modifier.height(TwinTheme.spacing.xl))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    RoundButton(Icons.Rounded.CallEnd, stringResource(R.string.calls_end), CallRed, onClick = { engine.hangUp() })
                }
                Spacer(Modifier.height(TwinTheme.spacing.s))
            }
        }
        if (state.video && state.localVideo != null && state.phase != CallPhase.Ended) {
            RtcVideoView(
                state.localVideo, state.egl, mirror = true, overlay = true,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(TwinTheme.spacing.l)
                    .width(108.dp).aspectRatio(0.62f).clip(RoundedCornerShape(18.dp))
                    .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
            )
        }
    }
}

private val CallBackground = Brush.verticalGradient(listOf(Color(0xFF4A0A2C), Color(0xFF210716), Color(0xFF0C0308)))
private val CallRed = Color(0xFFE5484D)
private val CallAmber = Color(0xFFFFC857)
private val CallGreen = Color(0xFF22C55E)
private val PanelShape = RoundedCornerShape(32.dp)

/** Avatar with two soft rings that expand and fade while the phone is ringing (still when animations are off). */
@Composable
private fun PulsingAvatar(name: String, pulsing: Boolean) {
    val reduceMotion = rememberReduceMotion()
    val transition = rememberInfiniteTransition(label = "callPulse")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "callPulsePhase",
    )
    Box(Modifier.size(AvatarOuter), contentAlignment = Alignment.Center) {
        if (pulsing && !reduceMotion) {
            listOf(0f, 0.5f).forEach { offset ->
                Box(
                    Modifier
                        .size(AvatarSize)
                        .graphicsLayer {
                            val t = (phase.value + offset) % 1f
                            val scale = 1f + 0.55f * t
                            scaleX = scale
                            scaleY = scale
                            alpha = 0.40f * (1f - t)
                        }
                        .border(2.dp, Color.White, CircleShape),
                )
            }
        }
        Box(
            Modifier
                .size(AvatarSize)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFFB0127A), Color(0xFF6B093A))))
                .border(2.dp, Color.White.copy(alpha = 0.22f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(name.take(1).uppercase(), style = MaterialTheme.typography.displayMedium, color = Color.White)
        }
    }
}

private val AvatarSize = 128.dp
private val AvatarOuter = 200.dp

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
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(tint).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.78f))
    }
}

@Composable
private fun ToggleButton(icon: ImageVector, label: String, on: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
        Box(
            Modifier.size(58.dp).clip(CircleShape)
                .background(if (on) Color.White else Color.White.copy(alpha = 0.12f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = if (on) Color(0xFF1A0612) else Color.White, modifier = Modifier.size(26.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.78f))
    }
}

private enum class UpgradeAction { Request, Accept }

/** "<name> wants to turn on video" with Not now / Turn on (the web app's upgrade banner). */
@Composable
private fun UpgradeBanner(text: String, onNotNow: () -> Unit, onTurnOn: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(PanelShape).background(Color.White.copy(alpha = 0.14f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), PanelShape)
            .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Color.White, modifier = Modifier.weight(1f))
        Text(
            stringResource(R.string.calls_upgrade_not_now), style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.78f),
            modifier = Modifier.clickable(onClick = onNotNow).padding(TwinTheme.spacing.s),
        )
        Text(
            stringResource(R.string.calls_upgrade_turn_on), style = MaterialTheme.typography.labelLarge, color = CallGreen,
            modifier = Modifier.clickable(onClick = onTurnOn).padding(TwinTheme.spacing.s),
        )
    }
}
