package com.twinhearts.app.feature.myspace.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.myspace.domain.MySpaceLogic
import com.twinhearts.app.feature.myspace.domain.model.Habit
import com.twinhearts.app.feature.myspace.domain.model.MySpaceData
import com.twinhearts.app.feature.myspace.domain.model.NewHabit

/** Web "✅ Habits": today's progress ring, a tip, the 12-week heatmap, and the list with streaks. */
@Composable
internal fun HabitsTab(
    data: MySpaceData,
    saving: Boolean,
    selection: Selection,
    onAdd: (NewHabit) -> Unit,
    onToggle: (String) -> Unit,
) {
    var showAdd by rememberSaveable { mutableStateOf(false) }
    val summary = remember(data.habits) { MySpaceLogic.habitSummary(data.habits) }
    val heat = remember(data.habits) { MySpaceLogic.heatmap(data.habits) }

    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.l)) {
            ConsistencyRing(percent = summary.consistency, modifier = Modifier.size(100.dp))
            Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
                Text(stringResource(R.string.myspace_habit_today), style = MaterialTheme.typography.labelMedium, color = TwinTheme.colors.inkMuted)
                Text(
                    "${summary.doneToday}/${summary.total}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TwinTheme.colors.ink,
                )
                Text(
                    pluralStringResource(R.plurals.myspace_habit_avg_streak, summary.total, summary.avgStreak, summary.total),
                    style = MaterialTheme.typography.bodySmall,
                    color = TwinTheme.colors.inkMuted,
                )
            }
        }
    }

    InsightCard(
        icon = when {
            summary.total == 0 -> "💡"
            summary.consistency >= 80 -> "🔥"
            summary.consistency >= 50 -> "✅"
            else -> "💪"
        },
        text = when {
            summary.total == 0 -> stringResource(R.string.myspace_habit_tip_none)
            summary.consistency >= 80 -> stringResource(R.string.myspace_habit_tip_great, summary.consistency)
            summary.consistency >= 50 -> stringResource(R.string.myspace_habit_tip_ok, summary.consistency)
            else -> stringResource(R.string.myspace_habit_tip_low, summary.consistency)
        },
    )

    if (data.habits.isNotEmpty()) {
        GlassCard {
            Text(stringResource(R.string.myspace_habit_heatmap), style = MaterialTheme.typography.titleSmall, color = TwinTheme.colors.ink)
            Heatmap(levels = heat, modifier = Modifier.padding(top = TwinTheme.spacing.m))
            Row(
                modifier = Modifier.padding(top = TwinTheme.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs),
            ) {
                Text(stringResource(R.string.myspace_less), style = MaterialTheme.typography.labelSmall, color = TwinTheme.colors.inkFaint)
                (0..3).forEach { level -> Box(Modifier.size(12.dp).clip(TwinTheme.shapes.small).background(heatColor(level))) }
                Text(stringResource(R.string.myspace_more), style = MaterialTheme.typography.labelSmall, color = TwinTheme.colors.inkFaint)
            }
        }
    }

    SectionTitle(stringResource(R.string.myspace_habit_mine, data.myName))
    if (data.habits.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.myspace_habit_empty_title),
            message = stringResource(R.string.myspace_habit_empty_hint),
        )
    } else {
        data.habits.forEach { habit -> HabitRow(habit, selection, onToggle) }
    }
    GlassButton(
        text = stringResource(R.string.myspace_habit_add),
        onClick = { showAdd = true },
        modifier = Modifier.fillMaxWidth(),
        style = GlassButtonStyle.Secondary,
        leadingIcon = Icons.Rounded.Add,
    )

    if (showAdd) {
        AddHabitDialog(
            saving = saving,
            onSave = {
                onAdd(it)
                showAdd = false
            },
            onDismiss = { showAdd = false },
        )
    }
}

@Composable
private fun HabitRow(habit: Habit, selection: Selection, onToggle: (String) -> Unit) {
    val colors = TwinTheme.colors
    val done = MySpaceLogic.doneToday(habit)
    SelectableCard(
        selectMode = selection.mode,
        selected = habit.id in selection.ids,
        onLongPress = { selection.onLongPress(habit.id) },
        onToggle = { selection.onToggle(habit.id) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
            val checkLabel = stringResource(if (done) R.string.myspace_habit_undo else R.string.myspace_habit_check, habit.name)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(TwinTheme.shapes.circle)
                    .background(if (done) colors.accent else Color.Transparent)
                    .border(2.dp, if (done) colors.accent else colors.inkFaint, TwinTheme.shapes.circle)
                    .semantics { contentDescription = checkLabel }
                    .clickable(enabled = !selection.mode, role = Role.Checkbox) { onToggle(habit.id) },
                contentAlignment = Alignment.Center,
            ) {
                if (done) Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.ink, modifier = Modifier.size(18.dp))
            }
            Text(habit.emoji, style = MaterialTheme.typography.titleLarge)
            Text(habit.name, style = MaterialTheme.typography.bodyLarge, color = colors.ink, modifier = Modifier.weight(1f))
            Text(
                "${habit.streak}🔥",
                style = MaterialTheme.typography.labelMedium,
                color = colors.pinkSoft,
                modifier = Modifier
                    .clip(TwinTheme.shapes.pill)
                    .background(colors.glass)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
            if (!selection.mode) TrashButton { selection.onDeleteOne(habit.id) }
        }
    }
}

@Composable
private fun AddHabitDialog(saving: Boolean, onSave: (NewHabit) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var emoji by rememberSaveable { mutableStateOf("") }
    var goal by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TwinTheme.colors.card,
        title = { Text(stringResource(R.string.myspace_habit_new), color = TwinTheme.colors.ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                TwinTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.myspace_habit_name),
                    placeholder = stringResource(R.string.myspace_habit_name_hint),
                    maxLength = 60,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                    TwinTextField(
                        value = emoji,
                        onValueChange = { emoji = it },
                        label = stringResource(R.string.myspace_habit_emoji),
                        placeholder = "✅",
                        maxLength = 4,
                        modifier = Modifier.width(96.dp),
                    )
                    TwinTextField(
                        value = goal,
                        onValueChange = { goal = it.filter(Char::isDigit) },
                        label = stringResource(R.string.myspace_habit_goal),
                        placeholder = "1",
                        maxLength = 2,
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && !saving,
                onClick = { onSave(NewHabit(name.trim(), emoji.trim().ifBlank { "✅" }, goal.toIntOrNull()?.coerceAtLeast(1) ?: 1)) },
            ) { Text(stringResource(R.string.myspace_habit_add_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.core_cancel)) } },
    )
}

/** A short tip with an icon, used under the ring and on the Wellness tab. */
@Composable
internal fun InsightCard(icon: String, text: String) {
    GlassCard(contentPadding = TwinTheme.spacing.m) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
            Text(icon, style = MaterialTheme.typography.titleLarge)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = TwinTheme.colors.inkMuted)
        }
    }
}

@Composable
private fun ConsistencyRing(percent: Int, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val track = colors.lineSoft
    val brush = colors.primaryBrush
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = 8.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke))
            drawArc(
                brush = brush, startAngle = -90f, sweepAngle = 360f * percent.coerceIn(0, 100) / 100f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$percent%", style = MaterialTheme.typography.titleLarge, color = colors.ink)
            Text(stringResource(R.string.myspace_habit_7day), style = MaterialTheme.typography.labelSmall, color = colors.inkFaint)
        }
    }
}

@Composable
private fun heatColor(level: Int): Color {
    val c = TwinTheme.colors
    return when (level) {
        0 -> c.lineSoft
        1 -> c.accent.copy(alpha = 0.30f)
        2 -> c.accent.copy(alpha = 0.60f)
        else -> c.accent
    }
}

/** 12 weeks as columns of 7 days, oldest on the left, newest at the bottom right. */
@Composable
private fun Heatmap(levels: List<Int>, modifier: Modifier = Modifier) {
    val palette = (0..3).map { heatColor(it) }
    val columns = (levels.size + 6) / 7
    Canvas(modifier = modifier.fillMaxWidth().aspectRatio(columns / 7f)) {
        val gap = 3.dp.toPx()
        val cell = (size.width - gap * (columns - 1)) / columns
        levels.forEachIndexed { index, level ->
            val col = index / 7
            val row = index % 7
            drawRoundRect(
                color = palette[level.coerceIn(0, 3)],
                topLeft = Offset(col * (cell + gap), row * (cell + gap)),
                size = Size(cell, cell),
                cornerRadius = CornerRadius(3.dp.toPx()),
            )
        }
    }
}
