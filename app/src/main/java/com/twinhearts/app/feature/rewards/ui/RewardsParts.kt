package com.twinhearts.app.feature.rewards.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twinhearts.app.R
import com.twinhearts.app.core.common.rememberReduceMotion
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.rewards.domain.CoinTx
import com.twinhearts.app.feature.rewards.domain.CycleDay
import com.twinhearts.app.feature.rewards.domain.RewardsSummary
import com.twinhearts.app.feature.rewards.domain.ScratchResult
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Coin gold and flame orange: fixed brand accents (web: --rw-gold, --rw-flame in rewards.css). */
internal object RewardsStyle {
    val Gold = Color(0xFFF5B83D)
    val GoldDeep = Color(0xFFD9941A)
    val GoldInk = Color(0xFF3A2400)
    val Flame = Color(0xFFFF7A3D)
}

@Composable
internal fun CoinIcon(size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Icon(Icons.Rounded.MonetizationOn, contentDescription = null, tint = RewardsStyle.Gold, modifier = modifier.size(size))
}

@Composable
internal fun FlameIcon(size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = RewardsStyle.Flame, modifier = modifier.size(size))
}

@Composable
internal fun WheelIcon(size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Icon(Icons.Rounded.Casino, contentDescription = null, tint = RewardsStyle.Gold, modifier = modifier.size(size))
}

/** Balance that counts up to its new value (web: countUp in rewards.js). Still when animations are off. */
@Composable
internal fun rememberCountedBalance(target: Int): Int {
    val reduce = rememberReduceMotion()
    val anim = remember { Animatable(target.toFloat()) }
    LaunchedEffect(target) {
        if (reduce) anim.snapTo(target.toFloat()) else anim.animateTo(target.toFloat(), tween(700))
    }
    return anim.value.toInt().coerceAtLeast(0)
}

/** The gold pill button used to claim. */
@Composable
internal fun ClaimButton(
    claimed: Boolean,
    claiming: Boolean,
    coins: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    claimedLabel: String = stringResource(R.string.rewards_claimed),
) {
    if (claimed) {
        GlassButton(text = claimedLabel, onClick = {}, modifier = modifier, style = GlassButtonStyle.Secondary, enabled = false)
    } else {
        Box(
            modifier = modifier
                .height(TwinTheme.shapes.minTouch)
                .clip(TwinTheme.shapes.pill)
                .background(Brush.verticalGradient(listOf(RewardsStyle.Gold, RewardsStyle.GoldDeep)))
                .clickable(enabled = !claiming, role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (claiming) stringResource(R.string.rewards_claiming) else stringResource(R.string.rewards_claim, coins),
                color = RewardsStyle.GoldInk,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.5.sp,
            )
        }
    }
}

@Composable
internal fun HeroCard(summary: RewardsSummary) {
    val colors = TwinTheme.colors
    val shown = rememberCountedBalance(summary.balance)
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
}

@Composable
internal fun CheckInCard(summary: RewardsSummary, claiming: Boolean, onClaim: () -> Unit) {
    val colors = TwinTheme.colors
    GlassCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(stringResource(R.string.rewards_checkin_title))
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.rewards_day_of, summary.cyclePosition), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            summary.cycle.forEach { box -> DayBox(box, Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ClaimButton(
                claimed = summary.checkedInToday, claiming = claiming, coins = summary.todayCoins, onClick = onClaim,
                claimedLabel = stringResource(R.string.rewards_claimed_today),
            )
        }
        Spacer(Modifier.height(8.dp))
        val note = when {
            summary.checkedInToday -> stringResource(R.string.rewards_note_done, summary.streak)
            summary.streak > 0 -> stringResource(R.string.rewards_note_keep, summary.streak + 1)
            else -> stringResource(R.string.rewards_note_start)
        }
        Text(note, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
    }
}

@Composable
private fun DayBox(day: CycleDay, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val border = when {
        day.isToday -> BorderStroke(2.dp, RewardsStyle.Gold)
        day.done -> BorderStroke(1.dp, colors.success)
        else -> BorderStroke(1.dp, colors.lineSoft)
    }
    val bg = when {
        day.done -> colors.success.copy(alpha = 0.14f)
        day.day == 7 -> RewardsStyle.Gold.copy(alpha = 0.14f)
        else -> colors.surfaceSubtle
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(border, RoundedCornerShape(14.dp))
            .background(bg)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(R.string.rewards_day_short, day.day), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
        if (day.done) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.success, modifier = Modifier.size(18.dp))
        } else {
            CoinIcon(18.dp)
        }
        Text("+${day.coins}", fontWeight = FontWeight.Bold, color = colors.ink, fontSize = 13.sp)
    }
}

/** Step 3: Native Compose Canvas Spin & Win card */
@Composable
internal fun SpinWheelCard(
    summary: RewardsSummary,
    spinning: Boolean,
    onSpin: () -> Unit,
) {
    val colors = TwinTheme.colors
    val slices = summary.wheelSlices
    val rotationAnim = remember { Animatable(0f) }
    val reduceMotion = rememberReduceMotion()

    LaunchedEffect(spinning) {
        if (spinning) {
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

    GlassCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(stringResource(R.string.rewards_spin_title))
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .clip(TwinTheme.shapes.pill)
                    .background(if (summary.spunToday) colors.surfaceSubtle else RewardsStyle.Gold.copy(alpha = 0.2f))
                    .border(1.dp, if (summary.spunToday) colors.lineSoft else RewardsStyle.Gold.copy(alpha = 0.5f), TwinTheme.shapes.pill)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    stringResource(if (summary.spunToday) R.string.rewards_spin_used else R.string.rewards_spin_avail),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (summary.spunToday) colors.inkMuted else RewardsStyle.Gold,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Center wheel
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

                        drawCircle(color = RewardsStyle.GoldDeep, radius = radius)
                        drawCircle(color = RewardsStyle.Gold, radius = radius - 4f)

                        val colorsList = listOf(
                            Color(0xFFF59E0B), Color(0xFFEC4899), Color(0xFF8B5CF6), Color(0xFF10B981),
                            Color(0xFF3B82F6), Color(0xFFF43F5E), Color(0xFF06B6D4), Color(0xFFEAB308),
                        )
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
                        .background(Brush.verticalGradient(listOf(RewardsStyle.Gold, RewardsStyle.GoldDeep)))
                        .clickable(enabled = !spinning, role = Role.Button, onClick = onSpin),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(if (spinning) R.string.rewards_spinning_btn else R.string.rewards_spin_btn),
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
    }
}

/** Step 3: Lucky Couple Scratch card */
@Composable
internal fun ScratchCard(
    summary: RewardsSummary,
    scratching: Boolean,
    lastResult: ScratchResult?,
    onScratch: () -> Unit,
) {
    val colors = TwinTheme.colors
    var revealed by remember { mutableStateOf(summary.scratchedToday) }
    LaunchedEffect(summary.scratchedToday) {
        if (summary.scratchedToday) revealed = true
    }

    GlassCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(stringResource(R.string.rewards_scratch_title))
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .clip(TwinTheme.shapes.pill)
                    .background(if (summary.scratchedToday) colors.surfaceSubtle else RewardsStyle.Gold.copy(alpha = 0.2f))
                    .border(1.dp, if (summary.scratchedToday) colors.lineSoft else RewardsStyle.Gold.copy(alpha = 0.5f), TwinTheme.shapes.pill)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    stringResource(if (summary.scratchedToday) R.string.rewards_scratch_used else R.string.rewards_scratch_avail),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (summary.scratchedToday) colors.inkMuted else RewardsStyle.Gold,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.5.dp, RewardsStyle.Gold.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                .background(RewardsStyle.Gold.copy(alpha = 0.12f))
                .clickable(enabled = !revealed && !scratching) {
                    revealed = true
                    onScratch()
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(lastResult?.emoji ?: "💖", fontSize = 28.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    lastResult?.title ?: stringResource(if (summary.scratchedToday) R.string.rewards_scratch_fortune_claimed else R.string.rewards_scratch_surprise),
                    fontWeight = FontWeight.Bold,
                    color = colors.ink,
                    fontSize = 15.sp,
                )
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .clip(TwinTheme.shapes.pill)
                        .background(RewardsStyle.Gold.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text(
                        "+" + (lastResult?.coins ?: if (summary.scratchedToday) "25" else "15–100") + " Gold Coins 🪙",
                        color = RewardsStyle.Gold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    lastResult?.message ?: stringResource(R.string.rewards_scratch_tap_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted,
                    fontSize = 11.5.sp,
                )
            }

            if (!revealed) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(RewardsStyle.GoldDeep, RewardsStyle.Gold, Color(0xFFFCD34D)))),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎫", fontSize = 26.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.rewards_scratch_tap_hint),
                            color = RewardsStyle.GoldInk,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (!revealed) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TwinTheme.shapes.minTouch)
                    .clip(TwinTheme.shapes.pill)
                    .background(Brush.verticalGradient(listOf(RewardsStyle.Gold, RewardsStyle.GoldDeep)))
                    .clickable(enabled = !scratching, role = Role.Button) {
                        revealed = true
                        onScratch()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(if (scratching) R.string.rewards_scratch_revealing else R.string.rewards_scratch_reveal),
                    color = RewardsStyle.GoldInk,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(if (summary.scratchedToday) R.string.rewards_scratch_note_done else R.string.rewards_scratch_note_ready),
            style = MaterialTheme.typography.bodySmall,
            color = colors.inkMuted,
        )
    }
}

@Composable
internal fun CoupleStreakCard(summary: RewardsSummary, partnerName: String) {
    val colors = TwinTheme.colors
    val partner = partnerName.ifBlank { stringResource(R.string.rewards_partner_fallback) }
    GlassCard(Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.rewards_couple_title))
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FlameIcon(38.dp)
            Spacer(Modifier.size(14.dp))
            Column {
                Text("${summary.coupleStreak}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = colors.ink, lineHeight = 28.sp)
                Text(stringResource(R.string.rewards_couple_days), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
            }
            Spacer(Modifier.weight(1f))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.End) {
                StatusPill(stringResource(R.string.rewards_you), summary.checkedInToday)
                StatusPill(partner, summary.partnerCheckedInToday)
            }
        }
        Spacer(Modifier.height(10.dp))
        val msg = when {
            summary.coupleDoneToday -> stringResource(R.string.rewards_couple_both)
            summary.checkedInToday -> stringResource(R.string.rewards_couple_waiting, partner)
            summary.partnerCheckedInToday -> stringResource(R.string.rewards_couple_turn, partner)
            else -> stringResource(R.string.rewards_couple_ask, partner)
        }
        Text(msg, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
    }
}

@Composable
private fun StatusPill(name: String, done: Boolean) {
    val colors = TwinTheme.colors
    val border = if (done) colors.success else colors.lineSoft
    val text = if (done) colors.success else colors.inkMuted
    Box(
        Modifier
            .clip(TwinTheme.shapes.pill)
            .border(1.dp, border, TwinTheme.shapes.pill)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text("$name ${if (done) "✓" else "·"}", fontSize = 12.sp, color = text)
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TwinTheme.colors.ink)
}

@Composable
internal fun HistoryCard(items: List<CoinTx>?) {
    val colors = TwinTheme.colors
    GlassCard(Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.rewards_history_title))
        Spacer(Modifier.height(8.dp))
        when {
            items == null -> Text(stringResource(R.string.rewards_loading), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
            items.isEmpty() -> Text(stringResource(R.string.rewards_history_empty), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
            else -> items.forEachIndexed { i, tx ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.lineSoft))
                Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(kindLabel(tx.kind, tx.note), color = colors.ink, fontSize = 14.sp)
                        Text(shortDate(tx.createdAtIso), color = colors.inkMuted, fontSize = 11.5.sp)
                    }
                    Text(
                        text = (if (tx.amount >= 0) "+" else "") + tx.amount,
                        fontWeight = FontWeight.Bold,
                        color = if (tx.amount >= 0) colors.success else colors.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun kindLabel(kind: String, note: String = ""): String = when (kind) {
    "checkin" -> stringResource(R.string.rewards_kind_checkin)
    "gift" -> if (note.isNotBlank()) stringResource(R.string.rewards_kind_gift_named, note) else stringResource(R.string.rewards_kind_gift)
    "gift_refund" -> stringResource(R.string.rewards_kind_gift_refund)
    "spin" -> stringResource(R.string.rewards_kind_spin)
    "scratch" -> stringResource(R.string.rewards_kind_scratch)
    else -> stringResource(R.string.rewards_kind_other)
}

private val shortFmt = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

private fun shortDate(iso: String): String = runCatching { OffsetDateTime.parse(iso).atZoneSameInstant(java.time.ZoneId.systemDefault()).format(shortFmt) }.getOrDefault("")
