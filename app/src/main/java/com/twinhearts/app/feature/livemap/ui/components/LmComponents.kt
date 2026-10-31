package com.twinhearts.app.feature.livemap.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import com.twinhearts.app.core.design.TwinLook
import com.twinhearts.app.core.design.twinGlassDark
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.PillSwitch
import com.twinhearts.app.core.design.TwinTheme

/** A row of a settings list: icon, title, optional subtitle, and a switch or an arrow on the right. */
@Composable
fun LmToggleRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    LmRow(icon, title, modifier, subtitle, onClick = { if (enabled) onChange(!checked) }) {
        PillSwitch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
fun LmRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    tint: Color = TwinTheme.colors.accent,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = TwinTheme.colors
    val tap = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Row(
        modifier = modifier.fillMaxWidth().then(tap).padding(vertical = TwinTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        Box(Modifier.size(40.dp).background(colors.neutralGlass, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
        trailing?.invoke()
    }
}

/** A small round icon button over the map (locate me, map type ...). */
@Composable
fun LmMapFab(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    val colors = TwinTheme.colors
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (active) Brush.linearGradient(listOf(colors.accent, colors.accent)) else TwinLook.DarkGlassFill)
            .border(1.dp, if (active) colors.accent else Color.White.copy(alpha = 0.18f), CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (active) Color.White else Color.White.copy(alpha = 0.92f), modifier = Modifier.size(22.dp))
    }
}

/** A quick action tile (Locate Me, Meeting Point ...). */
@Composable
fun LmTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier, onClick = onClick, contentPadding = TwinTheme.spacing.m) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
            Icon(icon, contentDescription = null, tint = TwinTheme.colors.accent, modifier = Modifier.size(24.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = TwinTheme.colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** A coloured banner (emergency, offline, permission). */
@Composable
fun LmBanner(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TwinTheme.colors.error,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .twinGlassDark(TwinTheme.shapes.small)
            .background(color.copy(alpha = 0.22f))
            .padding(TwinTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TwinTheme.colors.ink, modifier = Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = TwinTheme.colors.accent,
                modifier = Modifier.clickable(role = Role.Button, onClick = onAction).padding(TwinTheme.spacing.s),
            )
        }
    }
}

@Composable
fun LmDot(online: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.size(10.dp).background(if (online) TwinTheme.colors.success else TwinTheme.colors.inkFaint, CircleShape))
}
