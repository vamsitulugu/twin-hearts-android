package com.twinhearts.app.feature.us.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayCircleFilled
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.twinhearts.app.core.design.Avatar
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.SectionHeader
import com.twinhearts.app.core.design.StatTile
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.us.domain.model.CollectionItem

/** Collection screen (web: collection.html). Hero stats, search, shared memory list. */
@Composable
fun CollectionScreen(items: List<CollectionItem>) {
    val colors = TwinTheme.colors
    var query by remember { mutableStateOf("") }
    val visible = items.filter {
        query.isBlank() || it.sender.contains(query, ignoreCase = true) || it.type.contains(query, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SectionHeader(
            title = "Collection",
            modifier = Modifier.padding(horizontal = TwinTheme.spacing.l),
        )

        // Hero stats: 4 stat tiles
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TwinTheme.spacing.l),
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatTile(value = items.size.toString(), label = "Shared Items", modifier = Modifier.weight(1f))
            StatTile(value = "2", label = "Albums", modifier = Modifier.weight(1f))
            StatTile(value = "0", label = "Downloads", modifier = Modifier.weight(1f))
            StatTile(value = "0", label = "Favourites", modifier = Modifier.weight(1f))
        }

        // Toolbar: search
        TwinTextField(
            value = query,
            onValueChange = { query = it },
            label = "Search",
            placeholder = "Search memories",
            leadingIcon = Icons.Rounded.Search,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.s),
        )

        // Voice / media list
        SectionHeader(
            title = "Memories",
            modifier = Modifier.padding(horizontal = TwinTheme.spacing.l),
            actionLabel = "See all",
            onAction = {},
        )
        if (visible.isEmpty()) {
            Text(
                text = "No shared memories yet. Start by sharing a photo or voice note.",
                color = colors.inkMuted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TwinTheme.spacing.l),
                textAlign = TextAlign.Center,
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TwinTheme.spacing.l),
            ) {
                visible.forEach { item ->
                    CollectionItemRow(item = item)
                }
            }
        }
    }
}

/** One item in the collection list: avatar, sender, date, actions. */
@Composable
private fun CollectionItemRow(item: CollectionItem) {
    val colors = TwinTheme.colors
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = TwinTheme.spacing.xs),
        onClick = {},
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Avatar(name = item.sender, size = 36.dp)
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
            IconButton(onClick = {}, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Rounded.PlayCircleFilled, contentDescription = "Play", tint = colors.accent)
            }
        }
        IconButton(onClick = {}, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Rounded.FavoriteBorder, contentDescription = "Favorite", tint = colors.inkMuted)
        }
        IconButton(onClick = {}, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = colors.inkMuted)
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
