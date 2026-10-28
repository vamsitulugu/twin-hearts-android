package com.twinhearts.app.feature.collection.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.collection.data.CollectionRepository
import com.twinhearts.app.feature.collection.domain.CollectionLogic
import com.twinhearts.app.feature.collection.domain.model.CollectionData
import com.twinhearts.app.feature.collection.domain.model.CollectionTab
import com.twinhearts.app.feature.collection.domain.model.PhotoFilter
import com.twinhearts.app.feature.collection.domain.model.PhotoItem
import com.twinhearts.app.feature.collection.domain.model.VoiceFilter
import com.twinhearts.app.feature.collection.domain.model.VoiceItem
import com.twinhearts.app.feature.collection.domain.model.VoiceSort
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One-off result shown once as a toast (not part of the long-lived state). */
sealed interface CollectionMessage {
    data object SavedToLibrary : CollectionMessage
    data object RemovedFromLibrary : CollectionMessage
    data object Deleted : CollectionMessage
    data object PlayFailed : CollectionMessage
    data class Failed(val error: AppError) : CollectionMessage
}

data class CollectionUiState(
    val loading: Boolean = true,
    /** Set only when the first load failed. */
    val error: AppError? = null,
    val data: CollectionData? = null,
    val tab: CollectionTab = CollectionTab.Voice,
    val voiceFilter: VoiceFilter = VoiceFilter.All,
    val voiceSort: VoiceSort = VoiceSort.Newest,
    val photoFilter: PhotoFilter = PhotoFilter.All,
    val query: String = "",
    val searchOpen: Boolean = false,
    val selectMode: Boolean = false,
    val selected: Set<String> = emptySet(),
    val libraryFavorites: Set<String> = emptySet(),
    val photoFavorites: Set<String> = emptySet(),
    val playingId: String? = null,
    /** Index into the visible photo list, or null when the viewer is closed. */
    val lightboxIndex: Int? = null,
    val message: CollectionMessage? = null,
) {
    /** Voice-tab favourites are the shared chat stars of the signed-in person. */
    val voiceFavorites: Set<String>
        get() = data?.let { d -> d.voices.filter { d.role in it.starredBy }.map { it.id }.toSet() }.orEmpty()

    /** Favourites that belong to the tab being shown. */
    val activeFavorites: Set<String>
        get() = when (tab) {
            CollectionTab.Voice -> voiceFavorites
            CollectionTab.Library -> libraryFavorites
            CollectionTab.Photos -> photoFavorites
        }

    val visibleVoices: List<VoiceItem>
        get() {
            val d = data ?: return emptyList()
            val source = if (tab == CollectionTab.Library) d.library else d.voices
            return CollectionLogic.voices(
                list = source, filter = voiceFilter, sort = voiceSort, query = query,
                favorites = activeFavorites, myRole = d.role, myName = d.myName, partnerName = d.partnerName,
            )
        }

    val visiblePhotos: List<PhotoItem>
        get() = CollectionLogic.photos(data?.photos.orEmpty(), photoFilter, query, photoFavorites)
}

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val repository: CollectionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        CollectionUiState(
            libraryFavorites = repository.libraryFavorites(),
            photoFavorites = repository.photoFavorites(),
        ),
    )
    val state: StateFlow<CollectionUiState> = _state.asStateFlow()

    private val player = VoicePlayer(
        onStateChange = { id -> _state.update { it.copy(playingId = id) } },
        onError = { _state.update { it.copy(message = CollectionMessage.PlayFailed) } },
    )

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val r = repository.load()) {
                is AppResult.Success -> _state.update { it.copy(loading = false, data = r.data) }
                is AppResult.Failure -> _state.update { it.copy(loading = false, error = r.error) }
            }
        }
    }

    // ---- toolbar ----
    fun selectTab(tab: CollectionTab) {
        player.stop()
        _state.update { it.copy(tab = tab, selectMode = false, selected = emptySet(), query = "", lightboxIndex = null) }
    }

    fun setVoiceFilter(f: VoiceFilter) = _state.update { it.copy(voiceFilter = f) }
    fun setPhotoFilter(f: PhotoFilter) = _state.update { it.copy(photoFilter = f) }
    fun setSort(s: VoiceSort) = _state.update { it.copy(voiceSort = s) }
    fun setQuery(q: String) = _state.update { it.copy(query = q) }
    fun toggleSearch() = _state.update { it.copy(searchOpen = !it.searchOpen, query = if (it.searchOpen) "" else it.query) }

    // ---- select mode ----
    fun toggleSelectMode() = _state.update { it.copy(selectMode = !it.selectMode, selected = emptySet()) }

    fun toggleSelected(id: String) = _state.update {
        it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id)
    }

    /** Long-press starts selecting with that row ticked (same single selection system as the web). */
    fun startSelecting(id: String) = _state.update { it.copy(selectMode = true, selected = it.selected + id) }

    // ---- voice ----
    fun togglePlay(item: VoiceItem) {
        val url = item.url ?: return
        player.toggle(item.id, url)
    }

    fun toggleFavorite(id: String) {
        val s = _state.value
        when (s.tab) {
            CollectionTab.Voice -> viewModelScope.launch {
                // Optimistic: flip the star locally, then ask the server; reload if it failed.
                val d = s.data ?: return@launch
                _state.update { cur ->
                    cur.copy(data = d.copy(voices = d.voices.map { v ->
                        if (v.id != id) v else v.copy(
                            starredBy = if (d.role in v.starredBy) v.starredBy - d.role else v.starredBy + d.role,
                        )
                    }))
                }
                if (repository.toggleVoiceStar(id) is AppResult.Failure) load()
            }
            CollectionTab.Library -> {
                val next = s.libraryFavorites.let { if (id in it) it - id else it + id }
                repository.setLibraryFavorites(next)
                _state.update { it.copy(libraryFavorites = next) }
            }
            CollectionTab.Photos -> {
                val next = s.photoFavorites.let { if (id in it) it - id else it + id }
                repository.setPhotoFavorites(next)
                _state.update { it.copy(photoFavorites = next) }
            }
        }
    }

    fun saveToLibrary(items: List<VoiceItem>) {
        viewModelScope.launch {
            when (val r = repository.saveToLibrary(items)) {
                is AppResult.Success -> {
                    _state.update { it.copy(message = CollectionMessage.SavedToLibrary, selectMode = false, selected = emptySet()) }
                    load()
                }
                is AppResult.Failure -> _state.update { it.copy(message = CollectionMessage.Failed(r.error)) }
            }
        }
    }

    /** Deletes one or many: chat voice messages are hidden for me, Library entries are removed. */
    fun delete(ids: Set<String>) {
        if (ids.isEmpty()) return
        val tab = _state.value.tab
        viewModelScope.launch {
            val result: AppResult<Unit> = if (tab == CollectionTab.Library) {
                repository.removeFromLibrary(ids)
            } else {
                var failure: AppResult.Failure? = null
                for (id in ids) {
                    val r = repository.deleteVoice(id)
                    if (r is AppResult.Failure) failure = r
                }
                failure ?: AppResult.Success(Unit)
            }
            when (result) {
                is AppResult.Success -> {
                    _state.update { it.copy(message = CollectionMessage.Deleted, selectMode = false, selected = emptySet()) }
                    load()
                }
                is AppResult.Failure -> _state.update { it.copy(message = CollectionMessage.Failed(result.error)) }
            }
        }
    }

    // ---- photos ----
    fun openLightbox(index: Int) = _state.update { it.copy(lightboxIndex = index) }
    fun closeLightbox() = _state.update { it.copy(lightboxIndex = null) }

    fun messageShown() = _state.update { it.copy(message = null) }

    override fun onCleared() {
        player.stop()
        super.onCleared()
    }
}
