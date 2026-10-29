package com.twinhearts.app.feature.places.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.feature.places.data.PlacesRepository
import com.twinhearts.app.feature.places.domain.formatCoordinate
import com.twinhearts.app.feature.places.domain.isValidPlaceName
import com.twinhearts.app.feature.places.domain.mapsUrl
import com.twinhearts.app.feature.places.domain.model.MemoryNote
import com.twinhearts.app.feature.places.domain.model.PlaceMemory
import com.twinhearts.app.feature.places.domain.newPlaceId
import com.twinhearts.app.feature.places.domain.parseCoordinate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random

private const val MAX_PARALLEL_UPLOADS = 3
private const val LAT_LIMIT = 90.0
private const val LNG_LIMIT = 180.0

/**
 * Why this file exists: holds the Places page state and does what each tap means. Every change to
 * the list is shown at once, then sent to the server; if sending fails the list is read again from
 * the server so the screen never lies. Saves run one after another so they cannot overtake each other.
 */
@HiltViewModel
class PlacesViewModel @Inject constructor(
    private val repo: PlacesRepository,
) : ViewModel(), PlacesActions {

    private val _state = MutableStateFlow(PlacesUiState())
    val state: StateFlow<PlacesUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<PlacesEffect>(extraBufferCapacity = 8)
    val effects: SharedFlow<PlacesEffect> = _effects.asSharedFlow()

    private val saveLock = Mutex()
    private var loadJob: Job? = null

    init { load(silent = false) }

    private fun toast(res: Int) { _effects.tryEmit(PlacesEffect.Toast(res)) }

    // ---- loading and saving the list ----

    private fun load(silent: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (!silent) _state.update { it.copy(places = UiState.Loading) }
            val who = repo.identity()
            if (who == null) {
                _state.update { it.copy(places = UiState.Empty) }
                toast(R.string.places_toast_not_connected)
                return@launch
            }
            _state.update { it.copy(identity = who) }
            when (val r = repo.load(who)) {
                is AppResult.Success -> _state.update { it.copy(places = listState(r.data)) }
                is AppResult.Failure -> _state.update { s ->
                    if (silent && s.places is UiState.Content) s else s.copy(places = UiState.Error(r.error))
                }
            }
        }
    }

    private fun listState(list: List<PlaceMemory>): UiState<List<PlaceMemory>> =
        if (list.isEmpty()) UiState.Empty else UiState.Content(list)

    /** Shows [next] now and sends it to the server in the background. */
    private fun commit(next: List<PlaceMemory>, onSaved: (() -> Unit)? = null) {
        val who = _state.value.identity ?: return
        _state.update { it.copy(places = listState(next)) }
        viewModelScope.launch {
            saveLock.withLock {
                when (val r = repo.save(who, next)) {
                    is AppResult.Success -> onSaved?.invoke()
                    is AppResult.Failure -> {
                        _effects.tryEmit(PlacesEffect.Failure(r.error))
                        load(silent = true)
                    }
                }
            }
        }
    }

    private fun updatePlace(id: String, change: (PlaceMemory) -> PlaceMemory) {
        commit(_state.value.all.map { if (it.id == id) change(it) else it })
    }

    override fun refresh() = load(silent = false)

    // ---- list page ----

    override fun back() {
        _state.update { s ->
            when {
                s.lightboxIndex != null -> s.copy(lightboxIndex = null)
                s.form != null -> s.copy(form = null)
                s.deleteId != null -> s.copy(deleteId = null)
                s.detailId != null -> s.copy(detailId = null)
                else -> s
            }
        }
    }

    override fun setQuery(text: String) = _state.update { it.copy(query = text) }

    override fun toggleFavoritesOnly() = _state.update { it.copy(favoritesOnly = !it.favoritesOnly) }

    override fun quickFavorite(id: String) = updatePlace(id) { it.copy(favorite = !it.favorite) }

    // ---- add / edit form ----

    override fun openAdd() = _state.update { it.copy(form = PlaceForm(date = LocalDate.now())) }

    override fun openEdit(id: String) {
        val p = _state.value.all.firstOrNull { it.id == id } ?: return
        _state.update {
            it.copy(
                form = PlaceForm(
                    editId = p.id, name = p.name, location = p.location, date = p.visitDate, description = p.description,
                    banner = p.banner, photos = p.photos, lat = p.lat?.let(::formatCoordinate).orEmpty(),
                    lng = p.lng?.let(::formatCoordinate).orEmpty(), favorite = p.favorite,
                ),
            )
        }
    }

    override fun closeForm() = _state.update { it.copy(form = null) }

    private fun editForm(change: (PlaceForm) -> PlaceForm) = _state.update { s -> s.form?.let { s.copy(form = change(it)) } ?: s }

    override fun setName(text: String) = editForm { it.copy(name = text, nameError = false) }
    override fun setLocation(text: String) = editForm { it.copy(location = text) }
    override fun setDate(date: LocalDate?) = editForm { it.copy(date = date) }
    override fun setDescription(text: String) = editForm { it.copy(description = text) }
    override fun setLat(text: String) = editForm { it.copy(lat = text) }
    override fun setLng(text: String) = editForm { it.copy(lng = text) }
    override fun setFormFavorite(on: Boolean) = editForm { it.copy(favorite = on) }
    override fun removeBanner() = editForm { it.copy(banner = null) }
    override fun removeFormPhoto(index: Int) = editForm { it.copy(photos = it.photos.filterIndexed { i, _ -> i != index }) }

    override fun pickBanner(uri: String) {
        val who = _state.value.identity ?: return
        editForm { it.copy(bannerUploading = true) }
        viewModelScope.launch {
            when (val r = repo.uploadPhoto(who, uri)) {
                is AppResult.Success -> editForm { it.copy(banner = r.data, bannerUploading = false) }
                is AppResult.Failure -> { editForm { it.copy(bannerUploading = false) }; toast(R.string.places_toast_banner_failed) }
            }
        }
    }

    override fun addFormPhotos(uris: List<String>) {
        val who = _state.value.identity ?: return
        if (uris.isEmpty()) return
        editForm { it.copy(photosUploading = it.photosUploading + uris.size) }
        viewModelScope.launch {
            val urls = uploadAll(who, uris)
            editForm { it.copy(photos = it.photos + urls.filterNotNull(), photosUploading = (it.photosUploading - uris.size).coerceAtLeast(0)) }
            if (urls.any { it == null }) toast(R.string.places_toast_photo_failed)
        }
    }

    private suspend fun uploadAll(who: com.twinhearts.app.feature.places.domain.model.PlacesIdentity, uris: List<String>): List<String?> =
        coroutineScope {
            val gate = Semaphore(MAX_PARALLEL_UPLOADS)
            uris.map { uri ->
                async { gate.withPermit { (repo.uploadPhoto(who, uri) as? AppResult.Success)?.data } }
            }.awaitAll()
        }

    override fun useMyLocation() {
        if (!repo.hasLocationPermission()) { _effects.tryEmit(PlacesEffect.NeedLocationPermission); return }
        editForm { it.copy(locating = true) }
        toast(R.string.places_toast_locating)
        viewModelScope.launch {
            val fix = repo.currentLocation()
            if (fix == null) {
                editForm { it.copy(locating = false) }
                toast(R.string.places_toast_no_location)
            } else {
                editForm { it.copy(lat = formatCoordinate(fix.lat), lng = formatCoordinate(fix.lng), locating = false) }
                toast(R.string.places_toast_location_set)
            }
        }
    }

    override fun savePlace() {
        val s = _state.value
        val f = s.form ?: return
        if (!isValidPlaceName(f.name)) {
            editForm { it.copy(nameError = true) }
            toast(R.string.places_toast_name_needed)
            return
        }
        if (f.busy) { toast(R.string.places_toast_still_uploading); return }
        val existing = f.editId?.let { id -> s.all.firstOrNull { it.id == id } }
        val place = PlaceMemory(
            id = existing?.id ?: newPlaceId(System.currentTimeMillis(), Random.nextInt(0, 1000)),
            name = f.name.trim(),
            location = f.location.trim(),
            visitDate = f.date,
            description = f.description.trim(),
            banner = f.banner,
            photos = f.photos,
            lat = parseCoordinate(f.lat, LAT_LIMIT),
            lng = parseCoordinate(f.lng, LNG_LIMIT),
            favorite = f.favorite,
            memories = existing?.memories.orEmpty(),
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
        )
        val next = if (existing != null) s.all.map { if (it.id == place.id) place else it } else s.all + place
        _state.update { it.copy(form = null) }
        commit(next)
        toast(if (existing != null) R.string.places_toast_updated else R.string.places_toast_added)
    }

    // ---- detail page ----

    override fun openDetail(id: String) = _state.update { it.copy(detailId = id, tab = DetailTab.Gallery, note = NoteDraft()) }

    override fun closeDetail() = _state.update { it.copy(detailId = null, lightboxIndex = null) }

    override fun setTab(tab: DetailTab) = _state.update { it.copy(tab = tab) }

    override fun toggleDetailFavorite() {
        val id = _state.value.detailId ?: return
        updatePlace(id) { it.copy(favorite = !it.favorite) }
    }

    override fun requestDelete() = _state.update { s -> s.copy(deleteId = s.detailId) }
    override fun cancelDelete() = _state.update { it.copy(deleteId = null) }

    override fun confirmDelete() {
        val id = _state.value.deleteId ?: return
        _state.update { it.copy(deleteId = null, detailId = null, lightboxIndex = null) }
        commit(_state.value.all.filterNot { it.id == id })
        toast(R.string.places_toast_removed)
    }

    override fun addGalleryPhotos(uris: List<String>) {
        val s = _state.value
        val who = s.identity ?: return
        val id = s.detailId ?: return
        if (uris.isEmpty()) return
        _state.update { it.copy(galleryUploading = it.galleryUploading + uris.size) }
        if (uris.size == 1) toast(R.string.places_toast_uploading)
        viewModelScope.launch {
            val urls = uploadAll(who, uris)
            val ok = urls.filterNotNull()
            _state.update { it.copy(galleryUploading = (it.galleryUploading - uris.size).coerceAtLeast(0)) }
            if (ok.isNotEmpty()) updatePlace(id) { it.copy(photos = it.photos + ok) }
            if (ok.size < uris.size) toast(R.string.places_toast_photo_failed)
        }
    }

    override fun openLightbox(index: Int) = _state.update { it.copy(lightboxIndex = index) }
    override fun closeLightbox() = _state.update { it.copy(lightboxIndex = null) }

    override fun removeLightboxPhoto() {
        val s = _state.value
        val id = s.detailId ?: return
        val index = s.lightboxIndex ?: return
        _state.update { it.copy(lightboxIndex = null) }
        updatePlace(id) { p -> p.copy(photos = p.photos.filterIndexed { i, _ -> i != index }) }
    }

    // ---- timeline notes ----

    override fun setNoteText(text: String) = _state.update { it.copy(note = it.note.copy(text = text)) }
    override fun setNoteDate(date: LocalDate?) = _state.update { it.copy(note = it.note.copy(date = date)) }
    override fun setNoteFavorite(on: Boolean) = _state.update { it.copy(note = it.note.copy(favorite = on)) }

    override fun addNote() {
        val s = _state.value
        val id = s.detailId ?: return
        val draft = s.note
        if (draft.text.isBlank()) { toast(R.string.places_toast_write_first); return }
        val note = MemoryNote(
            id = newPlaceId(System.currentTimeMillis(), Random.nextInt(0, 1000)),
            date = draft.date ?: LocalDate.now(),
            text = draft.text.trim(),
            favorite = draft.favorite,
        )
        _state.update { it.copy(note = NoteDraft()) }
        updatePlace(id) { it.copy(memories = it.memories + note) }
        toast(R.string.places_toast_memory_added)
    }

    override fun toggleNoteFavorite(id: String) {
        val pid = _state.value.detailId ?: return
        updatePlace(pid) { p -> p.copy(memories = p.memories.map { if (it.id == id) it.copy(favorite = !it.favorite) else it }) }
    }

    override fun deleteNote(id: String) {
        val pid = _state.value.detailId ?: return
        updatePlace(pid) { p -> p.copy(memories = p.memories.filterNot { it.id == id }) }
    }

    override fun openMap() {
        val p = _state.value.detail ?: return
        val lat = p.lat ?: return
        val lng = p.lng ?: return
        _effects.tryEmit(PlacesEffect.OpenUrl(mapsUrl(lat, lng, p.name)))
    }
}
