@file:OptIn(ExperimentalLayoutApi::class)

package com.twinhearts.app.feature.globe.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.SerifFamily
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme

/*
 * Why this file exists: small building blocks only the Globe screens use (R-52 allows private
 * pieces when the core set has none). They are made from the same tokens as the core components.
 */

/**
 * The sheet that slides over the globe: 82% of the height (half for trip details over the map) at the bottom on phones, a 400 dp
 * side panel on wide screens (the web app does the same above 700 px).
 */
@Composable
fun BoxScope.SheetFrame(
    wide: Boolean,
    title: String,
    onClose: () -> Unit,
    heightFraction: Float = SHEET_FULL_FRACTION,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = TwinTheme.colors
    // Half-height sheets (trip details over the map) grow to almost full height when the handle is tapped.
    var expanded by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val fraction by androidx.compose.animation.core.animateFloatAsState(
        if (expanded) SHEET_EXPANDED_FRACTION else heightFraction,
        label = "sheet-height",
    )
    val shape = if (wide) RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp) else RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    Column(
        modifier = Modifier
            .align(if (wide) Alignment.CenterEnd else Alignment.BottomCenter)
            .then(if (wide) Modifier.fillMaxHeight().width(400.dp) else Modifier.fillMaxWidth().fillMaxHeight(fraction))
            .clip(shape)
            .background(colors.card.copy(alpha = 0.97f))
            .border(1.dp, colors.line, shape)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding(),
    ) {
        if (!wide) {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(CircleShape)
                    .clickable { expanded = !expanded }
                    .padding(vertical = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(colors.line, CircleShape),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = TwinTheme.spacing.l, end = TwinTheme.spacing.s, top = TwinTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = SerifFamily,
                color = colors.ink,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(TwinTheme.shapes.minTouch)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.globe_close), tint = colors.inkMuted)
            }
        }
        content()
    }
}

/** A compact pill for card actions ("View", "Edit", "Delete"). The core buttons are 52 dp tall, too big here. */
@Composable
fun SmallPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    primary: Boolean = false,
) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.pill
    val background = when {
        primary -> colors.primaryBrush
        else -> SolidColor(colors.neutralGlass)
    }
    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(background)
            .border(BorderStroke(1.dp, if (danger) colors.error.copy(alpha = 0.5f) else colors.lineSoft), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = TwinTheme.spacing.l),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (danger) colors.error else colors.ink,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** A coloured little label such as "₹5,000" or "☀️ Sunny". */
@Composable
fun Tag(text: String, tint: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = tint,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(TwinTheme.shapes.pill)
            .background(tint.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Multi-line text field in the same style as [TwinTextField] (that one is single-line only). */
@Composable
fun GlobeTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    hint: String,
    modifier: Modifier = Modifier,
    minLines: Int = 3,
) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.field
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
            minLines = minLines,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.ink),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(colors.glass)
                        .border(1.dp, colors.line, shape)
                        .padding(horizontal = TwinTheme.spacing.l, vertical = 14.dp),
                ) {
                    if (value.isEmpty()) {
                        Text(hint, style = MaterialTheme.typography.bodyLarge, color = colors.inkFaint)
                    }
                    inner()
                }
            },
        )
    }
}

/** "Add restaurant name..." style input: type, tap Add, and the entry becomes a removable chip. */
@Composable
fun ChipListInput(
    label: String,
    hint: String,
    items: List<String>,
    draft: String,
    onDraftChange: (String) -> Unit,
    onChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    fun commit() {
        val clean = draft.trim()
        if (clean.isNotEmpty()) {
            onChange(items + clean)
            onDraftChange("")
        }
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            TwinTextField(
                value = draft,
                onValueChange = onDraftChange,
                label = label,
                placeholder = hint,
                modifier = Modifier.weight(1f),
                onImeAction = ::commit,
            )
            SmallPill(text = stringResource(R.string.globe_form_add_chip), onClick = ::commit, modifier = Modifier.padding(bottom = 7.dp))
        }
        if (items.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .clip(TwinTheme.shapes.pill)
                            .background(colors.glass)
                            .border(1.dp, colors.line, TwinTheme.shapes.pill)
                            .clickable(role = Role.Button) { onChange(items.filterIndexed { i, _ -> i != index }) }
                            .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(item, style = MaterialTheme.typography.labelLarge, color = colors.ink)
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.globe_form_remove_chip),
                            tint = colors.inkMuted,
                            modifier = Modifier.padding(start = 4.dp).size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Read-only chips for restaurants, hotels and gifts. */
@Composable
fun ReadOnlyChips(items: List<String>, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { item ->
            Text(
                text = item,
                style = MaterialTheme.typography.labelLarge,
                color = colors.ink,
                modifier = Modifier
                    .clip(TwinTheme.shapes.pill)
                    .background(colors.glass)
                    .border(1.dp, colors.line, TwinTheme.shapes.pill)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

/** A small heading above a block of a sheet. */
@Composable
fun SheetLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = TwinTheme.colors.inkMuted,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier,
    )
}

/** Sheet heights on phones: normal, half (trip details over the map), and expanded by tapping the handle. */
const val SHEET_FULL_FRACTION = 0.82f
const val SHEET_HALF_FRACTION = 0.5f
const val SHEET_EXPANDED_FRACTION = 0.9f
