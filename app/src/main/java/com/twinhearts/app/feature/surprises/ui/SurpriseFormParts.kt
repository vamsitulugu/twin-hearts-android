package com.twinhearts.app.feature.surprises.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.surprises.domain.model.PickedMedia
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Shared bits of the New Surprise sheet and the Love Capsule form. */

/** Same look as `TwinTextField`, but several lines high (the shared one is single line only). */
@Composable
fun SurpriseTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    minHeight: Int = 96,
) {
    val colors = TwinTheme.colors
    val shapes = TwinTheme.shapes
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.inkMuted,
            modifier = Modifier.padding(bottom = TwinTheme.spacing.xs),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.ink),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = minHeight.dp)
                        .clip(shapes.field)
                        .background(colors.glass)
                        .border(1.dp, colors.line, shapes.field)
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
}

/** A tappable field that shows the chosen date, or a hint when none is chosen. */
@Composable
fun SurpriseDateField(label: String, date: LocalDate?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val shapes = TwinTheme.shapes
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.inkMuted,
            modifier = Modifier.padding(bottom = TwinTheme.spacing.xs),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = shapes.fieldHeight)
                .clip(shapes.field)
                .background(colors.glass)
                .border(1.dp, colors.line, shapes.field)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = TwinTheme.spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = colors.inkFaint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(TwinTheme.spacing.s))
            Text(
                text = if (date != null) formatDate(date) else stringResource(R.string.surprises_form_date_none),
                style = MaterialTheme.typography.bodyLarge,
                color = if (date != null) colors.ink else colors.inkFaint,
            )
        }
    }
}

fun formatDate(date: LocalDate): String = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

/** Picks a date from [minDate] on. A surprise for a past day would open at once, which defeats the point. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurpriseDatePicker(
    initial: LocalDate?,
    minDate: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    // The date picker works in UTC midnight milliseconds.
    val minMillis = minDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val start = (initial ?: minDate.plusDays(1)).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = start,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= minMillis
        },
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
            ) { Text(stringResource(R.string.surprises_picker_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.core_cancel)) } },
    ) {
        DatePicker(state = state)
    }
}

/** Reads name, type and size of a file the person picked. Null when the phone will not say what it is. */
fun readPickedMedia(context: Context, uri: Uri): PickedMedia? {
    val resolver = context.contentResolver
    var name = ""
    var size = -1L
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameColumn >= 0 && !cursor.isNull(nameColumn)) name = cursor.getString(nameColumn)
            val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) size = cursor.getLong(sizeColumn)
        }
    }
    val mime = resolver.getType(uri)
        ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(name).lowercase())
        ?: return null
    return PickedMedia(uri = uri.toString(), name = name, mime = mime, sizeBytes = size)
}
