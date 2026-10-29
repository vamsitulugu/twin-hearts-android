package com.twinhearts.app.feature.games.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.twinhearts.app.core.design.TwinTheme

/** The banner that says whose turn it is. It glows burgundy when it is mine. */
@Composable
fun TurnBar(text: String, mine: Boolean, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.pill
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = colors.ink,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (mine) colors.primaryBrush else androidx.compose.ui.graphics.SolidColor(colors.glass))
            .border(1.dp, if (mine) colors.accent else colors.lineSoft, shape)
            .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.m),
    )
}
