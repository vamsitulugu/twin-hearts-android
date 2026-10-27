package com.twinhearts.app.feature.twinai.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.twinhearts.app.core.common.rememberReduceMotion
import com.twinhearts.app.core.design.TwinTheme

/*
 * Why this file exists: Twin's illustrations (the robot holding a heart, the small chat avatar and
 * the orb). They are drawn in code so they stay sharp on every screen and need no image files.
 * The white/pink robot body colours below are part of the illustration itself (like the web SVG),
 * not UI colours; every UI colour still comes from TwinTheme (R-50).
 */
private object Art {
    val bodyLight = Color(0xFFFFFFFF)
    val bodyMid = Color(0xFFF6D9DE)
    val bodyShade = Color(0xFFB4697A)
    val visor = Color(0xFF0B0308)
    val avatarBase = Color(0xFF2A0419)
}

/** The big robot used in the hero card. Draws on a 200 x 220 grid, scaled to fit. */
@Composable
fun TwinRobot(modifier: Modifier = Modifier) {
    val accent = TwinTheme.colors.accent
    val primary = TwinTheme.colors.primaryStart
    Canvas(modifier = modifier.aspectRatio(200f / 220f)) {
        val s = size.width / 200f
        fun o(x: Float, y: Float) = Offset(x * s, y * s)

        // soft glow behind the robot
        drawCircle(
            brush = Brush.radialGradient(listOf(accent.copy(alpha = 0.35f), Color.Transparent), center = o(100f, 120f), radius = 100f * s),
            radius = 100f * s, center = o(100f, 120f),
        )
        val body = Brush.radialGradient(
            listOf(Art.bodyLight, Art.bodyMid, Art.bodyShade), center = o(84f, 130f), radius = 70f * s,
        )
        // feet
        drawOval(body, o(50f, 196f), Size(44f * s, 20f * s))
        drawOval(body, o(106f, 196f), Size(44f * s, 20f * s))
        // body
        drawOval(body, o(58f, 116f), Size(84f * s, 88f * s))
        // heart held in front
        drawHeart(o(100f, 150f), 26f * s, Brush.linearGradient(listOf(accent, primary)))
        // hands
        drawOval(body, o(44f, 134f), Size(28f * s, 40f * s))
        drawOval(body, o(128f, 134f), Size(28f * s, 40f * s))
        // ear pads
        drawOval(Brush.linearGradient(listOf(accent, primary)), o(26f, 56f), Size(26f * s, 46f * s))
        drawOval(Brush.linearGradient(listOf(accent, primary)), o(148f, 56f), Size(26f * s, 46f * s))
        // head
        drawOval(body, o(40f, 28f), Size(120f * s, 100f * s))
        // visor
        drawRoundRect(Art.visor, o(58f, 52f), Size(84f * s, 52f * s), CornerRadius(26f * s))
        // happy eyes and smile
        val stroke = Stroke(width = 5f * s, cap = StrokeCap.Round)
        drawArc(accent, 200f, 140f, false, o(70f, 68f), Size(20f * s, 18f * s), style = stroke)
        drawArc(accent, 200f, 140f, false, o(110f, 68f), Size(20f * s, 18f * s), style = stroke)
        drawArc(accent, 20f, 140f, false, o(90f, 80f), Size(20f * s, 12f * s), style = Stroke(width = 3f * s, cap = StrokeCap.Round))
    }
}

/** Small round face used next to Twin's chat bubbles. */
@Composable
fun TwinAvatar(modifier: Modifier = Modifier, size: Dp = 32.dp) {
    val accent = TwinTheme.colors.accent
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.width / 48f
        fun o(x: Float, y: Float) = Offset(x * s, y * s)
        drawCircle(Art.avatarBase, radius = 22.5f * s, center = o(24f, 24f))
        drawOval(
            Brush.radialGradient(listOf(Art.bodyLight, Art.bodyMid, Art.bodyShade), center = o(18f, 17f), radius = 22f * s),
            o(9f, 11.5f), Size(30f * s, 26f * s),
        )
        drawOval(accent, o(6.2f, 20.6f), Size(5.2f * s, 8.8f * s))
        drawOval(accent, o(36.6f, 20.6f), Size(5.2f * s, 8.8f * s))
        drawRoundRect(Art.visor, o(12.5f, 16.5f), Size(23f * s, 15f * s), CornerRadius(7f * s))
        val eye = Stroke(width = 2f * s, cap = StrokeCap.Round)
        drawArc(accent, 200f, 140f, false, o(17.5f, 21f), Size(5f * s, 5f * s), style = eye)
        drawArc(accent, 200f, 140f, false, o(25.5f, 21f), Size(5f * s, 5f * s), style = eye)
    }
}

/** The glowing Twin orb that sits on the input bar. Slowly spins unless animations are off (R-55). */
@Composable
fun TwinOrb(modifier: Modifier = Modifier, size: Dp = 56.dp) {
    val colors = TwinTheme.colors
    val reduce = rememberReduceMotion()
    val angle = if (reduce) {
        0f
    } else {
        val t = rememberInfiniteTransition(label = "orb")
        val a by t.animateFloat(0f, 360f, infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Restart), label = "orbAngle")
        a
    }
    Canvas(modifier = modifier.size(size)) {
        val r = this.size.width / 2f
        drawCircle(
            Brush.radialGradient(
                listOf(colors.accent, colors.primaryEnd, colors.background),
                center = Offset(r * 0.8f, r * 0.7f), radius = r * 1.3f,
            ),
            radius = r,
        )
        rotate(angle, pivot = center) {
            val line = Stroke(width = r * 0.05f, cap = StrokeCap.Round)
            val white = Color.White.copy(alpha = 0.75f)
            drawArc(white, 200f, 70f, false, Offset(r * 0.25f, r * 0.45f), Size(r * 1.5f, r * 0.9f), style = line)
            drawArc(white.copy(alpha = 0.5f), 20f, 80f, false, Offset(r * 0.3f, r * 0.6f), Size(r * 1.4f, r * 0.8f), style = line)
            drawArc(white.copy(alpha = 0.35f), 110f, 60f, false, Offset(r * 0.5f, r * 0.3f), Size(r * 1.0f, r * 1.3f), style = line)
        }
    }
}

private fun DrawScope.drawHeart(center: Offset, half: Float, brush: Brush) {
    val path = Path().apply {
        moveTo(center.x, center.y + half)
        cubicTo(center.x - half * 1.9f, center.y - half * 0.2f, center.x - half * 0.8f, center.y - half * 1.5f, center.x, center.y - half * 0.5f)
        cubicTo(center.x + half * 0.8f, center.y - half * 1.5f, center.x + half * 1.9f, center.y - half * 0.2f, center.x, center.y + half)
        close()
    }
    drawPath(path, brush)
}
