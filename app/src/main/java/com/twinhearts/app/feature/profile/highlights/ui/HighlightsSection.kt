package com.twinhearts.app.feature.profile.highlights.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.common.rememberCameraGate
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.profile.highlights.domain.ClipKind
import com.twinhearts.app.feature.profile.highlights.domain.Highlight
import com.twinhearts.app.feature.profile.highlights.domain.HighlightCategory
import com.twinhearts.app.feature.profile.highlights.domain.HighlightsMath
import com.twinhearts.app.feature.profile.highlights.domain.HighlightsOwner
import com.twinhearts.app.feature.profile.highlights.domain.PickedClip
import com.twinhearts.app.feature.profile.highlights.domain.UploadTarget
import com.twinhearts.app.feature.profile.ui.components.ProfileSheet
import java.io.File
import java.io.IOException

private val CIRCLE = 70.dp
private val RING = 3.dp
private val BADGE = 22.dp
private val ITEM_WIDTH = 76.dp
private val PREVIEW = 64.dp
private val MINI = 40.dp

/**
 * The "Highlights" row of Our Profile, placed right under the couple card: a "+" circle, then one
 * circle per highlight (mine first, then my partner's), with category chips when there are two or more
 * categories. Tap a circle to watch it; long-press one of mine to edit it. Owns its own ViewModel.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HighlightsSection(
    modifier: Modifier = Modifier,
    owner: HighlightsOwner = HighlightsOwner.Mine,
    vm: HighlightsViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colors = TwinTheme.colors

    var showSource by rememberSaveable { mutableStateOf(false) }
    // The highlight the next picked clips should join (set by "Add more clips"), or null for a new one.
    var addToId by rememberSaveable { mutableStateOf<String?>(null) }
    var viewerId by rememberSaveable { mutableStateOf<String?>(null) }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraFile by remember { mutableStateOf<File?>(null) }

    // Coming back to the profile (e.g. from a sub-screen) re-reads the list so the partner's news shows.
    LaunchedEffect(Unit) { vm.refresh() }

    val messageText = state.message?.let { messageText(it) }
    LaunchedEffect(state.message) {
        if (messageText != null) {
            Toast.makeText(context, messageText, Toast.LENGTH_SHORT).show()
            vm.messageShown()
        }
    }

    val pickMany = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(HighlightsMath.MAX_PICK)) { uris ->
        if (uris.isNotEmpty()) vm.onClipsPicked(uris.map(Uri::toString), addToId)
    }
    val recordVideo = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { ok ->
        val file = cameraFile
        cameraFile = null
        val uri = file?.let { fileUri(context, it) }
        if (ok && file != null && file.length() > 0L && uri != null) vm.onClipsPicked(listOf(uri.toString()), addToId) else file?.delete()
    }
    val withCamera = rememberCameraGate { Toast.makeText(context, R.string.profile_hl_camera_failed, Toast.LENGTH_SHORT).show() }

    fun record() {
        val file = newCaptureFile(context)
        val uri = file?.let { fileUri(context, it) }
        if (file == null || uri == null) {
            Toast.makeText(context, R.string.profile_hl_camera_failed, Toast.LENGTH_SHORT).show()
            return
        }
        cameraFile = file
        withCamera { recordVideo.launch(uri) }
    }

    val snapshot = (state.phase as? HighlightsPhase.Ready)?.snapshot
    val all = snapshot?.highlights.orEmpty()
    val owned = HighlightsMath.ownedBy(all, owner)
    val isMine = owner == HighlightsOwner.Mine
    val used = HighlightsMath.usedCategories(owned)
    val filter = state.filters[owner]?.takeIf { it in used }
    val shown = HighlightsMath.visible(owned, filter)

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        if (used.size >= 2) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                item { TwinFilterChip(stringResource(R.string.profile_hl_all), filter == null, { vm.setFilter(owner, null) }) }
                items(used, key = { it.wire }) { category ->
                    TwinFilterChip("${category.emoji} ${stringResource(category.label)}", filter == category, { vm.setFilter(owner, category) })
                }
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
            // Only my own row has the "+" circle; my partner's highlights are watch-only.
            if (isMine) item(key = "new") {
                CircleItem(label = stringResource(R.string.profile_hl_new), description = stringResource(R.string.profile_hl_new_cd), onClick = { addToId = null; showSource = true }) {
                    Box(Modifier.size(CIRCLE).border(1.5.dp, colors.inkMuted, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = colors.ink, modifier = Modifier.size(32.dp))
                    }
                }
            }
            if (state.phase == HighlightsPhase.Loading) {
                items(SKELETONS) { SkeletonItem() }
            }
            items(shown, key = { it.id }) { highlight ->
                CircleItem(
                    label = highlight.title,
                    description = stringResource(R.string.profile_hl_open_cd, highlight.title),
                    onClick = { viewerId = highlight.id },
                    onLongClick = if (highlight.mine) ({ editId = highlight.id }) else null,
                ) { HighlightCircle(highlight) }
            }
        }
        when {
            state.phase is HighlightsPhase.Failed -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.profile_hl_load_error), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                Text(
                    stringResource(R.string.profile_hl_retry),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.accent,
                    modifier = Modifier.clickable(role = Role.Button, onClick = vm::retry).padding(horizontal = TwinTheme.spacing.s, vertical = TwinTheme.spacing.xs),
                )
            }
            state.phase is HighlightsPhase.Ready && owned.isEmpty() ->
                Text(
                    if (isMine) stringResource(R.string.profile_hl_empty)
                    else stringResource(R.string.profile_hl_partner_empty, snapshot?.partnerName.orEmpty().ifBlank { stringResource(R.string.profile_hl_partner) }),
                    style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
        }
    }

    if (showSource) {
        SourceSheet(
            title = stringResource(if (addToId == null) R.string.profile_hl_title_new else R.string.profile_hl_title_add),
            onDismiss = { showSource = false },
            onRecord = { showSource = false; record() },
            onUpload = { showSource = false; pickMany.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
        )
    }

    state.pending?.let { pending ->
        AddClipsSheet(
            pending = pending,
            mine = all.filter { it.mine },
            progress = state.progress,
            error = state.uploadError,
            onSubmit = vm::startUpload,
            onDismiss = vm::cancelPending,
        )
    }

    val editing = all.firstOrNull { it.id == editId && it.mine }
    if (editing != null) {
        EditHighlightSheet(editing, state.busy, onDismiss = { editId = null }) { title, category ->
            vm.rename(editing.id, title, category) { editId = null }
        }
    }

    val viewing = all.firstOrNull { it.id == viewerId }
    if (viewing != null) {
        HighlightViewer(
            highlight = viewing,
            ownerName = if (viewing.mine) snapshot?.myName.orEmpty().ifBlank { stringResource(R.string.profile_hl_you) }
            else snapshot?.partnerName.orEmpty().ifBlank { stringResource(R.string.profile_hl_partner) },
            busy = state.busy,
            onClose = { viewerId = null },
            onEdit = { title, category, done -> vm.rename(viewing.id, title, category, done) },
            onAddMore = { viewerId = null; addToId = viewing.id; showSource = true },
            onDeleteClip = { clip, done -> vm.deleteClip(clip.id) { gone -> done(); if (gone) viewerId = null } },
            onDeleteHighlight = { done -> vm.deleteHighlight(viewing.id) { done(); viewerId = null } },
        )
    }
}

// ── circles ─────────────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CircleItem(
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    circle: @Composable () -> Unit,
) {
    Column(
        modifier.width(ITEM_WIDTH).combinedClickable(role = Role.Button, onClickLabel = description, onLongClick = onLongClick, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs + 2.dp),
    ) {
        circle()
        Text(label, style = MaterialTheme.typography.labelMedium, color = TwinTheme.colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

/** Gradient ring (white-ish for my partner's), the cover picture inside, the category emoji at the corner. */
@Composable
private fun HighlightCircle(highlight: Highlight) {
    val colors = TwinTheme.colors
    val ring = if (highlight.mine) Modifier.border(RING, colors.primaryBrush, CircleShape) else Modifier.border(RING, colors.line, CircleShape)
    Box(Modifier.size(CIRCLE)) {
        ClipThumb(
            source = highlight.cover.url,
            kind = highlight.cover.kind,
            modifier = Modifier.size(CIRCLE).then(ring).padding(RING + 2.dp).clip(CircleShape),
        )
        Box(
            Modifier.align(Alignment.BottomEnd).size(BADGE).background(colors.card, CircleShape).border(1.5.dp, colors.background, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(highlight.category.emoji, style = MaterialTheme.typography.labelSmall) }
    }
}

@Composable
private fun SkeletonItem() {
    Column(Modifier.width(ITEM_WIDTH), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        Box(Modifier.size(CIRCLE).background(TwinTheme.colors.glass, CircleShape))
        Box(Modifier.size(width = 46.dp, height = 10.dp).background(TwinTheme.colors.glass, MaterialTheme.shapes.small))
    }
}

private const val SKELETONS = 3

// ── sheets ──────────────────────────────────────────────────────────────────────────────────────

/** Step 1: where do the clips come from — the camera or the gallery. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourceSheet(title: String, onDismiss: () -> Unit, onRecord: () -> Unit, onUpload: () -> Unit) {
    val colors = TwinTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.card, contentColor = colors.ink) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = TwinTheme.spacing.screenEdge).padding(bottom = TwinTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = colors.ink)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                SourceTile(Icons.Rounded.Videocam, stringResource(R.string.profile_hl_record), onRecord, Modifier.weight(1f))
                SourceTile(Icons.Rounded.PhotoLibrary, stringResource(R.string.profile_hl_upload), onUpload, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SourceTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, modifier: Modifier) {
    val colors = TwinTheme.colors
    Column(
        modifier.clip(MaterialTheme.shapes.large).background(colors.glass).border(1.dp, colors.line, MaterialTheme.shapes.large)
            .clickable(role = Role.Button, onClick = onClick).padding(vertical = TwinTheme.spacing.l, horizontal = TwinTheme.spacing.s),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        Icon(icon, contentDescription = null, tint = colors.ink, modifier = Modifier.size(28.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, color = colors.ink, textAlign = TextAlign.Center)
    }
}

private enum class AddMode { New, Existing }

/** Step 2: preview the clips, then a new highlight (name + category) or one of mine. Shows upload progress. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddClipsSheet(
    pending: PendingUpload,
    mine: List<Highlight>,
    progress: UploadProgress?,
    error: AppError?,
    onSubmit: (UploadTarget) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = TwinTheme.colors
    val locked = pending.lockedHighlightId
    val start = locked ?: pending.preselectId?.takeIf { id -> mine.any { it.id == id } }
    var mode by rememberSaveable { mutableStateOf(if (start != null) AddMode.Existing else AddMode.New) }
    var name by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(HighlightCategory.Love) }
    var chosenId by rememberSaveable { mutableStateOf(start ?: mine.firstOrNull()?.id) }
    var needChoice by rememberSaveable { mutableStateOf(false) }
    // After a half-finished try, the rest must join the highlight that was already created.
    LaunchedEffect(locked) { if (locked != null) { mode = AddMode.Existing; chosenId = locked } }

    val sending = progress != null
    val fallbackTitle = stringResource(category.label)
    ProfileSheet(
        title = pluralStringResource(R.plurals.profile_hl_clips_ready, pending.clips.size, pending.clips.size),
        onDismiss = onDismiss,
        onSave = {
            if (mode == AddMode.Existing) {
                val id = chosenId
                if (id == null) needChoice = true else onSubmit(UploadTarget.Existing(id))
            } else {
                onSubmit(UploadTarget.NewHighlight(HighlightsMath.finalTitle(name, fallbackTitle), category))
            }
        },
        saveEnabled = !sending,
        saving = sending,
        saveLabel = stringResource(R.string.profile_hl_add_button),
    ) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            pending.clips.take(PREVIEW_MAX).forEach { clip -> PreviewTile(clip) }
        }
        if (locked == null && mine.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                TwinFilterChip(stringResource(R.string.profile_hl_mode_new), mode == AddMode.New, { mode = AddMode.New })
                TwinFilterChip(stringResource(R.string.profile_hl_mode_existing), mode == AddMode.Existing, { mode = AddMode.Existing })
            }
        }
        if (mode == AddMode.New) {
            TwinTextField(name, { name = it }, stringResource(R.string.profile_hl_name), placeholder = stringResource(R.string.profile_hl_name_hint), maxLength = HighlightsMath.TITLE_MAX)
            Text(stringResource(R.string.profile_hl_category), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
            CategoryChips(category) { category = it }
        } else {
            mine.forEach { highlight ->
                ExistingRow(highlight, selected = highlight.id == chosenId, enabled = locked == null) { chosenId = highlight.id; needChoice = false }
            }
            if (needChoice) Text(stringResource(R.string.profile_hl_choose_first), style = MaterialTheme.typography.bodySmall, color = colors.error)
        }
        if (progress != null) {
            LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.fillMaxWidth(), color = colors.accent, trackColor = colors.glass)
            Text(
                stringResource(R.string.profile_hl_uploading, (progress.done + 1).coerceAtMost(progress.total), progress.total),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted,
            )
        }
        if (error != null) Text(error.asText(), style = MaterialTheme.typography.bodySmall, color = colors.error)
    }
}

private const val PREVIEW_MAX = 10

@Composable
private fun PreviewTile(clip: PickedClip) {
    ClipThumb(
        source = clip.uri,
        kind = if (clip.isVideo) ClipKind.Video else ClipKind.Image,
        modifier = Modifier.size(PREVIEW).clip(MaterialTheme.shapes.medium),
    )
}

@Composable
private fun ExistingRow(highlight: Highlight, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    val shape = MaterialTheme.shapes.large
    Row(
        Modifier.fillMaxWidth().clip(shape).background(if (selected) colors.glass else Color.Transparent)
            .border(1.dp, if (selected) colors.accent else colors.line, shape)
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick).padding(TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        ClipThumb(highlight.cover.url, highlight.cover.kind, Modifier.size(MINI).clip(CircleShape))
        Column(Modifier.weight(1f)) {
            Text(highlight.title, style = MaterialTheme.typography.titleSmall, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${highlight.category.emoji} ${stringResource(highlight.category.label)} · " +
                    pluralStringResource(R.plurals.profile_hl_clip_count, highlight.clips.size, highlight.clips.size),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChips(selected: HighlightCategory, onSelect: (HighlightCategory) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        HighlightCategory.entries.forEach { category ->
            TwinFilterChip("${category.emoji} ${stringResource(category.label)}", selected == category, { onSelect(category) })
        }
    }
}

/** Rename and re-categorise one of my highlights. The name cannot be empty. */
@Composable
fun EditHighlightSheet(highlight: Highlight, busy: Boolean, onDismiss: () -> Unit, onSave: (String, HighlightCategory) -> Unit) {
    var name by rememberSaveable(highlight.id) { mutableStateOf(highlight.title) }
    var category by rememberSaveable(highlight.id) { mutableStateOf(highlight.category) }
    ProfileSheet(
        title = stringResource(R.string.profile_hl_edit_title),
        onDismiss = onDismiss,
        onSave = { onSave(name.trim(), category) },
        saveEnabled = name.isNotBlank(),
        saving = busy,
    ) {
        TwinTextField(name, { name = it }, stringResource(R.string.profile_hl_name), placeholder = stringResource(R.string.profile_hl_name_hint), maxLength = HighlightsMath.TITLE_MAX)
        Text(stringResource(R.string.profile_hl_category), style = MaterialTheme.typography.labelMedium, color = TwinTheme.colors.inkMuted)
        CategoryChips(category) { category = it }
    }
}

// ── helpers ─────────────────────────────────────────────────────────────────────────────────────

@Composable
private fun messageText(message: HighlightsMessage): String = when (message) {
    HighlightsMessage.Saved -> stringResource(R.string.profile_hl_saved)
    HighlightsMessage.EditSaved -> stringResource(R.string.profile_hl_edit_saved)
    HighlightsMessage.Deleted -> stringResource(R.string.profile_hl_deleted)
    HighlightsMessage.ClipDeleted -> stringResource(R.string.profile_hl_clip_deleted)
    HighlightsMessage.Skipped -> stringResource(R.string.profile_hl_skipped)
    HighlightsMessage.TooBig -> stringResource(R.string.profile_hl_too_big)
    HighlightsMessage.NotMedia -> stringResource(R.string.profile_hl_not_media)
    is HighlightsMessage.Failed -> message.error.asText()
}

/** A temp file in the cache folder the app's FileProvider already shares (same folder the chat camera uses). */
private fun newCaptureFile(context: Context): File? = try {
    val dir = File(context.cacheDir, "chat_camera").apply { mkdirs() }
    File.createTempFile("highlight-", ".mp4", dir)
} catch (e: IOException) {
    null
}

private fun fileUri(context: Context, file: File): Uri? = try {
    FileProvider.getUriForFile(context, "${context.packageName}.chatfiles", file)
} catch (e: IllegalArgumentException) {
    null
}
