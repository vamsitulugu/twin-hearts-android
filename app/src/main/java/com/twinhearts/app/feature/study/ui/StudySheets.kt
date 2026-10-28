package com.twinhearts.app.feature.study.ui

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.SerifFamily
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.study.domain.formatSlotTime
import com.twinhearts.app.feature.study.domain.model.NewSlot
import com.twinhearts.app.feature.study.domain.model.NewSubject
import com.twinhearts.app.feature.study.domain.model.SlotAlarm
import com.twinhearts.app.feature.study.domain.model.StudyDay
import com.twinhearts.app.feature.study.domain.parseTime
import com.twinhearts.app.feature.study.domain.toStoredTime
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

private const val MAX_NAME = 80
private const val MAX_EMOJI = 4
private const val MAX_CHAPTERS_DIGITS = 4
private const val DEFAULT_TOTAL = 10
private const val DEFAULT_HOUR = 18

/** Why this file exists: the "Add Subject" form of the web Study page (name, chapters, emoji). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubjectSheet(
    saving: Boolean,
    onSave: (NewSubject) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = TwinTheme.colors
    var name by rememberSaveable { mutableStateOf("") }
    var totalText by rememberSaveable { mutableStateOf("") }
    var doneText by rememberSaveable { mutableStateOf("") }
    var emoji by rememberSaveable { mutableStateOf("") }

    // Like the web: an empty "total" means 10 chapters, an empty "done" means 0.
    val total = (totalText.toIntOrNull() ?: DEFAULT_TOTAL).coerceAtLeast(1)
    val done = (doneText.toIntOrNull() ?: 0).coerceIn(0, total)

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
                text = stringResource(R.string.study_subject_form_title),
                color = colors.ink,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = SerifFamily,
            )
            TwinTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.study_subject_name),
                placeholder = stringResource(R.string.study_subject_name_hint),
                maxLength = MAX_NAME,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                TwinTextField(
                    value = totalText,
                    onValueChange = { totalText = it.filter(Char::isDigit) },
                    label = stringResource(R.string.study_subject_total),
                    placeholder = DEFAULT_TOTAL.toString(),
                    keyboardType = KeyboardType.Number,
                    maxLength = MAX_CHAPTERS_DIGITS,
                    modifier = Modifier.weight(1f),
                )
                TwinTextField(
                    value = doneText,
                    onValueChange = { doneText = it.filter(Char::isDigit) },
                    label = stringResource(R.string.study_subject_done),
                    placeholder = "0",
                    keyboardType = KeyboardType.Number,
                    maxLength = MAX_CHAPTERS_DIGITS,
                    modifier = Modifier.weight(1f),
                )
            }
            TwinTextField(
                value = emoji,
                onValueChange = { emoji = it },
                label = stringResource(R.string.study_subject_emoji),
                placeholder = "📐",
                imeAction = ImeAction.Done,
                maxLength = MAX_EMOJI,
            )
            GlassButton(
                text = stringResource(R.string.study_subject_save),
                onClick = { onSave(NewSubject(name.trim(), total, done, emoji.trim())) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TwinTheme.spacing.s),
                enabled = name.isNotBlank(),
                loading = saving,
            )
        }
    }
}

/** Why this file exists: the "Add Timetable Slot" form of the web Study page (day, times, subject, alarm). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSlotSheet(
    saving: Boolean,
    onSave: (NewSlot) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = TwinTheme.colors
    var day by rememberSaveable { mutableStateOf(StudyDay.Monday) }
    var start by rememberSaveable { mutableStateOf("") } // "HH:mm" or empty
    var end by rememberSaveable { mutableStateOf("") }
    var subject by rememberSaveable { mutableStateOf("") }
    var alarm by rememberSaveable { mutableStateOf(SlotAlarm.None) }
    var pickingStart by rememberSaveable { mutableStateOf(false) }
    var pickingEnd by rememberSaveable { mutableStateOf(false) }

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
                text = stringResource(R.string.study_slot_form_title),
                color = colors.ink,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = SerifFamily,
            )
            FieldLabel(stringResource(R.string.study_slot_day))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
            ) {
                StudyDay.entries.forEach { option ->
                    TwinFilterChip(
                        label = option.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        selected = day == option,
                        onClick = { day = option },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                TimeField(
                    label = stringResource(R.string.study_slot_start),
                    value = if (start.isBlank()) stringResource(R.string.study_slot_time_none) else formatSlotTime(start),
                    onClick = { pickingStart = true },
                    modifier = Modifier.weight(1f),
                )
                TimeField(
                    label = stringResource(R.string.study_slot_end),
                    value = if (end.isBlank()) stringResource(R.string.study_slot_time_none) else formatSlotTime(end),
                    onClick = { pickingEnd = true },
                    modifier = Modifier.weight(1f),
                )
            }
            TwinTextField(
                value = subject,
                onValueChange = { subject = it },
                label = stringResource(R.string.study_slot_subject),
                placeholder = stringResource(R.string.study_slot_subject_hint),
                imeAction = ImeAction.Done,
                maxLength = MAX_NAME,
            )
            FieldLabel(stringResource(R.string.study_slot_alarm))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
            ) {
                SlotAlarm.entries.forEach { option ->
                    TwinFilterChip(
                        label = stringResource(option.labelRes),
                        selected = alarm == option,
                        onClick = { alarm = option },
                    )
                }
            }
            if (alarm != SlotAlarm.None && start.isBlank()) {
                Text(
                    text = stringResource(R.string.study_slot_alarm_needs_start),
                    color = colors.inkMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            GlassButton(
                text = stringResource(R.string.study_slot_save),
                onClick = { onSave(NewSlot(day, start, end, subject.trim(), alarm)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TwinTheme.spacing.s),
                enabled = subject.isNotBlank(),
                loading = saving,
            )
        }
    }

    if (pickingStart) {
        TimePickDialog(
            initial = parseTime(start),
            onPick = {
                start = it.toStoredTime()
                pickingStart = false
            },
            onDismiss = { pickingStart = false },
        )
    }
    if (pickingEnd) {
        TimePickDialog(
            initial = parseTime(end) ?: parseTime(start)?.plusHours(1),
            onPick = {
                end = it.toStoredTime()
                pickingEnd = false
            },
            onDismiss = { pickingEnd = false },
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = TwinTheme.colors.inkMuted, style = MaterialTheme.typography.labelMedium)
}

/** A tappable glass field that shows a time and opens the time picker. */
@Composable
private fun TimeField(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.field
    Column(modifier = modifier) {
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
        ) {
            Text(value, color = colors.ink, style = MaterialTheme.typography.bodyLarge)
        }
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
                Text(stringResource(R.string.study_ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.core_cancel)) } },
        text = { TimePicker(state = state) },
    )
}
