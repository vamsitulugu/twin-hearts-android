package com.twinhearts.app.feature.us.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
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
import com.twinhearts.app.feature.us.domain.model.CollectionItem

/** Collection screen (web: collection.html). Tabs, hero stats, shared memory list. */
@Composable
fun CollectionScreen(items: List<CollectionItem>) {
    val colors = TwinTheme.colors
    Column(modifier = Modifier.fillMaxSize()) {
        SectionHeader(title = "Collection")

        // Hero stats: 4 stat tiles
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TwinTheme.spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatTile(value = items.size.toString(), label = "Shared Items")
            StatTile(value = "2", label = "Albums")
            StatTile(value = "0", label = "Downloads")
            StatTile(value = "0", label = "Favourites")
        }

        // Toolbar: search + filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TwinTheme.spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.TextField(
                value = "",
                onValueChange = {},
                placeholder = { Text(text = "Search memories", color = colors.inkMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                colors = androidx.compose.material3.TextFieldDefaults.textFieldColors(
                    backgroundColor = colors.glass,
                    focusedBackgroundColor = colors.glass,
                    unfocusedBackgroundColor = colors.glass,
                    disabledBackgroundColor = colors.glass,
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(TwinTheme.shapes.fieldHeight)
                    .clip(TwinTheme.shapes.field)
                    .border(1.dp, colors.line, TwinTheme.shapes.field),
            )
        }

        // Voice / media list
        SectionHeader(title = "Memories", actionLabel = "See all", onAction = {})
        if (items.isEmpty()) {
            androidx.compose.material3.Text(
                text = "No shared memories yet. Start by sharing a photo or voice note.",
                color = colors.inkMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TwinTheme.spacing.l),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TwinTheme.spacing.l),
            ) {
                items.forEach { item ->
                    CollectionItemRow(item = item)
                }
            }
        }
    }
}

/** One item in the collection list: avatar, play button, sender, date, actions. */
@Composable
private fun CollectionItemRow(item: CollectionItem) {
    val colors = TwinTheme.colors
    val isMine = item.sender == "You"
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = TwinTheme.spacing.xs),
        onClick = {},
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).clip(TwinTheme.shapes.circle),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item.sender.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.ink,
                    modifier = Modifier
                        .background(if (isMine) colors.primaryBrush else colors.accent)
                        .size(36.dp)
                        .clip(TwinTheme.shapes.circle),
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = TwinTheme.spacing.m)) {
                Text(
                    text = item.sender,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.ink,
                )
                Text(
                    text = formatTime(item.sentAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.inkMuted,
                )
            }
            IconButtonRow(type = item.type)
        }
    }
}

/** Play / favourite / delete buttons on the right of each item. */
@Composable
private fun IconButtonRow(type: String) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier.padding(horizontal = TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (type == "voice" || type == "video") {
            IconButton(
                onClick = {},
                modifier = Modifier.size(34.dp),
            ) {
                Icon(Icons.Default.PlayCircleFilled, contentDescription = "Play", tint = colors.accent)
            }
        }
        IconButton(onClick = {}, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorite", tint = colors.inkMuted)
        }
        IconButton(onClick = {}, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = colors.inkMuted)
        }
    }
}

/** Formats epoch ms to a short relative string. */
private fun formatTime(ts: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - ts
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${(diff / 60_000).toInt()}m ago"
        diff < 86_400_000 -> "${(diff / 3_600_000).toInt()}h ago"
        else -> "${(diff / 86_400_000).toInt()}d ago"
    }
}

/** Simple icon button without Material3 Surface. */
@Composable
private fun IconButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    androidx.compose.material3.IconButton(onClick = onClick, modifier = modifier) { content() }
}

// ┌─────────────────────────────────────────────────────────────────┐
// │ Previews (R-57)                                                │
// └─────────────────────────────────────────────────────────────────┘

@Preview(showBackground = true, name = "Collection – with items")
@Composable
private fun CollectionPreviewWithItems() {
    TwinTheme {
        CollectionScreen(
            items = listOf(
                CollectionItem(id = "1", type = "photo", url = null, sender = "You", sentAt = System.currentTimeMillis() - 3600000, durationSec = null),
                CollectionItem(id = "2", type = "voice", url = null, sender = "Partner", sentAt = System.currentTimeMillis() - 7200000, durationSec = 45),
                CollectionItem(id = "3", type = "photo", url = null, sender = "Partner", sentAt = System.currentTimeMillis() - 86400000, durationSec = null),
            ),
        )
    }
}

@Preview(showBackground = true, name = "Collection – empty")
@Composable
private fun CollectionPreviewEmpty() {
    TwinTheme {
        CollectionScreen(items = emptyList())
    }
}
