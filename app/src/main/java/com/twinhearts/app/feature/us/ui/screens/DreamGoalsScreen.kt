package com.twinhearts.app.feature.us.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

/** Dream Goals screen (web: dreamgoals.html). Hero stats, goal cards grid, add FAB. */
@Composable
fun DreamGoalsScreen(
    goals: List<DreamGoal>,
    onDreamGoalAdded: () -> Unit,
) {
    val colors = TwinTheme.colors
    Column(modifier = Modifier.fillMaxSize()) {
        SectionHeader(title = "Dream Goals", modifier = Modifier.padding(horizontal = TwinTheme.spacing.l))

        // Hero stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TwinTheme.spacing.l),
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatTile(value = goals.size.toString(), label = "Goals", modifier = Modifier.weight(1f))
            StatTile(value = goals.count { it.completed }.toString(), label = "Completed", modifier = Modifier.weight(1f))
            StatTile(value = goals.count { it.priority == "high" }.toString(), label = "High Priority", modifier = Modifier.weight(1f))
            val totalProgress = if (goals.isEmpty()) 0 else goals.map { it.progress }.average().toInt()
            StatTile(value = "$totalProgress%", label = "Progress", modifier = Modifier.weight(1f))
        }

        // Insight card (web: ga-insight-card)
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = TwinTheme.spacing.l),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(32.dp).clip(TwinTheme.shapes.circle),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Psychology, contentDescription = null, tint = colors.accent)
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = TwinTheme.spacing.m)) {
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

        // Category cards with progress bars (web: ga-cat-card)
        val completed = goals.filter { it.completed }
        val active = goals.filter { !it.completed }
        if (active.isNotEmpty()) {
            CategoryProgressCard(title = "In Progress", items = active, accent = colors.accent)
        }
        if (completed.isNotEmpty()) {
            CategoryProgressCard(title = "Completed", items = completed, accent = colors.success)
        }

        // Dream cards grid (web: dream-card)
        SectionHeader(title = "All Goals", modifier = Modifier.padding(horizontal = TwinTheme.spacing.l))
        if (goals.isEmpty()) {
            Text(
                text = "No dream goals yet. Add your first goal below.",
                color = colors.inkMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TwinTheme.spacing.l),
                textAlign = TextAlign.Center,
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TwinTheme.spacing.l),
            ) {
                items(goals) { goal ->
                    DreamCard(goal = goal)
                }
            }
        }
    }
}

/** Category card: title + progress bar + item list preview. */
@Composable
private fun CategoryProgressCard(title: String, items: List<DreamGoal>, accent: Color) {
    val colors = TwinTheme.colors
    val totalProgress = items.map { it.progress }.average().toFloat()
    GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.s)) {
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

/** Single dream card: emoji, title, badges, description, target, progress, actions. */
@Composable
private fun DreamCard(goal: DreamGoal) {
    val colors = TwinTheme.colors
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = TwinTheme.spacing.s),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top: emoji + title
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(36.dp).clip(TwinTheme.shapes.small),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = goal.emoji,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = TwinTheme.spacing.s)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.ink,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = TwinTheme.spacing.xs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        if (goal.targetDate != null) {
                            Text(
                                text = "Target: ${goal.targetDate}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.inkMuted,
                            )
                        }
                        Text(
                            text = if (goal.completed) "Done" else "Open",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (goal.completed) colors.success else colors.accent,
                        )
                    }
                }
            }

            // Description
            if (goal.description != null) {
                Text(
                    text = goal.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = TwinTheme.spacing.s),
                )
            }

            // Progress bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = TwinTheme.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Progress",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.inkMuted,
                    modifier = Modifier.width(48.dp),
                )
                ProgressBar(
                    fraction = goal.progress / 100f,
                    fill = colors.primaryBrush,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${goal.progress}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.inkMuted,
                    modifier = Modifier.width(32.dp),
                )
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (!goal.completed) {
                    TextButton(
                        text = "Complete",
                        onClick = {},
                        color = colors.success,
                    )
                }
                TextButton(text = "Edit", onClick = {}, color = colors.accent)
                TextButton(text = "Delete", onClick = {}, color = colors.error)
            }
        }
    }
}

/** Simple text button. */
@Composable
private fun TextButton(text: String, onClick: () -> Unit, color: Color) {
    androidx.compose.material3.TextButton(onClick = onClick) {
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = color)
    }
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
