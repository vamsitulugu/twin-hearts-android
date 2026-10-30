package com.twinhearts.app.feature.bucket.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.SerifFamily
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.feature.bucket.domain.model.BucketDream
import com.twinhearts.app.feature.bucket.domain.model.BucketVisibility
import com.twinhearts.app.feature.bucket.domain.progressLabel
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Bucket List page (web: `#page-bucket`). One card: title, done/total pill, "+ Add Dream",
 * then the dreams. Tap the circle to tick one off; long-press a row to select several and delete.
 * Dreams live in the couple's shared state, so the web app and this app show the same list.
 */
@Composable
fun BucketScreen(
    onBack: () -> Unit,
    viewModel: BucketViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<BucketDream?>(null) }
    var confirmBulk by rememberSaveable { mutableStateOf(false) }

    // Back leaves selection mode first, like the web.
    BackHandler(enabled = state.selecting) { viewModel.clearSelection() }

    val message = state.message
    val messageText = when (message) {
        null -> null
        BucketMessage.Added -> stringResource(R.string.bucket_added)
        is BucketMessage.Failed -> message.error.asText()
    }
    LaunchedEffect(message) {
        if (message == null) return@LaunchedEffect
        if (message == BucketMessage.Added) showAdd = false
        Toast.makeText(context, messageText.orEmpty(), Toast.LENGTH_SHORT).show()
        viewModel.messageShown()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TwinTopBar(
            title = stringResource(R.string.bucket_title),
            showSidebarButton = true,
            onSyncClick = viewModel::load,
        )
        val error = state.error
        when {
            state.loading && state.dreams.isEmpty() -> {
                Column(modifier = Modifier.padding(TwinTheme.spacing.screenEdge)) { LoadingSkeleton(lines = 4) }
            }
            error != null && state.dreams.isEmpty() -> {
                ErrorState(message = error.asText(), onRetry = viewModel::load)
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = TwinTheme.spacing.screenEdge)
                        .padding(bottom = TwinTheme.spacing.xl),
                ) {
                    GlassCard {
                        if (state.selecting) {
                            SelectionBar(
                                count = state.selected.size,
                                onExit = viewModel::clearSelection,
                                onDelete = { confirmBulk = true },
                            )
                        } else {
                            Header(progress = progressLabel(state.dreams), onAdd = { showAdd = true })
                        }
                        if (state.dreams.isEmpty()) {
                            EmptyState(
                                title = stringResource(R.string.bucket_empty_title),
                                message = stringResource(R.string.bucket_empty_hint),
                                icon = Icons.Rounded.AutoAwesome,
                                actionLabel = stringResource(R.string.bucket_add_dream),
                                onAction = { showAdd = true },
                            )
                        } else {
                            state.dreams.forEach { dream ->
                                DreamRow(
                                    dream = dream,
                                    selecting = state.selecting,
                                    selected = dream.id in state.selected,
                                    onToggleDone = { viewModel.toggleDone(dream.id) },
                                    onLongPress = { viewModel.startSelecting(dream.id) },
                                    onTap = { if (state.selecting) viewModel.toggleSelected(dream.id) },
                                    onDelete = { deleteTarget = dream },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddDreamSheet(saving = state.saving, onSave = viewModel::addDream, onDismiss = { showAdd = false })
    }
    deleteTarget?.let { dream ->
        ConfirmDialog(
            title = stringResource(R.string.bucket_delete_title),
            message = stringResource(R.string.bucket_delete_message, dream.title),
            onConfirm = {
                viewModel.deleteOne(dream.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }
    if (confirmBulk) {
        val count = state.selected.size
        ConfirmDialog(
            title = pluralStringResource(R.plurals.bucket_delete_many_title, count, count),
            message = stringResource(R.string.bucket_delete_many_message),
            onConfirm = {
                viewModel.deleteSelected()
                confirmBulk = false
            },
            onDismiss = { confirmBulk = false },
        )
    }
}

@Composable
private fun Header(progress: String, onAdd: () -> Unit) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        Icon(Icons.Rounded.Checklist, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(20.dp))
        Text(
            text = stringResource(R.string.bucket_card_title),
            style = MaterialTheme.typography.titleLarge,
            fontFamily = SerifFamily,
            color = colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = progress,
            style = MaterialTheme.typography.labelMedium,
            color = colors.ink,
            modifier = Modifier
                .clip(TwinTheme.shapes.pill)
                .background(colors.glass)
                .border(1.dp, colors.lineSoft, TwinTheme.shapes.pill)
                .padding(horizontal = TwinTheme.spacing.s, vertical = TwinTheme.spacing.xs),
        )
        Box(
            modifier = Modifier
                .heightIn(min = TwinTheme.shapes.minTouch)
                .clip(TwinTheme.shapes.pill)
                .background(colors.neutralGlass.copy(alpha = 0.35f))
                .border(1.dp, colors.lineSoft, TwinTheme.shapes.pill)
                .clickable(role = Role.Button, onClick = onAdd)
                .padding(horizontal = TwinTheme.spacing.m),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.bucket_add_dream_plus),
                style = MaterialTheme.typography.titleSmall,
                color = colors.ink,
            )
        }
    }
}

@Composable
private fun SelectionBar(count: Int, onExit: () -> Unit, onDelete: () -> Unit) {
    val colors = TwinTheme.colors
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onExit) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.bucket_exit_selection), tint = colors.ink)
        }
        Text(
            text = stringResource(R.string.bucket_selected_count, count),
            style = MaterialTheme.typography.titleMedium,
            color = colors.ink,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.bucket_delete_selected), tint = colors.error)
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun DreamRow(
    dream: BucketDream,
    selecting: Boolean,
    selected: Boolean,
    onToggleDone: () -> Unit,
    onLongPress: () -> Unit,
    onTap: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.small
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = TwinTheme.spacing.s)
            .clip(shape)
            .background(if (selected) colors.accent.copy(alpha = 0.25f) else colors.glass)
            .border(1.dp, if (selected) colors.accent else colors.lineSoft, shape)
            .combinedClickable(role = Role.Button, onClick = onTap, onLongClick = onLongPress)
            .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.xs)
            .heightIn(min = TwinTheme.shapes.minTouch),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        val doneText = stringResource(
            if (dream.done) R.string.bucket_mark_not_done else R.string.bucket_mark_done,
            dream.title,
        )
        Box(
            modifier = Modifier
                .size(TwinTheme.shapes.minTouch)
                .clickable(enabled = !selecting, role = Role.Checkbox, onClick = onToggleDone)
                .semantics { contentDescription = doneText },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(TwinTheme.shapes.circle)
                    .background(if (dream.done) colors.accent else Color.Transparent)
                    .border(2.dp, if (dream.done) colors.accent else colors.inkFaint, TwinTheme.shapes.circle),
                contentAlignment = Alignment.Center,
            ) {
                if (dream.done) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.ink, modifier = Modifier.size(16.dp))
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${dream.category.emoji} ${dream.title}",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.ink,
                textDecoration = if (dream.done) TextDecoration.LineThrough else null,
                modifier = Modifier.alpha(if (dream.done) 0.6f else 1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(dream.visibility.badgeRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (dream.visibility == BucketVisibility.Both) colors.inkFaint else colors.pinkSoft,
                )
                dream.date?.let {
                    Text(
                        text = it.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.inkFaint,
                    )
                }
            }
        }
        if (!selecting) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.bucket_delete_one), tint = colors.inkFaint)
            }
        }
    }
}

@Composable
private fun ConfirmDialog(title: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TwinTheme.colors.card,
        title = { Text(title, color = TwinTheme.colors.ink) },
        text = { Text(message, color = TwinTheme.colors.inkMuted) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.bucket_delete_confirm), color = TwinTheme.colors.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.core_cancel)) } },
    )
}
