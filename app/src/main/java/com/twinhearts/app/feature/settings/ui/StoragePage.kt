package com.twinhearts.app.feature.settings.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.settings.domain.formatBytes
import com.twinhearts.app.feature.settings.domain.model.StorageKind
import com.twinhearts.app.feature.settings.domain.model.StorageSummary
import com.twinhearts.app.feature.settings.ui.components.SettingsConfirmDialog
import com.twinhearts.app.feature.settings.ui.components.SettingsRow
import com.twinhearts.app.feature.settings.ui.components.SettingsSection
import com.twinhearts.app.feature.settings.ui.components.SubPageHeader
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Why this file exists: Storage & Data (web `#setSub-storage`): usage ring, manage buttons, backup and cloud sync. */

private val RingSize = 120.dp
private val RingStroke = 10.dp
private val CategoryBarHeight = 6.dp
private val ActionTileHeight = 84.dp

private enum class StorageDialog { ClearCache, Restore }

@Composable
internal fun StoragePage(
    state: SettingsUiState,
    actions: SettingsActions,
    launchers: SettingsLaunchers,
    pendingRestore: android.net.Uri?,
    onRestoreHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dialog by rememberSaveable { mutableStateOf<StorageDialog?>(null) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = TwinTheme.spacing.screenEdge)
            .padding(bottom = TwinTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.l),
    ) {
        SubPageHeader(stringResource(R.string.settings_storage), stringResource(R.string.settings_storage_head), actions::onBack)

        when (val storage = state.storage) {
            UiState.Loading, UiState.Empty -> {
                Text(stringResource(R.string.settings_calculating), style = MaterialTheme.typography.bodyMedium, color = TwinTheme.colors.inkFaint)
                LoadingSkeleton(lines = 4)
            }
            is UiState.Error -> ErrorState(storage.error.settingsText(), actions::onRetryStorage)
            is UiState.Content -> {
                UsageCard(storage.data)
                CategoryList(storage.data)
                ManageSection(
                    busy = state.storageBusy,
                    onClearCache = { dialog = StorageDialog.ClearCache },
                    onExportPhotos = launchers.choosePhotoFolder,
                    onFullBackup = launchers.createBackupFile,
                )
            }
        }

        SettingsSection(title = stringResource(R.string.settings_backup_sync)) {
            SettingsRow(
                icon = Icons.Rounded.Upload,
                iconTint = colorResource(R.color.settings_ico_storage),
                title = stringResource(R.string.settings_export_backup),
                subtitle = stringResource(R.string.settings_export_backup_sub),
                onClick = if (state.storageBusy) null else launchers.createBackupFile,
            )
            SettingsRow(
                icon = Icons.Rounded.Download,
                iconTint = colorResource(R.color.settings_ico_storage),
                title = stringResource(R.string.settings_import_backup),
                subtitle = stringResource(R.string.settings_import_backup_sub),
                onClick = if (state.storageBusy) null else launchers.chooseBackupFile,
            )
            SettingsRow(
                icon = Icons.Rounded.Cloud,
                iconTint = colorResource(R.color.settings_ico_storage),
                title = stringResource(R.string.settings_cloud_sync),
                subtitle = state.prefs.lastBackup?.let { stringResource(R.string.settings_last_backup, it) }
                    ?: stringResource(R.string.settings_synced_to),
                showChevron = false,
                trailing = {
                    GlassButton(
                        text = stringResource(R.string.settings_sync_now),
                        onClick = actions::onSync,
                        style = GlassButtonStyle.Secondary,
                    )
                },
            )
        }
    }

    // A backup file was chosen: ask before it replaces the shared data.
    LaunchedEffect(pendingRestore) { if (pendingRestore != null) dialog = StorageDialog.Restore }

    when (dialog) {
        StorageDialog.ClearCache -> SettingsConfirmDialog(
            title = null,
            body = stringResource(R.string.settings_clear_cache_confirm),
            confirmLabel = stringResource(R.string.settings_clear),
            danger = false,
            onConfirm = {
                dialog = null
                actions.onClearCache()
            },
            onDismiss = { dialog = null },
        )
        StorageDialog.Restore -> SettingsConfirmDialog(
            title = stringResource(R.string.settings_restore_title),
            body = stringResource(R.string.settings_restore_body),
            confirmLabel = stringResource(R.string.settings_restore),
            onConfirm = {
                dialog = null
                pendingRestore?.let(actions::onImportBackup)
                onRestoreHandled()
            },
            onDismiss = {
                dialog = null
                onRestoreHandled()
            },
        )
        null -> Unit
    }
}

@Composable
private fun UsageCard(summary: StorageSummary) {
    val colors = TwinTheme.colors
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            val ringBrush = Brush.linearGradient(listOf(colors.accent, colors.brand))
            val track = colors.lineSoft
            Box(modifier = Modifier.size(RingSize), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = RingStroke.toPx(), cap = StrokeCap.Round)
                    val inset = RingStroke.toPx() / 2
                    val arc = Size(size.width - inset * 2, size.height - inset * 2)
                    drawArc(track, 0f, 360f, false, topLeft = Offset(inset, inset), size = arc, style = stroke)
                    drawArc(ringBrush, -90f, 360f * summary.percent / 100f, false, topLeft = Offset(inset, inset), size = arc, style = stroke)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${summary.percent}%", style = MaterialTheme.typography.headlineSmall, color = colors.ink)
                    Text(stringResource(R.string.settings_used), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Figure(formatBytes(summary.totalBytes), R.string.settings_used)
                Figure(formatBytes(summary.availableBytes), R.string.settings_available)
                Figure(formatBytes(summary.capacityBytes), R.string.settings_total)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CategoryBarHeight)
                    .clip(TwinTheme.shapes.pill)
                    .background(colors.lineSoft),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(summary.percent / 100f)
                        .height(CategoryBarHeight)
                        .clip(TwinTheme.shapes.pill)
                        .background(Brush.linearGradient(listOf(colors.accent, colors.brand))),
                )
            }
            Text(
                text = stringResource(R.string.settings_last_sync, LocalTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkFaint,
            )
        }
    }
}

@Composable
private fun Figure(value: String, @StringRes label: Int) {
    val colors = TwinTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = colors.ink)
        Text(stringResource(label).uppercase(), style = MaterialTheme.typography.labelSmall, color = colors.inkFaint)
    }
}

@Composable
private fun CategoryList(summary: StorageSummary) {
    val colors = TwinTheme.colors
    val biggest = maxOf(summary.categories.maxOfOrNull { it.bytes } ?: 1L, 1L)
    Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
        summary.categories.forEach { usage ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                Text(categoryEmoji(usage.kind), style = MaterialTheme.typography.titleMedium)
                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(categoryName(usage.kind)), style = MaterialTheme.typography.bodyMedium, color = colors.ink)
                        Text(formatBytes(usage.bytes), style = MaterialTheme.typography.bodySmall, color = colors.inkFaint)
                    }
                    Box(
                        modifier = Modifier
                            .padding(top = TwinTheme.spacing.xs)
                            .fillMaxWidth((usage.bytes.toFloat() / biggest).coerceIn(0f, 1f))
                            .height(CategoryBarHeight)
                            .clip(TwinTheme.shapes.pill)
                            .background(colors.accent),
                    )
                }
            }
        }
    }
}

private fun categoryEmoji(kind: StorageKind): String = when (kind) {
    StorageKind.Photos -> "📸"
    StorageKind.Voice -> "🎙️"
    StorageKind.Chat -> "💬"
    StorageKind.Notes -> "📝"
    StorageKind.AppData -> "⚙️"
}

@StringRes
private fun categoryName(kind: StorageKind): Int = when (kind) {
    StorageKind.Photos -> R.string.settings_cat_photos
    StorageKind.Voice -> R.string.settings_cat_voice
    StorageKind.Chat -> R.string.settings_cat_chat
    StorageKind.Notes -> R.string.settings_cat_notes
    StorageKind.AppData -> R.string.settings_cat_app
}

@Composable
private fun ManageSection(busy: Boolean, onClearCache: () -> Unit, onExportPhotos: () -> Unit, onFullBackup: () -> Unit) {
    val colors = TwinTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            Text(stringResource(R.string.settings_manage_storage), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
            if (busy) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = colors.ink)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            ActionTile("🧹", R.string.settings_clear_cache, busy, onClearCache, Modifier.weight(1f))
            ActionTile("📸", R.string.settings_export_photos, busy, onExportPhotos, Modifier.weight(1f))
            ActionTile("📦", R.string.settings_full_backup, busy, onFullBackup, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ActionTile(emoji: String, @StringRes label: Int, busy: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.card
    Column(
        modifier = modifier
            .height(ActionTileHeight)
            .clip(shape)
            .background(colors.glass)
            .border(1.dp, colors.line, shape)
            .clickable(enabled = !busy, role = Role.Button, onClick = onClick)
            .padding(TwinTheme.spacing.s),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(emoji, style = MaterialTheme.typography.titleLarge)
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = colors.ink, textAlign = TextAlign.Center)
    }
}
