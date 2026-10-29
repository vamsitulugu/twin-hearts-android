package com.twinhearts.app.feature.myspace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.myspace.domain.MySpaceLogic
import com.twinhearts.app.feature.myspace.domain.model.MySpaceData
import com.twinhearts.app.feature.myspace.domain.model.WellnessDay
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Web "💆 Wellness": today's water, sleep and exercise, a 7-day sleep trend, and the week's moods. */
@Composable
internal fun WellnessTab(
    data: MySpaceData,
    saving: Boolean,
    onSave: (water: Int, sleep: Double, exercise: Int) -> Unit,
) {
    val colors = TwinTheme.colors
    val today = remember { LocalDate.now() }
    val todayDay = data.wellness[today.toString()] ?: WellnessDay()
    val week = remember(data.wellness) { MySpaceLogic.lastSevenDays(data.wellness, today) }
    val averages = remember(data.wellness) { MySpaceLogic.wellnessAverages(data.wellness) }
    val met = MySpaceLogic.goalsMet(todayDay)

    var water by rememberSaveable(todayDay) { mutableStateOf(todayDay.water.takeIf { it > 0 }?.toString().orEmpty()) }
    var sleep by rememberSaveable(todayDay) { mutableStateOf(todayDay.sleep.takeIf { it > 0 }?.let(::trimNumber).orEmpty()) }
    var exercise by rememberSaveable(todayDay) { mutableStateOf(todayDay.exercise.takeIf { it > 0 }?.toString().orEmpty()) }

    // Hero: the three goals of today.
    GlassCard {
        Text(stringResource(R.string.myspace_well_today_title), style = MaterialTheme.typography.titleSmall, color = colors.ink)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TwinTheme.spacing.m),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            GoalStat("💧", "${todayDay.water}/${MySpaceLogic.WATER_GOAL}", stringResource(R.string.myspace_well_glasses))
            GoalStat("😴", "${trimNumber(todayDay.sleep)}/${trimNumber(MySpaceLogic.SLEEP_GOAL)}", stringResource(R.string.myspace_well_hours))
            GoalStat("🏃", "${todayDay.exercise}/${MySpaceLogic.EXERCISE_GOAL}", stringResource(R.string.myspace_well_minutes))
        }
    }
    InsightCard(
        icon = when {
            todayDay.isEmpty -> "💡"
            met == 3 -> "🌟"
            met >= 1 -> "✅"
            else -> "🌱"
        },
        text = when {
            todayDay.isEmpty -> stringResource(R.string.myspace_well_tip_none)
            met == 3 -> stringResource(R.string.myspace_well_tip_all)
            met >= 1 -> stringResource(R.string.myspace_well_tip_some, met)
            else -> stringResource(R.string.myspace_well_tip_start)
        },
    )

    GlassCard {
        Text(stringResource(R.string.myspace_well_sleep_trend), style = MaterialTheme.typography.titleSmall, color = colors.ink)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TwinTheme.spacing.m),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            week.forEach { (date, day) ->
                SleepBar(label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), hours = day?.sleep ?: 0.0)
            }
        }
    }

    GlassCard {
        Text(stringResource(R.string.myspace_well_mood_history), style = MaterialTheme.typography.titleSmall, color = colors.ink)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TwinTheme.spacing.m),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            week.forEach { (date, day) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.inkFaint,
                    )
                    Text(
                        day?.mood?.substringBefore(' ')?.takeIf { it.isNotBlank() } ?: "—",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }
    }

    GlassCard {
        Text(stringResource(R.string.myspace_well_log_title), style = MaterialTheme.typography.titleSmall, color = colors.ink)
        Column(
            modifier = Modifier.padding(top = TwinTheme.spacing.m),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            TwinTextField(
                value = water,
                onValueChange = { water = it.filter(Char::isDigit) },
                label = stringResource(R.string.myspace_well_water),
                placeholder = "0",
                keyboardType = KeyboardType.Number,
                maxLength = 2,
            )
            TwinTextField(
                value = sleep,
                onValueChange = { sleep = it.filter { c -> c.isDigit() || c == '.' } },
                label = stringResource(R.string.myspace_well_sleep),
                placeholder = "0",
                keyboardType = KeyboardType.Decimal,
                maxLength = 4,
            )
            TwinTextField(
                value = exercise,
                onValueChange = { exercise = it.filter(Char::isDigit) },
                label = stringResource(R.string.myspace_well_exercise),
                placeholder = "0",
                keyboardType = KeyboardType.Number,
                maxLength = 3,
            )
            GlassButton(
                text = stringResource(R.string.myspace_well_save),
                onClick = {
                    onSave(
                        water.toIntOrNull()?.coerceIn(0, 20) ?: 0,
                        sleep.toDoubleOrNull()?.coerceIn(0.0, 24.0) ?: 0.0,
                        exercise.toIntOrNull()?.coerceIn(0, 300) ?: 0,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                loading = saving,
            )
        }
    }

    GlassCard {
        Text(stringResource(R.string.myspace_well_summary), style = MaterialTheme.typography.titleSmall, color = colors.ink)
        Text(
            stringResource(R.string.myspace_well_avg, "%.1f".format(averages.water), "%.1f".format(averages.sleep)),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkMuted,
            modifier = Modifier.padding(top = TwinTheme.spacing.s),
        )
    }
}

@Composable
private fun GoalStat(icon: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, style = MaterialTheme.typography.headlineSmall)
        Text(value, style = MaterialTheme.typography.titleMedium, color = TwinTheme.colors.ink)
        Text(unit, style = MaterialTheme.typography.labelSmall, color = TwinTheme.colors.inkFaint)
    }
}

@Composable
private fun SleepBar(label: String, hours: Double) {
    val colors = TwinTheme.colors
    val maxHeight = 64.dp
    val fraction = (hours / 10.0).coerceIn(0.0, 1.0).toFloat()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.height(maxHeight), contentAlignment = Alignment.BottomCenter) {
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(maxOf(4.dp, maxHeight * fraction))
                    .clip(TwinTheme.shapes.small)
                    .background(if (hours > 0) colors.accent else colors.lineSoft),
            )
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.inkFaint, modifier = Modifier.padding(top = TwinTheme.spacing.xs))
    }
}

/** 7.0 -> "7", 6.5 -> "6.5". */
private fun trimNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
