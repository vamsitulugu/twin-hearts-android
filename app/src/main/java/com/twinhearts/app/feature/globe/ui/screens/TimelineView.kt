package com.twinhearts.app.feature.globe.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.twinhearts.app.R
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.globe.domain.formatDate
import com.twinhearts.app.feature.globe.domain.model.MediaKind
import com.twinhearts.app.feature.globe.domain.model.Memory
import com.twinhearts.app.feature.globe.domain.preview

private const val NOTES_PREVIEW = 80

/** Every memory in the current filter, newest first, on a line of mood-coloured dots. */
@Composable
fun TimelineView(
    memories: List<Memory>,
    topPadding: Dp,
    bottomPadding: Dp,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sorted = remember(memories) { memories.sortedByDescending { it.dateFrom.orEmpty() } }
    if (sorted.isEmpty()) {
        Box(modifier = modifier.fillMaxSize().padding(top = topPadding, bottom = bottomPadding), contentAlignment = Alignment.Center) {
            EmptyState(title = stringResource(R.string.globe_no_memories), icon = Icons.Rounded.CalendarMonth)
        }
        return
    }
    val colors = TwinTheme.colors
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = TwinTheme.spacing.l, end = TwinTheme.spacing.l, top = topPadding, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        items(sorted, key = { it.id }) { memory ->
            val dot = moodColor(memory.moodEmoji)
            val photo = memory.mediaOf(MediaKind.Photo).firstOrNull()
            val line = colors.line
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(line, Offset(7.dp.toPx(), 0f), Offset(7.dp.toPx(), size.height + 12.dp.toPx()), strokeWidth = 2.dp.toPx())
                    },
                horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 18.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(dot),
                )
                GlassCard(onClick = { onOpen(memory.id) }, modifier = Modifier.weight(1f), contentPadding = TwinTheme.spacing.m) {
                    val date = formatDate(memory.dateFrom)
                    if (date.isNotEmpty()) Text(date, style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
                    Text(
                        text = memory.tripName.ifBlank { memory.city },
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.ink,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${memory.moodEmoji} ${listOf(memory.city, memory.country).filter { it.isNotBlank() }.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkMuted,
                    )
                    if (memory.notes.isNotBlank()) {
                        Text(
                            text = preview(memory.notes, NOTES_PREVIEW),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkMuted,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    if (photo != null) {
                        AsyncImage(
                            model = photo.url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .padding(top = TwinTheme.spacing.s)
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                                .clip(TwinTheme.shapes.small),
                        )
                    }
                }
            }
        }
    }
}

/** The dot colour of a mood, taken from the design tokens. */
@Composable
internal fun moodColor(emoji: String): Color {
    val c = TwinTheme.colors
    return when (emoji) {
        "😊" -> c.pinkSoft
        "🥰" -> c.accent
        "😌" -> c.success
        "😤" -> c.error
        "🤩" -> c.orchid
        else -> c.brand
    }
}
