package com.twinhearts.app.feature.dailyroutine.ui.components

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.dailyroutine.domain.formatTime12
import com.twinhearts.app.feature.dailyroutine.domain.toMinutes

/** Why this file exists: form fields Daily Routine needs that core/design does not have (time picker, several-line text). */

/** Label above, glass box below; tapping opens the system time picker. [value] is "HH:mm" or empty. */
@Composable
fun RoutineTimeField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.field
    val minutes = toMinutes(value)
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.inkMuted, modifier = Modifier.padding(bottom = TwinTheme.spacing.xs))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TwinTheme.shapes.fieldHeight)
                .clip(shape)
                .background(colors.glass)
                .border(1.dp, colors.line, shape)
                .clickable(role = Role.Button) {
                    TimePickerDialog(
                        context,
                        { _, h, m -> onValueChange("%02d:%02d".format(h, m)) },
                        if (value.isBlank()) DEFAULT_HOUR else minutes / 60,
                        if (value.isBlank()) 0 else minutes % 60,
                        false,
                    ).show()
                }
                .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.m),
        ) {
            Text(
                text = if (value.isBlank()) stringResource(R.string.dr_pick_time) else formatTime12(value),
                style = MaterialTheme.typography.bodyLarge,
                color = if (value.isBlank()) colors.inkFaint else colors.ink,
            )
        }
    }
}

/** Several lines of text in the same glass look. */
@Composable
fun RoutineTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    minHeight: Dp = 80.dp,
) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.field
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.inkMuted, modifier = Modifier.padding(bottom = TwinTheme.spacing.xs))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.ink),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier.fillMaxWidth(),
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
                    if (value.isEmpty() && placeholder != null) {
                        Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.inkFaint)
                    }
                    inner()
                }
            },
        )
    }
}

private const val DEFAULT_HOUR = 9
