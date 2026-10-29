package com.twinhearts.app.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Deep wine screen background with a soft burgundy glow at the top.
 * It is a cheap gradient, not a blur, so it never costs scroll performance (R-55).
 */
@Composable
fun TwinBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = TwinTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(
                        0f to colors.burgundy.copy(alpha = 0.92f),
                        0.30f to colors.backgroundGlow.copy(alpha = 0.55f),
                        0.62f to Color.Transparent,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(colors.backgroundGlow, Color.Transparent),
                        center = Offset(size.width / 2f, 0f),
                        radius = size.maxDimension * 0.85f,
                    ),
                )
            },
        content = content,
    )
}
