package com.twinhearts.app.feature.rewards.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.LocalReduceMotionSetting
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.feature.rewards.domain.SpinWheelSlice
import kotlin.random.Random

private enum class WheelPhase { Idle, Waiting, Landing }

private val FallbackColors = listOf(
    Color(0xFFF59E0B), Color(0xFFEC4899), Color(0xFF8B5CF6), Color(0xFF10B981),
    Color(0xFF3B82F6), Color(0xFFF43F5E), Color(0xFF06B6D4), Color(0xFFEAB308),
)
/** Same defaults as the server (routes/rewards.js), used only if the summary arrives without a wheel. */
private val FallbackSlices = listOf(
    SpinWheelSlice(0, 10, "10 Coins", "🪙", "#F59E0B"),
    SpinWheelSlice(1, 25, "25 Coins", "✨", "#EC4899"),
    SpinWheelSlice(2, 15, "15 Coins", "🪙", "#8B5CF6"),
    SpinWheelSlice(3, 50, "50 Coins", "🎉", "#10B981"),
    SpinWheelSlice(4, 20, "20 Coins", "🪙", "#3B82F6"),
    SpinWheelSlice(5, 100, "100 Coins", "💎", "#F43F5E"),
    SpinWheelSlice(6, 15, "15 Coins", "🪙", "#06B6D4"),
    SpinWheelSlice(7, 30, "30 Coins", "🌟", "#EAB308"),
)
private const val WAIT_DEGREES_PER_SECOND = 540f
private const val LAND_MILLIS = 4200f
private val LandEasing = CubicBezierEasing(0.12f, 0.6f, 0.1f, 1f)

/**
 * Spin & Win page. The coin balance is above the wheel, every slice shows how many coins it pays,
 * and a "You won N coins" line appears above the wheel when it stops.
 *
 * The wheel is turned by the frame clock (not Compose's animateTo) so it also spins on phones whose
 * animator scale is 0; only the in-app Reduce motion switch makes it jump straight to the result.
 * While the server decides the prize the wheel keeps turning, then it slows down and stops with the
 * winning slice under the pointer.
 */
@Composable
fun SpinWheelScreen(
    viewModel: RewardsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = TwinTheme.colors
    val summary = state.summary ?: return
    val appReduceMotion = LocalReduceMotionSetting.current
    val adPlayer = rememberAdPlayer()

    val slices = remember(summary.wheelSlices) { summary.wheelSlices.sortedBy { it.index }.ifEmpty { FallbackSlices } }
    val sliceCount = slices.size

    var rotation by remember { mutableFloatStateOf(0f) }
    var phase by remember { mutableStateOf(WheelPhase.Idle) }
    var balanceBefore by remember { mutableIntStateOf(summary.balance) }
    var wonCoins by remember { mutableIntStateOf(-1) }
    // Each time the wheel stops on a prize this goes up by one and a burst of coins flies out.
    var shower by remember { mutableIntStateOf(0) }

    // The balance on screen only changes once the wheel has stopped.
    val balanceTarget = if (phase == WheelPhase.Idle) summary.balance else balanceBefore
    val shown = rememberCountedBalance(balanceTarget)
    val bump = rememberCoinBump(balanceTarget)

    // The server's answer is here -> stop waiting and land on its slice.
    LaunchedEffect(state.lastSpinResult) {
        if (state.lastSpinResult != null && phase == WheelPhase.Waiting) phase = WheelPhase.Landing
    }
    LaunchedEffect(state.spinning) {
        if (state.spinning && phase == WheelPhase.Idle) {
            // A spin that started without the button (watch-an-ad flow): start turning the wheel too.
            balanceBefore = summary.balance
            wonCoins = -1
            phase = WheelPhase.Waiting
        } else if (!state.spinning && phase == WheelPhase.Waiting && state.lastSpinResult == null) {
            // The request ended without a result (offline, server asleep): give the wheel back.
            phase = WheelPhase.Idle
        }
    }

    LaunchedEffect(phase) {
        when (phase) {
            WheelPhase.Idle -> Unit
            WheelPhase.Waiting -> {
                if (!appReduceMotion) {
                    var last = withFrameNanos { it }
                    while (true) {
                        val now = withFrameNanos { it }
                        rotation += (now - last) / 1_000_000_000f * WAIT_DEGREES_PER_SECOND
                        last = now
                    }
                }
            }
            WheelPhase.Landing -> {
                val result = state.lastSpinResult
                if (result != null) {
                    val position = slices.indexOfFirst { it.index == result.sliceIndex }
                        .takeIf { it >= 0 } ?: result.sliceIndex.mod(sliceCount)
                    val sweep = 360f / sliceCount
                    val centre = position * sweep + sweep / 2f
                    val jitter = (Random.nextFloat() - 0.5f) * sweep * 0.6f
                    // The pointer is at the top (270 degrees on the canvas); rotate that slice under it.
                    val desired = (((270f - centre - jitter) % 360f) + 360f) % 360f
                    val start = rotation
                    val target = (start - (start % 360f)) + desired + 360f * 6
                    if (appReduceMotion) {
                        rotation = target
                    } else {
                        val t0 = withFrameNanos { it }
                        while (true) {
                            val now = withFrameNanos { it }
                            val t = ((now - t0) / 1_000_000f / LAND_MILLIS).coerceIn(0f, 1f)
                            rotation = start + (target - start) * LandEasing.transform(t)
                            if (t >= 1f) break
                        }
                    }
                    wonCoins = result.coins
                    if (result.coins > 0) shower++
                    viewModel.consumeEarned()
                }
                phase = WheelPhase.Idle
            }
        }
    }

    val busy = phase != WheelPhase.Idle
    // Phase 2: after the free daily spin, extra spins cost coins (price and daily cap come from the server).
    val paidMode = summary.spunToday
    val paidReady = paidMode && summary.paidSpinsLeft > 0
    val canAfford = summary.balance >= summary.paidSpinPrice
    val showDone = paidMode && !busy && !paidReady
    val canTap = !busy && (!paidMode || (paidReady && canAfford))

    Box(Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        TwinTopBar(
            title = stringResource(R.string.rewards_spin_title),
            showSidebarButton = true,
            onSyncClick = { viewModel.refresh() },
        )

        // Coin balance above the wheel.
        GlassCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CoinIcon(46.dp, Modifier.graphicsLayer { scaleX = bump; scaleY = bump })
                Spacer(Modifier.size(14.dp))
                Column {
                    Text("$shown", fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = colors.ink, lineHeight = 38.sp)
                    Text(stringResource(R.string.rewards_gold_coins), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        // Result line above the wheel (kept in the layout so the wheel does not jump).
        val winScale by animateFloatAsState(
            targetValue = if (wonCoins >= 0 && !busy) 1f else 0.5f,
            animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f),
            label = "winScale",
        )
        Box(Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.Center) {
            if (wonCoins >= 0 && !busy) {
                Text(
                    text = if (wonCoins > 0) stringResource(R.string.rewards_spin_won, wonCoins) else stringResource(R.string.rewards_spin_won_none),
                    color = RewardsStyle.Gold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.graphicsLayer { scaleX = winScale; scaleY = winScale },
                )
            }
        }

        Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.TopCenter) {
            Box(
                modifier = Modifier.fillMaxWidth(0.84f).aspectRatio(1f).padding(top = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Wheel(slices = slices, sliceCount = sliceCount, rotation = { rotation }, modifier = Modifier.fillMaxSize())
                Text("💖", fontSize = 16.sp)
            }
            Text("▼", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showDone) {
                GlassButton(
                    text = stringResource(R.string.rewards_spun_today),
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    style = GlassButtonStyle.Secondary,
                    enabled = false,
                )
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(TwinTheme.shapes.minTouch)
                        .clip(TwinTheme.shapes.pill)
                        .background(Brush.verticalGradient(listOf(RewardsStyle.Gold, RewardsStyle.GoldDeep)))
                        .graphicsLayer { alpha = if (canTap || busy) 1f else 0.45f }
                        .clickable(enabled = canTap, role = Role.Button) {
                            balanceBefore = summary.balance
                            wonCoins = -1
                            phase = WheelPhase.Waiting
                            viewModel.spin()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        when {
                            busy -> stringResource(R.string.rewards_spinning_btn)
                            paidMode -> stringResource(R.string.rewards_spin_again, summary.paidSpinPrice)
                            else -> stringResource(R.string.rewards_spin_btn)
                        },
                        color = RewardsStyle.GoldInk,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.5.sp,
                    )
                }
            }
        }
        // Phase 3: after the free spin, a rewarded ad gives one more spin without paying coins.
        if (adPlayer.allowed && summary.spunToday && summary.adSpinsLeft > 0 && !busy) {
            Spacer(Modifier.height(10.dp))
            val adTap = adPlayer.ready && !state.adBusy
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TwinTheme.shapes.minTouch)
                    .clip(TwinTheme.shapes.pill)
                    .border(1.5.dp, RewardsStyle.Gold, TwinTheme.shapes.pill)
                    .graphicsLayer { alpha = if (adTap) 1f else 0.45f }
                    .clickable(enabled = adTap, role = Role.Button) { viewModel.watchAdForSpin(adPlayer.show) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (adPlayer.ready) stringResource(R.string.rewards_ad_spin_btn, summary.adSpinsLeft) else stringResource(R.string.rewards_ad_loading),
                    color = RewardsStyle.Gold,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            when {
                busy -> ""
                !summary.spunToday -> stringResource(R.string.rewards_spin_note_ready)
                paidReady && canAfford -> stringResource(R.string.rewards_spin_note_paid, summary.paidSpinPrice)
                paidReady -> stringResource(R.string.rewards_spin_note_poor, summary.paidSpinPrice)
                else -> stringResource(R.string.rewards_spin_note_done)
            },
            style = MaterialTheme.typography.bodySmall,
            color = colors.inkMuted,
        )
        // Room for the floating bottom bar so the last text scrolls clear of it.
        Spacer(Modifier.height(112.dp))
    }
    // Coins fly out over the wheel when it stops on a prize.
    CoinShower(trigger = shower, modifier = Modifier.fillMaxSize())
    }
}

/** The wheel itself: coloured slices with the coin amount printed on each one. [rotation] is in degrees. */
@Composable
private fun Wheel(
    slices: List<SpinWheelSlice>,
    sliceCount: Int,
    rotation: () -> Float,
    modifier: Modifier = Modifier,
) {
    val textPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(4f, 0f, 2f, android.graphics.Color.argb(160, 0, 0, 0))
        }
    }
    Canvas(modifier = modifier.graphicsLayer { rotationZ = rotation() }) {
        val sweep = 360f / sliceCount
        val radius = size.minDimension / 2f
        val rim = 6.dp.toPx()
        val inner = radius - rim
        textPaint.textSize = 17.sp.toPx()

        drawCircle(color = RewardsStyle.GoldDeep, radius = radius)
        drawCircle(color = RewardsStyle.Gold, radius = radius - 2.dp.toPx())
        for (i in 0 until sliceCount) {
            val fill = slices.getOrNull(i)?.colorHex?.let { hex ->
                runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
            } ?: FallbackColors[i % FallbackColors.size]
            drawArc(
                color = fill,
                startAngle = i * sweep,
                sweepAngle = sweep,
                useCenter = true,
                size = Size(inner * 2, inner * 2),
                topLeft = Offset(rim, rim),
            )
        }
        // Coin amount on every slice, reading outwards from the centre.
        val labelRadius = inner * 0.66f
        for (i in 0 until sliceCount) {
            val slice = slices.getOrNull(i) ?: continue
            val text = if (slice.coins > 0) "${slice.coins}" else slice.label.ifBlank { "0" }
            rotate(degrees = i * sweep + sweep / 2f, pivot = center) {
                drawContext.canvas.nativeCanvas.drawText(
                    text,
                    center.x + labelRadius,
                    center.y + textPaint.textSize * 0.35f,
                    textPaint,
                )
            }
        }
        drawCircle(color = Color.White, radius = 14.dp.toPx())
        drawCircle(color = RewardsStyle.Gold, radius = 11.dp.toPx())
        drawCircle(color = RewardsStyle.GoldInk, radius = 8.dp.toPx())
    }
}
