package com.twinhearts.app.feature.rewards.ui

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(RewardsStyle.Gold, RewardsStyle.GoldDeep)))
                .clickable(enabled = !claiming, role = Role.Button, onClick = onClick)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (claiming) stringResource(R.string.rewards_claiming) else stringResource(R.string.rewards_claim, coins),
                color = RewardsStyle.GoldInk,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
internal fun SectionTitle(text: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TwinTheme.colors.ink)
        if (trailing != null) {
            Spacer(Modifier.weight(1f))
            Text(trailing, style = MaterialTheme.typography.bodySmall, color = TwinTheme.colors.inkMuted)
        }
    }
}

@Composable
internal fun HeroCard(s: RewardsSummary) {
    val colors = TwinTheme.colors
    val shown = rememberCountedBalance(s.balance)
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CoinIcon(size = 46.dp)
            Column {
                Text("$shown", fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = colors.ink)
                Text(stringResource(R.string.rewards_gold_coins), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text("${s.lifetimeEarned}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.ink)
                Text(stringResource(R.string.rewards_earned_total), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
            }
        }
    }
}

@Composable
internal fun CheckInCard(s: RewardsSummary, claiming: Boolean, onClaim: () -> Unit) {
    val colors = TwinTheme.colors
    GlassCard(Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.rewards_checkin_title), stringResource(R.string.rewards_day_of, s.cyclePosition))
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            s.cycle.forEach { DayBox(it) }
        }
        Spacer(Modifier.height(14.dp))
        ClaimButton(
            claimed = s.checkedInToday, claiming = claiming, coins = s.todayCoins, onClick = onClaim,
            modifier = Modifier.fillMaxWidth(), claimedLabel = stringResource(R.string.rewards_claimed_today),
        )
        Spacer(Modifier.height(8.dp))
        val note = when {
            s.checkedInToday -> stringResource(R.string.rewards_note_done, s.streak)
            s.streak > 0 -> stringResource(R.string.rewards_note_keep, s.streak + 1)
            else -> stringResource(R.string.rewards_note_start)
        }
        Text(note, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
    }
}

@Composable
private fun RowScope.DayBox(d: CycleDay) {
    val colors = TwinTheme.colors
    val border = when {
        d.done -> colors.success
        d.isToday -> RewardsStyle.Gold
        else -> colors.lineSoft
    }
    val fill = when {
        d.done -> colors.success.copy(alpha = 0.14f)
        d.day == 7 -> RewardsStyle.Gold.copy(alpha = 0.14f)
        else -> Color.White.copy(alpha = 0.05f)
    }
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(fill)
            .border(BorderStroke(if (d.isToday) 2.dp else 1.dp, border), RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.rewards_day_short, d.day), fontSize = 11.sp, color = colors.inkMuted, maxLines = 1)
        Spacer(Modifier.height(5.dp))
        if (d.done) Icon(Icons.Rounded.Check, null, tint = colors.success, modifier = Modifier.size(18.dp)) else CoinIcon(size = 18.dp)
        Spacer(Modifier.height(4.dp))
        Text("+${d.coins}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.ink, maxLines = 1)
    }
}

@Composable
internal fun CoupleStreakCard(s: RewardsSummary, partnerName: String) {
    val colors = TwinTheme.colors
    val partner = partnerName.ifBlank { stringResource(R.string.rewards_partner_fallback) }
    GlassCard(Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.rewards_couple_title))
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            FlameIcon(size = 38.dp)
            Column(Modifier.weight(1f)) {
                Text("${s.coupleStreak}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = colors.ink)
                Text(stringResource(R.string.rewards_couple_days), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.End) {
                StatusPill(stringResource(R.string.rewards_you), s.checkedInToday)
                StatusPill(partner, s.partnerCheckedInToday)
            }
        }
        Spacer(Modifier.height(10.dp))
        val msg = when {
            s.coupleDoneToday -> stringResource(R.string.rewards_couple_both)
            s.checkedInToday -> stringResource(R.string.rewards_couple_waiting, partner)
            s.partnerCheckedInToday -> stringResource(R.string.rewards_couple_turn, partner)
            else -> stringResource(R.string.rewards_couple_ask, partner)
        }
        Text(msg, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
    }
}

@Composable
private fun StatusPill(label: String, on: Boolean) {
    val colors = TwinTheme.colors
    val c = if (on) colors.success else colors.inkMuted
    Text(
        text = "$label ${if (on) "✓" else "·"}",
        fontSize = 12.sp, color = c, maxLines = 1,
        modifier = Modifier
            .border(1.dp, if (on) colors.success else colors.line2, TwinTheme.shapes.pill)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
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
    else -> stringResource(R.string.rewards_kind_other)
}

private val shortFmt = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

private fun shortDate(iso: String): String = runCatching { OffsetDateTime.parse(iso).atZoneSameInstant(java.time.ZoneId.systemDefault()).format(shortFmt) }.getOrDefault("")
