@file:OptIn(ExperimentalLayoutApi::class)

package com.twinhearts.app.feature.globe.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.globe.domain.formatDate
import com.twinhearts.app.feature.globe.domain.formatMoney
import com.twinhearts.app.feature.globe.domain.model.MediaKind
import com.twinhearts.app.feature.globe.domain.model.Memory
import com.twinhearts.app.feature.globe.domain.model.MemoryMedia
import com.twinhearts.app.feature.globe.ui.UploadUi
import com.twinhearts.app.feature.globe.ui.ViewerItem
import com.twinhearts.app.feature.globe.ui.components.GlobeTextArea
import com.twinhearts.app.feature.globe.ui.components.GlobeVoicePlayer
import com.twinhearts.app.feature.globe.ui.components.ReadOnlyChips
import com.twinhearts.app.feature.globe.ui.components.SHEET_HALF_FRACTION
import com.twinhearts.app.feature.globe.ui.components.SheetFrame
import com.twinhearts.app.feature.globe.ui.components.SheetLabel
import com.twinhearts.app.feature.globe.ui.components.SmallPill
import com.twinhearts.app.feature.globe.ui.components.VoiceControl
import com.twinhearts.app.feature.globe.ui.components.MediaPickers
import com.twinhearts.app.feature.globe.ui.components.rememberGlobeVoicePlayer

/** Everything the detail sheet can ask the page to do. */
class DetailActions(
    val onClose: () -> Unit,
    val onEdit: () -> Unit,
    val onDeleteMemory: () -> Unit,
    val onDeleteMedia: (String) -> Unit,
    val onOpenViewer: (List<ViewerItem>, Int) -> Unit,
    val onAddJournal: (String) -> Unit,
    val onRetryUpload: () -> Unit,
    val onDismissUpload: () -> Unit,
    val onPlayError: () -> Unit,
)

@Composable
fun BoxScope.DetailPanel(
    wide: Boolean,
    memory: Memory,
    flag: String,
    upload: UploadUi?,
    pickers: MediaPickers,
    voice: VoiceControl,
    actions: DetailActions,
) {
    val colors = TwinTheme.colors
    val player = rememberGlobeVoicePlayer()
    var pendingMediaDelete by remember { mutableStateOf<String?>(null) }
    var confirmMemoryDelete by remember { mutableStateOf(false) }
    var journalOpen by remember { mutableStateOf(false) }

    val photos = memory.mediaOf(MediaKind.Photo)
    val videos = memory.mediaOf(MediaKind.Video)
    val voices = memory.mediaOf(MediaKind.Voice)
    val journals = memory.mediaOf(MediaKind.Journal)
    val visual = viewerItemsOf(memory)

    SheetFrame(wide = wide, title = memory.tripName.ifBlank { memory.city }, onClose = actions.onClose, heightFraction = SHEET_HALF_FRACTION) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TwinTheme.spacing.l)
                .padding(bottom = TwinTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.l),
        ) {
            photos.firstOrNull()?.let { hero ->
                AsyncImage(
                    model = hero.url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { actions.onOpenViewer(visual, 0) },
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$flag ${listOf(memory.city, memory.country).filter { it.isNotBlank() }.joinToString(", ")}",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.ink,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(memory.moodEmoji, style = MaterialTheme.typography.displaySmall)
            }

            TripDetails(memory)

            if (memory.notes.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SheetLabel(stringResource(R.string.globe_section_notes))
                    Text(memory.notes, style = MaterialTheme.typography.bodyLarge, color = colors.ink)
                }
            }
            if (memory.favoriteMoment.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SheetLabel(stringResource(R.string.globe_section_favorite))
                    Text(memory.favoriteMoment, style = MaterialTheme.typography.bodyLarge, color = colors.ink)
                }
            }

            if (upload != null && upload.memoryId == memory.id) UploadBar(upload, actions)

            if (photos.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    SheetLabel(stringResource(R.string.globe_section_photos, photos.size))
                    MediaGrid(photos, isVideo = false, onOpen = { m -> actions.onOpenViewer(visual, visual.indexOfFirst { it.url == m.url }.coerceAtLeast(0)) }, onDelete = { pendingMediaDelete = it })
                }
            }
            if (videos.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    SheetLabel(stringResource(R.string.globe_section_videos, videos.size))
                    MediaGrid(videos, isVideo = true, onOpen = { m -> actions.onOpenViewer(visual, visual.indexOfFirst { it.url == m.url }.coerceAtLeast(0)) }, onDelete = { pendingMediaDelete = it })
                }
            }
            if (voices.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    SheetLabel(stringResource(R.string.globe_section_voice, voices.size))
                    voices.forEachIndexed { index, note ->
                        VoiceRow(index + 1, note, player, actions.onPlayError, onDelete = { pendingMediaDelete = note.id })
                    }
                }
            }
            if (journals.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    SheetLabel(stringResource(R.string.globe_section_journal, journals.size))
                    journals.forEach { entry ->
                        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.m) {
                            Row(verticalAlignment = Alignment.Top) {
                                Text(entry.journalText, style = MaterialTheme.typography.bodyMedium, color = colors.ink, modifier = Modifier.weight(1f))
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = stringResource(R.string.globe_delete_media_cd),
                                    tint = colors.error,
                                    modifier = Modifier.size(24.dp).clickable { pendingMediaDelete = entry.id },
                                )
                            }
                        }
                    }
                }
            }
            if (memory.restaurants.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    SheetLabel(stringResource(R.string.globe_section_restaurants))
                    ReadOnlyChips(memory.restaurants)
                }
            }
            if (memory.hotels.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    SheetLabel(stringResource(R.string.globe_section_hotels))
                    ReadOnlyChips(memory.hotels)
                }
            }
            if (memory.gifts.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    SheetLabel(stringResource(R.string.globe_section_gifts))
                    ReadOnlyChips(memory.gifts)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                SheetLabel(stringResource(R.string.globe_section_add_more))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    SmallPill(text = stringResource(R.string.globe_add_photo), onClick = pickers.pickPhotos)
                    SmallPill(text = stringResource(R.string.globe_add_video), onClick = pickers.pickVideo)
                    SmallPill(
                        text = stringResource(if (voice.recording) R.string.globe_form_stop else R.string.globe_add_voice),
                        onClick = voice.toggle,
                        primary = voice.recording,
                    )
                    SmallPill(text = stringResource(R.string.globe_add_journal), onClick = { journalOpen = true })
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.fillMaxWidth()) {
                GlassButton(
                    text = stringResource(R.string.globe_edit_memory),
                    onClick = actions.onEdit,
                    style = GlassButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                )
                GlassButton(
                    text = stringResource(R.string.globe_delete_memory),
                    onClick = { confirmMemoryDelete = true },
                    style = GlassButtonStyle.Ghost,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(TwinTheme.spacing.s))
        }
    }

    pendingMediaDelete?.let { id ->
        ConfirmDeleteDialog(
            text = stringResource(R.string.globe_confirm_delete_media),
            onConfirm = {
                pendingMediaDelete = null
                actions.onDeleteMedia(id)
            },
            onDismiss = { pendingMediaDelete = null },
        )
    }
    if (confirmMemoryDelete) {
        ConfirmDeleteDialog(
            text = stringResource(R.string.globe_confirm_delete_memory),
            onConfirm = {
                confirmMemoryDelete = false
                actions.onDeleteMemory()
            },
            onDismiss = { confirmMemoryDelete = false },
        )
    }
    if (journalOpen) {
        JournalDialog(
            onSave = {
                journalOpen = false
                actions.onAddJournal(it)
            },
            onDismiss = { journalOpen = false },
        )
    }
}

@Composable
private fun TripDetails(memory: Memory) {
    val from = formatDate(memory.dateFrom)
    val to = formatDate(memory.dateTo)
    val weather = memory.weather + if (memory.temperature.isNotBlank()) (if (memory.weather.isNotBlank()) ", " else "") + "${memory.temperature}°C" else ""
    val location = listOf(memory.city, memory.state, memory.country).filter { it.isNotBlank() }.joinToString(", ")
    val items = listOf(
        stringResource(R.string.globe_label_from) to from,
        stringResource(R.string.globe_label_to) to to,
        stringResource(R.string.globe_label_cost) to (memory.tripCost?.let { formatMoney(it, memory.currency) } ?: ""),
        stringResource(R.string.globe_label_weather) to weather,
        stringResource(R.string.globe_label_mood) to memory.mood,
        stringResource(R.string.globe_label_location) to location,
    ).filter { it.second.isNotBlank() }
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        SheetLabel(stringResource(R.string.globe_section_trip_details))
        items.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.fillMaxWidth()) {
                pair.forEach { (label, value) ->
                    GlassCard(modifier = Modifier.weight(1f), contentPadding = TwinTheme.spacing.m) {
                        Text(label, style = MaterialTheme.typography.labelSmall, color = TwinTheme.colors.inkMuted)
                        Text(value, style = MaterialTheme.typography.bodyMedium, color = TwinTheme.colors.ink, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MediaGrid(items: List<MemoryMedia>, isVideo: Boolean, onOpen: (MemoryMedia) -> Unit, onDelete: (String) -> Unit) {
    val colors = TwinTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { item ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.glass)
                            .clickable { onOpen(item) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isVideo) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.globe_play_video), tint = colors.ink, modifier = Modifier.size(36.dp))
                        } else {
                            AsyncImage(model = item.url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(colors.background.copy(alpha = 0.7f))
                                .clickable { onDelete(item.id) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.globe_delete_media_cd), tint = colors.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun VoiceRow(number: Int, note: MemoryMedia, player: GlobeVoicePlayer, onError: () -> Unit, onDelete: () -> Unit) {
    val colors = TwinTheme.colors
    val playing = player.playing == note.url
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.s) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.primaryBrush)
                    .clickable { player.toggle(note.url, onError) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(if (playing) R.string.globe_pause else R.string.globe_play),
                    tint = colors.ink,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.globe_voice_note_n, number), style = MaterialTheme.typography.bodyMedium, color = colors.ink, fontWeight = FontWeight.SemiBold)
                if (note.duration.isNotBlank()) Text(note.duration, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
            }
            Icon(
                Icons.Rounded.Delete,
                contentDescription = stringResource(R.string.globe_delete_media_cd),
                tint = colors.error,
                modifier = Modifier.size(28.dp).clickable(onClick = onDelete),
            )
        }
    }
}

@Composable
private fun UploadBar(upload: UploadUi, actions: DetailActions) {
    val colors = TwinTheme.colors
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.m) {
        if (upload.running) {
            Text(stringResource(R.string.globe_uploading_progress, upload.done, upload.total), style = MaterialTheme.typography.bodyMedium, color = colors.ink)
            LinearProgressIndicator(
                progress = { if (upload.total == 0) 0f else upload.done.toFloat() / upload.total },
                color = colors.accent,
                trackColor = colors.line,
                modifier = Modifier.fillMaxWidth().padding(top = TwinTheme.spacing.s),
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                Text(
                    stringResource(R.string.globe_toast_some_failed, upload.total - upload.failedUris.size, upload.failedUris.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.ink,
                    modifier = Modifier.weight(1f),
                )
                SmallPill(text = stringResource(R.string.globe_retry), onClick = actions.onRetryUpload, primary = true)
                SmallPill(text = stringResource(R.string.globe_cancel), onClick = actions.onDismissUpload)
            }
        }
    }
}

/** The journal entry box (the web app used a browser prompt here). */
@Composable
private fun JournalDialog(onSave: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = TwinTheme.colors
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        text = {
            GlobeTextArea(
                value = text,
                onValueChange = { text = it },
                label = stringResource(R.string.globe_journal_prompt),
                hint = stringResource(R.string.globe_form_journal_hint),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) { Text(stringResource(R.string.globe_save), color = colors.accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.globe_cancel), color = colors.inkMuted) }
        },
    )
}
