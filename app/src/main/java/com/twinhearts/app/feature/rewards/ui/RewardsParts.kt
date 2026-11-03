package com.twinhearts.app.feature.rewards.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
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
import com.twinhearts.app.core.design.TwinColors
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.rewards.domain.CycleDay
import com.twinhearts.app.feature.rewards.domain.RewardsSummary
import com.twinhearts.app.feature.rewards.domain.ScratchResult
import com.twinhearts.app.feature.rewards.domain.StoreItem

private val TwinColors.surfaceSubtle: Color get() = Color.White.copy(alpha = 0.06f)

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

/** Step 4: Twin Store card (themes, frames, decor, games) */
@Composable
internal fun TwinStoreCard(
    balance: Int,
    storeItems: List<StoreItem>,
    unlockedItemIds: List<String>,
    equipped: Map<String, String>,
    storeFilter: String,
    purchasingItemId: String?,
    equippingItemId: String?,
    onFilterChange: (String) -> Unit,
    onBuy: (StoreItem) -> Unit,
    onEquip: (String, String) -> Unit,
) {
    val colors = TwinTheme.colors
    val tabs = listOf(
        "all" to stringResource(R.string.rewards_store_tab_all),
        "theme" to stringResource(R.string.rewards_store_tab_themes),
        "frame" to stringResource(R.string.rewards_store_tab_frames),
        "decor" to stringResource(R.string.rewards_store_tab_decor),
        "game" to stringResource(R.string.rewards_store_tab_games),
    )

    val displayedItems = if (storeFilter == "all") {
        storeItems
    } else {
        storeItems.filter { it.category == storeFilter }
    }

    GlassCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(stringResource(R.string.rewards_store_title))
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.rewards_store_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = colors.inkMuted,
            )
        }

        Spacer(Modifier.height(12.dp))

        // Category Filter Chips
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tabs.forEach { (key, label) ->
                val selected = storeFilter == key
                val bg = if (selected) Brush.verticalGradient(listOf(RewardsStyle.Gold, RewardsStyle.GoldDeep)) else null
                val bgColor = if (!selected) colors.surfaceSubtle else Color.Transparent
                val border = if (selected) BorderStroke(1.dp, RewardsStyle.GoldDeep) else BorderStroke(1.dp, colors.lineSoft)

                Box(
                    modifier = Modifier
                        .clip(TwinTheme.shapes.pill)
                        .border(border, TwinTheme.shapes.pill)
                        .then(if (bg != null) Modifier.background(bg) else Modifier.background(bgColor))
                        .clickable { onFilterChange(key) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontSize = 12.5.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) RewardsStyle.GoldInk else colors.ink,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Items list
        if (displayedItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.rewards_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                displayedItems.forEach { item ->
                    val isUnlocked = unlockedItemIds.contains(item.id)
                    val isEquipped = isUnlocked && equipped[item.category] == item.id
                    val isPurchasing = purchasingItemId == item.id
                    val isEquipping = equippingItemId == item.id
                    val canAfford = balance >= item.price

                    StoreItemRow(
                        item = item,
                        isUnlocked = isUnlocked,
                        isEquipped = isEquipped,
                        isPurchasing = isPurchasing,
                        isEquipping = isEquipping,
                        canAfford = canAfford,
                        onBuy = { onBuy(item) },
                        onToggleEquip = {
                            if (isEquipped) {
                                onEquip(item.category, "")
                            } else {
                                onEquip(item.category, item.id)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StoreItemRow(
    item: StoreItem,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    isPurchasing: Boolean,
    isEquipping: Boolean,
    canAfford: Boolean,
    onBuy: () -> Unit,
    onToggleEquip: () -> Unit,
) {
    val colors = TwinTheme.colors
    val previewColor = remember(item.previewColor) {
        if (item.previewColor.isNotBlank()) {
            runCatching {
                val parsed = android.graphics.Color.parseColor(item.previewColor)
                Color(parsed)
            }.getOrNull()
        } else null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                BorderStroke(
                    1.dp,
                    if (isEquipped) colors.success.copy(alpha = 0.6f)
                    else if (isUnlocked) colors.lineSoft
                    else colors.lineSoft.copy(alpha = 0.5f)
                ),
                RoundedCornerShape(14.dp),
            )
            .background(
                if (isEquipped) colors.success.copy(alpha = 0.08f)
                else colors.surfaceSubtle.copy(alpha = 0.6f)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon / Emoji with optional preview color circle
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(previewColor?.copy(alpha = 0.25f) ?: colors.surfaceSubtle)
                .border(1.dp, previewColor ?: colors.lineSoft, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(item.emoji, fontSize = 22.sp)
        }

        Spacer(Modifier.size(12.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.name, fontWeight = FontWeight.Bold, color = colors.ink, fontSize = 14.sp)
                Box(
                    modifier = Modifier
                        .clip(TwinTheme.shapes.pill)
                        .background(colors.surfaceSubtle)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        item.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.inkMuted,
                        fontSize = 9.sp,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(item.description, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, fontSize = 12.sp)
        }

        Spacer(Modifier.size(8.dp))

        // Action button
        when {
            isEquipped -> {
                Box(
                    modifier = Modifier
                        .clip(TwinTheme.shapes.pill)
                        .background(colors.success.copy(alpha = 0.16f))
                        .border(1.dp, colors.success, TwinTheme.shapes.pill)
                        .clickable(enabled = !isEquipping, onClick = onToggleEquip)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.rewards_store_active),
                        color = colors.success,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                }
            }
            isUnlocked -> {
                Box(
                    modifier = Modifier
                        .clip(TwinTheme.shapes.pill)
                        .background(colors.surfaceSubtle)
                        .border(1.dp, colors.lineSoft, TwinTheme.shapes.pill)
                        .clickable(enabled = !isEquipping, onClick = onToggleEquip)
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isEquipping) stringResource(R.string.rewards_store_equipping) else stringResource(R.string.rewards_store_equip),
                        color = colors.ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .clip(TwinTheme.shapes.pill)
                        .then(
                            if (canAfford && !isPurchasing) Modifier.background(
                                Brush.verticalGradient(listOf(RewardsStyle.Gold, RewardsStyle.GoldDeep))
                            ) else Modifier.background(colors.surfaceSubtle)
                        )
                        .border(
                            1.dp,
                            if (canAfford) RewardsStyle.GoldDeep.copy(alpha = 0.5f) else colors.lineSoft,
                            TwinTheme.shapes.pill,
                        )
                        .clickable(enabled = canAfford && !isPurchasing, onClick = onBuy)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = when {
                            isPurchasing -> stringResource(R.string.rewards_store_unlocking)
                            else -> stringResource(R.string.rewards_store_unlock, item.price)
                        },
                        color = if (canAfford) RewardsStyle.GoldInk else colors.inkMuted,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}



/** Phase 1: "Earn more coins" list (same rows as the web Wallet tab). */
@Composable
internal fun EarnCard(items: List<com.twinhearts.app.feature.rewards.domain.EarnItem>) {
    if (items.isEmpty()) return
    val colors = TwinTheme.colors
    GlassCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(stringResource(R.string.rewards_earn_title))
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.rewards_earn_reset), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
        }
        Spacer(Modifier.height(8.dp))
        items.forEach { item ->
            val done = item.remaining <= 0
            Row(
                Modifier.fillMaxWidth().padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(item.emoji, fontSize = 20.sp, modifier = Modifier.size(38.dp).wrapContentSize(Alignment.Center))
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (done) colors.inkMuted else colors.ink)
                    Text(
                        if (done) stringResource(R.string.rewards_earn_done) else stringResource(R.string.rewards_earn_left, item.remaining, item.daily),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkMuted,
                    )
                }
                Text("+${item.coins} 🪙", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (done) colors.success else RewardsStyle.Gold)
            }
        }
    }
}


/** Phase 3: "Watch an ad" for coins. Hidden when ads are off for this couple (for example Pro). */
@Composable
internal fun WatchAdCard(summary: RewardsSummary, player: AdPlayer, busy: Boolean, onWatch: () -> Unit) {
    if (!player.allowed) return
    val colors = TwinTheme.colors
    val left = summary.adCoinsLeft
    val canTap = player.ready && !busy && left > 0
    GlassCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("📺", fontSize = 22.sp, modifier = Modifier.size(38.dp).wrapContentSize(Alignment.Center))
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                SectionTitle(stringResource(R.string.rewards_ad_title))
                Text(
                    when {
                        left <= 0 -> stringResource(R.string.rewards_ad_done)
                        !player.ready -> stringResource(R.string.rewards_ad_loading)
                        else -> stringResource(R.string.rewards_ad_sub, summary.adCoinsReward, left)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted,
                )
            }
            Box(
                modifier = Modifier
                    .height(TwinTheme.shapes.minTouch)
                    .clip(TwinTheme.shapes.pill)
                    .background(Brush.verticalGradient(listOf(RewardsStyle.Gold, RewardsStyle.GoldDeep)))
                    .graphicsLayer { alpha = if (canTap) 1f else 0.45f }
                    .clickable(enabled = canTap, role = Role.Button, onClick = onWatch)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("▶ +${summary.adCoinsReward} 🪙", color = RewardsStyle.GoldInk, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
        }
    }
}
