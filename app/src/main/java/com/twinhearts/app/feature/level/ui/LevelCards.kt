package com.twinhearts.app.feature.level.ui

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twinhearts.app.R
import com.twinhearts.app.core.common.rememberReduceMotion
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.level.domain.Badge
import com.twinhearts.app.feature.level.domain.LevelIcon
import com.twinhearts.app.feature.level.domain.LevelState
import com.twinhearts.app.feature.level.domain.XpAction
import com.twinhearts.app.feature.level.domain.XpTone

/**
 * Why this file exists: the cards of the Couple Level page, drawn the way the web page's
 * level.css draws them: hero ring, insight row, next unlock, locked and earned badge rows, XP guide.
 */

private const val GOLD = 0xFFFBBF24
private const val GREEN = 0xFF34D399
private const val PERCENT = 100f
private const val SMALL_PHONE_DP = 360

// ── Shared building blocks ──

@Composable
private fun LevelCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = TwinTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(colors.card, colors.card.copy(alpha = CARD_END_ALPHA))))
            .border(1.dp, colors.accent.copy(alpha = LINE_ALPHA), shape)
            .padding(16.dp),
        content = content,
    )
}

private const val CARD_END_ALPHA = 0.94f
private const val LINE_ALPHA = 0.32f
private const val TINT_ALPHA = 0.16f

@Composable
private fun tint(): Color = TwinTheme.colors.accent.copy(alpha = TINT_ALPHA)

@Composable
private fun line(): Color = TwinTheme.colors.accent.copy(alpha = LINE_ALPHA)

/** The thin progress bar: grows from empty to its value, like the web page. */
@Composable
private fun LevelBar(percent: Int, height: Dp, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val reduce = rememberReduceMotion()
    var go by remember { mutableStateOf(false) }
    LaunchedEffect(percent) { go = true }
    val fraction by animateFloatAsState(
        targetValue = if (go) percent / PERCENT else 0f,
        animationSpec = tween(if (reduce) 0 else BAR_MS), label = "levelBar",
    )
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(Color.White.copy(alpha = TRACK_ALPHA))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).clip(CircleShape).background(colors.primaryBrush))
    }
}

private const val BAR_MS = 1000
private const val RING_MS = 1200
private const val TRACK_ALPHA = 0.09f

@Composable
private fun CardHead(icon: LevelIcon, @StringRes title: Int, subtitle: String) {
    val colors = TwinTheme.colors
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon.vector(), null, Modifier.size(26.dp), tint = colors.accent)
        Text(stringResource(title), color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(
            subtitle, color = colors.inkMuted, fontSize = 12.5.sp, textAlign = TextAlign.End, lineHeight = 17.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

// ── Hero: ring + XP + insight ──

@Composable
fun HeroCard(state: LevelState, onInsight: (Int) -> Unit) {
    val narrow = LocalConfiguration.current.screenWidthDp < SMALL_PHONE_DP
    val ringSize = (LocalConfiguration.current.screenWidthDp * RING_SHARE).dp.coerceIn(112.dp, 150.dp)
    LevelCard {
        val ring = @Composable { LevelRing(state, ringSize) }
        val main = @Composable { modifier: Modifier -> HeroMain(state, narrow, modifier) }
        if (narrow) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ring()
                main(Modifier.fillMaxWidth())
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                ring()
                main(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(14.dp))
        InsightRow(state, onInsight)
    }
}

private const val RING_SHARE = 0.33f

@Composable
private fun LevelRing(state: LevelState, size: Dp) {
    val colors = TwinTheme.colors
    val reduce = rememberReduceMotion()
    var go by remember { mutableStateOf(false) }
    LaunchedEffect(state.levelPercent) { go = true }
    val sweep by animateFloatAsState(
        targetValue = if (go) state.levelPercent / PERCENT else 0f,
        animationSpec = tween(if (reduce) 0 else RING_MS), label = "levelRing",
    )
    val description = stringResource(R.string.level_ring_desc, state.level, state.xp, state.nextLevelXp)
    Box(Modifier.size(size).semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = this.size.minDimension * RING_STROKE
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(Color.White.copy(alpha = TRACK_ALPHA), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            if (sweep > 0f) {
                // soft glow under the arc, then the arc itself
                drawArc(colors.accent.copy(alpha = GLOW_ALPHA), -90f, 360f * sweep, false, Offset(inset, inset), arcSize, style = Stroke(stroke * 1.5f, cap = StrokeCap.Round))
                drawArc(colors.primaryBrush, -90f, 360f * sweep, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(state.level.toString(), color = colors.ink, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp)
            Text(stringResource(R.string.level_ring_label), color = colors.inkMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

private const val RING_STROKE = 10f / 140f
private const val GLOW_ALPHA = 0.25f

@Composable
private fun HeroMain(state: LevelState, narrow: Boolean, modifier: Modifier) {
    val colors = TwinTheme.colors
    val align = if (narrow) Alignment.CenterHorizontally else Alignment.Start
    Box(modifier) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = align, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(end = if (narrow) 0.dp else 42.dp).height(36.dp),
                horizontalArrangement = if (narrow) Arrangement.Center else Arrangement.Start,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(stringResource(R.string.level_xp_now, state.xp), color = colors.ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.level_xp_of, state.nextLevelXp), color = colors.inkMuted, fontSize = 13.sp,
                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp),
                )
            }
            LevelBar(state.levelPercent, 10.dp)
            Row(
                Modifier.clip(CircleShape).background(tint()).border(1.dp, line(), CircleShape).padding(start = 8.dp, top = 7.dp, bottom = 7.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(22.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                    Icon(LevelIcon.Zap.vector(), null, Modifier.size(16.dp), tint = colors.accent)
                }
                Text(stringResource(R.string.level_pill, state.xpToNext), color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
        if (!narrow) {
            Icon(
                LevelIcon.Heart.vector(), null, tint = colors.accent.copy(alpha = HEART_ALPHA),
                modifier = Modifier.align(Alignment.TopEnd).size(34.dp),
            )
        }
    }
}

private const val HEART_ALPHA = 0.9f

/** Tapping it scrolls to what to do next (index 1 = next unlock card, [EARN_INDEX] = how to earn). */
@Composable
private fun InsightRow(state: LevelState, onInsight: (Int) -> Unit) {
    val colors = TwinTheme.colors
    val next = state.nextBadge
    val (icon, target) = when {
        state.xp == 0 -> LevelIcon.Sprout to EARN_INDEX
        next != null -> LevelIcon.Target to NEXT_INDEX
        else -> LevelIcon.Crown to null
    }
    val bold = SpanStyle(fontWeight = FontWeight.SemiBold, color = colors.ink)
    val text: AnnotatedString = when {
        state.xp == 0 -> AnnotatedString(stringResource(R.string.level_insight_start))
        next != null -> {
            val away = stringResource(R.string.level_insight_away, stringResource(next.nameRes()), next.emoji)
            buildAnnotatedString { withStyle(bold) { append("${next.xp - state.xp} XP") }; append(away) }
        }
        else -> {
            val before = stringResource(R.string.level_insight_all_before)
            val after = stringResource(R.string.level_insight_all_after)
            buildAnnotatedString { append(before); withStyle(bold) { append("${state.xp} XP") }; append(after) }
        }
    }
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(tint()).border(1.dp, line(), shape)
            .then(if (target != null) Modifier.clickable { onInsight(target) } else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon.vector(), null, Modifier.size(28.dp), tint = colors.ink)
        Text(text, color = colors.ink.copy(alpha = INSIGHT_TEXT_ALPHA), fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
        if (target != null) Icon(LevelIcon.Chevron.vector(), null, Modifier.size(18.dp), tint = colors.inkMuted)
    }
}

private const val INSIGHT_TEXT_ALPHA = 0.92f

/** Index of the "Next unlock" and "How to earn XP" cards in the screen's list. */
const val NEXT_INDEX = 1
const val EARN_INDEX = 4

// ── Next unlock ──

@Composable
fun NextUnlockCard(state: LevelState) {
    val colors = TwinTheme.colors
    val next = state.nextBadge
    LevelCard {
        if (next == null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(LevelIcon.Crown.vector(), null, Modifier.size(40.dp), tint = colors.accent)
                Column {
                    Text(stringResource(R.string.level_all_title), color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.level_all_message), color = colors.inkMuted, fontSize = 13.5.sp)
                }
            }
            return@LevelCard
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HexBadge(next.icon, (LocalConfiguration.current.screenWidthDp * HEX_SHARE).dp.coerceIn(76.dp, 96.dp))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(stringResource(R.string.level_next_kicker), color = colors.pinkSoft, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.level_next_left, next.xp - state.xp), color = colors.pinkSoft, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1,
                        modifier = Modifier.clip(CircleShape).background(tint()).border(1.dp, line(), CircleShape).padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
                Text(stringResource(next.nameRes()), color = colors.ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
                Text(
                    stringResource(R.string.level_next_blurb, stringResource(next.blurbRes())), color = colors.inkMuted, fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    LevelBar(state.nextBadgePercent, 8.dp, Modifier.weight(1f))
                    Text(stringResource(R.string.level_next_progress, state.xp, next.xp), color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                }
            }
        }
    }
}

private const val HEX_SHARE = 0.22f

/** The hexagon outline with the badge icon inside. */
@Composable
private fun HexBadge(icon: LevelIcon, size: Dp) {
    val colors = TwinTheme.colors
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            // Same six points as the web page's 100x100 hexagon path
            val points = listOf(0.50f to 0.05f, 0.89f to 0.275f, 0.89f to 0.725f, 0.50f to 0.95f, 0.11f to 0.725f, 0.11f to 0.275f)
            val path = androidx.compose.ui.graphics.Path().apply {
                points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x * w, y * h) else lineTo(x * w, y * h) }
                close()
            }
            drawPath(path, Color.White.copy(alpha = 0.03f))
            drawPath(path, colors.accent, style = Stroke(w * HEX_STROKE, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }
        Icon(icon.vector(), null, Modifier.size(size * HEX_ICON), tint = colors.accent)
    }
}

private const val HEX_STROKE = 0.024f
private const val HEX_ICON = 0.44f

// ── Badge rows ──

@Composable
fun LockedBadgesCard(state: LevelState) {
    LevelCard {
        CardHead(LevelIcon.Lock, R.string.level_locked_title, if (state.locked.isEmpty()) "" else stringResource(R.string.level_locked_sub))
        if (state.locked.isEmpty()) {
            EmptyRow(LevelIcon.Crown, R.string.level_locked_empty_title, R.string.level_locked_empty_message)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.locked, key = { it.name }) { badge ->
                    BadgeTile(badge, Modifier.width(LOCKED_TILE_DP.dp), isNext = badge == state.locked.first(), earned = false)
                }
            }
        }
    }
}

@Composable
fun EarnedBadgesCard(state: LevelState) {
    LevelCard {
        CardHead(
            LevelIcon.Award, R.string.level_earned_title,
            if (state.earned.isEmpty()) stringResource(R.string.level_earned_sub_empty)
            else stringResource(R.string.level_earned_sub, state.earned.size, Badge.entries.size),
        )
        if (state.earned.isEmpty()) {
            EmptyRow(LevelIcon.Award, R.string.level_earned_empty_title, R.string.level_earned_empty_message)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.earned, key = { it.name }) { badge -> BadgeTile(badge, Modifier.width(EARNED_TILE_DP.dp), isNext = false, earned = true) }
            }
        }
    }
}

private const val LOCKED_TILE_DP = 100
private const val EARNED_TILE_DP = 110

@Composable
private fun EmptyRow(icon: LevelIcon, @StringRes title: Int, @StringRes message: Int) {
    val colors = TwinTheme.colors
    Row(Modifier.padding(start = 4.dp, top = 8.dp, end = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Icon(icon.vector(), null, Modifier.size(64.dp), tint = colors.accent)
        Column {
            Text(stringResource(title), color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(stringResource(message), color = colors.inkMuted, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun BadgeTile(badge: Badge, modifier: Modifier, isNext: Boolean, earned: Boolean) {
    val colors = TwinTheme.colors
    val shape = RoundedCornerShape(16.dp)
    val highlighted = isNext || earned
    Box(
        modifier.clip(shape)
            .background(
                if (highlighted) Brush.verticalGradient(listOf(tint(), Color.White.copy(alpha = 0.02f)))
                else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.02f))),
            )
            .border(1.dp, if (highlighted) colors.accent else Color.White.copy(alpha = 0.10f), shape),
    ) {
        if (!earned && !isNext) {
            Icon(LevelIcon.Lock.vector(), null, Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp).size(15.dp), tint = colors.inkMuted)
        }
        Column(
            Modifier.fillMaxWidth().padding(start = 6.dp, top = 16.dp, end = 6.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(badge.icon.vector(), null, Modifier.padding(bottom = 4.dp).size(38.dp), tint = if (highlighted) colors.pinkSoft else colors.inkMuted)
            Text(stringResource(badge.nameRes()), color = colors.ink, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 17.sp)
            Text(stringResource(badge.subRes()), color = colors.inkMuted, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 16.sp)
            Row(
                Modifier.padding(top = 8.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)).border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                if (!earned) Icon(LevelIcon.Lock.vector(), null, Modifier.size(13.dp), tint = colors.inkMuted)
                Text(stringResource(R.string.level_badge_xp, badge.xp), color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            }
        }
    }
}

// ── How to earn XP ──

@Composable
fun EarnXpCard() {
    val columns = if (LocalConfiguration.current.screenWidthDp < SMALL_PHONE_DP) 1 else 2
    LevelCard {
        CardHead(LevelIcon.Sprout, R.string.level_earn_title, "")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            XpAction.entries.chunked(columns).forEach { rowItems ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowItems.forEach { EarnItem(it, Modifier.weight(1f)) }
                    if (rowItems.size < columns) Box(Modifier.weight((columns - rowItems.size).toFloat()))
                }
            }
        }
    }
}

@Composable
private fun EarnItem(action: XpAction, modifier: Modifier) {
    val colors = TwinTheme.colors
    val shape = RoundedCornerShape(14.dp)
    val iconColor = when (action.tone) {
        XpTone.Accent -> colors.accent
        XpTone.Green -> Color(GREEN)
        XpTone.Gold -> Color(GOLD)
    }
    Column(
        modifier.clip(shape).background(Color.White.copy(alpha = 0.035f)).border(1.dp, Color.White.copy(alpha = TRACK_ALPHA), shape)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(action.icon.vector(), null, Modifier.size(26.dp), tint = iconColor)
            Column(Modifier.weight(1f)) {
                Text(stringResource(action.titleRes()), color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 17.sp)
                Text(stringResource(action.descRes()), color = colors.inkMuted, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 1.dp))
            }
        }
        Text(
            stringResource(R.string.level_earn_xp, action.xp), color = colors.pinkSoft, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
            modifier = Modifier.padding(start = 36.dp).clip(CircleShape).background(tint()).border(1.dp, line(), CircleShape).padding(horizontal = 9.dp, vertical = 4.dp),
        )
    }
}

// ── Icon and text lookups ──

internal fun LevelIcon.vector(): ImageVector = when (this) {
    LevelIcon.Crown -> Icons.Rounded.WorkspacePremium
    LevelIcon.Heart -> Icons.Rounded.Favorite
    LevelIcon.Target -> Icons.Rounded.TrackChanges
    LevelIcon.Chevron -> Icons.AutoMirrored.Rounded.KeyboardArrowRight
    LevelIcon.Zap -> Icons.Rounded.Bolt
    LevelIcon.Lock -> Icons.Rounded.Lock
    LevelIcon.Sprout -> Icons.Rounded.Eco
    LevelIcon.Handshake -> Icons.Rounded.Favorite
    LevelIcon.Users -> Icons.Rounded.Groups
    LevelIcon.Gem -> Icons.Rounded.Diamond
    LevelIcon.Award -> Icons.Rounded.EmojiEvents
    LevelIcon.Chat -> Icons.Rounded.ChatBubble
    LevelIcon.Check -> Icons.Rounded.Check
    LevelIcon.Music -> Icons.Rounded.MusicNote
    LevelIcon.Pulse -> Icons.Rounded.MonitorHeart
    LevelIcon.Capsule -> Icons.Rounded.Medication
    LevelIcon.Star -> Icons.Rounded.Star
    LevelIcon.Gift -> Icons.Rounded.CardGiftcard
}

@StringRes
internal fun Badge.nameRes(): Int = when (this) {
    Badge.SweetStart -> R.string.level_badge_sweet_start
    Badge.GettingClose -> R.string.level_badge_getting_close
    Badge.BestFriends -> R.string.level_badge_best_friends
    Badge.Soulmates -> R.string.level_badge_soulmates
    Badge.ForeverUs -> R.string.level_badge_forever_us
}

@StringRes
internal fun Badge.subRes(): Int = when (this) {
    Badge.SweetStart -> R.string.level_badge_sweet_start_sub
    Badge.GettingClose -> R.string.level_badge_getting_close_sub
    Badge.BestFriends -> R.string.level_badge_best_friends_sub
    Badge.Soulmates -> R.string.level_badge_soulmates_sub
    Badge.ForeverUs -> R.string.level_badge_forever_us_sub
}

@StringRes
internal fun Badge.blurbRes(): Int = when (this) {
    Badge.SweetStart -> R.string.level_badge_sweet_start_blurb
    Badge.GettingClose -> R.string.level_badge_getting_close_blurb
    Badge.BestFriends -> R.string.level_badge_best_friends_blurb
    Badge.Soulmates -> R.string.level_badge_soulmates_blurb
    Badge.ForeverUs -> R.string.level_badge_forever_us_blurb
}

@StringRes
internal fun XpAction.titleRes(): Int = when (this) {
    XpAction.Chatting -> R.string.level_action_chatting
    XpAction.HabitDone -> R.string.level_action_habit
    XpAction.FightResolved -> R.string.level_action_fight
    XpAction.ShareSong -> R.string.level_action_song
    XpAction.Wellness -> R.string.level_action_wellness
    XpAction.LoveCapsule -> R.string.level_action_capsule
    XpAction.DreamCompleted -> R.string.level_action_dream
    XpAction.Surprise -> R.string.level_action_surprise
}

@StringRes
internal fun XpAction.descRes(): Int = when (this) {
    XpAction.Chatting -> R.string.level_action_chatting_desc
    XpAction.HabitDone -> R.string.level_action_habit_desc
    XpAction.FightResolved -> R.string.level_action_fight_desc
    XpAction.ShareSong -> R.string.level_action_song_desc
    XpAction.Wellness -> R.string.level_action_wellness_desc
    XpAction.LoveCapsule -> R.string.level_action_capsule_desc
    XpAction.DreamCompleted -> R.string.level_action_dream_desc
    XpAction.Surprise -> R.string.level_action_surprise_desc
}
