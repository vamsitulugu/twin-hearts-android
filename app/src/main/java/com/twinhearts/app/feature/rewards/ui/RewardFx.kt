package com.twinhearts.app.feature.rewards.ui

import androidx.compose.animation.core.Animatable
import com.twinhearts.app.core.sound.TwinSound
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.twinhearts.app.R
import com.twinhearts.app.core.common.rememberReduceMotion
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.random.Random
import kotlinx.coroutines.launch

/**
 * Why this file exists: the little celebrations of the Rewards screens (R-55: all of them stop when Reduce motion is on).
 *  - [CoinShower]: gold coins burst out and fall.
 *  - [rememberCoinBump]: the coin icon pops when the balance goes up.
 *  - [ScratchFoil]: a gold foil you really scratch off with your finger.
 *  - [AdScratchDialog]: after a rewarded ad, a scratch card pays the coins.
 */

private class CoinParticle(
    val vx: Float, // dp per second
    val vy: Float, // dp per second (negative = up)
    val radius: Float, // dp
    val spin: Float,
    val spinSpeed: Float,
)

/** Fires one burst of coins each time [trigger] changes to a value above 0. Draws nothing the rest of the time. */
@Composable
internal fun CoinShower(trigger: Int, modifier: Modifier = Modifier, count: Int = 22) {
    val reduce = rememberReduceMotion()
    val progress = remember { Animatable(1f) }
    val particles = remember(trigger) {
        List(count) {
            val angle = Math.toRadians(Random.nextDouble(-155.0, -25.0))
            val speed = Random.nextFloat() * 260f + 220f
            CoinParticle(
                vx = (cos(angle) * speed).toFloat(),
                vy = (kotlin.math.sin(angle) * speed).toFloat(),
                radius = Random.nextFloat() * 5f + 8f,
                spin = Random.nextFloat() * 6.28f,
                spinSpeed = Random.nextFloat() * 10f + 6f,
            )
        }
    }
    LaunchedEffect(trigger) {
        if (trigger > 0 && !reduce) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMillis = 1600, easing = LinearEasing))
        }
    }
    Canvas(modifier) {
        val p = progress.value
        if (p <= 0f || p >= 1f) return@Canvas
        val seconds = p * 1.6f
        val d = density
        val originX = size.width / 2f
        val originY = size.height * 0.5f
        val fade = if (p > 0.7f) (1f - p) / 0.3f else 1f
        for (c in particles) {
            val x = originX + c.vx * seconds * d
            val y = originY + (c.vy * seconds + 0.5f * 900f * seconds * seconds) * d
            val r = c.radius * d
            // Squeezing the width with a cosine makes the coin look like it flips in the air.
            val flip = abs(cos(c.spin + c.spinSpeed * seconds)).coerceAtLeast(0.15f)
            val topLeft = Offset(x - r * flip, y - r)
            val oval = Size(2f * r * flip, 2f * r)
            drawOval(color = RewardsStyle.GoldDeep.copy(alpha = fade), topLeft = topLeft, size = oval)
            drawOval(
                color = RewardsStyle.Gold.copy(alpha = fade),
                topLeft = Offset(topLeft.x + r * 0.18f * flip, topLeft.y + r * 0.18f),
                size = Size(oval.width * 0.82f, oval.height * 0.82f),
            )
            drawOval(
                color = Color.White.copy(alpha = 0.55f * fade),
                topLeft = Offset(x - r * 0.45f * flip, y - r * 0.6f),
                size = Size(r * 0.5f * flip, r * 0.5f),
            )
        }
    }
}

/** A scale for the coin icon that pops (1 -> 1.35 -> 1) each time [balance] goes up. */
@Composable
internal fun rememberCoinBump(balance: Int): Float {
    val reduce = rememberReduceMotion()
    val bump = remember { Animatable(1f) }
    var previous by remember { mutableIntStateOf(balance) }
    LaunchedEffect(balance) {
        if (balance > previous && !reduce) {
            bump.animateTo(1.35f, tween(160))
            bump.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
        }
        previous = balance
    }
    return bump.value
}

/**
 * Gold foil that is scratched away by dragging a finger over it. When about half is gone it calls [onThreshold] once
 * (the caller then asks the server for the prize). [revealNow] fades what is left of the foil out.
 */
@Composable
internal fun ScratchFoil(
    hint: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    revealNow: Boolean = false,
    onThreshold: () -> Unit,
) {
    val density = LocalDensity.current
    val brushPx = with(density) { 34.dp.toPx() }
    val path = remember { Path() }
    var tick by remember { mutableIntStateOf(0) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var fired by remember { mutableStateOf(false) }
    val cols = 14
    val rows = 7
    val hit = remember { BooleanArray(cols * rows) }
    val latestThreshold by rememberUpdatedState(onThreshold)
    val foilAlpha by animateFloatAsState(if (revealNow) 0f else 1f, tween(450), label = "foilAlpha")
    val reduce = rememberReduceMotion()
    val shimmer = rememberInfiniteTransition(label = "foilShimmer")
        .animateFloat(
            initialValue = -0.4f,
            targetValue = 1.4f,
            animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
            label = "foilShimmerX",
        )

    fun mark(p: Offset) {
        if (size.width == 0 || size.height == 0) return
        TwinSound.scratch() // a soft scratch grain (it throttles itself)
        val cellW = size.width.toFloat() / cols
        val cellH = size.height.toFloat() / rows
        val reach = brushPx / 2f
        for (c in 0 until cols) {
            for (r in 0 until rows) {
                if (hypot((c + 0.5f) * cellW - p.x, (r + 0.5f) * cellH - p.y) <= reach) hit[r * cols + c] = true
            }
        }
        if (!fired && hit.count { it } >= (cols * rows * 0.45f)) {
            fired = true
            latestThreshold()
        }
    }

    Box(modifier) {
        Canvas(
            Modifier
                .fillMaxSize()
                .onSizeChanged { size = it }
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    alpha = foilAlpha
                }
                .pointerInput(enabled) {
                    if (enabled) {
                        detectTapGestures { o ->
                            path.moveTo(o.x, o.y)
                            path.lineTo(o.x + 0.1f, o.y)
                            mark(o)
                            tick++
                        }
                    }
                }
                .pointerInput(enabled) {
                    if (enabled) {
                        detectDragGestures(
                            onDragStart = { o ->
                                path.moveTo(o.x, o.y)
                                path.lineTo(o.x + 0.1f, o.y)
                                mark(o)
                                tick++
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                path.lineTo(change.position.x, change.position.y)
                                mark(change.position)
                                tick++
                            },
                        )
                    }
                },
        ) {
            if (tick < 0) return@Canvas // reading the counter makes the foil redraw after every scratch
            drawRect(Brush.linearGradient(listOf(RewardsStyle.GoldDeep, RewardsStyle.Gold, Color(0xFFFCD34D), RewardsStyle.GoldDeep)))
            // Tiny dots, like a real scratch card.
            val step = 22.dp.toPx()
            var y = step / 2f
            while (y < this.size.height) {
                var x = step / 2f
                while (x < this.size.width) {
                    drawCircle(Color.White.copy(alpha = 0.16f), radius = 2.5.dp.toPx(), center = Offset(x, y))
                    x += step
                }
                y += step
            }
            // A soft light band that keeps sliding across, so the foil looks alive.
            if (!reduce) {
                val bandX = this.size.width * shimmer.value
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
                        startX = bandX - this.size.width * 0.2f,
                        endX = bandX + this.size.width * 0.2f,
                    ),
                )
            }
            drawPath(
                path = path,
                color = Color.Black,
                style = Stroke(width = brushPx, cap = StrokeCap.Round, join = StrokeJoin.Round),
                blendMode = BlendMode.Clear,
            )
        }
        val hintAlpha by animateFloatAsState(if (tick > 0 || revealNow) 0f else 1f, tween(250), label = "hintAlpha")
        Column(
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = hintAlpha * foilAlpha },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("🎫", fontSize = 28.sp)
            Spacer(Modifier.height(4.dp))
            Text(hint, color = RewardsStyle.GoldInk, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

private enum class AdScratchPhase { Scratching, Claiming, Won, Failed }

/**
 * After a rewarded ad: a card pops up, the person scratches it, and only then are the coins paid.
 * [onClaim] asks the server (returns the coins it paid, or null if it could not). [onClose] is called when the person taps
 * Collect / Close; the screen behind then shows the new balance counting up.
 */
@Composable
internal fun AdScratchDialog(
    coins: Int,
    onClaim: suspend () -> Int?,
    onClose: () -> Unit,
) {
    var phase by remember { mutableStateOf(AdScratchPhase.Scratching) }
    var earned by remember { mutableIntStateOf(coins) }
    var shower by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val finished = phase == AdScratchPhase.Won || phase == AdScratchPhase.Failed

    fun reveal() {
        if (phase != AdScratchPhase.Scratching) return
        phase = AdScratchPhase.Claiming
        scope.launch {
            val paid = onClaim()
            if (paid == null) {
                phase = AdScratchPhase.Failed
            } else {
                earned = paid
                phase = AdScratchPhase.Won
                if (paid > 0) { shower++; TwinSound.ad() } // rising chime, then coins
            }
        }
    }

    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)) }
    val pop by animateFloatAsState(
        targetValue = if (phase == AdScratchPhase.Won) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow),
        label = "prizePop",
    )

    Dialog(
        onDismissRequest = { if (finished) onClose() },
        properties = DialogProperties(
            dismissOnBackPress = finished,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        val s = 0.6f + 0.4f * enter.value
                        scaleX = s
                        scaleY = s
                        rotationZ = (1f - enter.value) * -8f
                        alpha = enter.value.coerceIn(0f, 1f)
                    }
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF2E1B3F), Color(0xFF160A22))))
                    .border(1.5.dp, RewardsStyle.Gold.copy(alpha = 0.55f), RoundedCornerShape(26.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.ad_scratch_title),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.ad_scratch_sub),
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))

                val cardDescription = stringResource(R.string.ad_scratch_cd)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .semantics { contentDescription = cardDescription }
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.5.dp, RewardsStyle.Gold.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .background(RewardsStyle.Gold.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🪙", fontSize = 40.sp, modifier = Modifier.graphicsLayer {
                            val s = 1f + 0.35f * pop
                            scaleX = s
                            scaleY = s
                        })
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "+$earned",
                            color = RewardsStyle.Gold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 34.sp,
                            modifier = Modifier.graphicsLayer {
                                val s = 1f + 0.2f * pop
                                scaleX = s
                                scaleY = s
                            },
                        )
                        Text("Gold Coins", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    ScratchFoil(
                        hint = stringResource(R.string.ad_scratch_hint),
                        modifier = Modifier.fillMaxSize(),
                        enabled = phase == AdScratchPhase.Scratching,
                        revealNow = phase == AdScratchPhase.Claiming || phase == AdScratchPhase.Won,
                        onThreshold = ::reveal,
                    )
                }

                Spacer(Modifier.height(14.dp))
                if (phase == AdScratchPhase.Won) {
                    Text(
                        stringResource(R.string.ad_scratch_won, earned),
                        color = RewardsStyle.Gold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                } else if (phase == AdScratchPhase.Failed) {
                    Text(
                        stringResource(R.string.ad_scratch_failed),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.verticalGradient(listOf(RewardsStyle.Gold, RewardsStyle.GoldDeep)))
                        .graphicsLayer { alpha = if (phase == AdScratchPhase.Claiming) 0.5f else 1f }
                        .clickable(enabled = phase != AdScratchPhase.Claiming, role = Role.Button) {
                            if (finished) onClose() else reveal()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(
                            when (phase) {
                                AdScratchPhase.Scratching -> R.string.ad_scratch_reveal
                                AdScratchPhase.Claiming -> R.string.ad_scratch_claiming
                                AdScratchPhase.Won -> R.string.ad_scratch_collect
                                AdScratchPhase.Failed -> R.string.ad_scratch_close
                            },
                        ),
                        color = RewardsStyle.GoldInk,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.5.sp,
                    )
                }
            }
            // Coins burst over the whole dialog when the prize is paid.
            CoinShower(trigger = shower, modifier = Modifier.fillMaxSize())
        }
    }
}
