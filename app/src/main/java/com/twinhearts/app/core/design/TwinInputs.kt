package com.twinhearts.app.core.design

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R

/** Label above, glass field below (like the web forms). Shows [error] under the field in red. */
@Composable
fun TwinTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    isPassword: Boolean = false,
    enabled: Boolean = true,
    maxLength: Int? = null,
    leadingIcon: ImageVector? = null,
) {
    val colors = TwinTheme.colors
    val shapes = TwinTheme.shapes
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.inkMuted,
            modifier = Modifier.padding(bottom = TwinTheme.spacing.xs),
        )
        BasicTextField(
            value = value,
            onValueChange = { text -> if (maxLength == null || text.length <= maxLength) onValueChange(text) },
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.ink),
            cursorBrush = SolidColor(colors.accent),
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPassword && keyboardType == KeyboardType.Text) KeyboardType.Password else keyboardType,
                imeAction = imeAction,
            ),
            keyboardActions = if (onImeAction != null) KeyboardActions(onAny = { onImeAction() }) else KeyboardActions.Default,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { if (error != null) error(error) },
            decorationBox = { innerField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = shapes.fieldHeight)
                        .clip(shapes.field)
                        .background(colors.glass)
                        .border(1.dp, if (error != null) colors.error else colors.line, shapes.field)
                        .padding(start = TwinTheme.spacing.l, end = if (isPassword) TwinTheme.spacing.xs else TwinTheme.spacing.l),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (leadingIcon != null) {
                        Icon(leadingIcon, contentDescription = null, tint = colors.inkFaint, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(TwinTheme.spacing.s))
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.inkFaint)
                        }
                        innerField()
                    }
                    if (isPassword) {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = stringResource(
                                    if (passwordVisible) R.string.core_hide_password else R.string.core_show_password,
                                ),
                                tint = colors.inkFaint,
                            )
                        }
                    }
                }
            },
        )
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = colors.error,
                modifier = Modifier.padding(top = TwinTheme.spacing.xs),
            )
        }
    }
}

/** Pill-shaped tabs. The active tab is grey glass, as in the web app (R-52). */
@Composable
fun SegmentedTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    val pill = TwinTheme.shapes.pill
    // Same two-tone track as the Music / Movie tabs: frosted glass track, burgundy glass for the chosen tab.
    Row(modifier = modifier.fillMaxWidth().twinGlass(pill).padding(4.dp)) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .then(if (selected) Modifier.twinWine(pill) else Modifier.clip(pill))
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) Color.White else colors.inkMuted,
                )
            }
        }
    }
}

/** Small selectable pill, used for filters and quick chips. */
@Composable
fun TwinFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    val pill = TwinTheme.shapes.pill
    val fill: Brush = if (selected) colors.primaryBrush else SolidColor(colors.glass)
    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(pill)
            .background(fill)
            .border(1.dp, if (selected) Color.White.copy(alpha = 0.18f) else colors.line, pill)
            .selectable(selected = selected, role = Role.Button, onClick = onClick)
            .padding(horizontal = TwinTheme.spacing.l),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = colors.ink)
    }
}

/** On/off switch in the same pill style. */
@Composable
fun PillSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = TwinTheme.colors
    val pill = TwinTheme.shapes.pill
    val thumbOffset by animateDpAsState(if (checked) 22.dp else 0.dp, tween(150), label = "pillSwitchThumb")
    val fill: Brush = if (checked) colors.primaryBrush else SolidColor(colors.glass)
    Box(
        modifier = modifier
            .size(width = 52.dp, height = 30.dp)
            .clip(pill)
            .background(fill)
            .border(1.dp, colors.line, pill)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(3.dp),
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(24.dp)
                .clip(CircleShape)
                .background(colors.ink),
        )
    }
}
