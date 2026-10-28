package com.twinhearts.app.feature.study.ui

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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private const val FULL_CIRCLE = 360f
private const val TOP = -90f
private val RING_SIZE = 110.dp
private val RING_STROKE = 9.dp

/** Ring showing how many chapters are done across all subjects (web `study-ring`). */
@Composable
fun StudyRing(percent: Int, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val track = colors.lineSoft
    val brush = colors.primaryBrush
    val description = stringResource(R.string.study_ring_cd, percent)
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
            drawArc(
                color = track, startAngle = 0f, sweepAngle = FULL_CIRCLE, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize, style = stroke,
            )
            if (percent > 0) {
                drawArc(
                    brush = brush, startAngle = TOP, sweepAngle = FULL_CIRCLE * percent / 100f, useCenter = false,
                    topLeft = Offset(inset, inset), size = arcSize, style = stroke,
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$percent%", style = MaterialTheme.typography.titleLarge, color = colors.ink)
            Text(
                stringResource(R.string.study_complete),
                style = MaterialTheme.typography.labelSmall,
                color = colors.inkMuted,
            )
        }
    }
}

private val BAR_AREA = 72.dp
private val MIN_BAR = 3.dp
private const val BAR_WIDTH = 0.7f

/** Seven bars of scheduled hours per weekday, today highlighted (web `study-week-row`). */
@Composable
fun WeeklyHoursBars(
    hours: List<Pair<DayOfWeek, Double>>,
    today: DayOfWeek,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    val max = hours.maxOf { it.second }.coerceAtLeast(1.0)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(BAR_AREA + 24.dp),
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        verticalAlignment = Alignment.Bottom,
    ) {
        hours.forEach { (day, value) ->
            val label = day.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val barHeight = (BAR_AREA * (value / max).toFloat()).coerceAtLeast(MIN_BAR)
            val description = stringResource(R.string.study_week_bar_cd, label, "%.1f".format(value))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = description },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(BAR_WIDTH)
                        .height(barHeight)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(
                            when {
                                day == today -> colors.accent
                                value > 0 -> colors.pinkSoft
                                else -> colors.lineSoft
                            },
                        ),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day == today) colors.ink else colors.inkMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(top = TwinTheme.spacing.xs),
                )
            }
        }
    }
}
