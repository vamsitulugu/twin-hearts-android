package com.twinhearts.app.feature.fights.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.common.rememberReduceMotion
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.fights.domain.FightMath
import com.twinhearts.app.feature.fights.domain.model.Fight
import com.twinhearts.app.feature.fights.domain.model.FightCategory
import com.twinhearts.app.feature.fights.domain.model.FightInsight
import com.twinhearts.app.feature.fights.domain.model.FightRange
import com.twinhearts.app.feature.fights.domain.model.FightStats
import java.time.LocalDate

/** Why this file exists: the Fight Log dashboard parts: rate ring card, two metric tiles, recent fights, reconnect row. */

private val ringGreen = Color(0xFF34D399)
private val ringTeal = Color(0xFF2DD4BF)

@Composable
fun ResolutionRateCard(
    stats: FightStats,
    range: FightRange,
    onRange: (FightRange) -> Unit,
    onInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    GlassCard(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.fights_rate_title), style = MaterialTheme.typography.titleMedium, color = colors.ink, fontWeight = FontWeight.Bold)
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = stringResource(R.string.fights_rate_info),
                        tint = colors.inkMuted,
                        modifier = Modifier.size(TwinTheme.shapes.minTouch).clickable(role = Role.Button, onClick = onInfo).padding(14.dp),
                    )
                }
                Text(stringResource(R.string.fights_rate_sub), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
            }
            RangePicker(range, onRange)
        }
        Row(
            Modifier.fillMaxWidth().padding(top = TwinTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.l),
        ) {
            RateRing(stats)
            Legend(stats, Modifier.weight(1f))
        }
        InsightStrip(FightMath.insight(stats), Modifier.padding(top = TwinTheme.spacing.m))
    }
}

@Composable
private fun RangePicker(range: FightRange, onRange: (FightRange) -> Unit) {
    val colors = TwinTheme.colors
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .heightIn(min = 44.dp)
                .clip(TwinTheme.shapes.small)
                .background(colors.brand.copy(alpha = 0.35f))
                .border(1.dp, colors.lineSoft, TwinTheme.shapes.small)
                .clickable(role = Role.DropdownList) { open = true }
                .padding(horizontal = TwinTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs),
        ) {
            Text(stringResource(rangeLabel(range)), style = MaterialTheme.typography.labelLarge, color = colors.ink, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.fights_range_label), tint = colors.ink, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = colors.card) {
            FightRange.entries.forEach { r ->
                DropdownMenuItem(
                    text = { Text(stringResource(rangeLabel(r)), color = colors.ink) },
                    onClick = { open = false; onRange(r) },
                )
            }
        }
    }
}

/** The ring: a faint track and a green arc for the resolved share, with the percent in the middle. */
@Composable
private fun RateRing(stats: FightStats) {
    val colors = TwinTheme.colors
    val rate = FightMath.rate(stats)
    val reduce = rememberReduceMotion()
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(rate, reduce) {
        val target = rate * 3.6f
        if (reduce) sweep.snapTo(target) else sweep.animateTo(target, tween(durationMillis = 800))
    }
    Box(Modifier.size(128.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(128.dp)) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(colors.ink.copy(alpha = 0.12f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            if (sweep.value > 0f) {
                drawArc(
                    Brush.sweepGradient(listOf(ringGreen, ringTeal, ringGreen)),
                    -90f, sweep.value, false, Offset(inset, inset), arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (stats.total > 0) "$rate%" else "—",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.ink,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.fights_ring_caption).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = colors.inkMuted,
            )
        }
    }
}

@Composable
private fun Legend(stats: FightStats, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val rows = listOf(
        Triple(R.string.fights_legend_resolved, stats.resolved, colors.success),
        Triple(R.string.fights_legend_open, stats.open, colors.error),
        Triple(R.string.fights_legend_total, stats.total, colors.inkFaint),
    )
    Column(
        modifier
            .clip(TwinTheme.shapes.card)
            .background(colors.glass)
            .border(1.dp, colors.lineSoft, TwinTheme.shapes.card)
            .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.xs),
    ) {
        rows.forEachIndexed { index, (label, count, dot) ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
            ) {
                Box(Modifier.size(10.dp).clip(TwinTheme.shapes.circle).background(dot))
                Text(stringResource(label), style = MaterialTheme.typography.bodyMedium, color = colors.ink, modifier = Modifier.weight(1f))
                Text(count.toString(), style = MaterialTheme.typography.bodyMedium, color = colors.ink, fontWeight = FontWeight.Bold)
            }
            if (index < rows.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.lineSoft))
        }
    }
}

@Composable
private fun InsightStrip(insight: FightInsight, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val (icon, head, sub) = when (insight) {
        FightInsight.None -> Triple(Icons.Rounded.Favorite, R.string.fights_insight_none_head, R.string.fights_insight_none_sub)
        FightInsight.Intense -> Triple(Icons.Rounded.Warning, R.string.fights_insight_intense_head, R.string.fights_insight_intense_sub)
        FightInsight.AllResolved -> Triple(Icons.Rounded.Favorite, R.string.fights_insight_great_head, R.string.fights_insight_all_sub)
        FightInsight.Great -> Triple(Icons.Rounded.Favorite, R.string.fights_insight_great_head, R.string.fights_insight_great_sub)
        FightInsight.StillOpen -> Triple(Icons.Rounded.VolunteerActivism, R.string.fights_insight_open_head, R.string.fights_insight_open_sub)
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(TwinTheme.shapes.card)
            .background(colors.brand.copy(alpha = 0.18f))
            .padding(TwinTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        Icon(icon, null, tint = colors.pinkSoft, modifier = Modifier.size(22.dp))
        Column {
            Text(stringResource(head), style = MaterialTheme.typography.titleSmall, color = colors.ink, fontWeight = FontWeight.Bold)
            Text(stringResource(sub), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
        }
    }
}

/** The two small tiles: Avg Intensity and Avg Resolution Time, each with its change since last month. */
@Composable
fun MetricTiles(fights: List<Fight>, range: FightRange, today: LocalDate, modifier: Modifier = Modifier) {
    val inRange = FightMath.stats(FightMath.inRange(fights, today, range))
    val now = FightMath.stats(FightMath.window(fights, today, 30, 0))
    val before = FightMath.stats(FightMath.window(fights, today, 60, 30))
    val intensityText = inRange.intensity?.let { stringResource(R.string.fights_out_of_ten, FightMath.num(it)) } ?: "—"
    val days = inRange.days
    val daysText = when {
        days == null -> "—"
        days == 0.0 -> stringResource(R.string.fights_same_day)
        FightMath.num(days) == "1" -> stringResource(R.string.fights_day, "1")
        else -> stringResource(R.string.fights_days, FightMath.num(days))
    }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
        MetricTile(Icons.Rounded.BarChart, stringResource(R.string.fights_metric_intensity), intensityText, FightMath.delta(now.intensity, before.intensity), Modifier.weight(1f))
        MetricTile(Icons.Rounded.Schedule, stringResource(R.string.fights_metric_resolution), daysText, FightMath.delta(now.days, before.days), Modifier.weight(1f))
    }
}

@Composable
private fun MetricTile(icon: ImageVector, name: String, value: String, delta: Int?, modifier: Modifier) {
    val colors = TwinTheme.colors
    GlassCard(modifier, contentPadding = TwinTheme.spacing.m) {
        Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = colors.accent, modifier = Modifier.size(28.dp))
            Column {
                Text(name, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                Text(value, style = MaterialTheme.typography.titleLarge, color = colors.ink, fontWeight = FontWeight.Bold)
            }
        }
        if (delta == null) {
            Text(stringResource(R.string.fights_metric_no_history), style = MaterialTheme.typography.bodySmall, color = colors.inkFaint, modifier = Modifier.padding(top = TwinTheme.spacing.m))
        } else {
            // For both numbers lower is better, so a drop is good news (green) and a rise is bad (red).
            val tone = if (delta < 0) colors.success else if (delta > 0) colors.error else colors.inkMuted
            val arrow = if (delta < 0) Icons.Rounded.TrendingDown else if (delta > 0) Icons.Rounded.TrendingUp else Icons.Rounded.Remove
            Row(Modifier.padding(top = TwinTheme.spacing.m), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
                Icon(arrow, null, tint = tone, modifier = Modifier.size(16.dp))
                Text((if (delta > 0) "+" else "") + delta + "%", style = MaterialTheme.typography.labelMedium, color = tone, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.fights_metric_than_last), style = MaterialTheme.typography.bodySmall, color = colors.inkFaint)
            }
        }
    }
}

@Composable
fun LogFightButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassButton(stringResource(R.string.fights_log_cta), onClick, modifier.fillMaxWidth(), leadingIcon = Icons.Rounded.NoteAdd)
}

@Composable
fun RecentHeader(showAll: Boolean, canToggle: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.fights_recent), style = MaterialTheme.typography.titleLarge, color = colors.ink, fontWeight = FontWeight.Bold)
        if (canToggle) {
            Row(
                Modifier.heightIn(min = 44.dp).clip(TwinTheme.shapes.pill).clickable(role = Role.Button, onClick = onToggle).padding(horizontal = TwinTheme.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(if (showAll) R.string.fights_show_less else R.string.fights_view_all), style = MaterialTheme.typography.labelLarge, color = colors.pinkSoft)
                Icon(if (showAll) Icons.Rounded.KeyboardArrowUp else Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.pinkSoft, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun FightRow(fight: Fight, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val category = FightCategory.from(fight.category)
    GlassCard(modifier.fillMaxWidth(), onClick = onClick, contentPadding = TwinTheme.spacing.m) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
            ToneTile(categoryIcon(category), toneColor(category.tone))
            Column(Modifier.weight(1f)) {
                Text(fight.title, style = MaterialTheme.typography.titleSmall, color = colors.ink, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(formatFightDate(fight.date), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
            }
            StatePill(fight.isOpen, stringResource(if (fight.isOpen) R.string.fights_state_open else R.string.fights_state_resolved))
            Text(stringResource(R.string.fights_out_of_ten, fight.intensity.toString()), style = MaterialTheme.typography.labelLarge, color = colors.inkMuted)
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.inkFaint, modifier = Modifier.size(20.dp))
        }
    }
}

/** "No conflicts recorded." with a heart, the explanation and a Reconnect Activities button. */
@Composable
fun NoFightsYet(onActivities: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    Column(
        modifier.fillMaxWidth().padding(vertical = TwinTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        Box(
            Modifier.size(84.dp).clip(TwinTheme.shapes.circle).background(colors.glass).border(1.dp, colors.lineSoft, TwinTheme.shapes.circle),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.FavoriteBorder, null, tint = colors.inkMuted, modifier = Modifier.size(34.dp)) }
        Text(stringResource(R.string.fights_empty_title), style = MaterialTheme.typography.titleMedium, color = colors.ink, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.fights_empty_desc), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted, textAlign = TextAlign.Center)
        GlassButton(stringResource(R.string.fights_reconnect), onActivities, style = com.twinhearts.app.core.design.GlassButtonStyle.Secondary)
    }
}

@Composable
fun ReconnectRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    GlassCard(modifier.fillMaxWidth(), onClick = onClick, contentPadding = TwinTheme.spacing.m) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
            ToneTile(Icons.Rounded.VolunteerActivism, colors.success)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.fights_reconnect), style = MaterialTheme.typography.titleSmall, color = colors.ink, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.fights_reconnect_sub), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.inkFaint, modifier = Modifier.size(20.dp))
        }
    }
}
