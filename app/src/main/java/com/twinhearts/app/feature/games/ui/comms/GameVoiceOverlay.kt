package com.twinhearts.app.feature.games.ui.comms

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeUp
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.TwinTheme
import kotlin.math.roundToInt

/**
 * Why this file exists: what the in-game voice call looks like. No call page: a tiny pill with ONLY
 * mute, speaker and end (drag it away from the board), and a small card when my partner calls.
 * Also shows the short "ended / not answered / lost" notes as a toast, once.
 */
@Composable
fun BoxScope.GameVoiceOverlay(ui: GameVoiceUi, hooks: GameVoiceHooks) {
    val context = LocalContext.current
    LaunchedEffect(ui.notice) {
        val note = ui.notice ?: return@LaunchedEffect
        val name = ui.partnerName.ifBlank { context.getString(R.string.games_voice_partner) }
        val text = when (note) {
            GameVoiceUiNotice.Ended -> context.getString(R.string.games_voice_n_ended, name)
            GameVoiceUiNotice.Declined -> context.getString(R.string.games_voice_n_declined, name)
            GameVoiceUiNotice.NoAnswer -> context.getString(R.string.games_voice_n_no_answer, name)
            GameVoiceUiNotice.Lost -> context.getString(R.string.games_voice_n_lost)
            GameVoiceUiNotice.Failed -> context.getString(R.string.games_voice_n_failed)
            GameVoiceUiNotice.NeedsMic -> context.getString(R.string.games_voice_n_needs_mic)
            GameVoiceUiNotice.Busy -> context.getString(R.string.games_voice_n_busy)
            GameVoiceUiNotice.NotPaired -> context.getString(R.string.games_comms_connect_first)
        }
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        hooks.clearNotice()
    }

    if (ui.phase == GameVoiceUiPhase.Ringing) {
        IncomingCard(ui.partnerName, onDecline = hooks.decline, onAccept = hooks.accept)
    }
    if (ui.running) VoicePill(ui, hooks)
}

@Composable
private fun BoxScope.IncomingCard(name: String, onDecline: () -> Unit, onAccept: () -> Unit) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.card
    Column(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(top = CARD_TOP, start = TwinTheme.spacing.l, end = TwinTheme.spacing.l)
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.line, shape)
            .padding(TwinTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        Text(
            text = stringResource(R.string.games_voice_incoming, name.ifBlank { stringResource(R.string.games_voice_partner) }),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            GlassButton(
                text = stringResource(R.string.games_voice_decline),
                onClick = onDecline,
                style = GlassButtonStyle.Secondary,
                modifier = Modifier.weight(1f),
            )
            GlassButton(
                text = stringResource(R.string.games_voice_accept),
                onClick = onAccept,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Mute · Speaker · End, nothing else. The border is green once connected and pink while connecting. */
@Composable
private fun VoicePill(ui: GameVoiceUi, hooks: GameVoiceHooks) {
    val colors = TwinTheme.colors
    val density = LocalDensity.current
    var dx by remember { mutableFloatStateOf(0f) }
    var dy by remember { mutableFloatStateOf(0f) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val edge = with(density) { EDGE.toPx() }
    val top = with(density) { PILL_TOP.toPx() }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val maxW = with(density) { maxWidth.toPx() }
        val maxH = with(density) { maxHeight.toPx() }
        // The pill starts at the top-right; dragging moves it left (negative x) and down (positive y).
        val minDx = -(maxW - size.width - 2 * edge).coerceAtLeast(0f)
        val maxDy = (maxH - size.height - top - edge).coerceAtLeast(0f)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = PILL_TOP, end = EDGE)
                .offset { IntOffset(dx.roundToInt(), dy.roundToInt()) }
                .onSizeChanged { size = it }
                .pointerInput(minDx, maxDy) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        dx = (dx + drag.x).coerceIn(minDx, 0f)
                        dy = (dy + drag.y).coerceIn(0f, maxDy)
                    }
                }
                .clip(TwinTheme.shapes.pill)
                .background(colors.scrim)
                .border(
                    width = 1.5.dp,
                    color = if (ui.phase == GameVoiceUiPhase.Active) colors.success else colors.accent,
                    shape = TwinTheme.shapes.pill,
                )
                .padding(PILL_PADDING),
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        ) {
            PillButton(
                icon = if (ui.muted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                description = stringResource(R.string.games_voice_mute),
                fill = if (ui.muted) colors.error else colors.glass,
                onClick = hooks.toggleMute,
            )
            PillButton(
                icon = if (ui.speaker) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeDown,
                description = stringResource(R.string.games_voice_speaker),
                fill = if (ui.speaker) colors.success.copy(alpha = 0.28f) else colors.glass,
                onClick = hooks.toggleSpeaker,
            )
            PillButton(
                icon = Icons.Rounded.CallEnd,
                description = stringResource(R.string.games_voice_end_call),
                fill = colors.error,
                onClick = hooks.hangUp,
            )
        }
    }
}

@Composable
private fun PillButton(icon: ImageVector, description: String, fill: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(TwinTheme.shapes.minTouch)
            .clip(CircleShape)
            .background(fill)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = TwinTheme.colors.ink, modifier = Modifier.size(22.dp))
    }
}

private val CARD_TOP = 64.dp
private val PILL_TOP = 64.dp
private val EDGE = 10.dp
private val PILL_PADDING = 6.dp
