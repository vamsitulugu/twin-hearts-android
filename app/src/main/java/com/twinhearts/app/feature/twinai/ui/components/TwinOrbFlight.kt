package com.twinhearts.app.feature.twinai.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.twinhearts.app.core.common.rememberReduceMotion
import kotlin.math.roundToInt

/**
 * Why this file exists: when you ask the first question, a small Twin orb flies from the robot in
 * the hero card down to the orb on the input bar (the web's orb "travels between hero and dock").
 * [from] and [to] are centres in this overlay's own coordinates. Skipped when animations are off.
 */
@Composable
fun TwinOrbFlight(from: Offset, to: Offset, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val reduce = rememberReduceMotion()
    val progress = remember { Animatable(0f) }
    val half = with(LocalDensity.current) { 20.dp.toPx() }
    LaunchedEffect(Unit) {
        if (!reduce) progress.animateTo(1f, tween(durationMillis = 750, easing = FastOutSlowInEasing))
        onDone()
    }
    val t = progress.value
    // a gentle arc: the orb rises a little before it drops to the dock
    val x = from.x + (to.x - from.x) * t
    val y = from.y + (to.y - from.y) * t - 120f * kotlin.math.sin(Math.PI * t).toFloat()
    Box(
        modifier
            .offset { IntOffset((x - half).roundToInt(), (y - half).roundToInt()) }
            .graphicsLayer { alpha = 1f - 0.4f * t; scaleX = 1f - 0.2f * t; scaleY = 1f - 0.2f * t },
    ) {
        TwinOrb(size = 40.dp)
    }
}
