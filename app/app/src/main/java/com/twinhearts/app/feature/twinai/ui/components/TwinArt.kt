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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.twinhearts.app.core.common.rememberReduceMotion
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.twinai.domain.model.OrbState
import kotlin.math.PI
import kotlin.math.sin

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

    /** The eyes, smile and bow are the web's fixed pink, not a UI colour. */
    val eye = Color(0xFFC21A8B)
    val floaterPink = Color(0xFFB0127A)
    val bowShine = Color(0xFFFDD5EC)

    val visorBrush = Brush.verticalGradient(listOf(Color(0xFF24101B), Color(0xFF040102)), startY = 50f, endY = 118f)

    /** The web's `HEART` path, centred on (0, 0). */
    val heartPath: Path = Path().apply {
        moveTo(0f, 24f)
        cubicTo(-38f, -2f, -28f, -34f, -11f, -31f)
        cubicTo(-4f, -30f, 0f, -22f, 0f, -18f)
        cubicTo(0f, -22f, 4f, -30f, 11f, -31f)
        cubicTo(28f, -34f, 38f, -2f, 0f, 24f)
        close()
    }

    fun bodyBrush(): Brush = Brush.radialGradient(
        0f to Color(0xFFFFF2F4), 0.5f to Color(0xFFE0C5D5), 1f to Color(0xFF9B4D5E),
        center = Offset(94f, 151f), radius = 95f,
    )

    fun headBrush(): Brush = Brush.radialGradient(
        0f to bodyLight, 0.42f to bodyMid, 1f to bodyShade,
        center = Offset(87f, 50f), radius = 128f,
    )

    fun redBrush(cx: Float = 36f, cy: Float = 90f, r: Float = 40f): Brush = Brush.radialGradient(
        0f to Color(0xFFC21A8B), 0.5f to Color(0xFFB0127A), 1f to Color(0xFF940857),
        center = Offset(cx, cy), radius = r,
    )
}

/** The single robot of the YOU hero, holding a heart. Same shapes and gradients as the web SVG (220 x 250 grid). */
@Composable
fun TwinRobot(modifier: Modifier = Modifier) {
    val floats = rememberFloatState()
    Canvas(modifier = modifier.aspectRatio(220f / 250f).floatingLayer(floats)) {
        val k = size.width / 220f
        withTransform({ scale(k, k, Offset.Zero) }) {
            drawFloaters(floats.phase(), listOf(Floater(206f, 70f, .2f, 0f), Floater(20f, 120f, .16f, 1.2f), Floater(196f, 168f, .13f, .6f)))
            drawRobot(bow = false)
            drawHeart(110f, 176f, 1.25f)
            drawArms(110f, 176f)
        }
    }
}

/** The pair of robots sharing a heart: the hero of the BOTH tab (400 x 262 grid). */
@Composable
fun TwinRobotPair(modifier: Modifier = Modifier) {
    val floats = rememberFloatState()
    Canvas(modifier = modifier.aspectRatio(400f / 262f).floatingLayer(floats)) {
        val k = size.width / 400f
        withTransform({ scale(k, k, Offset.Zero) }) {
            drawFloaters(
                floats.phase(),
                listOf(Floater(30f, 60f, .22f, 0f), Floater(372f, 150f, .2f, 1f), Floater(356f, 40f, .14f, .5f), Floater(48f, 170f, .14f, 1.6f)),
            )
            withTransform({ translate(6f, 12f); scale(.93f, .93f, Offset.Zero) }) { drawRobot(bow = false) }
            withTransform({ translate(176f, 6f); scale(.93f, .93f, Offset.Zero) }) { drawRobot(bow = true) }
            drawHeart(200f, 198f, 1.7f)
            rotate(-24f, Offset(148f, 204f)) { drawOval(Art.bodyBrush(), Offset(131f, 179f), Size(34f, 50f)) }
            rotate(24f, Offset(252f, 204f)) { drawOval(Art.bodyBrush(), Offset(235f, 179f), Size(34f, 50f)) }
        }
    }
}

/** One small floating heart around the robots. [delay] (seconds) shifts its blink so they do not pulse together. */
private class Floater(val x: Float, val y: Float, val scale: Float, val delay: Float)

/**
 * The slow bobbing and blinking, shared by the whole illustration. The values are read only while
 * drawing / laying out the layer, so the screen does not recompose every frame.
 */
private class FloatState(private val bob: State<Float>, private val blink: State<Float>) {
    fun bobValue() = bob.value
    fun phase() = blink.value
}

@Composable
private fun rememberFloatState(): FloatState {
    val reduce = rememberReduceMotion()
    if (reduce) {
        return remember { FloatState(mutableFloatStateOf(0f), mutableFloatStateOf(0f)) }
    }
    val t = rememberInfiniteTransition(label = "robot")
    val bob = t.animateFloat(0f, 1f, infiniteRepeatable(tween(3_200, easing = LinearEasing), RepeatMode.Reverse), label = "bob")
    val blink = t.animateFloat(0f, 1f, infiniteRepeatable(tween(3_000, easing = LinearEasing), RepeatMode.Restart), label = "blink")
    return remember(bob, blink) { FloatState(bob, blink) }
}

/** Moves the whole illustration up and down by 6 dp (web `twFloat`). */
private fun Modifier.floatingLayer(state: FloatState): Modifier = graphicsLayer {
    translationY = -6.dp.toPx() * state.bobValue()
}

private fun DrawScope.drawFloaters(phase: Float, list: List<Floater>) {
    list.forEach { f ->
        val wave = 0.5f + 0.5f * sin(2.0 * PI * (phase + f.delay / 3f)).toFloat()
        withTransform({ translate(f.x, f.y); scale(f.scale, f.scale, Offset.Zero) }) {
            drawPath(Art.heartPath, Art.floaterPink.copy(alpha = 0.55f + 0.3f * wave))
        }
    }
}

/** One robot on a 220 x 250 grid: shadow, feet, body, head, ear pads, visor, eyes, smile and (for the girl) the bow. */
private fun DrawScope.drawRobot(bow: Boolean) {
    val eye = Art.eye
    drawOval(Color.Black.copy(alpha = 0.5f), Offset(46f, 229f), Size(128f, 18f))
    val body = Art.bodyBrush()
    drawOval(body, Offset(61f, 217f), Size(42f, 22f))
    drawOval(body, Offset(117f, 217f), Size(42f, 22f))
    drawPath(
        Path().apply {
            moveTo(60f, 180f)
            cubicTo(60f, 148f, 82f, 134f, 110f, 134f)
            cubicTo(138f, 134f, 160f, 148f, 160f, 180f)
            cubicTo(160f, 208f, 138f, 230f, 110f, 230f)
            cubicTo(82f, 230f, 60f, 208f, 60f, 180f)
            close()
        },
        body,
    )
    drawOval(Art.headBrush(), Offset(39f, 24f), Size(142f, 120f))
    drawOval(Art.redBrush(), Offset(27f, 71f), Size(22f, 38f))
    drawOval(Art.redBrush(), Offset(171f, 71f), Size(22f, 38f))
    drawRoundRect(Art.visorBrush, Offset(52f, 50f), Size(116f, 68f), CornerRadius(32f))
    drawRoundRect(Color.White.copy(alpha = 0.14f), Offset(52f, 50f), Size(116f, 68f), CornerRadius(32f), style = Stroke(1f))
    drawPath(
        Path().apply { moveTo(66f, 67f); quadraticBezierTo(86f, 55f, 112f, 57f) },
        Color.White.copy(alpha = 0.22f), style = Stroke(3f, cap = StrokeCap.Round),
    )
    val eyes = Path().apply {
        moveTo(78f, 93f); quadraticBezierTo(88f, 76f, 98f, 93f)
        moveTo(122f, 93f); quadraticBezierTo(132f, 76f, 142f, 93f)
    }
    // Soft glow under the eyes (the web blurs a copy), then the crisp line.
    drawPath(eyes, eye.copy(alpha = 0.35f), style = Stroke(11f, cap = StrokeCap.Round))
    drawPath(eyes, eye, style = Stroke(4.5f, cap = StrokeCap.Round))
    drawPath(
        Path().apply { moveTo(101f, 103f); quadraticBezierTo(110f, 111f, 119f, 103f) },
        eye, style = Stroke(3.5f, cap = StrokeCap.Round),
    )
    if (bow) drawBow()
}

private fun DrawScope.drawBow() {
    rotate(14f, Offset(150f, 38f)) {
        val right = Path().apply { moveTo(150f, 40f); lineTo(188f, 14f); quadraticBezierTo(196f, 34f, 188f, 58f); close() }
        val left = Path().apply { moveTo(150f, 40f); lineTo(112f, 14f); quadraticBezierTo(104f, 34f, 112f, 58f); close() }
        drawPath(right, Art.eye)
        drawPath(left, Art.eye)
        drawCircle(Art.eye, 10f, Offset(150f, 40f))
        drawCircle(Art.bowShine.copy(alpha = 0.8f), 3.5f, Offset(147f, 37f))
    }
}

private fun DrawScope.drawHeart(x: Float, y: Float, factor: Float) {
    withTransform({ translate(x, y); scale(factor, factor, Offset.Zero) }) {
        withTransform({ scale(1.12f, 1.12f, Offset.Zero) }) { drawPath(Art.heartPath, Art.floaterPink.copy(alpha = 0.3f)) }
        drawPath(Art.heartPath, Art.redBrush(cx = -10f, cy = -14f, r = 40f))
        drawPath(
            Path().apply { moveTo(-17f, -18f); cubicTo(-23f, -14f, -24f, -6f, -21f, -1f) },
            Color.White.copy(alpha = 0.55f), style = Stroke(3.2f, cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.drawArms(cx: Float, cy: Float) {
    rotate(-20f, Offset(cx - 38f, cy)) { drawOval(Art.bodyBrush(), Offset(cx - 55f, cy - 25f), Size(34f, 50f)) }
    rotate(20f, Offset(cx + 38f, cy)) { drawOval(Art.bodyBrush(), Offset(cx + 21f, cy - 25f), Size(34f, 50f)) }
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

/**
 * The glowing Twin orb. It spins slowly unless animations are off (R-55). [state] changes how it
 * glows (thinking and speaking pulse, an error turns the glow red, listening follows your voice
 * through [level], 0..1), like the states of the web orb.
 */
@Composable
fun TwinOrb(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    state: OrbState = OrbState.Idle,
    level: Float = 0f,
) {
    val colors = TwinTheme.colors
    val reduce = rememberReduceMotion()
    val angle: Float
    val pulse: Float
    if (reduce) {
        angle = 0f
        pulse = 0f
    } else {
        val t = rememberInfiniteTransition(label = "orb")
        val a by t.animateFloat(0f, 360f, infiniteRepeatable(tween(if (state == OrbState.Thinking) 5_000 else 14_000, easing = LinearEasing), RepeatMode.Restart), label = "orbAngle")
        val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1_100, easing = LinearEasing), RepeatMode.Reverse), label = "orbPulse")
        angle = a
        pulse = p
    }
    val glowColor = when (state) {
        OrbState.Error -> colors.error
        OrbState.Offline -> colors.inkFaint
        else -> colors.accent
    }
    val glow = when (state) {
        OrbState.Idle -> 0.0f
        OrbState.Responding -> 0.18f
        OrbState.Error -> 0.35f
        OrbState.Thinking -> 0.25f + 0.2f * pulse
        OrbState.Speaking -> 0.3f + 0.25f * pulse
        OrbState.Listening -> 0.25f + 0.5f * level
        OrbState.Offline -> 0.12f
    }
    val grow = if (state == OrbState.Listening) 1f + 0.12f * level else 1f
    Canvas(modifier = modifier.size(size).graphicsLayer { scaleX = grow; scaleY = grow }) {
        val r = this.size.width / 2f
        if (glow > 0f) {
            drawCircle(
                Brush.radialGradient(
                    listOf(glowColor.copy(alpha = glow), Color.Transparent),
                    center = center, radius = r * (1.55f + 0.2f * pulse),
                ),
                radius = r * (1.55f + 0.2f * pulse),
            )
        }
        drawCircle(
            Brush.radialGradient(
                listOf(if (state == OrbState.Offline) colors.inkFaint else colors.accent, if (state == OrbState.Offline) colors.inkFaint.copy(alpha = 0.6f) else colors.primaryEnd, colors.background),
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
