package com.twinhearts.app.feature.fights.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.twinhearts.app.core.design.TwinTheme

/** Why this file exists: small pieces every Fight Log screen reuses (chips, tiles, text box). */

/** A rounded-square icon tile in a category's colour, as in the reference rows. */
@Composable
fun ToneTile(icon: ImageVector, tone: Color, modifier: Modifier = Modifier, size: Int = 44) {
    Box(
        modifier
            .size(size.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(tone.copy(alpha = 0.16f))
            .border(1.dp, tone.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tone, modifier = Modifier.size((size * 0.5f).dp))
    }
}

/** A choice chip. Chosen chips get the burgundy fill and an accent border. */
@Composable
fun FightChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.pill
    Row(
        modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(if (selected) colors.brand.copy(alpha = 0.45f) else colors.glass)
            .border(1.dp, if (selected) colors.accent else colors.lineSoft, shape)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (selected) colors.ink else colors.inkMuted, modifier = Modifier.size(18.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) colors.ink else colors.inkMuted,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** A glass text box. [maxLines] 1 gives a single line; [maxLength] limits what can be typed. */
@Composable
fun FightTextBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    minHeight: Int = 52,
    maxLines: Int = 1,
    maxLength: Int? = null,
    isError: Boolean = false,
) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.field
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = minHeight.dp)
            .clip(shape)
            .background(colors.glass)
            .border(1.dp, if (isError) colors.error else colors.lineSoft, shape),
        contentAlignment = Alignment.TopStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = { text -> onValueChange(if (maxLength != null) text.take(maxLength) else text) },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.ink),
            cursorBrush = SolidColor(colors.accent),
            maxLines = maxLines,
            singleLine = maxLines == 1,
            modifier = Modifier.fillMaxWidth().padding(TwinTheme.spacing.l),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.inkFaint)
                inner()
            },
        )
    }
}

/** The small coloured pill that says Open (red) or Resolved (green). */
@Composable
fun StatePill(open: Boolean, label: String, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val tone = if (open) colors.error else colors.success
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = tone,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(TwinTheme.shapes.pill)
            .background(tone.copy(alpha = 0.16f))
            .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.xs),
    )
}

/** Bold section heading used above groups of choices. */
@Composable
fun FightQuestion(text: String, hint: String? = null, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), verticalAlignment = Alignment.Bottom) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = colors.ink, fontWeight = FontWeight.Bold)
        if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = colors.inkFaint)
    }
}
