package com.twinhearts.app.feature.dailyroutine.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.dailyroutine.domain.formatTime12
import com.twinhearts.app.feature.dailyroutine.domain.model.Reminder
import com.twinhearts.app.feature.dailyroutine.domain.model.Repeat
import com.twinhearts.app.feature.dailyroutine.domain.model.Routine
import com.twinhearts.app.feature.dailyroutine.domain.model.RoutineDraft
import com.twinhearts.app.feature.dailyroutine.domain.model.RoutineType
import com.twinhearts.app.feature.dailyroutine.domain.model.Visibility
import com.twinhearts.app.feature.dailyroutine.ui.FormError
import com.twinhearts.app.feature.dailyroutine.ui.FormTarget
import com.twinhearts.app.feature.dailyroutine.ui.asText

/** Why this file exists: the two pop-up sheets of Daily Routine: Add / Edit form, and an activity's details with quick actions. */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetFrame(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val colors = TwinTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.card,
        contentColor = colors.ink,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TwinTheme.spacing.screenEdge)
                .padding(bottom = TwinTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = colors.ink)
            content()
        }
    }
}

/** Add Routine / Edit Routine. [existing] is the routine being edited (null when adding). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RoutineFormSheet(
    target: FormTarget,
    existing: Routine?,
    error: FormError?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (RoutineDraft) -> Unit,
) {
    val colors = TwinTheme.colors
    var title by rememberSaveable { mutableStateOf(existing?.title.orEmpty()) }
    var start by rememberSaveable { mutableStateOf(existing?.startTime ?: target.startTime) }
    var end by rememberSaveable { mutableStateOf(existing?.endTime ?: target.endTime) }
    var type by rememberSaveable { mutableStateOf((existing?.type ?: RoutineType.Routine).key) }
    var description by rememberSaveable { mutableStateOf(existing?.description.orEmpty()) }
    // The form offers four repeats; a "custom" routine edited here becomes "does not repeat" (same as the web).
    var repeat by rememberSaveable { mutableStateOf((existing?.repeat?.takeIf { it in Repeat.formOptions } ?: Repeat.None).key) }
    var reminder by rememberSaveable { mutableStateOf((existing?.reminder ?: Reminder.None).key) }
    var visibility by rememberSaveable { mutableStateOf((existing?.visibility ?: Visibility.Private).key) }
    var location by rememberSaveable { mutableStateOf(existing?.location.orEmpty()) }
    var notes by rememberSaveable { mutableStateOf(existing?.notes.orEmpty()) }

    SheetFrame(stringResource(if (existing == null) R.string.dr_add_routine else R.string.dr_edit_routine), onDismiss) {
        error?.let { Text(it.asText(), style = MaterialTheme.typography.bodySmall, color = colors.error) }
        TwinTextField(
            value = title, onValueChange = { title = it }, label = stringResource(R.string.dr_activity),
            placeholder = stringResource(R.string.dr_activity_hint),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
            RoutineTimeField(stringResource(R.string.dr_start_time), start, { start = it }, Modifier.weight(1f))
            RoutineTimeField(stringResource(R.string.dr_end_time), end, { end = it }, Modifier.weight(1f))
        }
        Text(stringResource(R.string.dr_type), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            RoutineType.entries.forEach { option ->
                TwinFilterChip(
                    label = option.emoji + " " + stringResource(option.labelRes),
                    selected = option.key == type,
                    onClick = { type = option.key },
                )
            }
        }
        RoutineTextArea(
            value = description, onValueChange = { description = it }, label = stringResource(R.string.dr_description),
            placeholder = stringResource(R.string.dr_description_hint),
        )
        Text(stringResource(R.string.dr_repeat), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            Repeat.formOptions.forEach { option ->
                TwinFilterChip(label = stringResource(option.labelRes), selected = option.key == repeat, onClick = { repeat = option.key })
            }
        }
        Text(stringResource(R.string.dr_reminder), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            Reminder.entries.forEach { option ->
                TwinFilterChip(label = stringResource(option.labelRes), selected = option.key == reminder, onClick = { reminder = option.key })
            }
        }
        Text(stringResource(R.string.dr_visibility), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            Visibility.entries.forEach { option ->
                TwinFilterChip(
                    label = option.emoji + " " + stringResource(option.labelRes),
                    selected = option.key == visibility,
                    onClick = { visibility = option.key },
                )
            }
        }
        TwinTextField(
            value = location, onValueChange = { location = it }, label = stringResource(R.string.dr_location),
            placeholder = stringResource(R.string.dr_location_hint),
        )
        RoutineTextArea(
            value = notes, onValueChange = { notes = it }, label = stringResource(R.string.dr_notes),
            placeholder = stringResource(R.string.dr_notes_hint),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
            GlassButton(
                text = stringResource(R.string.core_cancel), onClick = onDismiss,
                modifier = Modifier.weight(1f), style = GlassButtonStyle.Secondary,
            )
            GlassButton(
                text = stringResource(R.string.dr_save),
                onClick = {
                    onSave(
                        RoutineDraft(
                            title = title, startTime = start, endTime = end,
                            type = RoutineType.fromKey(type), description = description,
                            repeat = Repeat.fromKey(repeat), reminder = Reminder.fromKey(reminder),
                            visibility = Visibility.fromKey(visibility), location = location, notes = notes,
                        ),
                    )
                },
                modifier = Modifier.weight(1f),
                loading = isSaving,
            )
        }
    }
}

/**
 * Details of one activity with its quick actions. Only my own routines can be changed; the partner's
 * shared ones are read-only (a note says so).
 */
@Composable
internal fun RoutineDetailSheet(
    routine: Routine,
    isMine: Boolean,
    partnerName: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = TwinTheme.colors
    SheetFrame(routine.type.emoji + " " + routine.title, onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
            Text(
                "${formatTime12(routine.startTime)} – ${formatTime12(routine.endTime)}",
                style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted,
            )
            if (routine.description.isNotBlank()) Text(routine.description, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
            if (routine.location.isNotBlank()) Text("📍 " + routine.location, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
            if (routine.notes.isNotBlank()) Text("📝 " + routine.notes, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
        }
        if (isMine) {
            GlassButton(stringResource(R.string.dr_mark_complete), onComplete, Modifier.fillMaxWidth(), loading = isSaving)
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                GlassButton(stringResource(R.string.dr_skip), onSkip, Modifier.weight(1f), style = GlassButtonStyle.Secondary, enabled = !isSaving)
                GlassButton(stringResource(R.string.dr_edit), onEdit, Modifier.weight(1f), style = GlassButtonStyle.Secondary, enabled = !isSaving)
                GlassButton(stringResource(R.string.dr_duplicate), onDuplicate, Modifier.weight(1f), style = GlassButtonStyle.Secondary, enabled = !isSaving)
            }
            GlassButton(stringResource(R.string.dr_delete), onDelete, Modifier.fillMaxWidth(), style = GlassButtonStyle.Ghost, enabled = !isSaving)
        } else {
            Text(stringResource(R.string.dr_read_only, partnerName), style = MaterialTheme.typography.labelMedium, color = colors.inkFaint)
        }
    }
}
