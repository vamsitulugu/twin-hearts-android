package com.twinhearts.app.feature.twinai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.twinhearts.app.core.design.TwinTheme

/**
 * Why this file exists: the web Twin page paints its buttons in one solid wine colour (#6B093A),
 * while the shared GlassButton uses the brighter pink gradient. Twin buttons use this one so the
 * page matches the web (R-31: built inside the feature instead of editing core/).
 */
@Composable
fun TwinBrandButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    trailingIcon: ImageVector? = null,
) {
    val colors = TwinTheme.colors
    Row(
        modifier = modifier
            .heightIn(min = TwinTheme.shapes.minTouch)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(TwinTheme.shapes.pill)
            .background(colors.brand)
            .clickable(enabled = enabled && !loading, role = Role.Button, onClick = onClick)
            .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.s),
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.ink)
        } else {
            Text(text, style = MaterialTheme.typography.titleSmall, color = colors.ink, fontWeight = FontWeight.Bold)
            if (trailingIcon != null) Icon(trailingIcon, null, tint = colors.ink, modifier = Modifier.size(20.dp))
        }
    }
}
