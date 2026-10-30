package com.twinhearts.app.feature.memories.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.memories.data.MemoriesRepository
import com.twinhearts.app.feature.memories.domain.model.Memory
import com.twinhearts.app.feature.memories.domain.model.UploadedMedia
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject

/** One-off result shown once as a toast (R-42: not part of the long-lived state). */
sealed interface MemoriesMessage {
    data class Added(val count: Int) : MemoriesMessage
    data class PartlyAdded(val added: Int, val failed: Int) : MemoriesMessage
    data class Deleted(val count: Int) : MemoriesMessage
    data class PartlyDeleted(val deleted: Int, val failed: Int) : MemoriesMessage
    data class Failed(val error: AppError) : MemoriesMessage
}

data class UploadProgress(val done: Int, val total: Int)

data class MemoriesUiState(
    val loading: Boolean = true,
    /** Set only when the first load failed; later errors come as a [MemoriesMessage]. */
    val error: AppError? = null,
    val items: List<Memory> = emptyList(),
    /** True while the album on screen is the saved copy because there is no connection. */
    val offline: Boolean = false,
    /** Ids picked with a long press; non-empty means the page is in "select" mode. */
    val selection: Set<String> = emptySet(),
    val uploading: UploadProgress? = null,
    /** Files that could not be uploaded; the "Retry" button sends them again. */
    val failedUploads: List<Uri> = emptyList(),
    /** True while a delete or favourite change is being saved. */
    val working: Boolean = false,
    val message: MemoriesMessage? = null,
    /** Goes up by one each time something is added; the screen plays the petals on every change. */
    val petals: Int = 0,
)

/** Loads the shared album, uploads new photos/videos, deletes, and marks favourites. */
@HiltViewModel
class MemoriesViewModel @Inject constructor(
    private val repository: MemoriesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MemoriesUiState())
    val state: StateFlow<MemoriesUiState> = _state.asStateFlow()

    /** Every album change reads the saved list first, so changes run one after another. */
    private val writeLock = Mutex()

    init {
        viewModelScope.launch {
            repository.cached()?.let { saved ->
                _state.update { if (it.items.isEmpty()) it.copy(loading = false, items = saved) else it }
            }
            fetch(silent = false)
        }
    }

    /** The refresh button and the Retry button. */
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            fetch(silent = false)
        }
    }

    /** Called every ~30 s while the page is open: picks up the partner's changes without a spinner. */
    fun refresh() {
        if (busy()) return
        viewModelScope.launch { fetch(silent = true) }
    }

    private fun busy() = _state.value.uploading != null || _state.value.working || writeLock.isLocked

    /**
     * Goes up at the start and at the end of every add / delete / favourite. A load that began before
     * the latest change brings an OLDER album (the free server can take many seconds to answer), so
     * its answer is thrown away instead of wiping the photos that were just added.
     */
    private var changes = 0

    private suspend fun fetch(silent: Boolean) {
        val startedAt = changes
        val result = repository.load()
        // A change that started or finished while this was loading has newer data; do not overwrite it.
        if (result is AppResult.Success && (busy() || changes != startedAt)) {
            _state.update { if (it.loading) it.copy(loading = false) else it }
            return
        }
        when (result) {
            is AppResult.Success -> _state.update {
                it.copy(
                    loading = false,
                    error = null,
                    offline = false,
                    items = result.data,
                    selection = it.selection.intersect(result.data.map { m -> m.id }.toSet()),
                )
            }
            is AppResult.Failure -> _state.update {
                when {
                    it.items.isNotEmpty() && result.error == AppError.Network -> it.copy(loading = false, offline = true)
                    silent -> it
                    it.items.isNotEmpty() -> it.copy(loading = false, message = MemoriesMessage.Failed(result.error))
                    else -> it.copy(loading = false, error = result.error)
                }
            }
        }
    }

    /** Uploads the picked files (three at a time, like the web), then adds them to the album in one save. */
    fun upload(uris: List<Uri>) {
        if (uris.isEmpty() || _state.value.uploading != null) return
        viewModelScope.launch {
            changes++
            _state.update { it.copy(uploading = UploadProgress(0, uris.size), failedUploads = emptyList()) }
            val gate = Semaphore(PARALLEL_UPLOADS)
            val results = coroutineScope {
                uris.map { uri ->
                    async {
                        gate.withPermit {
                            val r = repository.upload(uri)
                            _state.update { s -> s.copy(uploading = s.uploading?.let { p -> p.copy(done = p.done + 1) }) }
                            uri to r
                        }
                    }
                }.awaitAll()
            }
            val ok = results.mapNotNull { (_, r) -> (r as? AppResult.Success<UploadedMedia>)?.data }
            val failedUris = results.filter { it.second is AppResult.Failure }.map { it.first }
            val firstError = results.firstNotNullOfOrNull { (it.second as? AppResult.Failure)?.error }

            if (ok.isEmpty()) {
                changes++
                _state.update {
                    it.copy(uploading = null, failedUploads = failedUris, message = MemoriesMessage.Failed(firstError ?: AppError.Unknown))
                }
                return@launch
            }
            val saved = writeLock.withLock { repository.addToAlbum(ok) }
            changes++
            when (saved) {
                is AppResult.Success -> _state.update {
                    it.copy(
                        uploading = null,
                        items = saved.data,
                        failedUploads = failedUris,
                        petals = it.petals + 1,
                        message = if (failedUris.isEmpty()) {
                            MemoriesMessage.Added(ok.size)
                        } else {
                            MemoriesMessage.PartlyAdded(ok.size, failedUris.size)
                        },
                    )
                }
                is AppResult.Failure -> _state.update {
                    it.copy(uploading = null, failedUploads = failedUris, message = MemoriesMessage.Failed(saved.error))
                }
            }
        }
    }

    fun retryFailedUploads() = upload(_state.value.failedUploads)

    fun dismissFailedUploads() = _state.update { it.copy(failedUploads = emptyList()) }

    /** Long press on a tile starts selecting; while selecting, a tap adds or removes the tile. */
    fun toggleSelect(id: String) = _state.update {
        it.copy(selection = if (id in it.selection) it.selection - id else it.selection + id)
    }

    fun clearSelection() = _state.update { it.copy(selection = emptySet()) }

    fun deleteSelected() = delete(_state.value.selection)

    fun delete(ids: Set<String>) {
        if (ids.isEmpty() || _state.value.working) return
        viewModelScope.launch {
            changes++
            _state.update { it.copy(working = true) }
            val result = writeLock.withLock { repository.delete(ids) }
            changes++
            when (result) {
                is AppResult.Success -> _state.update {
                    val removed = ids.size - result.data.failed
                    it.copy(
                        working = false,
                        items = result.data.items,
                        selection = emptySet(),
                        message = if (result.data.failed == 0) {
                            MemoriesMessage.Deleted(removed)
                        } else {
                            MemoriesMessage.PartlyDeleted(removed, result.data.failed)
                        },
                    )
                }
                is AppResult.Failure -> _state.update {
                    it.copy(working = false, message = MemoriesMessage.Failed(result.error))
                }
            }
        }
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            changes++
            val result = writeLock.withLock { repository.toggleFavorite(id) }
            changes++
            when (result) {
                is AppResult.Success -> _state.update { it.copy(items = result.data) }
                is AppResult.Failure -> _state.update { it.copy(message = MemoriesMessage.Failed(result.error)) }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }

    private companion object {
        const val PARALLEL_UPLOADS = 3
    }
}
