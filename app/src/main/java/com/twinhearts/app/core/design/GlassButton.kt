package com.twinhearts.app.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

enum class GlassButtonStyle { Primary, Secondary, Ghost }

/** Pill button: burgundy gradient (Primary), grey glass (Secondary) or outline only (Ghost). */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GlassButtonStyle = GlassButtonStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    val colors = TwinTheme.colors
    val shapes = TwinTheme.shapes
    val base = modifier
        .heightIn(min = shapes.buttonHeight)
        .alpha(if (enabled) 1f else 0.5f)
        .clip(shapes.pill)
    val styled = when (style) {
        GlassButtonStyle.Primary -> base
            .background(colors.primaryBrush)
            .border(1.dp, Color.White.copy(alpha = 0.18f), shapes.pill)
        GlassButtonStyle.Secondary -> base
            .background(colors.neutralGlass.copy(alpha = 0.35f))
            .border(1.dp, colors.lineSoft, shapes.pill)
        GlassButtonStyle.Ghost -> base.border(1.dp, colors.line, shapes.pill)
    }
    Row(
        modifier = styled
            .clickable(enabled = enabled && !loading, role = Role.Button, onClick = onClick)
            .padding(horizontal = TwinTheme.spacing.xl),
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = colors.ink,
            )
        } else {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = colors.ink, modifier = Modifier.size(20.dp))
            }
            Text(text = text, style = MaterialTheme.typography.titleMedium, color = colors.ink)
        }
    }
}

/** Round glass icon button. Always give it a contentDescription for screen readers. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    val shapes = TwinTheme.shapes
    Box(
        modifier = modifier
            .size(shapes.minTouch)
            .clip(shapes.circle)
            .background(colors.glass)
            .border(1.dp, colors.lineSoft, shapes.circle)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = colors.ink, modifier = Modifier.size(22.dp))
    }
}
