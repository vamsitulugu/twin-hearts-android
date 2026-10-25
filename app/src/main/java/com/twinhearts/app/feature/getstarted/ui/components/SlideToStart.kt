package com.twinhearts.app.feature.getstarted.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.twinhearts.app.core.common.rememberReduceMotion
import com.twinhearts.app.core.design.TwinTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val TrackHeight = 68.dp
private val KnobPadding = 7.dp
private val LabelStartPadding = 48.dp
private const val FINISH_FRACTION = 0.82f // drag past 82% to finish, like the web slider
private const val NUDGE_DP = 10f

/**
 * "Slide to get started" pill. Drag the round knob to the end to continue; letting go early
 * snaps it back. TalkBack users can double-tap the whole control instead of dragging.
 * Only the knob offset changes while dragging, and it is read in layout/draw, so no recomposition per frame.
 */
@Composable
internal fun SlideToStart(
    label: String,
    description: String,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val reduceMotion = rememberReduceMotion()

    val knobSize = TrackHeight - KnobPadding * 2
    val knobPx = with(density) { knobSize.toPx() }
    val padPx = with(density) { KnobPadding.toPx() }
    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val maxPx = (trackWidthPx - knobPx - padPx * 2).coerceAtLeast(0f)
    val offset = remember { Animatable(0f) }
    var finished by remember { mutableStateOf(false) }
    var dragged by remember { mutableStateOf(false) }

    // A small hint nudge every few seconds, until the user touches the knob (web: msNudge).
    val nudge = if (reduceMotion || dragged) {
        null
    } else {
        rememberInfiniteTransition(label = "slideNudge").animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                keyframes {
                    durationMillis = 2400
                    0f at 0
                    0f at 1680
                    NUDGE_DP at 1968
                    0f at 2208
                },
            ),
            label = "slideNudgeValue",
        )
    }

    fun finish() {
        if (finished) return
        finished = true
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            offset.animateTo(maxPx, tween(200))
            onComplete()
        }
    }

    Box(
        modifier = modifier
            .height(TrackHeight)
            .clip(TwinTheme.shapes.pill)
            .background(colors.slideTrack)
            .onSizeChanged { trackWidthPx = it.width.toFloat() }
            .drawBehind {
                val fillWidth = offset.value + knobPx + padPx
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(colors.slideFillStart, colors.slideFillEnd),
                        endX = fillWidth,
                    ),
                    size = Size(fillWidth, size.height),
                    cornerRadius = CornerRadius(size.height / 2f),
                )
            }
            .semantics(mergeDescendants = true) {
                contentDescription = description
                role = Role.Button
                onClick(label = label) {
                    finish()
                    true
                }
            },
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = colors.slideLabel,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = LabelStartPadding)
                .graphicsLayer {
                    val progress = if (maxPx > 0f) offset.value / maxPx else 0f
                    alpha = (1f - progress * 1.6f).coerceIn(0f, 1f)
                },
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = KnobPadding)
                .offset {
                    val x = offset.value + (nudge?.value ?: 0f) * density.density
                    IntOffset(x.roundToInt(), 0)
                }
                .size(knobSize)
                .clip(TwinTheme.shapes.circle)
                .background(Brush.linearGradient(listOf(colors.knobStart, colors.knobEnd)))
                .draggable(
                    orientation = Orientation.Horizontal,
                    enabled = !finished,
                    state = rememberDraggableState { delta ->
                        scope.launch { offset.snapTo((offset.value + delta).coerceIn(0f, maxPx)) }
                    },
                    onDragStarted = { dragged = true },
                    onDragStopped = {
                        if (maxPx > 0f && offset.value >= maxPx * FINISH_FRACTION) {
                            finish()
                        } else {
                            scope.launch { offset.animateTo(0f, tween(280)) }
                        }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = colors.ink,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
