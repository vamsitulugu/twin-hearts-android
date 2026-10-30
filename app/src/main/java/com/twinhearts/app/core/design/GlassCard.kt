package com.twinhearts.app.core.design

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The frosted "glass" card. Glass is faked with a translucent fill and a 1 dp border,
 * because real blur on scrolling content is what made the web app slow (R-55).
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = TwinTheme.spacing.l,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = TwinTheme.shapes.card
    val tap = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier = modifier
            .twinGlass(shape)
            .then(tap)
            .padding(contentPadding),
        content = content,
    )
}
