package com.twinhearts.app.core.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.twinhearts.app.core.common.rememberReduceMotion
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/*
 * Why this file exists: the Twin orb must look and move like the web orb (twin-orb.js): a dark
 * magenta sphere with drifting plasma clouds, silver energy ribbons, a breathing core and tiny
 * particles, lit like a real ball. It is drawn in code, so it stays sharp at any size and needs no
 * image files. The colours are part of the illustration (like the robot in TwinArt), not UI colours.
 * One composable is used everywhere (bottom bar, Twin input dock, voice overlay) so they all match.
 */

private class Blob(val rx: Float, val ry: Float, val r: Float, val fx: Float, val fy: Float, val ph: Float, val tone: Int)

private class Ribbon(
    val depth: Int, // 0 back, 1 mid, 2 front
    val fx1: Float, val fx2: Float, val ph: Float, val amp: Float, val speed: Float, val y0: Float,
)

private class Particle(val a: Float, val rr: Float, val speed: Float, val ph: Float, val size: Float)

private val Blobs = listOf(
    Blob(0.30f, 0.22f, 0.55f, 0.31f, 0.27f, 0.0f, 0),
    Blob(0.62f, 0.60f, 0.60f, 0.19f, 0.24f, 1.1f, 1),
    Blob(0.40f, 0.70f, 0.48f, 0.26f, 0.18f, 2.4f, 0),
    Blob(0.72f, 0.32f, 0.42f, 0.22f, 0.29f, 3.6f, 2),
    Blob(0.50f, 0.50f, 0.70f, 0.12f, 0.15f, 4.4f, 1),
    Blob(0.25f, 0.55f, 0.40f, 0.28f, 0.21f, 5.2f, 2),
)

private val Ribbons = listOf(
    Ribbon(0, 0.17f, 0.21f, 0.4f, 0.16f, 0.35f, 0.30f),
    Ribbon(1, 0.20f, 0.15f, 1.6f, 0.20f, 0.42f, 0.48f),
    Ribbon(2, 0.24f, 0.18f, 2.7f, 0.22f, 0.50f, 0.55f),
    Ribbon(1, 0.14f, 0.22f, 3.9f, 0.18f, 0.31f, 0.62f),
    Ribbon(0, 0.19f, 0.16f, 5.1f, 0.15f, 0.38f, 0.42f),
)

private val Particles = Random(7).let { rnd ->
    List(16) {
        Particle(
            a = rnd.nextFloat() * 6.2832f,
            rr = 0.25f + rnd.nextFloat() * 0.55f,
            speed = 0.04f + rnd.nextFloat() * 0.06f,
            ph = rnd.nextFloat() * 6.2832f,
            size = 0.6f + rnd.nextFloat() * 1.3f,
        )
    }
}

/**
 * The animated Twin orb. [speed] and [energy] make it calmer or busier (thinking, listening, ...).
 * With "remove animations" on in the phone settings it shows one still frame (R-55).
 */
@Composable
fun EnergyOrb(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    speed: Float = 1f,
    energy: Float = 1f,
    offline: Boolean = false,
    error: Boolean = false,
) {
    val reduce = rememberReduceMotion()
    val currentSpeed by rememberUpdatedState(speed)
    var phase by remember { mutableFloatStateOf(2.5f) }

    LaunchedEffect(reduce) {
        if (reduce) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                phase += (now - last) / 1_000_000_000f * currentSpeed
                last = now
            }
        }
    }

    // `phase` is only read while drawing, so each frame redraws the orb without recomposing.
    Canvas(modifier = modifier.size(size)) {
        drawEnergyOrb(phase, energy, offline, error)
    }
}

private fun DrawScope.drawEnergyOrb(tt: Float, energy: Float, offline: Boolean, error: Boolean) {
    val w = size.width
    val r = w / 2f
    val k = w / 176f // the web orb is drawn on a 176 px canvas; widths below scale from that
    val c = Offset(r, r)
    val sphere = Path().apply { addOval(Rect(center = c, radius = r * 0.97f)) }

    clipPath(sphere) {
        // Layer 1: deep base shading
        drawRect(
            Brush.radialGradient(
                0f to Color(0xFF12020B), 0.35f to Color(0xFF740642), 0.7f to Color(0xFF1C0311), 1f to Color(0xFF050003),
                center = Offset(r * 0.9f, r * 0.8f), radius = r * 1.15f,
            ),
        )
        drawRect(
            Brush.radialGradient(
                0f to Color(0x8CB0127A), 0.4f to Color(0x66B0127A), 1f to Color(0x0012020B),
                center = Offset(r * 0.5f, r * 0.52f), radius = r * 0.95f,
            ),
        )

        // Layer 2: drifting plasma clouds, blended with "screen" so they merge into one soft glow
        for (b in Blobs) {
            val px = r + cos(tt * b.fx + b.ph) * r * b.rx
            val py = r + sin(tt * b.fy + b.ph * 1.3f) * r * b.ry
            val pr = r * b.r * (0.85f + 0.15f * sin(tt * 0.6f + b.ph))
            val base = when (b.tone) {
                0 -> Color(200, 20, 20)
                1 -> Color(150, 0, 0)
                else -> Color(196, 150, 160)
            }
            val a = (0.22f * energy).coerceIn(0f, 1f)
            drawCircle(
                Brush.radialGradient(
                    listOf(base.copy(alpha = a), base.copy(alpha = 0f)),
                    center = Offset(px, py), radius = pr,
                ),
                radius = pr, center = Offset(px, py), blendMode = BlendMode.Screen,
            )
        }

        // Layer 3: silver energy ribbons, back to front
        for (depth in 0..2) {
            for (rb in Ribbons) if (rb.depth == depth) drawRibbon(rb, tt, w, k, energy)
        }

        // Layer 4: breathing core
        val breath = 0.55f + 0.45f * sin(tt * 2.1f)
        val coreR = r * (0.42f + 0.06f * breath * energy)
        val coreC = Offset(r * 0.92f, r * 0.9f)
        drawCircle(
            Brush.radialGradient(
                0f to Color(0xFFF4F4F4).copy(alpha = (0.30f + 0.22f * breath * energy).coerceIn(0f, 1f)),
                0.4f to Color(0xFFB0127A).copy(alpha = (0.20f * energy).coerceIn(0f, 1f)),
                1f to Color(0x00B0127A),
                center = coreC, radius = coreR,
            ),
            radius = coreR, center = coreC, blendMode = BlendMode.Screen,
        )

        // Layer 5: tiny drifting particles
        if (!offline) {
            for (p in Particles) {
                val ang = p.a + tt * p.speed
                val rad = r * p.rr
                val fade = 0.25f + 0.35f * sin(tt * 1.4f + p.ph)
                if (fade <= 0f) continue
                drawCircle(
                    Color(0xFFE4E5E7).copy(alpha = (fade * 0.6f * energy).coerceIn(0f, 1f)),
                    radius = max(0.6f, p.size * k),
                    center = Offset(r + cos(ang) * rad, r + sin(ang * 0.8f) * rad * 0.9f),
                )
            }
        }

        // Spherical lighting on top: edge vignette, soft highlight, lower shadow
        drawRect(
            Brush.radialGradient(
                0f to Color.Transparent, 0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f),
                center = c, radius = r,
            ),
        )
        drawRect(
            Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(r * 0.36f, r * 0.30f), radius = r * 0.55f,
            ),
        )
        drawRect(
            Brush.radialGradient(
                listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent),
                center = Offset(r * 0.62f, r * 0.82f), radius = r * 0.7f,
            ),
        )

        if (error) {
            val flick = abs(sin(tt * 8f))
            drawRect(Color(0xFFF87171).copy(alpha = 0.18f * flick))
        }
    }

    // Rim light along the top-left edge
    val rim = r * 0.94f
    drawArc(
        color = Color(0xFFE4E5E7).copy(alpha = 0.28f),
        startAngle = 189f, sweepAngle = 126f, useCenter = false,
        topLeft = Offset(r - rim, r - rim), size = androidx.compose.ui.geometry.Size(rim * 2f, rim * 2f),
        style = Stroke(width = max(1f, r * 0.02f), cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawRibbon(rb: Ribbon, tt: Float, w: Float, k: Float, energy: Float) {
    val ph = tt * rb.speed + rb.ph
    val y0 = rb.y0 * w
    val amp = rb.amp * w * (0.85f + 0.15f * energy)
    val path = Path().apply {
        moveTo(-w * 0.15f, y0)
        cubicTo(
            w * rb.fx1 * 3f, y0 + sin(ph) * amp,
            w - w * rb.fx2 * 3f, y0 + cos(ph * 1.3f + 1f) * amp,
            w * 1.15f, y0 + sin(ph * 0.7f + 2f) * amp * 0.6f,
        )
    }
    // width, glow alpha, body alpha, core alpha, colour
    val lineW: Float
    val glow: Float
    val body: Float
    val core: Float
    val tint: Color
    when (rb.depth) {
        0 -> { lineW = 1.1f; glow = 0.10f; body = 0.16f; core = 0.20f; tint = Color(196, 150, 160) }
        1 -> { lineW = 1.6f; glow = 0.16f; body = 0.30f; core = 0.50f; tint = Color(191, 194, 199) }
        else -> { lineW = 2.1f; glow = 0.22f; body = 0.42f; core = 0.90f; tint = Color(228, 229, 231) }
    }
    val round = StrokeCap.Round
    // soft glow: two wide, faint passes stand in for the web blur
    drawPath(path, tint, alpha = (glow * 0.6f * energy).coerceIn(0f, 1f), style = Stroke(lineW * 6.5f * k, cap = round))
    drawPath(path, tint, alpha = (glow * energy).coerceIn(0f, 1f), style = Stroke(lineW * 4.2f * k, cap = round))
    // silver body
    drawPath(path, tint, alpha = (body * energy).coerceIn(0f, 1f), style = Stroke(lineW * 1.6f * k, cap = round))
    // thin bright core
    drawPath(path, Color.White, alpha = core, style = Stroke(max(0.6f, lineW * 0.55f * k), cap = round))
}
