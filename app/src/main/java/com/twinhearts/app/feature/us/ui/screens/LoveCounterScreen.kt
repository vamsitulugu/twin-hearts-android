package com.twinhearts.app.feature.us.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.SectionHeader
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.us.domain.model.LoveCounterState

/** Love Counter screen (web: lovecounter.html). Days-together hero, 3 x 2 time grid, sections. */
@Composable
fun LoveCounterScreen(counter: LoveCounterState) {
    val colors = TwinTheme.colors
    val edge = TwinTheme.spacing.l
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
    ) {
        SectionHeader(title = "Love Counter", modifier = Modifier.padding(horizontal = edge, vertical = TwinTheme.spacing.s))

        // Hero: label, big number, unit
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = edge, vertical = TwinTheme.spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = counter.anniversary?.takeIf { it.isNotBlank() }?.let { "Since $it" } ?: "Days together",
                style = MaterialTheme.typography.labelLarge,
                color = colors.inkMuted,
            )
            Text(
                text = counter.daysTogether.toString(),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp, lineHeight = 80.sp),
                color = colors.ink,
                maxLines = 1,
                modifier = Modifier.padding(vertical = 4.dp),
            )
            Text(
                text = "days together",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.accent,
            )
        }

        Spacer(Modifier.height(TwinTheme.spacing.m))
        SectionHeader(title = "Time Together", modifier = Modifier.padding(horizontal = edge, vertical = TwinTheme.spacing.s))

        // Time grid: 3 columns x 2 rows, every card the same size, labels on one line.
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = edge),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                TimeCounterCard(Modifier.weight(1f), counter.years.toString(), "Years")
                TimeCounterCard(Modifier.weight(1f), counter.months.toString(), "Months")
                TimeCounterCard(Modifier.weight(1f), counter.daysTogether.toString(), "Days")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                TimeCounterCard(Modifier.weight(1f), counter.remainingHours.toString(), "Hours")
                TimeCounterCard(Modifier.weight(1f), counter.remainingMinutes.toString(), "Minutes")
                TimeCounterCard(Modifier.weight(1f), counter.remainingSeconds.toString(), "Seconds")
            }
        }

        // Live indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = edge, vertical = TwinTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(8.dp).clip(TwinTheme.shapes.circle).background(colors.success))
            Text(text = "Live", style = MaterialTheme.typography.labelLarge, color = colors.success)
        }

        Spacer(Modifier.height(TwinTheme.spacing.m))
        SectionHeader(title = "Milestones", modifier = Modifier.padding(horizontal = edge, vertical = TwinTheme.spacing.s))
        InfoCard(
            title = "Day ${counter.daysTogether} together",
            body = "Celebrating every day with your partner.",
            modifier = Modifier.fillMaxWidth().padding(horizontal = edge),
        )

        Spacer(Modifier.height(TwinTheme.spacing.m))
        SectionHeader(title = "Anniversary", modifier = Modifier.padding(horizontal = edge, vertical = TwinTheme.spacing.s))
        InfoCard(
            title = counter.anniversary?.takeIf { it.isNotBlank() } ?: "Not set yet",
            body = "Your anniversary date appears here once you set it in profile.",
            modifier = Modifier.fillMaxWidth().padding(horizontal = edge),
        )
    }
}

/** A card with a title line and a small grey line under it. */
@Composable
private fun InfoCard(title: String, body: String, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    GlassCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = colors.ink)
            Text(text = body, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
        }
    }
}

/**
 * One time unit card: big number over a small label. Fixed height so all six cards line up, the
 * label never wraps, and a long number (the seconds) shrinks instead of overflowing.
 */
@Composable
private fun TimeCounterCard(modifier: Modifier, value: String, label: String) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.card
    val numberSize = when {
        value.length <= 3 -> 28.sp
        value.length <= 5 -> 24.sp
        value.length <= 7 -> 19.sp
        else -> 15.sp
    }
    Column(
        modifier = modifier
            .height(88.dp)
            .clip(shape)
            .background(colors.glass)
            .border(1.dp, colors.line, shape)
            .padding(horizontal = 6.dp, vertical = TwinTheme.spacing.s),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = numberSize, lineHeight = numberSize * 1.2f),
            color = colors.ink,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.sp),
            color = colors.inkMuted,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
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
