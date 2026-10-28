package com.twinhearts.app.core.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.twinhearts.app.core.common.rememberReduceMotion

private const val PETAL_COUNT = 22
private const val DURATION_MS = 1800

/**
 * A short burst of petals falling over the screen, the web app's celebration for adding something.
 * Plays once each time [trigger] changes to a value above 0; draws nothing when "remove animations"
 * is on (R-55). It does not take touches. Place it last inside a full-screen Box.
 */
@Composable
fun PetalBurst(trigger: Int, modifier: Modifier = Modifier) {
    val reduceMotion = rememberReduceMotion()
    val progress = remember { Animatable(1f) }
    val colors = TwinTheme.colors
    val palette = remember(colors) { listOf(colors.pinkSoft, colors.accent, colors.orchid, Color(0xFFFF8FB8)) }
    // Positions are fixed per burst so they do not jump while falling.
    val seeds = remember(trigger) { List(PETAL_COUNT) { Triple(Math.random().toFloat(), Math.random().toFloat(), Math.random().toFloat()) } }

    LaunchedEffect(trigger) {
        if (trigger > 0 && !reduceMotion) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(DURATION_MS, easing = LinearEasing))
        }
    }

    if (trigger > 0 && !reduceMotion && progress.value < 1f) {
        Canvas(modifier.fillMaxSize()) {
            val t = progress.value
            seeds.forEachIndexed { index, (xSeed, delaySeed, sizeSeed) ->
                val local = ((t - delaySeed * 0.35f) / 0.65f).coerceIn(0f, 1f)
                if (local <= 0f) return@forEachIndexed
                val x = size.width * xSeed + kotlin.math.sin(local * 6f + index) * 24f
                val y = -40f + (size.height + 80f) * local
                val radius = 7f + sizeSeed * 9f
                rotate(degrees = local * 220f + index * 30f, pivot = Offset(x, y)) {
                    drawOval(
                        color = palette[index % palette.size].copy(alpha = 1f - local * 0.6f),
                        topLeft = Offset(x - radius, y - radius * 1.4f),
                        size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2.8f),
                    )
                }
            }
        }
    }
}
