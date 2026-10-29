package com.twinhearts.app.feature.period.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.SerifFamily
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.period.domain.DEFAULT_CYCLE_DAYS
import com.twinhearts.app.feature.period.domain.DEFAULT_FLOW_DAYS
import com.twinhearts.app.feature.period.domain.model.NewPeriod
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Why this file exists: the web "Log Period" form (start date, flow days, cycle length, notes).
 * Limits match the web inputs: flow 1–14 days, cycle 20–45 days.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogPeriodSheet(
    saving: Boolean,
    onSave: (NewPeriod) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = TwinTheme.colors
    var startText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var flowText by rememberSaveable { mutableStateOf("") }
    var cycleText by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val start = LocalDate.parse(startText)

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
                text = stringResource(R.string.period_form_title),
                color = colors.ink,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = SerifFamily,
            )
            Column {
                Text(
                    stringResource(R.string.period_form_start),
                    color = colors.inkMuted,
                    style = MaterialTheme.typography.labelMedium,
                )
                val shape = TwinTheme.shapes.field
                Row(
                    modifier = Modifier
                        .padding(top = TwinTheme.spacing.xs)
                        .fillMaxWidth()
                        .heightIn(min = TwinTheme.shapes.fieldHeight)
                        .clip(shape)
                        .border(1.dp, colors.line, shape)
                        .clickable(role = Role.Button) { showPicker = true }
                        .padding(horizontal = TwinTheme.spacing.l),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = start.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                        color = colors.ink,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                TwinTextField(
                    value = flowText,
                    onValueChange = { flowText = it.filter(Char::isDigit) },
                    label = stringResource(R.string.period_form_flow),
                    placeholder = DEFAULT_FLOW_DAYS.toString(),
                    keyboardType = KeyboardType.Number,
                    maxLength = 2,
                    modifier = Modifier.weight(1f),
                )
                TwinTextField(
                    value = cycleText,
                    onValueChange = { cycleText = it.filter(Char::isDigit) },
                    label = stringResource(R.string.period_form_cycle),
                    placeholder = DEFAULT_CYCLE_DAYS.toString(),
                    keyboardType = KeyboardType.Number,
                    maxLength = 2,
                    modifier = Modifier.weight(1f),
                )
            }
            TwinTextField(
                value = note,
                onValueChange = { note = it },
                label = stringResource(R.string.period_form_notes),
                placeholder = stringResource(R.string.period_form_notes_hint),
                imeAction = ImeAction.Done,
                maxLength = MAX_NOTE,
            )
            GlassButton(
                text = stringResource(R.string.period_form_save),
                onClick = {
                    onSave(
                        NewPeriod(
                            start = start,
                            flow = flowText.toIntOrNull()?.coerceIn(MIN_FLOW, MAX_FLOW) ?: DEFAULT_FLOW_DAYS,
                            cycle = cycleText.toIntOrNull()?.coerceIn(MIN_CYCLE, MAX_CYCLE) ?: DEFAULT_CYCLE_DAYS,
                            note = note.trim(),
                        ),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TwinTheme.spacing.s),
                loading = saving,
            )
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let {
                            startText = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()
                        }
                        showPicker = false
                    },
                ) { Text(stringResource(R.string.period_ok)) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.core_cancel)) } },
        ) {
            DatePicker(state = state)
        }
    }
}

private const val MAX_NOTE = 200
private const val MIN_FLOW = 1
private const val MAX_FLOW = 14
private const val MIN_CYCLE = 20
private const val MAX_CYCLE = 45
