package com.twinhearts.app.feature.twinai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.twinhearts.app.core.design.TwinTheme

/** Why this file exists: a multi-line glass text box (hint, grows up to [maxLines]) used by the dock and BOTH answers. */
@Composable
fun GlassTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    minHeight: Dp = TwinTheme.shapes.fieldHeight,
    maxLines: Int = 4,
    shape: Shape = TwinTheme.shapes.field,
    trailing: (@Composable () -> Unit)? = null,
    /** When set, the Enter key / keyboard Send button calls this instead of adding a new line (Shift+Enter still adds one). */
    onEnter: (() -> Unit)? = null,
) {
    val colors = TwinTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(shape)
            .background(colors.glass)
            .border(1.dp, colors.lineSoft, shape),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.ink),
            cursorBrush = SolidColor(colors.accent),
            maxLines = maxLines,
            keyboardOptions = KeyboardOptions(imeAction = if (onEnter != null) ImeAction.Send else ImeAction.Default),
            keyboardActions = KeyboardActions(onSend = { onEnter?.invoke() }),
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
                    if (onEnter != null && event.type == KeyEventType.KeyDown && event.key == Key.Enter && !event.isShiftPressed) {
                        onEnter()
                        true
                    } else {
                        false
                    }
                }
                .padding(start = TwinTheme.spacing.l, end = if (trailing != null) 52.dp else TwinTheme.spacing.l, top = TwinTheme.spacing.m, bottom = TwinTheme.spacing.m),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(hint, style = MaterialTheme.typography.bodyLarge, color = colors.inkFaint)
                }
                inner()
            },
        )
        if (trailing != null) {
            Box(Modifier.align(Alignment.CenterEnd)) { trailing() }
        }
    }
}
