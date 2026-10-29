package com.twinhearts.app.feature.memories.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.annotation.PluralsRes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AutoRefresh
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.OfflineBanner
import com.twinhearts.app.core.design.PetalBurst
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.feature.memories.domain.memoryInsight
import com.twinhearts.app.feature.memories.domain.model.Memory
import com.twinhearts.app.feature.memories.domain.model.MemoryInsight
import com.twinhearts.app.feature.memories.domain.monthlyCounts
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private const val GRID_COLUMNS = 3
private const val MAX_PICKS = 20
private val BAR_AREA = 60.dp

/**
 * Memories page (web: `#page-camera`, "Our Memories"): take a photo or video, add from the gallery,
 * a tip and a six-month chart, and the shared album as a grid. Tap a tile for the full-screen viewer,
 * long-press to select several to share or delete. The album is the `photos` list in the couple's
 * shared state, so the web app shows the same pictures.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MemoriesScreen(
    onBack: () -> Unit,
    viewModel: MemoriesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var viewerIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var deleteIds by remember { mutableStateOf<Set<String>?>(null) }
    var cameraFile by remember { mutableStateOf<File?>(null) }
    val selecting = state.selection.isNotEmpty()

    AutoRefresh(onRefresh = viewModel::refresh)
    BackHandler(enabled = selecting && viewerIndex == null) { viewModel.clearSelection() }

    // One-off messages: toast once, then clear.
    val message = state.message
    val messageText = when (message) {
        null -> null
        is MemoriesMessage.Added -> plural(R.plurals.memories_added, message.count, message.count)
        is MemoriesMessage.PartlyAdded -> stringResource(R.string.memories_partly_added, message.added, message.failed)
        is MemoriesMessage.Deleted -> plural(R.plurals.memories_deleted, message.count, message.count)
        is MemoriesMessage.PartlyDeleted -> stringResource(R.string.memories_partly_deleted, message.deleted, message.failed)
        is MemoriesMessage.Failed -> message.error.asText()
    }
    LaunchedEffect(message) {
        if (message == null) return@LaunchedEffect
        Toast.makeText(context, messageText.orEmpty(), Toast.LENGTH_SHORT).show()
        viewModel.messageShown()
    }

    val pickMany = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PICKS)) { uris ->
        viewModel.upload(uris)
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val file = cameraFile
        cameraFile = null
        if (ok && file != null && file.length() > 0L) fileUri(context, file)?.let { viewModel.upload(listOf(it)) } else file?.delete()
    }
    val recordVideo = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { ok ->
        val file = cameraFile
        cameraFile = null
        if (ok && file != null && file.length() > 0L) fileUri(context, file)?.let { viewModel.upload(listOf(it)) } else file?.delete()
    }
    fun capture(video: Boolean) {
        val file = newCaptureFile(context, if (video) ".mp4" else ".jpg")
        val uri = file?.let { fileUri(context, it) }
        if (file == null || uri == null) {
            Toast.makeText(context, R.string.memories_camera_failed, Toast.LENGTH_SHORT).show()
            return
        }
        cameraFile = file
        if (video) recordVideo.launch(uri) else takePhoto.launch(uri)
    }
    fun pick(type: ActivityResultContracts.PickVisualMedia.VisualMediaType) {
        pickMany.launch(PickVisualMediaRequest(type))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (selecting) {
                SelectionBar(
                    count = state.selection.size,
                    onCancel = viewModel::clearSelection,
                    onShare = { shareLinks(context, state.items.filter { it.id in state.selection }) },
                    onDelete = { deleteIds = state.selection },
                )
            } else {
                TwinTopBar(
                    title = stringResource(R.string.memories_title),
                    onBackClick = onBack,
                    onSyncClick = viewModel::load,
                )
            }
            if (state.offline) OfflineBanner()
            when {
                state.loading && state.items.isEmpty() -> {
                    Column(modifier = Modifier.padding(TwinTheme.spacing.screenEdge)) { LoadingSkeleton(lines = 4) }
                }
                state.error != null && state.items.isEmpty() -> {
                    ErrorState(message = (state.error ?: AppError.Unknown).asText(), onRetry = viewModel::load)
                }
                else -> MemoriesGrid(
                    state = state,
                    onTakePhoto = { capture(false) },
                    onRecordVideo = { capture(true) },
                    onAddPhotos = { pick(ActivityResultContracts.PickVisualMedia.ImageOnly) },
                    onAddVideo = { pick(ActivityResultContracts.PickVisualMedia.VideoOnly) },
                    onAddAny = { pick(ActivityResultContracts.PickVisualMedia.ImageAndVideo) },
                    onRetry = viewModel::retryFailedUploads,
                    onDismissFailed = viewModel::dismissFailedUploads,
                    onTap = { index, item ->
                        if (selecting) viewModel.toggleSelect(item.id) else viewerIndex = index
                    },
                    onLongPress = { item -> viewModel.toggleSelect(item.id) },
                )
            }
        }
        PetalBurst(trigger = state.petals)
    }

    viewerIndex?.let { start ->
        MemoriesViewer(
            items = state.items,
            startIndex = start,
            onDismiss = { viewerIndex = null },
            onFavorite = { viewModel.toggleFavorite(it.id) },
            onShare = { shareLinks(context, listOf(it)) },
            onDownload = { openUrl(context, it.url) },
            onDelete = { deleteIds = setOf(it.id) },
        )
    }

    deleteIds?.let { ids ->
        val anyVideo = state.items.any { it.id in ids && it.isVideo }
        AlertDialog(
            onDismissRequest = { deleteIds = null },
            containerColor = TwinTheme.colors.card,
            title = {
                Text(
                    text = plural(
                        if (anyVideo) R.plurals.memories_delete_title_media else R.plurals.memories_delete_title_photos,
                        ids.size,
                        ids.size,
                    ),
                    color = TwinTheme.colors.ink,
                )
            },
            text = { Text(stringResource(R.string.memories_delete_message), color = TwinTheme.colors.inkMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(ids)
                        deleteIds = null
                    },
                ) { Text(stringResource(R.string.memories_delete), color = TwinTheme.colors.error) }
            },
            dismissButton = { TextButton(onClick = { deleteIds = null }) { Text(stringResource(R.string.core_cancel)) } },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MemoriesGrid(
    state: MemoriesUiState,
    onTakePhoto: () -> Unit,
    onRecordVideo: () -> Unit,
    onAddPhotos: () -> Unit,
    onAddVideo: () -> Unit,
    onAddAny: () -> Unit,
    onRetry: () -> Unit,
    onDismissFailed: () -> Unit,
    onTap: (Int, Memory) -> Unit,
    onLongPress: (Memory) -> Unit,
) {
    val today = remember { LocalDate.now() }
    val insight = remember(state.items) { memoryInsight(state.items, today) }
    val months = remember(state.items) { monthlyCounts(state.items, today) }
    val spacing = TwinTheme.spacing
    val full: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = spacing.screenEdge, end = spacing.screenEdge, bottom = spacing.xl),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        item(span = full) {
            GlassCard(modifier = Modifier.padding(bottom = spacing.s)) {
                Text(
                    "📷 " + stringResource(R.string.memories_capture),
                    style = MaterialTheme.typography.titleMedium,
                    color = TwinTheme.colors.ink,
                )
                Row(
                    modifier = Modifier.padding(top = spacing.m),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s),
                ) {
                    GlassButton(stringResource(R.string.memories_take_photo), onTakePhoto, Modifier.weight(1f), enabled = state.uploading == null)
                    GlassButton(
                        stringResource(R.string.memories_record_video),
                        onRecordVideo,
                        Modifier.weight(1f),
                        style = GlassButtonStyle.Secondary,
                        enabled = state.uploading == null,
                    )
                }
                Row(
                    modifier = Modifier.padding(top = spacing.s),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s),
                ) {
                    GlassButton(
                        stringResource(R.string.memories_add_photos),
                        onAddPhotos,
                        Modifier.weight(1f),
                        style = GlassButtonStyle.Secondary,
                        enabled = state.uploading == null,
                    )
                    GlassButton(
                        stringResource(R.string.memories_add_video),
                        onAddVideo,
                        Modifier.weight(1f),
                        style = GlassButtonStyle.Secondary,
                        enabled = state.uploading == null,
                    )
                }
            }
        }
        state.uploading?.let { progress ->
            item(span = full) { UploadStrip(progress) }
        }
        if (state.failedUploads.isNotEmpty() && state.uploading == null) {
            item(span = full) { FailedStrip(state.failedUploads.size, onRetry, onDismissFailed) }
        }
        item(span = full) { InsightCard(insight) }
        if (state.items.isNotEmpty()) {
            item(span = full) {
                GlassCard(modifier = Modifier.padding(vertical = spacing.s)) {
                    Text(
                        "📅 " + stringResource(R.string.memories_by_month),
                        style = MaterialTheme.typography.titleMedium,
                        color = TwinTheme.colors.ink,
                    )
                    MonthBars(months, modifier = Modifier.padding(top = spacing.m))
                }
            }
        }
        item(span = full) {
            Text(
                text = "💝 " + stringResource(R.string.memories_our_memories) + "  " + state.items.size,
                style = MaterialTheme.typography.titleMedium,
                color = TwinTheme.colors.ink,
                modifier = Modifier.padding(top = spacing.s, bottom = spacing.xs),
            )
        }
        item { AddTile(onAddAny) }
        itemsIndexedCompat(state.items) { index, memory ->
            MemoryTile(
                memory = memory,
                selecting = state.selection.isNotEmpty(),
                selected = memory.id in state.selection,
                onTap = { onTap(index, memory) },
                onLongPress = { onLongPress(memory) },
            )
        }
    }
}

/** `items` with the position, written out because the grid scope has no indexed version in this Compose version. */
private fun androidx.compose.foundation.lazy.grid.LazyGridScope.itemsIndexedCompat(
    list: List<Memory>,
    content: @Composable androidx.compose.foundation.lazy.grid.LazyGridItemScope.(Int, Memory) -> Unit,
) {
    items(count = list.size, key = { list[it].id }) { index -> content(index, list[index]) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AddTile(onClick: () -> Unit) {
    val colors = TwinTheme.colors
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.glass)
            .border(BorderStroke(1.dp, colors.line), RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.Add, null, tint = colors.ink)
        Text(stringResource(R.string.memories_add), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MemoryTile(
    memory: Memory,
    selecting: Boolean,
    selected: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val colors = TwinTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(colors.glass)
            .border(BorderStroke(if (selected) 2.dp else 0.dp, if (selected) colors.accent else Color.Transparent), shape)
            .combinedClickable(onClick = onTap, onLongClick = onLongPress),
    ) {
        if (memory.isVideo) {
            // Video thumbnails need an extra library; a play badge on a dark tile keeps the grid light.
            Box(Modifier.fillMaxSize().background(Color(0xFF2A0A1E)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.PlayArrow, stringResource(R.string.memories_video_cd), tint = Color.White, modifier = Modifier.size(36.dp))
            }
        } else {
            AsyncImage(
                model = memory.url,
                contentDescription = stringResource(R.string.memories_photo_cd),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (memory.favorite) {
            Icon(
                Icons.Rounded.Star,
                null,
                tint = Color(0xFFFFC857),
                modifier = Modifier.align(Alignment.BottomStart).padding(4.dp).size(16.dp),
            )
        }
        if (selecting) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(if (selected) colors.accent else Color.Black.copy(alpha = 0.4f))
                    .border(1.5.dp, Color.White, androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun InsightCard(insight: MemoryInsight) {
    val (icon, text) = when (insight) {
        MemoryInsight.AddFirst -> "💡" to stringResource(R.string.memories_insight_first)
        is MemoryInsight.ThisMonth -> "📸" to plural(
            R.plurals.memories_insight_month, insight.thisMonth, insight.thisMonth, insight.total, insight.videos,
        )
        is MemoryInsight.SoFar -> "🕰️" to stringResource(R.string.memories_insight_so_far, insight.total)
    }
    GlassCard(modifier = Modifier.padding(bottom = TwinTheme.spacing.xs), contentPadding = TwinTheme.spacing.m) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
            Text(icon, style = MaterialTheme.typography.titleLarge)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = TwinTheme.colors.ink)
        }
    }
}

@Composable
private fun MonthBars(months: List<com.twinhearts.app.feature.memories.domain.model.MonthCount>, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val max = months.maxOf { it.count }.coerceAtLeast(1)
    Row(
        modifier = modifier.fillMaxWidth().height(BAR_AREA + 40.dp),
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        verticalAlignment = Alignment.Bottom,
    ) {
        months.forEach { m ->
            val label = m.month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                if (m.count > 0) Text("${m.count}", style = MaterialTheme.typography.labelSmall, color = colors.ink)
                Box(
                    Modifier
                        .fillMaxWidth(0.7f)
                        .height((BAR_AREA * (m.count.toFloat() / max)).coerceAtLeast(3.dp))
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(if (m.count > 0) colors.accent else colors.lineSoft),
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.inkMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(top = TwinTheme.spacing.xs),
                )
            }
        }
    }
}

@Composable
private fun UploadStrip(progress: UploadProgress) {
    GlassCard(modifier = Modifier.padding(bottom = TwinTheme.spacing.s), contentPadding = TwinTheme.spacing.m) {
        Text(
            stringResource(R.string.memories_uploading, progress.done, progress.total),
            style = MaterialTheme.typography.bodyMedium,
            color = TwinTheme.colors.ink,
        )
        androidx.compose.material3.LinearProgressIndicator(
            progress = { if (progress.total == 0) 0f else progress.done.toFloat() / progress.total },
            modifier = Modifier.fillMaxWidth().padding(top = TwinTheme.spacing.s),
            color = TwinTheme.colors.accent,
            trackColor = TwinTheme.colors.lineSoft,
        )
    }
}

@Composable
private fun FailedStrip(count: Int, onRetry: () -> Unit, onDismiss: () -> Unit) {
    GlassCard(modifier = Modifier.padding(bottom = TwinTheme.spacing.s), contentPadding = TwinTheme.spacing.m) {
        Text(
            plural(R.plurals.memories_failed_uploads, count, count),
            style = MaterialTheme.typography.bodyMedium,
            color = TwinTheme.colors.error,
        )
        Row(Modifier.padding(top = TwinTheme.spacing.s), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            GlassButton(stringResource(R.string.memories_retry), onRetry, Modifier.weight(1f))
            GlassButton(stringResource(R.string.memories_dismiss), onDismiss, Modifier.weight(1f), style = GlassButtonStyle.Secondary)
        }
    }
}

/** Replaces the top bar while tiles are selected: close, how many, share, delete. */
@Composable
private fun SelectionBar(count: Int, onCancel: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = TwinTheme.spacing.s, vertical = TwinTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onCancel) { Icon(Icons.Rounded.Close, stringResource(R.string.core_cancel), tint = colors.ink) }
        Text(
            stringResource(R.string.memories_selected_count, count),
            style = MaterialTheme.typography.titleMedium,
            color = colors.ink,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onShare) { Icon(Icons.Rounded.Share, stringResource(R.string.memories_share), tint = colors.ink) }
        IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, stringResource(R.string.memories_delete), tint = colors.error) }
    }
}

/** A fresh file in the app's cache for the camera to write to (the folder Chat's camera already uses). */
private fun newCaptureFile(context: Context, extension: String): File? = try {
    val dir = File(context.cacheDir, "chat_camera").apply { mkdirs() }
    File.createTempFile("memory-", extension, dir)
} catch (e: IOException) {
    null
}

private fun fileUri(context: Context, file: File): Uri? = try {
    FileProvider.getUriForFile(context, "${context.packageName}.chatfiles", file)
} catch (e: IllegalArgumentException) {
    null
}

/** Offers the picture links to the share sheet (the web shares the links too). */
private fun shareLinks(context: Context, items: List<Memory>) {
    if (items.isEmpty()) return
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, items.joinToString("\n") { it.url })
    }
    try {
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        // Nothing on this phone can take text; nothing more to do.
    }
}

/** "Download" opens the file's address, so the browser can save it. */
private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        // No browser installed; nothing more to do.
    }
}

/** Text that changes with a count ("1 memory" / "3 memories"). */
@Composable
private fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): String =
    LocalContext.current.resources.getQuantityString(id, count, *args)