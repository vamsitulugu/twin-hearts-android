package com.twinhearts.app.feature.us.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.SectionHeader
import com.twinhearts.app.core.design.StatTile
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.us.domain.model.DreamGoal

/**
 * Dream Goals screen (web: dreamgoals.html). One scrolling page: stats, insight, progress cards,
 * then the goal cards in two even columns.
 */
@Composable
fun DreamGoalsScreen(
    goals: List<DreamGoal>,
    onDreamGoalAdded: () -> Unit,
) {
    val colors = TwinTheme.colors
    val edge = TwinTheme.spacing.l
    val gap = TwinTheme.spacing.s
    val totalProgress = if (goals.isEmpty()) 0 else goals.map { it.progress }.average().toInt()
    val completed = goals.filter { it.completed }
    val active = goals.filter { !it.completed }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = edge, end = edge, top = TwinTheme.spacing.s, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        item { SectionHeader(title = "Dream Goals") }

        // Hero stats: 2 x 2, every tile the same size, labels on one line.
        item {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    DreamStat(goals.size.toString(), "Goals", Modifier.weight(1f))
                    DreamStat(completed.size.toString(), "Completed", Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    DreamStat(goals.count { it.priority == "high" }.toString(), "High priority", Modifier.weight(1f))
                    DreamStat("$totalProgress%", "Progress", Modifier.weight(1f))
                }
            }
        }

        // Insight card (web: ga-insight-card)
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Psychology, contentDescription = null, tint = colors.accent, modifier = Modifier.size(28.dp))
                    Column(modifier = Modifier.weight(1f).padding(start = TwinTheme.spacing.m)) {
                        Text(
                            text = "You've dreamed ${goals.size} times together",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.ink,
                        )
                        Text(
                            text = "Keep adding goals — big or small.",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.inkMuted,
                        )
                    }
                }
            }
        }

        // Category cards with progress bars (web: ga-cat-card)
        if (active.isNotEmpty()) item { CategoryProgressCard(title = "In Progress", items = active, accent = colors.accent) }
        if (completed.isNotEmpty()) item { CategoryProgressCard(title = "Completed", items = completed, accent = colors.success) }

        item { Spacer(Modifier.height(TwinTheme.spacing.xs)) }
        item { SectionHeader(title = "All Goals") }

        if (goals.isEmpty()) {
            item {
                Text(
                    text = "No dream goals yet. Add your first goal below.",
                    color = colors.inkMuted,
                    modifier = Modifier.fillMaxWidth().padding(vertical = TwinTheme.spacing.l),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            // Two even columns: each row is as tall as its taller card.
            items(goals.chunked(2)) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    pair.forEach { goal -> DreamCard(goal = goal, modifier = Modifier.weight(1f).fillMaxHeight()) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** Stat tile with a one-line label (the shared StatTile lets long labels wrap and misalign). */
@Composable
private fun DreamStat(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = colors.ink, maxLines = 1)
            Text(label, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 1, softWrap = false)
        }
    }
}

/** Category card: title + progress bar + item list preview. */
@Composable
private fun CategoryProgressCard(title: String, items: List<DreamGoal>, accent: Color) {
    val colors = TwinTheme.colors
    val totalProgress = items.map { it.progress }.average().toFloat()
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
            )
            Text(
                text = "${items.size} ${if (items.size == 1) "goal" else "goals"}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.inkMuted,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = TwinTheme.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Progress",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.inkMuted,
                    modifier = Modifier.width(48.dp),
                )
                ProgressBar(
                    fraction = totalProgress / 100f,
                    fill = SolidColor(accent),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${totalProgress.toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.inkMuted,
                    modifier = Modifier.width(32.dp),
                )
            }
        }
    }
}

/** Track + fill. Uses a fraction of the width, so 0% is fine (Modifier.weight() would crash at 0). */
@Composable
private fun ProgressBar(fraction: Float, fill: Brush, modifier: Modifier = Modifier) {
    val shape = TwinTheme.shapes.small
    Box(
        modifier = modifier
            .height(6.dp)
            .clip(shape)
            .background(TwinTheme.colors.lineSoft),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(fill),
        )
    }
}

/** Single dream card for the two-column grid: emoji + title, status, description, progress, actions. */
@Composable
private fun DreamCard(goal: DreamGoal, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    GlassCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
                Text(text = goal.emoji, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = goal.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (goal.completed) "Done" else "Open",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (goal.completed) colors.success else colors.accent,
                )
                if (goal.targetDate != null) {
                    Text(
                        text = "Target: ${goal.targetDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.inkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (goal.description != null) {
                    Text(
                        text = goal.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkMuted,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(modifier = Modifier.padding(top = TwinTheme.spacing.s)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressBar(
                        fraction = goal.progress / 100f,
                        fill = colors.primaryBrush,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${goal.progress}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.inkMuted,
                        modifier = Modifier.padding(start = TwinTheme.spacing.s),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = TwinTheme.spacing.s),
                    horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
                ) {
                    if (!goal.completed) CardAction("Done", colors.success)
                    CardAction("Edit", colors.accent)
                    CardAction("Delete", colors.error)
                }
            }
        }
    }
}

/** Small text action inside a goal card (keeps three actions on one line in a half-width card). */
@Composable
private fun CardAction(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1,
        modifier = Modifier.clickable(role = Role.Button, onClick = {}).padding(vertical = 4.dp),
    )
}

// ┌─────────────────────────────────────────────────────────────────┐
// │ Previews (R-57)                                                │
// └─────────────────────────────────────────────────────────────────┘

@Preview(showBackground = true, name = "Dream Goals – with goals")
@Composable
private fun DreamGoalsPreviewWithData() {
    TwinTheme {
        DreamGoalsScreen(
            goals = listOf(
                DreamGoal(
                    id = "1",
                    title = "Visit Paris together",
                    emoji = "\uD83C\uDF48",
                    description = "See the Eiffel Tower at sunset",
                    targetDate = "2027-06-01",
                    priority = "high",
                    progress = 30,
                    completed = false,
                    createdBy = "user1",
                ),
                DreamGoal(
                    id = "2",
                    title = "Learn to cook together",
                    emoji = "\uD83C\uDF73",
                    description = "Master 5 signature dishes",
                    targetDate = "2026-12-25",
                    priority = "medium",
                    progress = 75,
                    completed = false,
                    createdBy = "user2",
                ),
                DreamGoal(
                    id = "3",
                    title = "Buy a home",
                    emoji = "\uD83D\uDCE0",
                    description = "Our first place together",
                    targetDate = "2028-01-01",
                    priority = "high",
                    progress = 100,
                    completed = true,
                    createdBy = "user1",
                ),
            ),
            onDreamGoalAdded = {},
        )
    }
}

@Preview(showBackground = true, name = "Dream Goals – empty")
@Composable
private fun DreamGoalsPreviewEmpty() {
    TwinTheme {
        DreamGoalsScreen(goals = emptyList(), onDreamGoalAdded = {})
    }
}
