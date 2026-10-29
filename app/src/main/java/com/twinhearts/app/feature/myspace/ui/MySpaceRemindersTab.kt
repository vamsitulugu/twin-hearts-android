package com.twinhearts.app.feature.myspace.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsNone
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.twinhearts.app.R
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.myspace.domain.MySpaceLogic
import com.twinhearts.app.feature.myspace.domain.model.MySpaceData
import com.twinhearts.app.feature.myspace.domain.model.NewReminder
import com.twinhearts.app.feature.myspace.domain.model.Reminder
import com.twinhearts.app.feature.myspace.domain.model.Visibility
import java.time.LocalDate
import java.time.LocalTime

/** Web "🔔 Reminders": a title, a date and time, who it is for, and the list soonest first. */
@Composable
internal fun RemindersTab(
    data: MySpaceData,
    saving: Boolean,
    resetKey: Int,
    selection: Selection,
    onSave: (NewReminder) -> Unit,
) {
    var title by rememberSaveable(resetKey) { mutableStateOf("") }
    var date by rememberSaveable(resetKey) { mutableStateOf("") }
    var time by rememberSaveable(resetKey) { mutableStateOf("") }
    var forWhom by rememberSaveable(resetKey) { mutableStateOf(Visibility.Both) }
    var note by rememberSaveable(resetKey) { mutableStateOf("") }
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    val sorted = remember(data.reminders) { MySpaceLogic.sortedReminders(MySpaceLogic.visibleReminders(data.reminders)) }

    GlassCard {
        Text(stringResource(R.string.myspace_rem_add_title), style = MaterialTheme.typography.titleSmall, color = TwinTheme.colors.ink)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            TwinTextField(
                value = title,
                onValueChange = { title = it },
                label = stringResource(R.string.myspace_rem_title),
                placeholder = stringResource(R.string.myspace_rem_title_hint),
                maxLength = 120,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                PickerRow(
                    label = stringResource(R.string.myspace_date),
                    value = if (date.isBlank()) stringResource(R.string.myspace_pick_date) else formatDay(date),
                    onClick = { showDate = true },
                    modifier = Modifier.weight(1f),
                )
                PickerRow(
                    label = stringResource(R.string.myspace_time),
                    value = if (time.isBlank()) stringResource(R.string.myspace_pick_time) else formatClock(time),
                    onClick = { showTime = true },
                    modifier = Modifier.weight(1f),
                )
            }
            FieldLabel(stringResource(R.string.myspace_rem_for))
            VisibilitySelect(
                selected = forWhom,
                onSelect = { forWhom = it },
                labelFor = { stringResource(reminderTargetLabel(it)) },
            )
            TwinTextField(
                value = note,
                onValueChange = { note = it },
                label = stringResource(R.string.myspace_rem_note),
                placeholder = stringResource(R.string.myspace_rem_note_hint),
                maxLength = 200,
                modifier = Modifier.fillMaxWidth(),
            )
            GlassButton(
                text = stringResource(R.string.myspace_rem_add),
                onClick = { onSave(NewReminder(title.trim(), date, time, forWhom, note.trim())) },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && date.isNotBlank() && time.isNotBlank(),
                loading = saving,
            )
        }
    }

    if (sorted.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.myspace_rem_empty_title),
            message = stringResource(R.string.myspace_rem_empty_hint),
            icon = Icons.Rounded.NotificationsNone,
        )
    } else {
        sorted.forEach { ReminderCard(it, selection) }
    }

    if (showDate) {
        DatePickDialog(
            initial = MySpaceLogic.parseDateOrNull(date) ?: LocalDate.now(),
            onPick = {
                date = it.toString()
                showDate = false
            },
            onDismiss = { showDate = false },
        )
    }
    if (showTime) {
        TimePickDialog(
            initial = time.takeIf { it.isNotBlank() }?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
            onPick = {
                time = it.toStoredTime()
                showTime = false
            },
            onDismiss = { showTime = false },
        )
    }
}

private fun reminderTargetLabel(target: Visibility): Int = when (target) {
    Visibility.Both -> R.string.myspace_rem_for_both
    Visibility.Self -> R.string.myspace_rem_for_self
    Visibility.Partner -> R.string.myspace_rem_for_partner
}

@Composable
private fun ReminderCard(reminder: Reminder, selection: Selection) {
    val colors = TwinTheme.colors
    SelectableCard(
        selectMode = selection.mode,
        selected = reminder.id in selection.ids,
        onLongPress = { selection.onLongPress(reminder.id) },
        onToggle = { selection.onToggle(reminder.id) },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "🔔 ${reminder.title}",
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (!selection.mode) TrashButton { selection.onDeleteOne(reminder.id) }
        }
        Text(
            "${formatDay(reminder.date)} · ${formatClock(reminder.time).ifBlank { "—" }}",
            style = MaterialTheme.typography.bodySmall,
            color = colors.inkFaint,
        )
        Text(
            "📣 ${stringResource(reminderTargetLabel(reminder.forWhom))}",
            style = MaterialTheme.typography.labelSmall,
            color = colors.pinkSoft,
        )
        if (reminder.note.isNotBlank()) {
            Text(reminder.note, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
        }
    }
}
