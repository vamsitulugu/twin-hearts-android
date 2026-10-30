package com.twinhearts.app.feature.calls.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.feature.calls.domain.CallPhase
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Why this file exists: the call pieces of the Watch Together player, drawn like the web page
 * (movie.html "mini call UI"): a voice bar that folds into a small pill, a small portrait video
 * window with a runtime pill and a vertical menu under it, and a round bubble when minimised.
 * It reuses the app-wide CallEngine; no second call system. Draws nothing when there is no call.
 */
@Composable
fun WatchCallOverlay(modifier: Modifier = Modifier, viewModel: CallViewModel = hiltViewModel()) {
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

    var voiceFolded by remember { mutableStateOf(false) }
    var minimized by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val time = "%02d:%02d".format(state.seconds / 60, state.seconds % 60)
    val letter = state.partnerName.take(1).uppercase().ifEmpty { "P" }
    val partner = state.partnerName.ifEmpty { stringResource(R.string.movie_partner) }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val maxWpx = with(density) { maxWidth.toPx() }
        val maxHpx = with(density) { maxHeight.toPx() }
        if (state.video) {
            if (minimized) {
                DraggableAnchor(maxWpx, maxHpx, with(density) { BUBBLE.dp.toPx() }, with(density) { BUBBLE.dp.toPx() }, onTap = { minimized = false }) {
                    Box(
                        Modifier.size(BUBBLE.dp).clip(CircleShape).background(Color(0xFF111111))
                            .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.remoteVideo != null) {
                            RtcVideoView(state.remoteVideo, state.egl, mirror = false, overlay = true, modifier = Modifier.fillMaxSize())
                        } else {
                            Text(letter, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                val frameH = minOf(WINDOW_H.dp, maxHeight * WINDOW_MAX_FRACTION)
                val frameW = frameH * WINDOW_ASPECT
                val frameHpx = with(density) { frameH.toPx() }
                val frameWpx = with(density) { frameW.toPx() }
                DraggableAnchor(maxWpx, maxHpx, frameWpx, frameHpx, onTap = null) { offsetY ->
                    // Open downwards unless there is no room, then open upwards (the web page does the same).
                    val opensUp = offsetY + frameHpx + with(density) { (PILL_H + MENU_ESTIMATE).dp.toPx() } > maxHpx
                    Column(Modifier.width(frameW), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (opensUp) VideoMenu(menuOpen, state.muted, state.speaker, onClose = { menuOpen = false },
                            onMute = { engine.toggleMute() }, onSpeaker = { engine.toggleSpeaker() },
                            onSwap = { engine.switchCamera() }, onMinimize = { menuOpen = false; minimized = true },
                            onEnd = { menuOpen = false; engine.hangUp() })
                        Box(
                            Modifier.size(frameW, frameH).clip(RoundedCornerShape(16.dp)).background(Color(0xFF111111))
                                .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (state.remoteVideo != null) {
                                RtcVideoView(state.remoteVideo, state.egl, mirror = false, overlay = true, modifier = Modifier.fillMaxSize())
                            } else {
                                Text(letter, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        RuntimePill(time, onClick = { menuOpen = !menuOpen }, modifier = Modifier.padding(top = 6.dp))
                        if (!opensUp) VideoMenu(menuOpen, state.muted, state.speaker, onClose = { menuOpen = false },
                            onMute = { engine.toggleMute() }, onSpeaker = { engine.toggleSpeaker() },
                            onSwap = { engine.switchCamera() }, onMinimize = { menuOpen = false; minimized = true },
                            onEnd = { menuOpen = false; engine.hangUp() })
                    }
                }
            }
        } else if (voiceFolded) {
            Row(
                Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 10.dp, end = 12.dp)
                    .clip(CircleShape).background(Color(0xFF0F0F0F).copy(alpha = 0.7f)).clickable { voiceFolded = false }
                    .semantics { role = Role.Button }.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Rounded.Call, stringResource(R.string.movie_call_expand), tint = CallGreen, modifier = Modifier.size(14.dp))
                Text(time, color = Color.White, fontSize = 12.sp)
            }
        } else {
            // The whole bar folds it; the three real buttons keep their own taps.
            Row(
                Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 10.dp, start = 12.dp, end = 12.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)).background(Color(0xFF0F0F0F).copy(alpha = 0.7f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .clickable { voiceFolded = true }.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Call, null, tint = CallGreen, modifier = Modifier.size(16.dp))
                Text(partner, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(time, color = Color(0xFFCCCCCC), fontSize = 12.sp)
                BarButton(if (state.muted) Icons.Rounded.MicOff else Icons.Rounded.Mic, stringResource(R.string.movie_call_mute), Color.White.copy(alpha = 0.1f)) { engine.toggleMute() }
                BarButton(if (state.speaker) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff, stringResource(R.string.movie_call_speaker), Color.White.copy(alpha = 0.1f)) { engine.toggleSpeaker() }
                BarButton(Icons.Rounded.CallEnd, stringResource(R.string.movie_call_end), CallEndRed) { engine.hangUp() }
            }
        }
    }
}

/**
 * Holds something that starts at the top-right corner, can be dragged around and snaps to the left or
 * right edge when let go (like the web page). A plain tap runs [onTap] when given.
 */
@Composable
private fun androidx.compose.foundation.layout.BoxWithConstraintsScope.DraggableAnchor(
    maxWpx: Float,
    maxHpx: Float,
    widthPx: Float,
    heightPx: Float,
    onTap: (() -> Unit)?,
    content: @Composable (offsetYpx: Float) -> Unit,
) {
    val density = LocalDensity.current
    val margin = with(density) { EDGE_MARGIN.dp.toPx() }
    val reserveBottom = with(density) { DRAG_BOTTOM_RESERVE.dp.toPx() }
    val topInset = with(density) { TOP_START.dp.toPx() }
    var dx by remember { mutableFloatStateOf(0f) }
    var dy by remember { mutableFloatStateOf(0f) }
    val minDx = -(maxWpx - widthPx - 2 * margin).coerceAtLeast(0f)
    val maxDy = (maxHpx - heightPx - reserveBottom - topInset).coerceAtLeast(0f)
    Box(
        Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = TOP_START.dp, end = EDGE_MARGIN.dp)
            .offset { IntOffset(dx.roundToInt(), dy.roundToInt()) }
            .pointerInput(onTap) {
                detectDragGestures(
                    onDragEnd = {
                        // Snap to the nearest side, back to the top, like snapToSafeEdge() on the web page.
                        dx = if (dx < minDx / 2f) minDx else 0f
                        dy = 0f
                    },
                ) { change, drag ->
                    change.consume()
                    dx = (dx + drag.x).coerceIn(minDx, 0f)
                    dy = (dy + drag.y).coerceIn(0f, maxDy)
                }
            }
            .then(if (onTap != null) Modifier.clickable(onClick = onTap) else Modifier),
    ) { content(topInset + dy) }
}

@Composable
private fun BarButton(icon: ImageVector, description: String, fill: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(fill).clickable(onClick = onClick).semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = Color.White, modifier = Modifier.size(18.dp)) }
}

/** The little "▣ 00:12" pill hanging under the video window; tapping it opens the menu. */
@Composable
private fun RuntimePill(time: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF0F0F0F).copy(alpha = 0.85f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).semantics { role = Role.Button }.padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(Icons.Rounded.Videocam, null, tint = CallGreen, modifier = Modifier.size(12.dp))
        Text(time, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/** Mute · Speaker · Swap · Minimize · End, stacked, opening and closing smoothly. */
@Composable
private fun VideoMenu(
    open: Boolean,
    muted: Boolean,
    speaker: Boolean,
    onClose: () -> Unit,
    onMute: () -> Unit,
    onSpeaker: () -> Unit,
    onSwap: () -> Unit,
    onMinimize: () -> Unit,
    onEnd: () -> Unit,
) {
    AnimatedVisibility(
        visible = open,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Column(
            Modifier.padding(vertical = 6.dp).width(52.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFF0F0F0F).copy(alpha = 0.85f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp)).padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MenuButton(if (muted) Icons.Rounded.MicOff else Icons.Rounded.Mic, stringResource(R.string.movie_call_mute), active = muted, onClick = onMute)
            MenuButton(if (speaker) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff, stringResource(R.string.movie_call_speaker), active = speaker, onClick = onSpeaker)
            MenuButton(Icons.Rounded.Cameraswitch, stringResource(R.string.movie_call_swap), onClick = onSwap)
            MenuButton(Icons.Rounded.CloseFullscreen, stringResource(R.string.movie_call_minimize), onClick = onMinimize)
            MenuButton(Icons.Rounded.CallEnd, stringResource(R.string.movie_call_end), danger = true, onClick = onEnd)
        }
    }
}

@Composable
private fun MenuButton(icon: ImageVector, label: String, active: Boolean = false, danger: Boolean = false, onClick: () -> Unit) {
    val fill = when {
        danger -> CallEndRed
        active -> Color(0xFFFFB43C).copy(alpha = 0.25f)
        else -> Color.White.copy(alpha = 0.1f)
    }
    Column(
        Modifier.size(42.dp).clip(CircleShape).background(fill).clickable(onClick = onClick).semantics { role = Role.Button },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
        Text(label, color = if (danger) Color.White else Color(0xFFDDDDDD), fontSize = 7.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

private val CallGreen = Color(0xFF34D399)
private val CallEndRed = Color(0xFF6B093A)
private const val ENDED_MS = 1_200L
private const val WINDOW_H = 192
private const val WINDOW_ASPECT = 9f / 16f
private const val WINDOW_MAX_FRACTION = 0.38f
private const val BUBBLE = 120
private const val EDGE_MARGIN = 12
private const val TOP_START = 10
private const val DRAG_BOTTOM_RESERVE = 150
private const val PILL_H = 32
private const val MENU_ESTIMATE = 250
