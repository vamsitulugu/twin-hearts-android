package com.twinhearts.app.feature.vault.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.twinhearts.app.R
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.PetalBurst
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.feature.vault.domain.model.VaultItem
import com.twinhearts.app.feature.vault.domain.model.VaultKind

/**
 * Secret Vault (web: `#page-vault`): a 4-digit PIN, then private photos and videos kept in the
 * couple's private storage. The server judges every PIN; leaving the page locks it again.
 */
@Composable
fun VaultScreen(
    onBack: () -> Unit,
    viewModel: VaultViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var viewerIndex by remember { mutableStateOf<Int?>(null) }
    var deleteTarget by remember { mutableStateOf<VaultItem?>(null) }

    SecurePageEffect()

    // Menu and search are built in a later step everywhere, so they say so (same as Fights).
    val pending = { Toast.makeText(context, R.string.core_pending_message, Toast.LENGTH_SHORT).show() }

    // One-off messages: toast once, then clear.
    val message = state.message
    val messageText = when (message) {
        null -> null
        is VaultMessage.Done -> stringResource(message.textRes)
        is VaultMessage.Failed -> message.error.asText()
        is VaultMessage.Uploaded -> when {
            message.failed > 0 -> stringResource(R.string.vault_upload_partial, message.added, message.failed)
            message.kind == VaultKind.Video -> stringResource(R.string.vault_video_added)
            else -> stringResource(R.string.vault_photo_added)
        }
    }
    LaunchedEffect(message) {
        if (message == null) return@LaunchedEffect
        Toast.makeText(context, messageText.orEmpty(), Toast.LENGTH_SHORT).show()
        viewModel.messageShown()
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        TwinTopBar(
            title = stringResource(R.string.vault_title),
            onBackClick = onBack,
            onMenuClick = pending,
            onSearchClick = pending,
            onSyncClick = viewModel::refresh,
        )
        when (state.stage) {
            VaultStage.Checking -> Column(modifier = Modifier.padding(TwinTheme.spacing.screenEdge)) { LoadingSkeleton(lines = 3) }
            VaultStage.Enter, VaultStage.Setup, VaultStage.Confirm -> VaultLockContent(
                state = state,
                onKey = viewModel::pinKey,
                onDelete = viewModel::pinDelete,
                onSubmit = viewModel::submitPin,
                onForgot = viewModel::openReset,
            )
            VaultStage.Open -> VaultOpenContent(
                state = state,
                onLock = viewModel::lock,
                onRetry = viewModel::loadItems,
                onAdd = viewModel::add,
                onOpen = { viewerIndex = it },
                onDelete = { deleteTarget = it },
            )
        }
    }

    PetalBurst(trigger = state.petals)
    }

    if (state.resetOpen) {
        VaultResetDialog(
            busy = state.busy,
            problem = state.resetProblem,
            onSubmit = viewModel::resetPin,
            onDismiss = viewModel::closeReset,
        )
    }
    viewerIndex?.let { index ->
        VaultViewer(items = state.items, startIndex = index, onClose = { viewerIndex = null })
    }
    deleteTarget?.let { item ->
        val noun = stringResource(if (item.kind == VaultKind.Video) R.string.vault_noun_video else R.string.vault_noun_photo)
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = TwinTheme.colors.card,
            title = { Text(stringResource(R.string.vault_delete_title, noun), color = TwinTheme.colors.ink) },
            text = { Text(stringResource(R.string.vault_delete_message, noun), color = TwinTheme.colors.inkMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(item.id)
                        deleteTarget = null
                    },
                ) { Text(stringResource(R.string.vault_delete_confirm), color = TwinTheme.colors.error) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.core_cancel)) } },
        )
    }
}

/** Web `#vaultOpen`: the "🔐 Vault" card with + Photo, + Video and Lock, then the 3-column grid. */
@Composable
private fun VaultOpenContent(
    state: VaultUiState,
    onLock: () -> Unit,
    onRetry: () -> Unit,
    onAdd: (List<android.net.Uri>, VaultKind) -> Unit,
    onOpen: (Int) -> Unit,
    onDelete: (VaultItem) -> Unit,
) {
    // The system photo picker needs no storage permission.
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        onAdd(uris, VaultKind.Photo)
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onAdd(listOf(uri), VaultKind.Video)
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = TwinTheme.spacing.screenEdge,
            end = TwinTheme.spacing.screenEdge,
            bottom = TwinTheme.spacing.xl,
        ),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            GlassCard(modifier = Modifier.padding(bottom = TwinTheme.spacing.s)) {
                Text(stringResource(R.string.vault_card_title), style = MaterialTheme.typography.titleMedium, color = TwinTheme.colors.ink)
                Text(stringResource(R.string.vault_card_sub), style = MaterialTheme.typography.bodySmall, color = TwinTheme.colors.inkFaint)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = TwinTheme.spacing.m),
                    horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
                ) {
                    GlassButton(
                        text = stringResource(R.string.vault_add_photo),
                        onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.weight(1f),
                        enabled = !state.uploading,
                    )
                    GlassButton(
                        text = stringResource(R.string.vault_add_video),
                        onClick = { videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                        modifier = Modifier.weight(1f),
                        style = GlassButtonStyle.Secondary,
                        enabled = !state.uploading,
                    )
                    GlassButton(
                        text = stringResource(R.string.vault_lock),
                        onClick = onLock,
                        modifier = Modifier.weight(1f),
                        style = GlassButtonStyle.Secondary,
                    )
                }
                if (state.uploading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = TwinTheme.spacing.m),
                        color = TwinTheme.colors.accent,
                    )
                    Text(
                        stringResource(R.string.vault_working),
                        style = MaterialTheme.typography.bodySmall,
                        color = TwinTheme.colors.inkMuted,
                        modifier = Modifier.padding(top = TwinTheme.spacing.xs),
                    )
                }
            }
        }
        when {
            state.itemsLoading && state.items.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) { LoadingSkeleton(lines = 3) }
            state.itemsError != null && state.items.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                ErrorState(message = state.itemsError.asText(), onRetry = onRetry)
            }
            state.items.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    title = stringResource(R.string.vault_empty_title),
                    message = stringResource(R.string.vault_empty_hint),
                    icon = Icons.Rounded.Lock,
                )
            }
            else -> items(state.items, key = { it.id }) { item ->
                VaultTile(
                    item = item,
                    onOpen = { onOpen(state.items.indexOf(item)) },
                    onDelete = { onDelete(item) },
                )
            }
        }
    }
}

@Composable
private fun VaultTile(item: VaultItem, onOpen: () -> Unit, onDelete: () -> Unit) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.small
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(colors.glass)
            .border(1.dp, colors.line, shape)
            .clickable(role = Role.Button, onClick = onOpen),
    ) {
        when (item.kind) {
            VaultKind.Photo -> AsyncImage(
                model = item.url,
                contentDescription = stringResource(R.string.vault_photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // A video has no preview picture here, so it shows a play mark on a dark tile.
            VaultKind.Video -> Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.vault_video),
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(36.dp),
        ) {
            Icon(
                Icons.Rounded.Delete,
                contentDescription = stringResource(R.string.vault_delete),
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .background(Color.Black.copy(alpha = 0.45f), TwinTheme.shapes.circle)
                    .padding(3.dp),
            )
        }
    }
}
