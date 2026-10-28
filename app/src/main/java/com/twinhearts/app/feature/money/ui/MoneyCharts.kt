package com.twinhearts.app.feature.money.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.money.domain.formatRupees
import com.twinhearts.app.feature.money.domain.model.CategoryTotal
import com.twinhearts.app.feature.money.domain.model.WeekTotal

private const val FULL_CIRCLE = 360f
private const val TOP = -90f

/** Same colours the web uses per category; any other category gets the "Other" grey. */
private val categoryColors = mapOf(
    "Food" to Color(0xFFFF6B6B),
    "Transport" to Color(0xFF8E5AF7),
    "Entertainment" to Color(0xFF38BDF8),
    "Shopping" to Color(0xFFFBBF24),
    "Travel" to Color(0xFF34D399),
    "Health" to Color(0xFFF472B6),
    "Date" to Color(0xFFB4235F),
)
private val otherColor = Color(0xFFB19BA7)

private fun colorFor(category: String): Color = categoryColors[category] ?: otherColor

/** Ring showing how much of the income was kept (web `fin-ring`). */
@Composable
fun SavedRing(percent: Int, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val track = colors.lineSoft
    val brush = colors.primaryBrush
    val description = stringResource(R.string.money_ring_cd, percent)
    Box(
        modifier = modifier
            .size(RING_SIZE)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(RING_SIZE)) {
            val stroke = Stroke(width = RING_STROKE.toPx(), cap = StrokeCap.Round)
            val inset = RING_STROKE.toPx() / 2
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            drawArc(color = track, startAngle = 0f, sweepAngle = FULL_CIRCLE, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = stroke)
            if (percent > 0) {
                drawArc(
                    brush = brush,
                    startAngle = TOP,
                    sweepAngle = FULL_CIRCLE * percent / 100f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = stroke,
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$percent%", style = MaterialTheme.typography.titleLarge, color = colors.ink)
            Text(stringResource(R.string.money_saved), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
        }
    }
}

private val RING_SIZE = 110.dp
private val RING_STROKE = 9.dp

/** Donut of this month's spending per category, with a legend beside / below it. */
@Composable
fun CategoryDonut(items: List<CategoryTotal>, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val total = items.sumOf { it.amount }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
        Box(modifier = Modifier.align(Alignment.CenterHorizontally), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(DONUT_SIZE)) {
                val strokeWidth = DONUT_STROKE.toPx()
                val inset = strokeWidth / 2
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                var start = TOP
                items.forEach { item ->
                    val sweep = (item.amount / total * FULL_CIRCLE).toFloat()
                    drawArc(
                        color = colorFor(item.category),
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = strokeWidth),
                    )
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatRupees(total), style = MaterialTheme.typography.titleMedium, color = colors.ink)
                Text(stringResource(R.string.money_total), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
            }
        }
        items.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                Box(Modifier.size(10.dp).clip(TwinTheme.shapes.circle).background(colorFor(item.category)))
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(formatRupees(item.amount), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
            }
        }
    }
}

private val DONUT_SIZE = 150.dp
private val DONUT_STROKE = 18.dp

/** Eight weekly bars (web `fin-trend`). The tallest week fills the height. */
@Composable
fun WeeklyTrend(weeks: List<WeekTotal>, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val max = weeks.maxOf { it.amount }.coerceAtLeast(1.0)
    Row(
        modifier = modifier.fillMaxWidth().height(TREND_HEIGHT + 24.dp),
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        verticalAlignment = Alignment.Bottom,
    ) {
        weeks.forEach { week ->
            val barHeight = (TREND_HEIGHT * (week.amount / max).toFloat()).coerceAtLeast(MIN_BAR)
            val description = stringResource(R.string.money_trend_week_cd, week.label, formatRupees(week.amount))
            Column(
                modifier = Modifier.weight(1f).semantics { contentDescription = description },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(BAR_WIDTH)
                        .height(barHeight)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(if (week.amount > 0) colors.accent else colors.lineSoft),
                )
                Text(
                    text = week.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.inkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = TwinTheme.spacing.xs),
                )
            }
        }
    }
}

private val TREND_HEIGHT = 72.dp
private val MIN_BAR = 3.dp
private const val BAR_WIDTH = 0.7f
