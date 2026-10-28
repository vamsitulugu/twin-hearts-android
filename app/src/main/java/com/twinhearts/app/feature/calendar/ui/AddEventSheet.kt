package com.twinhearts.app.feature.calendar.ui

import android.text.format.DateFormat
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.SerifFamily
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.calendar.domain.formatEventTime
import com.twinhearts.app.feature.calendar.domain.model.EventAlarm
import com.twinhearts.app.feature.calendar.domain.model.EventCategory
import com.twinhearts.app.feature.calendar.domain.model.NewEvent
import com.twinhearts.app.feature.calendar.domain.toStoredTime
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Why this file exists: the "Add Event" form of the web calendar (title, date, time, note,
 * category, alarm). Saving writes the same `events` item the web writes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventSheet(
    initialDate: LocalDate,
    saving: Boolean,
    onSave: (NewEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = TwinTheme.colors
    var title by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var dateText by rememberSaveable { mutableStateOf(initialDate.toString()) }
    var timeText by rememberSaveable { mutableStateOf("") } // "HH:mm" or empty
    var category by rememberSaveable { mutableStateOf(EventCategory.Date) }
    var alarm by rememberSaveable { mutableStateOf(EventAlarm.None) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    val date = LocalDate.parse(dateText)
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.card,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = TwinTheme.spacing.screenEdge, vertical = TwinTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            Text(
                text = stringResource(R.string.calendar_form_title),
                color = colors.ink,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = SerifFamily,
            )
            TwinTextField(
                value = title,
                onValueChange = { title = it },
                label = stringResource(R.string.calendar_form_name),
                placeholder = stringResource(R.string.calendar_form_name_hint),
                maxLength = MAX_TITLE,
            )
            PickerRow(
                label = stringResource(R.string.calendar_form_date),
                value = date.format(dateFormat),
                onClick = { showDatePicker = true },
            )
            PickerRow(
                label = stringResource(R.string.calendar_form_time),
                value = if (timeText.isBlank()) stringResource(R.string.calendar_form_time_none) else formatEventTime(timeText),
                onClick = { showTimePicker = true },
                trailing = if (timeText.isBlank()) null else stringResource(R.string.calendar_form_clear_time),
                onTrailingClick = {
                    timeText = ""
                    alarm = EventAlarm.None
                },
            )
            TwinTextField(
                value = note,
                onValueChange = { note = it },
                label = stringResource(R.string.calendar_form_note),
                imeAction = ImeAction.Done,
                maxLength = MAX_NOTE,
            )
            FieldLabel(stringResource(R.string.calendar_form_category))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
            ) {
                EventCategory.entries.forEach { option ->
                    TwinFilterChip(
                        label = "${option.emoji} ${stringResource(option.labelRes)}",
                        selected = category == option,
                        onClick = { category = option },
                    )
                }
            }
            FieldLabel(stringResource(R.string.calendar_form_alarm))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
            ) {
                EventAlarm.entries.forEach { option ->
                    TwinFilterChip(
                        label = stringResource(option.labelRes),
                        selected = alarm == option,
                        onClick = { alarm = option },
                    )
                }
            }
            if (alarm != EventAlarm.None && timeText.isBlank()) {
                Text(
                    text = stringResource(R.string.calendar_form_alarm_needs_time),
                    color = colors.inkMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            GlassButton(
                text = stringResource(R.string.calendar_form_save),
                onClick = {
                    onSave(
                        NewEvent(
                            title = title.trim(),
                            date = date,
                            time = timeText,
                            note = note.trim(),
                            category = category,
                            alarm = alarm,
                        ),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TwinTheme.spacing.s),
                enabled = title.isNotBlank(),
                loading = saving,
            )
        }
    }

    if (showDatePicker) {
        DatePickDialog(
            initial = date,
            onPick = {
                dateText = it.toString()
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
    if (showTimePicker) {
        TimePickDialog(
            initial = timeText.takeIf { it.isNotBlank() }?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
            onPick = {
                timeText = it.toStoredTime()
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = TwinTheme.colors.inkMuted, style = MaterialTheme.typography.labelMedium)
}

/** A tappable glass field that shows a value and opens a picker. */
@Composable
private fun PickerRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    trailing: String? = null,
    onTrailingClick: () -> Unit = {},
) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.field
    Column {
        FieldLabel(label)
        Row(
            modifier = Modifier
                .padding(top = TwinTheme.spacing.xs)
                .fillMaxWidth()
                .heightIn(min = TwinTheme.shapes.fieldHeight)
                .clip(shape)
                .border(1.dp, colors.line, shape)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = TwinTheme.spacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(value, color = colors.ink, style = MaterialTheme.typography.bodyLarge)
            if (trailing != null) {
                Text(
                    text = trailing,
                    color = colors.accent,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .clickable(role = Role.Button, onClick = onTrailingClick)
                        .padding(vertical = TwinTheme.spacing.s),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickDialog(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    // The date picker works in UTC midnight milliseconds.
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        onPick(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    } else {
                        onDismiss()
                    }
                },
            ) { Text(stringResource(R.string.calendar_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.core_cancel)) } },
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickDialog(initial: LocalTime?, onPick: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val start = initial ?: LocalTime.of(DEFAULT_HOUR, 0)
    val state = rememberTimePickerState(
        initialHour = start.hour,
        initialMinute = start.minute,
        is24Hour = DateFormat.is24HourFormat(context),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TwinTheme.colors.card,
        confirmButton = {
            TextButton(onClick = { onPick(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(R.string.calendar_ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.core_cancel)) } },
        text = { TimePicker(state = state) },
    )
}

private const val MAX_TITLE = 120
private const val MAX_NOTE = 300
private const val DEFAULT_HOUR = 19
