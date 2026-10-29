package com.twinhearts.app.feature.settings.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.selection.toggleable
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.GlassIconButton
import com.twinhearts.app.core.design.PillSwitch
import com.twinhearts.app.core.design.TwinTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Why this file exists: the small building blocks every Settings page shares (rows, headers, pickers, dialogs). */

private val RowMinHeight = 56.dp
private val RowIconSize = 24.dp
private val ChevronSize = 20.dp

private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

internal fun LocalDate.asSettingsText(): String = format(dateFormat)
internal fun LocalTime.asSettingsText(): String = format(timeFormat)

/** A titled group: small grey caps title, then one glass card holding the rows (see the reference screenshot). */
@Composable
internal fun SettingsSection(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        if (title != null) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                color = TwinTheme.colors.inkMuted,
                modifier = Modifier
                    .padding(start = TwinTheme.spacing.s)
                    .semantics { heading() },
            )
        }
        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 0.dp, content = content)
    }
}

/** One tappable line: coloured icon, title, small grey text, optional value and arrow. */
@Composable
internal fun SettingsRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    danger: Boolean = false,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = TwinTheme.colors
    val tap = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .then(tap)
            .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.l),
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(RowIconSize))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = if (danger) colors.error else colors.ink,
            )
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = colors.inkFaint)
            }
        }
        if (value != null) {
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = colors.inkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing?.invoke()
        if (showChevron && onClick != null) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.inkFaint,
                modifier = Modifier.size(ChevronSize),
            )
        }
    }
}

/** A row with an on/off pill. The whole row is the switch, so the pill itself is hidden from screen readers. */
@Composable
internal fun SettingsSwitchRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsRow(
        icon = icon,
        iconTint = iconTint,
        title = title,
        subtitle = subtitle,
        modifier = modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        trailing = {
            PillSwitch(checked = checked, onCheckedChange = {}, enabled = false, modifier = Modifier.clearAndSetSemantics {})
        },
    )
}

/** Label on the left, current value on the right; tapping opens a picker (dates, times). */
@Composable
internal fun SettingsValueRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.ink)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = colors.inkMuted)
    }
}

/** Header of a sub-page: round back button, title, small grey line (the web's `.set-bar`). */
@Composable
internal fun SubPageHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.core_back), onBack)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = colors.ink, modifier = Modifier.semantics { heading() })
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.inkFaint)
        }
    }
}

/** Small grey note under a card. */
@Composable
internal fun SettingsNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = TwinTheme.colors.inkFaint,
        modifier = modifier.padding(horizontal = TwinTheme.spacing.s),
    )
}

/** Date picker in a dialog. "Clear" empties the date, like clearing the web's date input. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsDatePickerDialog(
    initial: LocalDate?,
    onPicked: (LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = TwinTheme.colors
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val picked = state.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                onPicked(picked)
            }) { Text(stringResource(R.string.settings_ok), color = colors.pinkSoft) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onPicked(null) }) { Text(stringResource(R.string.settings_clear), color = colors.inkMuted) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel), color = colors.inkMuted) }
            }
        },
    ) {
        DatePicker(state = state)
    }
}

/** Clock picker in a dialog (quiet hours). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsTimePickerDialog(
    initial: LocalTime,
    onPicked: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = TwinTheme.colors
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = DateFormat.is24HourFormat(LocalContext.current),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onPicked(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(R.string.settings_ok), color = colors.pinkSoft)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel), color = colors.inkMuted) } },
    )
}

/** Yes / no question. [danger] colours the confirm button red. */
@Composable
internal fun SettingsConfirmDialog(
    title: String?,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = true,
) {
    val colors = TwinTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        title = if (title != null) {
            { Text(title, color = colors.ink) }
        } else {
            null
        },
        text = { Text(body, color = colors.inkMuted) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, color = if (danger) colors.error else colors.pinkSoft) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel), color = colors.inkMuted) } },
    )
}
