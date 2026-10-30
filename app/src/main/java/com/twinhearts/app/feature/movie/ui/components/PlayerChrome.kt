package com.twinhearts.app.feature.movie.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twinhearts.app.R
import com.twinhearts.app.feature.movie.domain.formatClock

/**
 * Why this file exists: the pieces of the cinema screen, drawn like the web page's player
 * (movie.html / movie.css): round dark buttons on the picture, a status pill under the title,
 * a red timeline and the six icon-over-label buttons (Sync, Message, Log, Voice, Video, End).
 * PlayerScreen only decides when to show them; the look lives here.
 */

/** The red of the web timeline and its thumb. */
internal val PlayerAccent = Color(0xFFC8102E)
private val PillWarn = Color(0xFFFFB43C)
private val PillGood = Color(0xFF34D399)
private val LabelGrey = Color(0xFF9A9A9A)
private val IconWhite = Color(0xFFDDDDDD)
private val ChatYellow = Color(0xFFFFD54A)
private val DarkButton = Color.Black.copy(alpha = 0.40f)

/** What the small pill under the movie title says. */
internal enum class PillKind { Warn, Good, Plain }

internal data class PillState(val text: String, val kind: PillKind)

/** Round 44 dp dark button used on the picture (back, more, fullscreen). */
@Composable
internal fun PlayerRoundButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(44.dp).clip(CircleShape).background(DarkButton).border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
            .clickable(onClick = onClick).semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = Color.White, modifier = Modifier.size(22.dp)) }
}

/** Top row: back on the left, title and status pill in the middle, ⋮ and fullscreen on the right. */
@Composable
internal fun PlayerTopBar(
    title: String,
    pill: PillState?,
    fullscreen: Boolean,
    onBack: () -> Unit,
    onMore: () -> Unit,
    onFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)))
            .statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PlayerRoundButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.movie_back), onBack)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            )
            if (pill != null) StatusPill(pill) else WatchTag()
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PlayerRoundButton(Icons.Rounded.MoreVert, stringResource(R.string.movie_more_options), onMore)
            PlayerRoundButton(
                if (fullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                stringResource(if (fullscreen) R.string.movie_exit_fullscreen else R.string.movie_fullscreen), onFullscreen,
            )
        }
    }
}

/** The small burgundy "Watching Together" tag shown while nothing else needs saying. */
@Composable
private fun WatchTag() {
    Text(
        stringResource(R.string.movie_watching_together), color = Color(0xFFE5668F), fontSize = 10.5.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(CircleShape).background(Color(0xFF6B093A).copy(alpha = 0.22f))
            .border(1.dp, Color(0xFF6B093A).copy(alpha = 0.55f), CircleShape).padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** "Paused by Partner", "✓ In Sync", "Partner reconnecting…". */
@Composable
internal fun StatusPill(state: PillState, modifier: Modifier = Modifier) {
    val tone = when (state.kind) {
        PillKind.Warn -> PillWarn
        PillKind.Good -> PillGood
        PillKind.Plain -> IconWhite
    }
    Text(
        state.text, color = tone, fontSize = 11.sp, fontWeight = FontWeight.Medium,
        modifier = modifier.clip(CircleShape)
            .background(if (state.kind == PillKind.Plain) Color.White.copy(alpha = 0.10f) else tone.copy(alpha = 0.18f))
            .border(1.dp, tone.copy(alpha = if (state.kind == PillKind.Plain) 0.12f else 0.45f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

/** Back 10 s, play / pause and forward 10 s in the middle of the picture. */
@Composable
internal fun PlayerCenterControls(
    playing: Boolean,
    landscape: Boolean,
    onBack10: () -> Unit,
    onPlayPause: () -> Unit,
    onForward10: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playSize = if (landscape) 84.dp else 76.dp
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(if (landscape) 40.dp else 28.dp), verticalAlignment = Alignment.CenterVertically) {
        SeekButton(forward = false, label = stringResource(R.string.movie_back_10), onClick = onBack10)
        Box(
            Modifier.size(playSize).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f))
                .border(2.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                .clickable(onClick = onPlayPause).semantics { role = Role.Button },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                stringResource(if (playing) R.string.movie_pause else R.string.movie_play),
                tint = Color.White, modifier = Modifier.size(38.dp),
            )
        }
        SeekButton(forward = true, label = stringResource(R.string.movie_forward_10), onClick = onForward10)
    }
}

/** 52 dp round button: a turning arrow with a tiny "10" under it (the web page's rotate-ccw / rotate-cw). */
@Composable
private fun SeekButton(forward: Boolean, label: String, onClick: () -> Unit) {
    Column(
        Modifier.size(52.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)).border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape).clickable(onClick = onClick)
            .semantics { role = Role.Button },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // The same arrow, mirrored, is the clockwise one.
        Icon(
            Icons.Rounded.Replay, label, tint = Color.White,
            modifier = Modifier.size(24.dp).graphicsLayer { if (forward) scaleX = -1f },
        )
        Text("10", color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(top = 0.dp))
    }
}

/** Current time, red timeline and total time. */
@Composable
internal fun PlayerTimeline(
    positionSec: Double,
    durationSec: Double,
    onScrub: (Double) -> Unit,
    onScrubDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(formatClock(positionSec), color = IconWhite, fontSize = 11.sp, modifier = Modifier.defaultMinSize(minWidth = 34.dp))
        TimelineBar(positionSec, durationSec, onScrub, onScrubDone, Modifier.weight(1f))
        Text(
            formatClock(durationSec), color = IconWhite, fontSize = 11.sp, textAlign = TextAlign.End,
            modifier = Modifier.defaultMinSize(minWidth = 34.dp),
        )
    }
}

/**
 * The red timeline: grey track, red played part, round red thumb with a white edge. It is drawn by hand
 * (tap or drag anywhere on it) so it needs no experimental Material3 slider API.
 */
@Composable
private fun TimelineBar(
    positionSec: Double,
    durationSec: Double,
    onScrub: (Double) -> Unit,
    onScrubDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fraction = if (durationSec > 0.0) (positionSec / durationSec).toFloat().coerceIn(0f, 1f) else 0f
    var widthPx by remember { mutableFloatStateOf(1f) }
    val scrub by rememberUpdatedState(onScrub)
    val scrubDone by rememberUpdatedState(onScrubDone)
    val thumbPx = with(LocalDensity.current) { THUMB_DP.dp.toPx() }
    fun at(x: Float): Double = (x / widthPx).coerceIn(0f, 1f) * durationSec
    Box(
        modifier.heightIn(min = 28.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(durationSec) {
                detectTapGestures { offset ->
                    scrub(at(offset.x))
                    scrubDone()
                }
            }
            .pointerInput(durationSec) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> scrub(at(offset.x)) },
                    onDragEnd = { scrubDone() },
                    onDragCancel = { scrubDone() },
                ) { change, _ ->
                    change.consume()
                    scrub(at(change.position.x))
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.25f))) {
            Box(Modifier.fillMaxWidth(fraction).height(4.dp).background(PlayerAccent))
        }
        Box(
            Modifier.offset { IntOffset((fraction * (widthPx - thumbPx)).roundToInt().coerceAtLeast(0), 0) }
                .size(THUMB_DP.dp).clip(CircleShape).background(PlayerAccent).border(2.dp, Color.White, CircleShape),
        )
    }
}

private const val THUMB_DP = 18

/** One of the six buttons under the timeline: icon over a tiny label. */
@Composable
private fun SecondaryButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).semantics { role = Role.Button },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
        Text(label, color = LabelGrey, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

/** Sync · Message · Log · Voice · Video · End, in that order, like the web page. */
@Composable
internal fun PlayerSecondaryRow(
    onSync: () -> Unit,
    onMessage: () -> Unit,
    onLog: () -> Unit,
    onVoice: () -> Unit,
    onVideo: () -> Unit,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        SecondaryButton(Icons.Outlined.Sync, stringResource(R.string.movie_sync), onSync, Modifier.weight(1f))
        SecondaryButton(Icons.Outlined.ChatBubbleOutline, stringResource(R.string.movie_message), onMessage, Modifier.weight(1f))
        SecondaryButton(Icons.AutoMirrored.Outlined.FormatListBulleted, stringResource(R.string.movie_log), onLog, Modifier.weight(1f))
        SecondaryButton(Icons.Outlined.Call, stringResource(R.string.movie_voice), onVoice, Modifier.weight(1f))
        SecondaryButton(Icons.Outlined.Videocam, stringResource(R.string.movie_video), onVideo, Modifier.weight(1f))
        SecondaryButton(Icons.Outlined.CropSquare, stringResource(R.string.movie_end), onEnd, Modifier.weight(1f))
    }
}

/** The bottom block: timeline on top, six buttons under it, on a fade to black. */
@Composable
internal fun PlayerBottomOverlay(
    positionSec: Double,
    durationSec: Double,
    onScrub: (Double) -> Unit,
    onScrubDone: () -> Unit,
    actions: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
            .navigationBarsPadding().padding(start = 14.dp, end = 14.dp, top = 20.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PlayerTimeline(positionSec, durationSec, onScrub, onScrubDone)
        actions()
    }
}

/** 😊 button at the far right; when open, the five emoji sit in a row to its left (the web page's reaction row). */
@Composable
internal fun ReactionColumn(open: Boolean, onToggle: () -> Unit, onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (open) {
            Row(
                Modifier.clip(RoundedCornerShape(26.dp)).background(Color.Black.copy(alpha = 0.5f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(26.dp)).padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                reactionEmoji.forEach { emoji ->
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).clickable { onPick(emoji) },
                        contentAlignment = Alignment.Center,
                    ) { Text(emoji, fontSize = 19.sp) }
                }
            }
        }
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)).clickable(onClick = onToggle)
                .semantics { role = Role.Button },
            contentAlignment = Alignment.Center,
        ) { Text("😊", fontSize = 20.sp) }
    }
}

/** A partner or own message that floats above the timeline for a few seconds, then fades. */
@Composable
internal fun ChatToast(text: String?, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = text != null,
            enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 2 },
            exit = fadeOut(tween(250)) + slideOutVertically(tween(250)) { -it / 4 },
        ) {
            Text(
                text.orEmpty(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 320.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF0F0F0F).copy(alpha = 0.62f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

/** The "Message your partner…" bar that opens over the bottom of the picture. */
@Composable
internal fun PlayerChatComposer(
    draft: String,
    onDraft: (String) -> Unit,
    onSend: () -> Unit,
    onClose: () -> Unit,
    focus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().background(Color(0xFF0A0A0A).copy(alpha = 0.92f))
            .navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.weight(1f).heightIn(min = 42.dp).clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(22.dp)).padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (draft.isEmpty()) {
                Text(stringResource(R.string.movie_chat_placeholder), color = Color.White.copy(alpha = 0.45f), fontSize = 14.sp)
            }
            BasicTextField(
                value = draft, onValueChange = { if (it.length <= MAX_CHAT) onDraft(it) }, singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
        CircleAction(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.movie_send), PlayerAccent, onSend)
        CircleAction(Icons.Rounded.Close, stringResource(R.string.movie_chat_close), Color.White.copy(alpha = 0.08f), onClose)
    }
}

@Composable
private fun CircleAction(icon: ImageVector, description: String, fill: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(fill).clickable(onClick = onClick).semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = Color.White, modifier = Modifier.size(18.dp)) }
}

/** The short "Fit / Fill / Zoom" label that fades in the middle of the picture. */
@Composable
internal fun ModeLabel(text: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = text != null, enter = fadeIn(tween(150)), exit = fadeOut(tween(300)), modifier = modifier) {
        Text(
            text.orEmpty(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(alpha = 0.65f)).padding(horizontal = 18.dp, vertical = 7.dp),
        )
    }
}

/** Yellow chat text colour shared with the chat log sheet. */
internal val MovieChatYellow = ChatYellow

/** A plain full-size tap area (no ripple) used to show or hide the controls. */
@Composable
internal fun Modifier.tapNoRipple(onTap: () -> Unit): Modifier =
    this.clickable(interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }, indication = null, onClick = onTap)

private const val MAX_CHAT = 500
