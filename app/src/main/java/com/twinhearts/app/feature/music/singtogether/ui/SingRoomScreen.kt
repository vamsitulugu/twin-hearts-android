package com.twinhearts.app.feature.music.singtogether.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.MicNone
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.music.player.PlayerState
import com.twinhearts.app.feature.music.singtogether.data.SingState
import com.twinhearts.app.feature.music.singtogether.domain.DuetLine
import com.twinhearts.app.feature.music.singtogether.domain.DuetLrcParser
import com.twinhearts.app.feature.music.singtogether.domain.FloatingReaction
import com.twinhearts.app.feature.music.singtogether.domain.LyricsLoad
import com.twinhearts.app.feature.music.singtogether.domain.SingPhase
import com.twinhearts.app.feature.music.singtogether.domain.Speaker

/** What the room's buttons do. The screen itself holds no logic (R-42). */
internal class SingRoomActions(
    val onLeave: () -> Unit,
    val onPrevious: () -> Unit,
    val onTogglePlay: () -> Unit,
    val onNext: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onMic: () -> Unit,
    val onCamera: () -> Unit,
    val onFlip: () -> Unit,
    val onSpeaker: () -> Unit,
    val onReaction: (String) -> Unit,
)

private val REACTIONS = listOf("❤️", "😂", "👏", "🔥", "🥹")

// The lyric starts a touch early so it never lags behind the voice (the web app uses the same 0.2 s).
private const val LYRIC_LEAD_MS = 200L
private const val SELF_WIDTH_DP = 110
private const val SELF_HEIGHT_DP = 160
private const val REACTION_RISE_DP = 420
private const val REACTION_MS = 3_000

/**
 * Why this file exists: the singing room. The partner's picture fills the screen, mine sits in a
 * small tile, the current lyric line floats over the picture, and the controls are at the bottom.
 * Follows the web app's couple-karaoke.js room.
 */
@Composable
internal fun SingRoomScreen(
    state: SingState,
    player: PlayerState,
    actions: SingRoomActions,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        PartnerPicture(state)

        Column(Modifier.fillMaxSize()) {
            TopBar(state, actions.onLeave)
            if (state.stale) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { StaleBanner() }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                PartnerLabel(state.partnerName)
                SelfTile(state)
                LyricStage(state, player.positionMs, Modifier.align(Alignment.BottomCenter))
                ReactionLayer(state.reactions)
            }
            Controls(state, player, actions)
        }

        if (state.phase == SingPhase.Connecting) ConnectingOverlay()
    }
}

@Composable
private fun PartnerPicture(state: SingState) {
    val colors = TwinTheme.colors
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.card)
            // The web room's soft glow at the top and bottom, visible while no picture arrives.
            .background(Brush.verticalGradient(listOf(colors.backgroundGlow, Color.Transparent, colors.backgroundGlow))),
        contentAlignment = Alignment.Center,
    ) {
        if (state.remoteVideo != null) {
            SingVideoView(track = state.remoteVideo, egl = state.egl, mirror = false, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Rounded.Person, contentDescription = null, tint = colors.inkFaint, modifier = Modifier.size(96.dp))
        }
        // Soft shade at the top and bottom so the buttons and lyrics stay readable on any picture.
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(colors.scrim, Color.Transparent, Color.Transparent, colors.scrim))),
        )
    }
}

@Composable
private fun TopBar(state: SingState, onLeave: () -> Unit) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(TwinTheme.shapes.pill)
                .background(colors.glass)
                .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        ) {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
            val name = state.songTitle + if (state.songArtist.isNotBlank()) " — ${state.songArtist}" else ""
            Text(name, style = MaterialTheme.typography.labelLarge, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        RoundButton(Icons.Rounded.Close, stringResource(R.string.music_together_leave), false, onLeave, 40)
    }
}

@Composable
private fun StaleBanner() {
    val colors = TwinTheme.colors
    Text(
        stringResource(R.string.music_together_reconnecting),
        style = MaterialTheme.typography.labelMedium,
        color = colors.ink,
        modifier = Modifier
            .clip(TwinTheme.shapes.pill)
            .background(colors.glass)
            .border(BorderStroke(1.dp, colors.line), TwinTheme.shapes.pill)
            .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.xs),
    )
}

/** Who is on the big picture (the web room shows the same small name tag). */
@Composable
private fun BoxScope.PartnerLabel(name: String) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(start = TwinTheme.spacing.m, top = TwinTheme.spacing.xs)
            .clip(TwinTheme.shapes.pill)
            .background(colors.glass)
            .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs),
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(colors.accent))
        Text(
            name.ifBlank { stringResource(R.string.music_together_partner_fallback) },
            style = MaterialTheme.typography.labelMedium,
            color = colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** My own picture, small, in the top right corner (below the top bar so the Leave button stays free). */
@Composable
private fun BoxScope.SelfTile(state: SingState) {
    val colors = TwinTheme.colors
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(end = TwinTheme.spacing.m, top = TwinTheme.spacing.xs)
            .size(SELF_WIDTH_DP.dp, SELF_HEIGHT_DP.dp)
            .clip(TwinTheme.shapes.field)
            .background(colors.card)
            .border(BorderStroke(1.dp, colors.line), TwinTheme.shapes.field),
        contentAlignment = Alignment.Center,
    ) {
        if (state.localVideo != null && !state.cameraOff) {
            SingVideoView(track = state.localVideo, egl = state.egl, mirror = true, modifier = Modifier.fillMaxSize(), overlay = true)
        } else {
            Icon(
                if (state.hasCamera) Icons.Rounded.VideocamOff else Icons.Rounded.Person,
                contentDescription = null,
                tint = colors.inkFaint,
                modifier = Modifier.size(36.dp),
            )
        }
        Text(
            stringResource(R.string.music_together_you),
            style = MaterialTheme.typography.labelSmall,
            color = colors.ink,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(TwinTheme.spacing.xs)
                .clip(TwinTheme.shapes.pill)
                .background(colors.glass)
                .padding(horizontal = TwinTheme.spacing.s, vertical = 2.dp),
        )
    }
}

/** The line being sung (coloured by who sings it) and the next one. */
@Composable
private fun LyricStage(state: SingState, positionMs: Long, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val index = DuetLrcParser.activeIndex(state.lines, positionMs + LYRIC_LEAD_MS)
    val current: DuetLine? = state.lines.getOrNull(index)
    val next: DuetLine? = state.lines.getOrNull(index + 1) ?: if (index < 0) state.lines.firstOrNull() else null

    val currentText = when {
        current != null -> current.text
        state.lyrics == LyricsLoad.Loading -> stringResource(R.string.music_together_lyrics_loading)
        state.lyrics == LyricsLoad.Missing -> stringResource(R.string.music_together_lyrics_none)
        else -> stringResource(R.string.music_together_lyrics_waiting)
    }
    val currentColor = when (current?.speaker) {
        Speaker.A, Speaker.B -> colors.accent
        else -> colors.pinkSoft
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TwinTheme.spacing.xl, vertical = TwinTheme.spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            currentText,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 32.sp,
                shadow = Shadow(color = currentColor, blurRadius = 24f),
            ),
            color = currentColor,
            textAlign = TextAlign.Center,
        )
        if (next != null) {
            Text(
                next.text,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.inkMuted,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = TwinTheme.spacing.s),
            )
        }
    }
}

/** Emojis the partner (or I) sent, floating up the picture. */
@Composable
private fun ReactionLayer(reactions: List<FloatingReaction>) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        reactions.forEach { reaction ->
            key(reaction.id) { FloatingEmoji(reaction, maxWidth.value) }
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.FloatingEmoji(reaction: FloatingReaction, widthDp: Float) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(REACTION_MS, easing = LinearEasing)) }
    val p = progress.value
    // Fade in quickly, hold, fade out at the end.
    val alpha = when {
        p < 0.15f -> p / 0.15f
        p > 0.7f -> (1f - p) / 0.3f
        else -> 1f
    }
    Text(
        reaction.emoji,
        fontSize = 30.sp,
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = (widthDp * reaction.xFraction).dp, bottom = TwinTheme.spacing.s)
            .graphicsLayer {
                translationY = -REACTION_RISE_DP.dp.toPx() * p
                this.alpha = alpha.coerceIn(0f, 1f)
                val scale = 0.5f + 0.6f * minOf(p / 0.15f, 1f)
                scaleX = scale
                scaleY = scale
            },
    )
}

@Composable
private fun Controls(state: SingState, player: PlayerState, actions: SingRoomActions) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = TwinTheme.spacing.m),
    ) {
        Progress(player, enabled = state.isHost, onSeek = actions.onSeek)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = TwinTheme.spacing.xs),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            REACTIONS.forEach { emoji ->
                Text(
                    emoji,
                    fontSize = 24.sp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { actions.onReaction(emoji) }
                        .padding(horizontal = TwinTheme.spacing.s, vertical = TwinTheme.spacing.xs),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = TwinTheme.spacing.s),
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundButton(Icons.Rounded.SkipPrevious, stringResource(R.string.music_together_previous), false, actions.onPrevious, 44)
            RoundButton(
                if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                stringResource(if (player.isPlaying) R.string.music_together_pause else R.string.music_together_play),
                true,
                actions.onTogglePlay,
                54,
            )
            RoundButton(Icons.Rounded.SkipNext, stringResource(R.string.music_together_next), false, actions.onNext, 44)
            RoundButton(
                if (state.micOff) Icons.Rounded.MicOff else Icons.Rounded.MicNone,
                stringResource(if (state.micOff) R.string.music_together_mic_off else R.string.music_together_mic_on),
                state.micOff,
                actions.onMic,
                44,
            )
            RoundButton(
                if (state.cameraOff) Icons.Rounded.VideocamOff else Icons.Rounded.Videocam,
                stringResource(if (state.cameraOff) R.string.music_together_cam_off else R.string.music_together_cam_on),
                state.cameraOff,
                actions.onCamera,
                44,
            )
            RoundButton(Icons.Rounded.FlipCameraAndroid, stringResource(R.string.music_together_flip), false, actions.onFlip, 44)
            RoundButton(
                if (state.speakerOff) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                stringResource(if (state.speakerOff) R.string.music_together_speaker_off else R.string.music_together_speaker_on),
                state.speakerOff,
                actions.onSpeaker,
                44,
            )
        }
    }
}

/** Song position. Only the host can move it; the guest sees it follow. */
@Composable
private fun Progress(player: PlayerState, enabled: Boolean, onSeek: (Long) -> Unit) {
    val colors = TwinTheme.colors
    val duration = player.durationMs.coerceAtLeast(1L)
    var dragging by remember { mutableStateOf<Float?>(null) }
    val fraction = dragging ?: (player.positionMs.toFloat() / duration).coerceIn(0f, 1f)
    val seekLabel = stringResource(R.string.music_together_seek)
    Column(Modifier.fillMaxWidth().padding(horizontal = TwinTheme.spacing.l)) {
        Slider(
            value = fraction,
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { onSeek((it * duration).toLong()) }
                dragging = null
            },
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.line,
                disabledThumbColor = colors.accent,
                disabledActiveTrackColor = colors.accent,
                disabledInactiveTrackColor = colors.line,
            ),
            modifier = Modifier.semantics { contentDescription = seekLabel },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(time(player.positionMs), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
            Text(time(player.durationMs), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
        }
    }
}

private fun time(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}

@Composable
private fun RoundButton(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit, sizeDp: Int) {
    val colors = TwinTheme.colors
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(if (active) colors.accent else colors.glass)
            .border(BorderStroke(1.dp, colors.line), CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = colors.ink, modifier = Modifier.size((sizeDp * 0.45f).dp))
    }
}

@Composable
private fun ConnectingOverlay() {
    val colors = TwinTheme.colors
    Column(
        modifier = Modifier.fillMaxSize().background(colors.scrim),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m, Alignment.CenterVertically),
    ) {
        CircularProgressIndicator(color = colors.accent)
        Text(stringResource(R.string.music_together_connecting), style = MaterialTheme.typography.bodyLarge, color = colors.ink)
    }
}
