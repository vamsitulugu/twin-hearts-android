package com.twinhearts.app.feature.us.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.SectionHeader
import com.twinhearts.app.core.design.StatTile
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.us.domain.model.LoveCounterState

/** Love Counter screen (web: lovecounter.html). Days-together ring, time grid, sections. */
@Composable
fun LoveCounterScreen(counter: LoveCounterState) {
    val colors = TwinTheme.colors
    Column(modifier = Modifier.fillMaxSize()) {
        SectionHeader(title = "Love Counter")

        // Hero: couple names + days counter
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = counter.anniversary?.takeIf { it.isNotBlank() }?.let { "Since $it" } ?: "Days together",
                style = MaterialTheme.typography.labelLarge,
                color = colors.inkMuted,
            )
            Text(
                text = counter.daysTogether.toString(),
                style = MaterialTheme.typography.displayLarge,
                color = colors.ink,
                modifier = Modifier
                    .graphicsLayer { scaleX = 1.4f; scaleY = 1.4f }
                    .padding(top = 4.dp, bottom = 4.dp),
            )
            Text(
                text = "days together",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.accent,
            )
        }

        // Time grid: years / months / days / hours / minutes / seconds
        SectionHeader(title = "Time Together")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TwinTheme.spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimeCounterCard(value = counter.years.toString(), label = "Years")
            TimeCounterCard(value = counter.months.toString(), label = "Months")
            TimeCounterCard(value = counter.daysTogether.toString(), label = "Days")
            TimeCounterCard(value = counter.remainingHours.toString(), label = "Hours")
            TimeCounterCard(value = counter.remainingMinutes.toString(), label = "Minutes")
            TimeCounterCard(value = counter.remainingSeconds.toString(), label = "Seconds")
        }

        // Live indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TwinTheme.spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(TwinTheme.shapes.circle)
                    .background(colors.success),
                contentAlignment = Alignment.Center,
            )
            Text(
                text = "Live",
                style = MaterialTheme.typography.labelLarge,
                color = colors.success,
            )
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(TwinTheme.spacing.l))

        // Section cards matching web lovecounter sections
        SectionHeader(title = "Milestones")
        GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = TwinTheme.spacing.l)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Day ${counter.daysTogether} together",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink,
                )
                Text(
                    text = "Celebrating every day with your partner.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted,
                )
            }
        }

        SectionHeader(title = "Anniversary")
        GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = TwinTheme.spacing.l)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = counter.anniversary?.takeIf { it.isNotBlank() } ?: "Not set yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink,
                )
                Text(
                    text = "Your anniversary date appears here once you set it in profile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted,
                )
            }
        }
    }
}

/** One time unit card: big number + small label. */
@Composable
private fun TimeCounterCard(value: String, label: String) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.card
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(shape)
            .background(colors.glass)
            .border(1.dp, colors.line, shape)
            .padding(TwinTheme.spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge,
            color = colors.ink,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.inkMuted,
        )
    }
}

// ┌─────────────────────────────────────────────────────────────────┐
// │ Previews (R-57: every screen has previews for each state)      │
// └─────────────────────────────────────────────────────────────────┘

@Preview(showBackground = true, name = "Love Counter – with data")
@Composable
private fun LoveCounterPreviewWithData() {
    TwinTheme {
        LoveCounterScreen(
            counter = LoveCounterState(
                anniversary = "2024-01-15",
                startedAt = "2024-01-15",
                daysTogether = 312,
                years = 0,
                months = 10,
                remainingDays = 53,
                remainingHours = 1272,
                remainingMinutes = 76320,
                remainingSeconds = 4579200,
            ),
        )
    }
}

@Preview(showBackground = true, name = "Love Counter – no anniversary")
@Composable
private fun LoveCounterPreviewNoAnniversary() {
    TwinTheme {
        LoveCounterScreen(
            counter = LoveCounterState(
                anniversary = null,
                startedAt = null,
                daysTogether = 0,
                years = 0,
                months = 0,
                remainingDays = 0,
                remainingHours = 0,
                remainingMinutes = 0,
                remainingSeconds = 0,
            ),
        )
    }
}
