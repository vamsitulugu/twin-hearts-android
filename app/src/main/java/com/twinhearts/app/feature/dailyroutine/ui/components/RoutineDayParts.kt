package com.twinhearts.app.feature.dailyroutine.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.dailyroutine.domain.durationMinutes
import com.twinhearts.app.feature.dailyroutine.domain.formatTime12
import com.twinhearts.app.feature.dailyroutine.domain.model.DayRow
import com.twinhearts.app.feature.dailyroutine.domain.model.DaySummary
import com.twinhearts.app.feature.dailyroutine.domain.model.Occurrence
import com.twinhearts.app.feature.dailyroutine.domain.model.RoutineStatus
import com.twinhearts.app.feature.dailyroutine.domain.model.Visibility
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/*
 * Why this file exists: the parts of the Daily Routine day view (date bar, week strip, summary
 * card and the list of activities). Layout and wording follow public/daily-routine.html.
 */

private const val STRIP_BEFORE = -2L
private const val STRIP_AFTER = 4L
private const val MINUTES_PER_HOUR = 60

/** Whole minutes as "1h 30m", "2h" or "45m" (the web's duration text). */
fun minutesText(minutes: Int): String {
    val hours = minutes / MINUTES_PER_HOUR
    val rest = minutes % MINUTES_PER_HOUR
    return if (hours > 0) "${hours}h" + if (rest > 0) " ${rest}m" else "" else "${rest}m"
}

/** ‹ TODAY / Sun 25 Oct ›. Tapping the middle opens the date picker. */
@Composable
internal fun DateBar(date: LocalDate, today: LocalDate, onShift: (Long) -> Unit, onPick: (LocalDate) -> Unit) {
    val colors = TwinTheme.colors
    val context = LocalContext.current
    val badge = when {
        date == today -> R.string.dr_today
        date.isBefore(today) -> R.string.dr_past
        else -> R.string.dr_upcoming
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        ArrowButton(left = true, description = stringResource(R.string.dr_previous_day)) { onShift(-1) }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable(role = Role.Button) {
                DatePickerDialog(
                    context,
                    { _, y, m, d -> onPick(LocalDate.of(y, m + 1, d)) },
                    date.year, date.monthValue - 1, date.dayOfMonth,
                ).show()
            },
        ) {
            Text(stringResource(badge), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
            Text(dateLabel(date), style = MaterialTheme.typography.titleSmall, color = colors.ink)
        }
        ArrowButton(left = false, description = stringResource(R.string.dr_next_day)) { onShift(1) }
    }
}

/** "Sun 25 Oct". */
private fun dateLabel(date: LocalDate): String =
    "${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${date.dayOfMonth} " +
        date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())

@Composable
private fun ArrowButton(left: Boolean, description: String, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.small
    Box(
        modifier = Modifier
            .size(TwinTheme.shapes.minTouch)
            .clip(shape)
            .background(colors.glass)
            .border(1.dp, colors.line, shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (left) Icons.AutoMirrored.Rounded.KeyboardArrowLeft else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = description, tint = colors.ink,
        )
    }
}

/** Seven days around today (2 back, 4 ahead). The window follows today, not the open day, so it never jumps. */
@Composable
internal fun WeekStrip(selected: LocalDate, today: LocalDate, onPick: (LocalDate) -> Unit) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.small
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        (STRIP_BEFORE..STRIP_AFTER).forEach { offset ->
            val day = today.plusDays(offset)
            val on = day == selected
            Column(
                modifier = Modifier
                    .width(44.dp)
                    .clip(shape)
                    .background(if (on) colors.neutralGlass else colors.glass)
                    .border(1.dp, if (day == today) colors.accent else colors.line, shape)
                    .clickable(role = Role.Button) { onPick(day) }
                    .padding(vertical = TwinTheme.spacing.s),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                    style = MaterialTheme.typography.labelSmall, color = colors.inkMuted,
                )
                Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, color = colors.ink)
            }
        }
    }
}

/** "Today 50%" with a bar, "1 of 2 activities completed · 1 remaining" and the Next line. */
@Composable
internal fun SummaryCard(summary: DaySummary, date: LocalDate, isToday: Boolean) {
    val colors = TwinTheme.colors
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = if (isToday) stringResource(R.string.dr_today_title) else "${date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${date.dayOfMonth}",
                style = MaterialTheme.typography.titleSmall, color = colors.ink,
            )
            Text("${summary.percent}%", style = MaterialTheme.typography.titleSmall, color = colors.ink)
        }
        Spacer(Modifier.height(TwinTheme.spacing.s))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(colors.glass),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(summary.percent / 100f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(colors.primaryBrush),
            )
        }
        Spacer(Modifier.height(TwinTheme.spacing.s))
        Text(
            stringResource(R.string.dr_summary_line, summary.done, summary.total, summary.remaining),
            style = MaterialTheme.typography.bodySmall, color = colors.inkMuted,
        )
        summary.next?.let { next ->
            Spacer(Modifier.height(TwinTheme.spacing.s))
            val minutes = next.minutesAway
            val whenText = when {
                next.occurrence.status == RoutineStatus.InProgress -> stringResource(R.string.dr_happening_now)
                minutes > 0 -> stringResource(R.string.dr_in_time, minutesText(minutes))
                else -> stringResource(R.string.dr_starting_soon)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                Text(
                    stringResource(R.string.dr_next),
                    style = MaterialTheme.typography.labelSmall, color = colors.ink,
                    modifier = Modifier
                        .clip(TwinTheme.shapes.pill)
                        .background(colors.neutralGlass)
                        .padding(horizontal = TwinTheme.spacing.s, vertical = 2.dp),
                )
                Text(
                    stringResource(R.string.dr_next_line, formatTime12(next.occurrence.startTime), next.occurrence.routine.title, whenText),
                    style = MaterialTheme.typography.bodySmall, color = colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** One row of the day: an activity card, a free-time gap or the NOW line. */
@Composable
internal fun DayRowItem(row: DayRow, onOpen: (Occurrence) -> Unit, onAddInGap: (String, String) -> Unit) {
    when (row) {
        is DayRow.Activity -> ActivityRow(row.occurrence) { onOpen(row.occurrence) }
        is DayRow.FreeTime -> FreeTimeRow(row, onAddInGap)
        is DayRow.Now -> NowRow(row.time)
    }
}

@Composable
private fun statusColor(status: RoutineStatus): Color {
    val colors = TwinTheme.colors
    return when (status) {
        RoutineStatus.Completed -> colors.success
        RoutineStatus.InProgress -> colors.accent
        RoutineStatus.Missed -> colors.error
        RoutineStatus.Skipped, RoutineStatus.Upcoming -> colors.inkFaint
    }
}

@Composable
private fun ActivityRow(occurrence: Occurrence, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    val routine = occurrence.routine
    val status = occurrence.status
    val tint = statusColor(status)
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick, contentPadding = TwinTheme.spacing.m) {
        Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
            Column(modifier = Modifier.width(76.dp)) {
                Text(formatTime12(occurrence.startTime), style = MaterialTheme.typography.labelMedium, color = colors.ink)
                Text(
                    "${formatTime12(occurrence.endTime)} · ${minutesText(durationMinutes(occurrence.startTime, occurrence.endTime))}",
                    style = MaterialTheme.typography.labelSmall, color = colors.inkFaint,
                )
            }
            Box(modifier = Modifier.padding(top = 4.dp).size(10.dp).clip(CircleShape).background(tint))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    Text(
                        "${routine.type.emoji} ${routine.title}",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (status == RoutineStatus.Completed) colors.inkMuted else colors.ink,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    Text(stringResource(routine.type.labelRes), style = MaterialTheme.typography.labelSmall, color = colors.inkFaint)
                }
                if (routine.visibility == Visibility.Shared) {
                    Text(stringResource(R.string.dr_shared_badge), style = MaterialTheme.typography.labelSmall, color = colors.pinkSoft)
                }
                if (routine.description.isNotBlank()) {
                    Text(routine.description, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text(stringResource(status.labelRes), style = MaterialTheme.typography.labelSmall, color = tint)
            }
        }
    }
}

@Composable
private fun FreeTimeRow(row: DayRow.FreeTime, onAdd: (String, String) -> Unit) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            stringResource(R.string.dr_free_time, formatTime12(row.from), formatTime12(row.to), minutesText(row.minutes)),
            style = MaterialTheme.typography.labelSmall, color = colors.inkFaint, modifier = Modifier.weight(1f),
        )
        Text(
            stringResource(R.string.dr_free_add),
            style = MaterialTheme.typography.labelMedium, color = colors.ink,
            modifier = Modifier
                .clip(TwinTheme.shapes.pill)
                .clickable(role = Role.Button) { onAdd(row.from, row.to) }
                .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.xs),
        )
    }
}

@Composable
private fun NowRow(time: String) {
    val colors = TwinTheme.colors
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.accent))
        Text(stringResource(R.string.dr_now, formatTime12(time)), style = MaterialTheme.typography.labelSmall, color = colors.accent)
        Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.accent))
    }
}
