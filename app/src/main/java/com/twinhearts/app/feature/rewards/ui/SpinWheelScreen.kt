package com.twinhearts.app.feature.rewards.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.rememberReduceMotion
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.feature.rewards.domain.RewardsSummary
import com.twinhearts.app.feature.rewards.domain.SpinWheelSlice

/**
 * Spin & Win page opened as a separate screen from the Rewards Wallet tab.
 * The coin balance is shown above the wheel.
 */
@Composable
fun SpinWheelScreen(
    viewModel: RewardsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = TwinTheme.colors
    val summary = state.summary ?: return
    val shown = rememberCountedBalance(summary.balance)
    val slices = summary.wheelSlices
    val rotationAnim = remember { Animatable(0f) }
    val reduceMotion = rememberReduceMotion()

    LaunchedEffect(state.spinning) {
        if (state.spinning) {
            val targetDegrees = rotationAnim.value + 360f * 5 + (0..7).random() * 45f
            if (reduceMotion) {
                rotationAnim.snapTo(targetDegrees)
            } else {
                rotationAnim.animateTo(
                    targetValue = targetDegrees,
                    animationSpec = tween(durationMillis = 3500, easing = FastOutSlowInEasing),
                )
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TwinTopBar(
            title = stringResource(R.string.rewards_spin_title),
            showSidebarButton = true,
            onSyncClick = { viewModel.refresh() },
        )

        // Coin balance shown above the wheel.
        GlassCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CoinIcon(46.dp)
                Spacer(Modifier.size(14.dp))
                Column {
                    Text("$shown", fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = colors.ink, lineHeight = 38.sp)
                    Text(stringResource(R.string.rewards_gold_coins), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("${summary.lifetimeEarned}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.ink)
                    Text(stringResource(R.string.rewards_earned_total), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("▼", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Box(modifier = Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .size(240.dp)
                            .graphicsLayer { rotationZ = rotationAnim.value },
                    ) {
                        val numSlices = if (slices.isNotEmpty()) slices.size else 8
                        val sweep = 360f / numSlices
                        val radius = size.minDimension / 2f
                        val colorsList = listOf(
                            Color(0xFFF59E0B), Color(0xFFEC4899), Color(0xFF8B5CF6), Color(0xFF10B981),
                            Color(0xFF3B82F6), Color(0xFFF43F5E), Color(0xFF06B6D4), Color(0xFFEAB308),
                        )
                        drawCircle(color = RewardsStyle.GoldDeep, radius = radius)
                        drawCircle(color = RewardsStyle.Gold, radius = radius - 4f)
                        for (i in 0 until numSlices) {
                            drawArc(
                                color = colorsList[i % colorsList.size],
                                startAngle = i * sweep,
                                sweepAngle = sweep,
                                useCenter = true,
                                size = Size(radius * 2 - 16f, radius * 2 - 16f),
                                topLeft = Offset(8f, 8f),
                            )
                        }
                        drawCircle(color = Color.White, radius = 28f)
                        drawCircle(color = RewardsStyle.Gold, radius = 22f)
                        drawCircle(color = RewardsStyle.GoldInk, radius = 16f)
                    }
                    Text("💖", fontSize = 16.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (summary.spunToday) {
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
                        .background(BrushGradient())
                        .clickable(enabled = !state.spinning, role = Role.Button, onClick = { viewModel.spin() }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(if (state.spinning) R.string.rewards_spinning_btn else R.string.rewards_spin_btn),
                        color = RewardsStyle.GoldInk,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.5.sp,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(if (summary.spunToday) R.string.rewards_spin_note_done else R.string.rewards_spin_note_ready),
            style = MaterialTheme.typography.bodySmall,
            color = colors.inkMuted,
        )
        // Room for the floating bottom bar so the last text scrolls clear of it.
        Spacer(Modifier.height(112.dp))
    }
}

private fun BrushGradient() = androidx.compose.ui.graphics.Brush.verticalGradient(
    listOf(RewardsStyle.Gold, RewardsStyle.GoldDeep),
)
