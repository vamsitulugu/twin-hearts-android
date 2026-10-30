package com.twinhearts.app.feature.collection.ui

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.feature.collection.domain.CollectionLogic
import com.twinhearts.app.feature.collection.domain.model.CollectionTab
import com.twinhearts.app.feature.collection.domain.model.PhotoItem

private val Gold = androidx.compose.ui.graphics.Color(0xFFF5B82E)

/**
 * Collection page (web: collection.html): Voice Messages, Library and Photo Collection.
 * Layout follows docs/reference/images.pdf page 35.
 */
@Composable
fun CollectionScreen(
    onBack: () -> Unit,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // One-off toasts (R-42).
    val message = state.message
    val text = when (message) {
        null -> null
        CollectionMessage.SavedToLibrary -> stringResource(R.string.collection_msg_saved)
        CollectionMessage.RemovedFromLibrary -> stringResource(R.string.collection_msg_removed)
        CollectionMessage.Deleted -> stringResource(R.string.collection_msg_deleted)
        CollectionMessage.PlayFailed -> stringResource(R.string.collection_msg_play_failed)
        is CollectionMessage.Failed -> stringResource(R.string.collection_msg_failed)
    }
    LaunchedEffect(message) {
        if (text != null) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            viewModel.messageShown()
        }
    }

    TwinBackground {
        Column(Modifier.fillMaxSize()) {
            TwinTopBar(
                title = stringResource(R.string.collection_title),
                onBackClick = onBack,
                onSearchClick = { viewModel.toggleSearch() },
                onSyncClick = { viewModel.load() },
            )
            CollectionTabs(state.tab, viewModel::selectTab)

            val data = state.data
            when {
                state.loading && data == null -> Column(
                    Modifier.fillMaxSize().padding(TwinTheme.spacing.screenEdge),
                    verticalArrangement = Arrangement.Center,
                ) { LoadingSkeleton(lines = 4) }

                state.error != null && data == null -> ErrorState(
                    message = state.error!!.asText(),
                    onRetry = { viewModel.load() },
                )

                data != null -> Content(state, viewModel, context)
            }
        }

        val photos = state.visiblePhotos
        state.lightboxIndex?.let { index ->
            if (photos.isNotEmpty()) {
                PhotoLightbox(
                    photos = photos,
                    startIndex = index,
                    favorites = state.photoFavorites,
                    onFavorite = viewModel::toggleFavorite,
                    onDownload = { download(context, it) },
                    onOpenVideo = { openVideo(context, it) },
                    onClose = viewModel::closeLightbox,
                )
            }
        }
    }
}

@Composable
private fun Content(state: CollectionUiState, vm: CollectionViewModel, context: Context) {
    val data = state.data ?: return
    val spacing = TwinTheme.spacing
    val isVoiceLike = state.tab != CollectionTab.Photos
    val isLibrary = state.tab == CollectionTab.Library

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing.m)) {
        StatGrid(statsFor(state), Modifier.padding(horizontal = spacing.l))

        if (state.searchOpen) {
            TwinTextField(
                value = state.query,
                onValueChange = vm::setQuery,
                label = stringResource(R.string.core_search),
                placeholder = stringResource(
                    if (isVoiceLike) R.string.collection_search_hint else R.string.collection_search_photos_hint,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.l),
            )
        }

        if (isVoiceLike) {
            VoiceToolbar(
                filter = state.voiceFilter,
                sort = state.voiceSort,
                selectMode = state.selectMode,
                onFilter = vm::setVoiceFilter,
                onSort = vm::setSort,
                onToggleSelect = vm::toggleSelectMode,
            )
        } else {
            PhotoToolbar(state.photoFilter, vm::setPhotoFilter)
        }

        if (state.selectMode && state.selected.isNotEmpty()) {
            val chosen = data.voices.filter { it.id in state.selected }
            BulkBar(
                count = state.selected.size,
                canSave = !isLibrary,
                onSave = { vm.saveToLibrary(chosen) },
                onDelete = { vm.delete(state.selected) },
                onCancel = vm::toggleSelectMode,
            )
        }

        if (isVoiceLike) {
            val source = if (isLibrary) data.library else data.voices
            val visible = state.visibleVoices
            when {
                visible.isEmpty() -> EmptyNote(
                    icon = if (isLibrary) Icons.Rounded.Bookmarks else Icons.Rounded.Mic,
                    message = stringResource(
                        when {
                            source.isNotEmpty() -> R.string.collection_empty_filtered_voice
                            isLibrary -> R.string.collection_empty_library
                            else -> R.string.collection_empty_voice
                        },
                    ),
                )
                else -> VoiceList(
                    voices = visible,
                    isLibrary = isLibrary,
                    favorites = state.activeFavorites,
                    libraryIds = data.library.map { it.id }.toSet(),
                    selectMode = state.selectMode,
                    selected = state.selected,
                    playingId = state.playingId,
                    myRole = data.role,
                    myName = data.myName,
                    partnerName = data.partnerName,
                    onPlay = vm::togglePlay,
                    onFavorite = vm::toggleFavorite,
                    onSave = { vm.saveToLibrary(listOf(it)) },
                    onDelete = { vm.delete(setOf(it)) },
                    onTapRow = { id -> if (state.selectMode) vm.toggleSelected(id) },
                    onLongPressRow = vm::startSelecting,
                )
            }
        } else {
            val visible = state.visiblePhotos
            if (visible.isEmpty()) {
                EmptyNote(
                    icon = Icons.Rounded.PhotoLibrary,
                    message = stringResource(
                        if (data.photos.isNotEmpty()) R.string.collection_empty_filtered_photos else R.string.collection_empty_photos,
                    ),
                )
            } else {
                PhotoGrid(visible, state.photoFavorites, onOpen = vm::openLightbox, onFavorite = vm::toggleFavorite)
            }
        }
    }
}

/** The four hero numbers, per tab (same rules as the web `renderHero`). */
@Composable
private fun statsFor(state: CollectionUiState): List<Triple<String, String, androidx.compose.ui.graphics.Color?>> {
    val data = state.data ?: return emptyList()
    return when (state.tab) {
        CollectionTab.Voice -> listOf(
            Triple(data.voices.size.toString(), stringResource(R.string.collection_stat_voice_msgs), null),
            Triple(state.voiceFavorites.size.toString(), stringResource(R.string.collection_stat_favorites), Gold),
            Triple(CollectionLogic.totalDuration(data.voices.sumOf { it.durationSec }), stringResource(R.string.collection_stat_total_duration), null),
            Triple(data.voices.count { it.by == data.role }.toString(), stringResource(R.string.collection_stat_my_messages), null),
        )
        CollectionTab.Library -> listOf(
            Triple(data.library.size.toString(), stringResource(R.string.collection_stat_saved), null),
            Triple(data.library.count { it.id in state.libraryFavorites }.toString(), stringResource(R.string.collection_stat_favorites), Gold),
            Triple(CollectionLogic.totalDuration(data.library.sumOf { it.durationSec }), stringResource(R.string.collection_stat_total_duration), null),
            Triple(data.library.count { it.by == data.role }.toString(), stringResource(R.string.collection_stat_mine), null),
        )
        CollectionTab.Photos -> listOf(
            Triple(data.photos.size.toString(), stringResource(R.string.collection_stat_total_photos), null),
            Triple(data.photos.count { it.id in state.photoFavorites }.toString(), stringResource(R.string.collection_stat_favorites), Gold),
            Triple(data.photos.count { it.isVideo }.toString(), stringResource(R.string.collection_stat_videos), null),
            Triple(data.photos.count { it.fromChat }.toString(), stringResource(R.string.collection_stat_from_chat), null),
        )
    }
}

/** Icon + one muted sentence, as in the reference's empty state. */
@Composable
private fun EmptyNote(icon: ImageVector, message: String) {
    val colors = TwinTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(top = TwinTheme.spacing.xxl, start = TwinTheme.spacing.xl, end = TwinTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.l),
    ) {
        Icon(icon, null, tint = colors.ink, modifier = Modifier.size(44.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted, textAlign = TextAlign.Center)
    }
}

/** Saves a photo to the phone's Downloads with the system DownloadManager (no permission needed). */
private fun download(context: Context, photo: PhotoItem) {
    try {
        val name = "twin-hearts-${photo.id}.${if (photo.isVideo) "mp4" else "jpg"}"
        val request = DownloadManager.Request(Uri.parse(photo.url))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
        (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
        Toast.makeText(context, R.string.collection_msg_download_started, Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, R.string.collection_msg_failed, Toast.LENGTH_SHORT).show()
    }
}

private fun openVideo(context: Context, photo: PhotoItem) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(photo.url)).setDataAndType(Uri.parse(photo.url), "video/*"),
        )
    } catch (e: Exception) {
        Toast.makeText(context, R.string.collection_msg_failed, Toast.LENGTH_SHORT).show()
    }
}
