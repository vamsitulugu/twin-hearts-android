package com.twinhearts.app.feature.myspace.ui

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.Avatar
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.myspace.domain.model.Visibility
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Small grey heading above a block, like the web `card-title` / "YOUR ENTRIES". */
@Composable
internal fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = TwinTheme.colors.inkMuted,
        modifier = modifier.padding(top = TwinTheme.spacing.s),
    )
}

@Composable
internal fun FieldLabel(text: String) {
    Text(text, color = TwinTheme.colors.inkMuted, style = MaterialTheme.typography.labelMedium)
}

/** Several lines of text in the same glass field as [com.twinhearts.app.core.design.TwinTextField]. */
@Composable
internal fun TwinTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    minHeight: androidx.compose.ui.unit.Dp = 120.dp,
    maxLength: Int = 2000,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.field
    BasicTextField(
        value = value,
        onValueChange = { if (it.length <= maxLength) onValueChange(it) },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.ink),
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = modifier.fillMaxWidth().then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .clip(shape)
                    .background(colors.glass)
                    .border(1.dp, colors.line, shape)
                    .padding(TwinTheme.spacing.l),
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.inkFaint)
                }
                inner()
            }
        },
    )
}

/** "Only Me / Both See / Partner Only" as a dropdown field, like the web `priv-select`. */
@Composable
internal fun VisibilitySelect(
    selected: Visibility,
    onSelect: (Visibility) -> Unit,
    modifier: Modifier = Modifier,
    labelFor: @Composable (Visibility) -> String = { stringResource(it.labelRes) },
) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.field
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 46.dp)
                .clip(shape)
                .background(colors.glass)
                .border(1.dp, colors.line, shape)
                .clickable(role = Role.DropdownList) { open = true }
                .padding(horizontal = TwinTheme.spacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(labelFor(selected), style = MaterialTheme.typography.bodyMedium, color = colors.ink)
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = colors.inkFaint)
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.background(colors.card),
        ) {
            Visibility.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text("${option.emoji} ${labelFor(option)}", color = colors.ink) },
                    onClick = {
                        onSelect(option)
                        open = false
                    },
                )
            }
        }
    }
}

/** Round avatar and an UPPERCASE heading, like the web `user-block-head`. */
@Composable
internal fun UserBlockHead(name: String, title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(top = TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        Avatar(name = name, size = 28.dp)
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = TwinTheme.colors.ink,
        )
    }
}

/** A glass card that ticks on long-press / tap while the page is in select mode (web `sd-selectable`). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SelectableCard(
    selectMode: Boolean,
    selected: Boolean,
    onLongPress: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    fill: Color = TwinTheme.colors.glass,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.card
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(fill)
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.accent else colors.line, shape)
            .semantics { this.selected = selected }
            .combinedClickable(
                role = Role.Button,
                onClick = { if (selectMode) onToggle() },
                onLongClick = { if (!selectMode) onLongPress() else onToggle() },
            ),
    ) {
        Column(modifier = Modifier.padding(TwinTheme.spacing.l), content = content)
        if (selectMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(TwinTheme.spacing.s)
                    .size(24.dp)
                    .clip(TwinTheme.shapes.circle)
                    .background(if (selected) colors.accent else Color.Transparent)
                    .border(1.5.dp, if (selected) colors.accent else colors.inkFaint, TwinTheme.shapes.circle),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.ink, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** A tappable glass field that shows a value and opens a picker. */
@Composable
internal fun PickerRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    onTrailingClick: () -> Unit = {},
) {
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
internal fun DatePickDialog(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
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
                    if (millis != null) onPick(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()) else onDismiss()
                },
            ) { Text(stringResource(R.string.myspace_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.core_cancel)) } },
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimePickDialog(initial: LocalTime?, onPick: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val start = initial ?: LocalTime.of(9, 0)
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
                Text(stringResource(R.string.myspace_ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.core_cancel)) } },
        text = { TimePicker(state = state) },
    )
}

private val MediumDate: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
private val ShortTime: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

/** `2026-10-28` -> "28 Oct 2026" in the phone's language. Unreadable text is shown as it is. */
internal fun formatDay(text: String): String =
    runCatching { LocalDate.parse(text).format(MediumDate) }.getOrDefault(text)

/** `19:30` -> "7:30 PM" (or 19:30), following the phone's language. Empty stays empty. */
internal fun formatClock(text: String): String =
    if (text.isBlank()) text else runCatching { LocalTime.parse(text).format(ShortTime) }.getOrDefault(text)

internal fun LocalTime.toStoredTime(): String = "%02d:%02d".format(hour, minute)
