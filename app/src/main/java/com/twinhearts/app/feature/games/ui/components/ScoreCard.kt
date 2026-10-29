package com.twinhearts.app.feature.games.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.twinhearts.app.core.design.TwinTheme

/** A player's name over their score. [active] outlines the player whose turn it is. */
@Composable
fun ScoreCard(label: String, score: Int, modifier: Modifier = Modifier, active: Boolean = false) {
    val colors = TwinTheme.colors
    Column(
        modifier = modifier
            .clip(TwinTheme.shapes.card)
            .background(colors.glass)
            .border(1.dp, if (active) colors.accent else colors.lineSoft, TwinTheme.shapes.card)
            .padding(TwinTheme.spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.inkMuted, textAlign = TextAlign.Center)
        Text("$score", style = MaterialTheme.typography.headlineSmall, color = colors.ink)
    }
}
