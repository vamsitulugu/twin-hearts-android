package com.twinhearts.app.feature.profile.ui.components

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.profile.domain.model.TogetherSpan
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Long-ish date in the phone's language, e.g. "27 Oct 2026". */
fun formatDate(date: LocalDate): String = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

/** "1 Year • 2 Months • 3 Days": zero parts are left out, "0 Days" shows when it is today (same as the web). */
fun spanText(context: Context, span: TogetherSpan): String {
    if (span.notStarted) return ""
    val r = context.resources
    val parts = buildList {
        if (span.years > 0) add(r.getQuantityString(R.plurals.profile_years, span.years, span.years))
        if (span.months > 0) add(r.getQuantityString(R.plurals.profile_months, span.months, span.months))
        if (span.days > 0 || isEmpty()) add(r.getQuantityString(R.plurals.profile_days, span.days, span.days))
    }
    return parts.joinToString(" • ")
}

/** "5 minutes ago" in the phone's language. */
fun relativeTime(nowMs: Long, thenMs: Long): String =
    DateUtils.getRelativeTimeSpanString(thenMs, nowMs, DateUtils.MINUTE_IN_MILLIS).toString()

/** A round icon on a soft tile: the small icon chip the web uses in front of rows and stats. */
@Composable
fun ProfileChip(icon: ImageVector, modifier: Modifier = Modifier, tint: androidx.compose.ui.graphics.Color = TwinTheme.colors.inkMuted) {
    Box(
        modifier = modifier.size(40.dp).clip(TwinTheme.shapes.small).background(TwinTheme.colors.neutralGlass.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

/**
 * Big round profile picture. Shows the photo, or the plain person silhouette like the web.
 * [showCamera] adds the small camera badge on the corner that means "tap to change".
 */
@Composable
fun ProfileAvatar(
    imageUrl: String?,
    description: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    showCamera: Boolean = false,
    uploading: Boolean = false,
) {
    val colors = TwinTheme.colors
    Box(modifier = modifier.size(size)) {
        val tap = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(colors.primaryBrush)
                .border(BorderStroke(3.dp, colors.inkMuted), CircleShape)
                .then(tap)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Person, contentDescription = null, tint = colors.ink, modifier = Modifier.size(size * 0.55f))
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.size(size),
                )
            }
            if (uploading) {
                Box(Modifier.size(size).background(colors.background.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(color = colors.accent, modifier = Modifier.size(size * 0.35f))
                }
            }
        }
        if (showCamera) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(colors.background)
                    .border(1.dp, colors.line, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = colors.ink, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** Multi-line text box styled like the design system's single-line field (that one cannot grow). */
@Composable
fun ProfileTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    maxLength: Int = 300,
) {
    val colors = TwinTheme.colors
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.inkMuted, modifier = Modifier.padding(bottom = TwinTheme.spacing.xs))
        BasicTextField(
            value = value,
            onValueChange = { if (it.length <= maxLength) onValueChange(it) },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.ink),
            cursorBrush = SolidColor(colors.accent),
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 110.dp)
                        .clip(TwinTheme.shapes.field)
                        .background(colors.glass)
                        .border(1.dp, colors.line, TwinTheme.shapes.field)
                        .padding(TwinTheme.spacing.l),
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.inkFaint)
                    }
                    inner()
                }
            },
        )
    }
}

/** A tappable field that opens a calendar. Shows [placeholder] while no date is chosen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDateField(
    label: String,
    value: LocalDate?,
    onChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String,
    clearable: Boolean = true,
) {
    val colors = TwinTheme.colors
    var open by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.inkMuted, modifier = Modifier.padding(bottom = TwinTheme.spacing.xs))
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = TwinTheme.shapes.fieldHeight)
                .clip(TwinTheme.shapes.field)
                .background(colors.glass)
                .border(1.dp, colors.line, TwinTheme.shapes.field)
                .clickable(role = Role.Button) { open = true }
                .padding(start = TwinTheme.spacing.l, end = TwinTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value?.let(::formatDate) ?: placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = if (value == null) colors.inkFaint else colors.ink,
                modifier = Modifier.weight(1f),
            )
            if (clearable && value != null) {
                IconButton(onClick = { onChange(null) }) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.profile_clear_date), tint = colors.inkMuted)
                }
            }
        }
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = value?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms -> onChange(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()) }
                    open = false
                }) { Text(stringResource(R.string.profile_ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.profile_cancel)) } },
        ) { DatePicker(state = state) }
    }
}

/** A tappable field that opens a clock. [value] is "HH:mm" or empty. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTimeField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String,
) {
    val colors = TwinTheme.colors
    var open by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.inkMuted, modifier = Modifier.padding(bottom = TwinTheme.spacing.xs))
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = TwinTheme.shapes.fieldHeight)
                .clip(TwinTheme.shapes.field)
                .background(colors.glass)
                .border(1.dp, colors.line, TwinTheme.shapes.field)
                .clickable(role = Role.Button) { open = true }
                .padding(start = TwinTheme.spacing.l, end = TwinTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value.ifBlank { placeholder },
                style = MaterialTheme.typography.bodyLarge,
                color = if (value.isBlank()) colors.inkFaint else colors.ink,
                modifier = Modifier.weight(1f),
            )
            if (value.isNotBlank()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.profile_clear_time), tint = colors.inkMuted)
                }
            }
        }
    }
    if (open) {
        val parts = value.split(":")
        val state = rememberTimePickerState(
            initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 9,
            initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { open = false },
            containerColor = colors.card,
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    onChange(String.format(java.util.Locale.US, "%02d:%02d", state.hour, state.minute))
                    open = false
                }) { Text(stringResource(R.string.profile_ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.profile_cancel)) } },
        )
    }
}

/** One choice out of a few, shown as pills (gender, category, milestone type, alarm). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ProfileChoice(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TwinTheme.colors.inkMuted, modifier = Modifier.padding(bottom = TwinTheme.spacing.xs))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            options.forEach { option ->
                com.twinhearts.app.core.design.TwinFilterChip(optionLabel(option), option == selected, { onSelect(option) })
            }
        }
    }
}

/** The bottom sheet every editor uses: title, close button, the fields, then a Save button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSheet(
    title: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean,
    saving: Boolean,
    saveLabel: String = stringResource(R.string.profile_save),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = TwinTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.card,
        contentColor = colors.ink,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = TwinTheme.spacing.screenEdge).padding(bottom = TwinTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = colors.ink, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.profile_close), tint = colors.inkMuted)
                }
            }
            content()
            GlassButton(saveLabel, onSave, Modifier.fillMaxWidth(), enabled = saveEnabled && !saving, loading = saving)
        }
    }
}
