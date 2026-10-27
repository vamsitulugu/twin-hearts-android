package com.twinhearts.app.feature.twinai.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.twinhearts.app.core.common.rememberReduceMotion
import com.twinhearts.app.core.design.TwinTheme
import java.time.Duration
import java.time.Instant

/** Why this file exists: the two small motions of the web Twin page: bubbles that pop in, and the blinking caret while Twin types. */

/** A bubble this young (just created) pops in; older ones that scroll back into view stay still. */
private const val FRESH_MS = 2_000L

@Composable
fun Modifier.popIn(created: Instant): Modifier {
    val reduce = rememberReduceMotion()
    val fresh = remember { Duration.between(created, Instant.now()).toMillis() < FRESH_MS }
    val progress = remember { Animatable(if (fresh && !reduce) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) progress.animateTo(1f, tween(durationMillis = 220))
    }
    // Only alpha, scale and translation change, so nothing is re-measured (R-55).
    return this.graphicsLayer {
        val v = progress.value
        alpha = v
        translationY = (1f - v) * 12.dp.toPx()
        scaleX = 0.97f + 0.03f * v
        scaleY = 0.97f + 0.03f * v
    }
}

/** The little bar after the last word while Twin is still writing. Stands still when animations are off. */
@Composable
fun StreamCaret() {
    val reduce = rememberReduceMotion()
    val alpha = if (reduce) {
        1f
    } else {
        val t = rememberInfiniteTransition(label = "caret")
        val a by t.animateFloat(0.15f, 1f, infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "caretAlpha")
        a
    }
    Text(
        text = "▍",
        style = MaterialTheme.typography.bodyLarge,
        color = TwinTheme.colors.accent,
        modifier = Modifier.graphicsLayer { this.alpha = alpha },
    )
}
