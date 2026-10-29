@file:OptIn(ExperimentalLayoutApi::class)

package com.twinhearts.app.feature.globe.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.globe.domain.formatDateRange
import com.twinhearts.app.feature.globe.domain.formatMoney
import com.twinhearts.app.feature.globe.domain.model.CityGroup
import com.twinhearts.app.feature.globe.domain.model.MediaKind
import com.twinhearts.app.feature.globe.domain.model.Memory
import com.twinhearts.app.feature.globe.domain.preview
import com.twinhearts.app.feature.globe.ui.ViewerItem
import com.twinhearts.app.feature.globe.ui.components.SheetFrame
import com.twinhearts.app.feature.globe.ui.components.SmallPill
import com.twinhearts.app.feature.globe.ui.components.Tag

private const val MAX_CARD_THUMBS = 4
private const val NOTES_PREVIEW = 100

/** The sheet that opens when a pin is tapped: the city, and one card per memory in it. */
@Composable
fun BoxScope.CityPanel(
    wide: Boolean,
    group: CityGroup,
    flag: String,
    onClose: () -> Unit,
    onAddHere: () -> Unit,
    onOpen: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onMedia: (List<ViewerItem>, Int) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    SheetFrame(wide = wide, title = "$flag ${group.city}", onClose = onClose) {
        LazyColumn(
            contentPadding = PaddingValues(start = TwinTheme.spacing.l, end = TwinTheme.spacing.l, top = TwinTheme.spacing.s, bottom = TwinTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            item {
                Text(
                    text = stringResource(
                        R.string.globe_city_sub,
                        group.country,
                        pluralStringResource(R.plurals.globe_memory_count, group.count, group.count),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TwinTheme.colors.inkMuted,
                )
            }
            item {
                GlassButton(text = stringResource(R.string.globe_add_memory_here), onClick = onAddHere, modifier = Modifier.fillMaxWidth())
            }
            if (group.memories.isEmpty()) {
                item { EmptyState(title = stringResource(R.string.globe_no_memories)) }
            }
            items(group.memories, key = { it.id }) { memory ->
                MemoryCard(
                    memory = memory,
                    onOpen = { onOpen(memory.id) },
                    onEdit = { onEdit(memory.id) },
                    onDelete = { pendingDelete = memory.id },
                    onMedia = onMedia,
                )
            }
        }
    }
    pendingDelete?.let { id ->
        ConfirmDeleteDialog(
            text = stringResource(R.string.globe_confirm_delete_memory),
            onConfirm = {
                pendingDelete = null
                onDelete(id)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun MemoryCard(
    memory: Memory,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMedia: (List<ViewerItem>, Int) -> Unit,
) {
    val colors = TwinTheme.colors
    val visual = viewerItemsOf(memory)
    val voices = memory.mediaOf(MediaKind.Voice).size
    val journals = memory.mediaOf(MediaKind.Journal).size
    GlassCard(onClick = onOpen, modifier = Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.m) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = memory.tripName.ifBlank { stringResource(R.string.globe_card_default_title) },
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val dates = formatDateRange(memory.dateFrom, memory.dateTo)
                if (dates.isNotEmpty()) {
                    Text(dates, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                }
            }
            Text(memory.moodEmoji, style = MaterialTheme.typography.headlineSmall)
        }
        val hasTags = memory.tripCost != null || memory.weather.isNotBlank() || voices > 0 || journals > 0
        if (hasTags) {
            FlowRow(
                modifier = Modifier.padding(top = TwinTheme.spacing.s),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                memory.tripCost?.let { Tag(formatMoney(it, memory.currency), colors.success) }
                if (memory.weather.isNotBlank()) {
                    Tag(memory.weather + if (memory.temperature.isNotBlank()) " ${memory.temperature}°" else "", colors.pinkSoft)
                }
                if (voices > 0) Tag(stringResource(R.string.globe_tag_voice, voices), colors.accent)
                if (journals > 0) Tag(stringResource(R.string.globe_tag_journal), colors.orchid)
            }
        }
        if (visual.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.padding(top = TwinTheme.spacing.s),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(visual.take(MAX_CARD_THUMBS).withIndex().toList(), key = { it.index }) { (index, item) ->
                    MediaThumb(url = item.url, isVideo = item.isVideo, size = 52.dp, onClick = { onMedia(visual, index) })
                }
            }
        }
        if (memory.notes.isNotBlank()) {
            Text(
                text = preview(memory.notes, NOTES_PREVIEW),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkMuted,
                modifier = Modifier.padding(top = TwinTheme.spacing.s),
            )
        }
        Row(
            modifier = Modifier.padding(top = TwinTheme.spacing.m),
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        ) {
            SmallPill(text = stringResource(R.string.globe_view), onClick = onOpen, primary = true)
            SmallPill(text = stringResource(R.string.globe_edit), onClick = onEdit)
            SmallPill(text = stringResource(R.string.globe_delete_icon), onClick = onDelete, danger = true)
        }
    }
}
