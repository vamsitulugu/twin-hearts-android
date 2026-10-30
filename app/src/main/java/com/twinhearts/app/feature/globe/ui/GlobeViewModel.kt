package com.twinhearts.app.feature.globe.ui

import com.twinhearts.app.feature.globe.ui.globe.PIN_HANDOFF_GLOBE_ZOOM
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.globe.data.GlobeLocationSource
import com.twinhearts.app.feature.globe.data.GlobeRepository
import com.twinhearts.app.feature.globe.data.WorldAtlas
import com.twinhearts.app.feature.globe.data.WorldAtlasSource
import com.twinhearts.app.feature.globe.domain.arcLegs
import com.twinhearts.app.feature.globe.domain.countryGlows
import com.twinhearts.app.feature.globe.domain.filterMemories
import com.twinhearts.app.feature.globe.domain.groupByCity
import com.twinhearts.app.feature.globe.domain.haversineKm
import com.twinhearts.app.feature.globe.domain.availableYears
import com.twinhearts.app.feature.globe.domain.model.ArcLeg
import com.twinhearts.app.feature.globe.domain.model.CityGroup
import com.twinhearts.app.feature.globe.domain.model.CountryGlow
import com.twinhearts.app.feature.globe.domain.model.GeoPoint
import com.twinhearts.app.feature.globe.domain.model.GlobeFilter
import com.twinhearts.app.feature.globe.domain.model.GlobeSummary
import com.twinhearts.app.feature.globe.domain.model.GlobeViewMode
import com.twinhearts.app.feature.globe.domain.model.LivePerson
import com.twinhearts.app.feature.globe.domain.model.MediaKind
import com.twinhearts.app.feature.globe.domain.model.Memory
import com.twinhearts.app.feature.globe.domain.model.PlaceHit
import com.twinhearts.app.feature.globe.domain.parseCoordinates
import com.twinhearts.app.feature.globe.domain.summaryOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import javax.inject.Inject

/** Loading the memory list: the first load shows a spinner, later reloads happen quietly. */
sealed interface GlobeLoad {
    data object Loading : GlobeLoad
    data object Ready : GlobeLoad
    data class Failed(val error: AppError) : GlobeLoad
}

/** The sheets that slide over the globe. The last one in the list is on top. */
sealed interface GlobePanel {
    data class City(val key: String) : GlobePanel
    data class Detail(val id: String) : GlobePanel

    /** [token] is new for every opening, so the form knows when to start from scratch. */
    data class Form(val token: Long, val editingId: String?, val cityKey: String?) : GlobePanel
}

/** "Fly the camera there". A new [id] is a new request, even to the same place. */
data class CameraRequest(val id: Long, val lat: Double, val lng: Double, val globeZoom: Double, val mapZoom: Double, val resetSpin: Boolean = false, val jump: Boolean = false, val toStreet: Boolean = false, val fromGlobe: Boolean = false)

data class ViewerItem(val url: String, val isVideo: Boolean)

data class ViewerState(val items: List<ViewerItem>, val index: Int)

/** Progress of adding photos to an existing memory. */
data class UploadUi(val memoryId: String, val kind: MediaKind, val done: Int, val total: Int, val failedUris: List<String>, val running: Boolean)

data class LiveUi(
    val enabled: Boolean = false,
    val me: LivePerson? = null,
    val partner: LivePerson? = null,
    val myTrail: List<GeoPoint> = emptyList(),
    val partnerTrail: List<GeoPoint> = emptyList(),
) {
    /** Distance between the two, only when both are online. */
    val distanceKm: Double?
        get() = if (me != null && partner != null && me.online && partner.online) haversineKm(me.point, partner.point) else null
}

/** The search box at the top: what was typed and what it found. */
data class SearchUi(
    val query: String = "",
    val hits: List<PlaceHit> = emptyList(),
    /** Set when the text is a "lat, lng" pair. */
    val coordinates: GeoPoint? = null,
)

data class GlobeUiState(
    val load: GlobeLoad = GlobeLoad.Loading,
    val memories: List<Memory> = emptyList(),
    val filter: GlobeFilter = GlobeFilter.All,
    val years: List<String> = emptyList(),
    val view: GlobeViewMode = GlobeViewMode.Globe,
    /** True while the satellite map is shown inside the Globe tab (zoomed in close). */
    val street: Boolean = false,
    /** Where the satellite map was opened; used to go back to the globe if the map cannot load. */
    val streetAt: GeoPoint? = null,
    val visible: List<Memory> = emptyList(),
    val groups: List<CityGroup> = emptyList(),
    val summary: GlobeSummary = GlobeSummary(0, 0, 0, 0.0),
    val legs: List<ArcLeg> = emptyList(),
    val glows: List<CountryGlow> = emptyList(),
    val atlas: WorldAtlas? = null,
    val roads: Map<String, List<GeoPoint>> = emptyMap(),
    val selectedKey: String? = null,
    val panels: List<GlobePanel> = emptyList(),
    val statsOpen: Boolean = false,
    val viewer: ViewerState? = null,
    val live: LiveUi = LiveUi(),
    val camera: CameraRequest? = null,
    val searchPin: GeoPoint? = null,
    val upload: UploadUi? = null,
    val partnerName: String = "",
    val checkingPro: Boolean = false,
    val search: SearchUi = SearchUi(),
) {
    val isEmpty: Boolean get() = memories.isEmpty()
    fun group(key: String): CityGroup? = groups.firstOrNull { it.key == key }
    fun memory(id: String): Memory? = memories.firstOrNull { it.id == id }
}

/** One-off things the screen must do or say. */
sealed interface GlobeEvent {
    data class Message(@StringRes val res: Int, val arg1: Any? = null, val arg2: Any? = null) : GlobeEvent
    data object AskLocationPermission : GlobeEvent
}

/**
 * Why this file exists: holds everything the Memory Globe page knows: the memories, the filter,
 * which view and which sheet are open, camera requests, live location and uploads (R-42).
 * The pin form has its own ViewModel (MemoryFormViewModel) because it is large.
 */
@HiltViewModel
class GlobeViewModel @Inject constructor(
    private val repository: GlobeRepository,
    private val atlasSource: WorldAtlasSource,
    private val locationSource: GlobeLocationSource,
) : ViewModel() {

    private val _state = MutableStateFlow(GlobeUiState())
    val state: StateFlow<GlobeUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<GlobeEvent>(extraBufferCapacity = EVENT_BUFFER)
    val events: SharedFlow<GlobeEvent> = _events.asSharedFlow()

    private var sequence = 0L
    private var pendingOpen: Pair<Long, String>? = null
    private var pendingZoomIn: Pair<Long, String>? = null
    private var isPro = false
    private var visible = true
    private var liveJob: Job? = null
    private var pollJob: Job? = null
    private var roadJob: Job? = null
    private var searchJob: Job? = null
    private var lastPing: Triple<Double, Double, Long>? = null
    private val roadCache = HashMap<String, List<GeoPoint>?>()

    init {
        viewModelScope.launch {
            val who = repository.identity()
            if (who != null) _state.update { it.copy(partnerName = who.partnerName) }
        }
        viewModelScope.launch {
            val atlas = atlasSource.atlas()
            _state.update { it.copy(atlas = atlas) }
        }
        load(quiet = false)
    }

    private fun say(@StringRes res: Int, a: Any? = null, b: Any? = null) {
        _events.tryEmit(GlobeEvent.Message(res, a, b))
    }

    // ── loading ──────────────────────────────────────────────────────────────────────────────

    fun retry() = load(quiet = false)

    private fun load(quiet: Boolean) {
        viewModelScope.launch {
            if (!quiet) _state.update { it.copy(load = GlobeLoad.Loading) }
            when (val r = repository.memories()) {
                is AppResult.Success -> {
                    _state.update { it.copy(load = GlobeLoad.Ready, memories = r.data) }
                    recompute()
                }
                is AppResult.Failure -> if (!quiet) _state.update { it.copy(load = GlobeLoad.Failed(r.error)) }
            }
        }
    }

    /** Works out everything that depends on the memories and the filter. */
    private fun recompute() {
        _state.update { s ->
            val shown = filterMemories(s.memories, s.filter)
            s.copy(
                years = availableYears(s.memories),
                visible = shown,
                groups = groupByCity(shown),
                summary = summaryOf(shown),
                legs = arcLegs(shown),
                glows = countryGlows(shown),
            )
        }
        if (_state.value.view == GlobeViewMode.Map) loadRoads()
    }

    // ── views and filters ────────────────────────────────────────────────────────────────────

    fun setView(mode: GlobeViewMode) {
        _state.update { it.copy(view = mode, street = false) }
        if (mode == GlobeViewMode.Map) loadRoads()
    }

    fun setFilter(filter: GlobeFilter) {
        _state.update { it.copy(filter = filter) }
        recompute()
    }

    /** Real road routes for short legs, fetched one by one and remembered (the web app did the same). */
    private fun loadRoads() {
        roadJob?.cancel()
        val legs = _state.value.legs.filter { it.driveable }
        roadJob = viewModelScope.launch {
            legs.forEach { leg ->
                if (!roadCache.containsKey(leg.routeKey)) {
                    val r = repository.roadRoute(leg.from, leg.to)
                    roadCache[leg.routeKey] = (r as? AppResult.Success)?.data
                }
                val road = roadCache[leg.routeKey]
                if (road != null) _state.update { it.copy(roads = it.roads + (leg.routeKey to road)) }
            }
        }
    }

    // ── camera ───────────────────────────────────────────────────────────────────────────────

    private fun fly(lat: Double, lng: Double, globeZoom: Double, mapZoom: Double, resetSpin: Boolean = false): Long {
        val id = ++sequence
        _state.update { it.copy(camera = CameraRequest(id, lat, lng, globeZoom, mapZoom, resetSpin)) }
        return id
    }

    /** The globe was pinched in far enough: the satellite 3D map fades in over it, still on the Globe tab. */
    fun enterStreetLevel(lat: Double, lng: Double, mapZoom: Double) {
        if (_state.value.street) return
        val id = ++sequence
        _state.update { it.copy(street = true, streetAt = GeoPoint(lat, lng), camera = CameraRequest(id, lat, lng, RESET_ZOOM, mapZoom, jump = true, toStreet = true)) }
        loadRoads()
    }

    /** The map was zoomed back out: show the globe again at the same spot and scale. */
    fun exitStreetLevel(lat: Double, lng: Double, globeZoom: Double) {
        val id = ++sequence
        _state.update { it.copy(street = false, camera = CameraRequest(id, lat, lng, globeZoom, MAP_WORLD_ZOOM, jump = true)) }
    }

    fun resetView() {
        pendingOpen = null
        pendingZoomIn = null
        _state.update { it.copy(selectedKey = null, searchPin = null, street = false) }
        fly(RESET_LAT, RESET_LNG, RESET_ZOOM, MAP_WORLD_ZOOM, resetSpin = true)
        say(R.string.globe_toast_reset)
    }

    fun goToPlace(lat: Double, lng: Double, label: String) {
        pendingOpen = null
        _state.update { it.copy(searchPin = GeoPoint(lat, lng)) }
        fly(lat, lng, SEARCH_GLOBE_ZOOM, SEARCH_MAP_ZOOM)
        say(R.string.globe_toast_place, label)
    }

    /** A pin was tapped: fly to it, and open its sheet when the camera arrives. */
    fun onPinTap(key: String) {
        val group = _state.value.group(key) ?: return
        _state.update { it.copy(selectedKey = key) }
        if (_state.value.view == GlobeViewMode.Globe && !_state.value.street) {
            // Step 1: the globe turns to the city and closes in. Step 2 (onCameraArrived): the satellite 3D map
            // takes over and zooms down to street level. Step 3: the trip sheet opens over the map.
            pendingZoomIn = fly(group.lat, group.lng, PIN_HANDOFF_GLOBE_ZOOM, PIN_MAP_ZOOM) to key
            pendingOpen = null
        } else {
            pendingZoomIn = null
            val id = fly(group.lat, group.lng, PIN_GLOBE_ZOOM, PIN_MAP_ZOOM)
            pendingOpen = id to key
        }
    }

    /** What opens over the map: the trip itself when the city has one memory, else the city's list. */
    private fun panelFor(key: String): GlobePanel {
        val group = _state.value.group(key)
        return if (group != null && group.count == 1) GlobePanel.Detail(group.memories[0].id) else GlobePanel.City(key)
    }

    fun onCameraArrived(id: Long) {
        // The request is done; clearing it stops it from replaying when the page is opened again.
        _state.update { if (it.camera?.id == id) it.copy(camera = null) else it }
        val zoomIn = pendingZoomIn
        if (zoomIn != null && zoomIn.first == id) {
            pendingZoomIn = null
            val group = _state.value.group(zoomIn.second) ?: return
            val next = ++sequence
            pendingOpen = next to zoomIn.second
            _state.update {
                it.copy(
                    street = true,
                    streetAt = GeoPoint(group.lat, group.lng),
                    camera = CameraRequest(next, group.lat, group.lng, PIN_HANDOFF_GLOBE_ZOOM, PIN_MAP_ZOOM, toStreet = true, fromGlobe = true),
                )
            }
            loadRoads()
            return
        }
        val pending = pendingOpen ?: return
        if (pending.first != id) return
        pendingOpen = null
        _state.update { it.copy(panels = listOf(panelFor(pending.second))) }
    }

    // ── search box ───────────────────────────────────────────────────────────────────────────

    /** Typing in the search box: coordinates are recognised at once, places are searched after a short pause. */
    fun onSearchQuery(text: String) {
        searchJob?.cancel()
        val coordinates = parseCoordinates(text)
        val skip = coordinates != null || text.trim().length < MIN_SEARCH_LETTERS
        _state.update { it.copy(search = SearchUi(text, if (skip) emptyList() else it.search.hits, coordinates)) }
        if (skip) return
        searchJob = viewModelScope.launch {
            delay(SEARCH_DELAY_MS)
            val r = repository.searchPlaces(text.trim()) as? AppResult.Success ?: return@launch
            _state.update { it.copy(search = it.search.copy(hits = r.data)) }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _state.update { it.copy(search = SearchUi()) }
    }

    fun pickSearchHit(hit: PlaceHit) {
        goToPlace(hit.lat, hit.lng, hit.displayName.substringBefore(',').ifBlank { hit.city })
        clearSearch()
    }

    fun pickCoordinates(point: GeoPoint) {
        goToPlace(point.lat, point.lng, "%.4f, %.4f".format(java.util.Locale.ROOT, point.lat, point.lng))
        clearSearch()
    }

    // ── sheets ───────────────────────────────────────────────────────────────────────────────

    fun openDetail(id: String) = _state.update { s ->
        val top = s.panels.lastOrNull()
        val base = if (top is GlobePanel.Detail) s.panels.dropLast(1) else s.panels
        s.copy(panels = base + GlobePanel.Detail(id))
    }

    fun closeTopPanel() = _state.update {
        val left = it.panels.dropLast(1)
        it.copy(panels = left, selectedKey = if (left.isEmpty()) null else it.selectedKey)
    }

    private fun closeAllPanels() = _state.update { it.copy(panels = emptyList(), selectedKey = null) }

    fun openStats() = _state.update { it.copy(statsOpen = true) }

    fun closeStats() = _state.update { it.copy(statsOpen = false) }

    fun openViewer(items: List<ViewerItem>, index: Int) {
        if (items.isEmpty()) return
        _state.update { it.copy(viewer = ViewerState(items, index.coerceIn(0, items.lastIndex))) }
    }

    fun closeViewer() = _state.update { it.copy(viewer = null) }

    /** Back button: closes the top-most thing. Returns false when nothing was open. */
    fun back(): Boolean {
        val s = _state.value
        when {
            s.viewer != null -> closeViewer()
            s.statsOpen -> closeStats()
            s.panels.isNotEmpty() -> closeTopPanel()
            else -> return false
        }
        return true
    }

    // ── adding, editing, deleting ────────────────────────────────────────────────────────────

    /** "Pin a Memory": only couples with Pro may add memories, like the web app. */
    fun requestPin() {
        if (isPro) {
            openForm(editingId = null, cityKey = null)
            return
        }
        if (_state.value.checkingPro) return
        viewModelScope.launch {
            _state.update { it.copy(checkingPro = true) }
            val r = repository.isPro()
            _state.update { it.copy(checkingPro = false) }
            when (r) {
                is AppResult.Success -> if (r.data) {
                    isPro = true
                    openForm(null, null)
                } else {
                    say(R.string.globe_toast_pro)
                }
                is AppResult.Failure -> say(R.string.globe_toast_pro_check_failed)
            }
        }
    }

    fun addMemoryHere(cityKey: String) = openForm(editingId = null, cityKey = cityKey)

    fun editMemory(id: String) = openForm(editingId = id, cityKey = null, replaceDetail = true)

    private fun openForm(editingId: String?, cityKey: String?, replaceDetail: Boolean = false) {
        _state.update { s ->
            val base = if (replaceDetail && s.panels.lastOrNull() is GlobePanel.Detail) s.panels.dropLast(1) else s.panels
            s.copy(panels = base + GlobePanel.Form(++sequence, editingId, cityKey))
        }
    }

    /** The form saved a memory: close it and refresh the list. */
    fun onMemorySaved() {
        closeTopPanel()
        load(quiet = true)
    }

    fun deleteMemory(id: String) {
        viewModelScope.launch {
            when (repository.deleteMemory(id)) {
                is AppResult.Success -> {
                    say(R.string.globe_toast_memory_removed)
                    closeAllPanels()
                    load(quiet = true)
                }
                is AppResult.Failure -> say(R.string.globe_toast_error)
            }
        }
    }

    fun deleteMedia(mediaId: String) {
        viewModelScope.launch {
            when (repository.deleteMedia(mediaId)) {
                is AppResult.Success -> {
                    say(R.string.globe_toast_deleted)
                    load(quiet = true)
                }
                is AppResult.Failure -> say(R.string.globe_toast_error)
            }
        }
    }

    /** Photos or videos picked for an existing memory: upload (three at a time), then attach. */
    fun addPicked(memoryId: String, uris: List<String>, kind: MediaKind) {
        if (uris.isEmpty() || _state.value.upload?.running == true) return
        viewModelScope.launch {
            val single = uris.size == 1
            if (single) say(R.string.globe_toast_uploading)
            _state.update { it.copy(upload = UploadUi(memoryId, kind, 0, uris.size, emptyList(), true)) }
            val gate = Semaphore(UPLOAD_PARALLEL)
            val results = uris.map { uri ->
                async {
                    gate.withPermit {
                        val r = repository.uploadPicked(uri, kind)
                        _state.update { s -> s.upload?.let { u -> s.copy(upload = u.copy(done = u.done + 1)) } ?: s }
                        uri to r
                    }
                }
            }.awaitAll()
            val failed = mutableListOf<String>()
            var added = 0
            results.forEach { (uri, r) ->
                val url = (r as? AppResult.Success)?.data
                val attached = url != null && repository.addMedia(memoryId, kind, url, null) is AppResult.Success
                if (attached) added++ else failed += uri
            }
            _state.update { it.copy(upload = if (failed.isEmpty()) null else UploadUi(memoryId, kind, uris.size, uris.size, failed, false)) }
            when {
                single && failed.isEmpty() -> say(R.string.globe_toast_added)
                single -> say(R.string.globe_toast_upload_failed)
                failed.isEmpty() -> say(R.string.globe_toast_photos_added, added)
                else -> say(R.string.globe_toast_some_failed, added, failed.size)
            }
            if (added > 0) load(quiet = true)
        }
    }

    fun retryUpload() {
        val u = _state.value.upload ?: return
        if (u.running || u.failedUris.isEmpty()) return
        addPicked(u.memoryId, u.failedUris, u.kind)
    }

    fun dismissUpload() = _state.update { it.copy(upload = null) }

    /** A voice note recorded for an existing memory. */
    fun addVoice(memoryId: String, file: File, seconds: Int) {
        viewModelScope.launch {
            val duration = "%d:%02d".format(seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)
            val up = repository.uploadVoice(file)
            file.delete()
            val url = (up as? AppResult.Success)?.data
            if (url == null || repository.addMedia(memoryId, MediaKind.Voice, url, duration) !is AppResult.Success) {
                say(R.string.globe_toast_voice_failed)
                return@launch
            }
            say(R.string.globe_toast_voice_saved)
            load(quiet = true)
        }
    }

    fun addJournal(memoryId: String, text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            when (repository.addJournal(memoryId, clean)) {
                is AppResult.Success -> {
                    say(R.string.globe_toast_journal_saved)
                    load(quiet = true)
                }
                is AppResult.Failure -> say(R.string.globe_toast_error)
            }
        }
    }

    // ── live location ────────────────────────────────────────────────────────────────────────

    /** The 📡 button. [granted] says whether the location permission is already given. */
    fun onLiveButton(granted: Boolean) {
        when {
            _state.value.live.enabled -> stopLive()
            granted -> startLive()
            else -> _events.tryEmit(GlobeEvent.AskLocationPermission)
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        if (granted) startLive() else say(R.string.globe_toast_no_location_permission)
    }

    private fun startLive() {
        _state.update { it.copy(live = it.live.copy(enabled = true)) }
        say(R.string.globe_toast_live_on)
        applyLive()
    }

    private fun stopLive() {
        _state.update { it.copy(live = LiveUi()) }
        cancelLive()
        viewModelScope.launch { repository.stopSharing() }
        say(R.string.globe_toast_live_off)
    }

    /** Live updates run only while the page is on screen (R-66). */
    /** The satellite map did not appear in time (offline?): tell the user and go back to the globe. */
    fun satelliteUnavailable() {
        val at = _state.value.streetAt
        say(R.string.globe_map_unavailable)
        if (at != null) exitStreetLevel(at.lat, at.lng, STREET_FALLBACK_GLOBE_ZOOM) else _state.update { it.copy(street = false) }
    }

    fun onVisible() {
        visible = true
        applyLive()
    }

    fun onHidden() {
        visible = false
        cancelLive()
    }

    private fun cancelLive() {
        liveJob?.cancel()
        pollJob?.cancel()
        liveJob = null
        pollJob = null
    }

    private fun applyLive() {
        if (!_state.value.live.enabled || !visible) return
        if (liveJob?.isActive != true) {
            liveJob = viewModelScope.launch {
                locationSource.updates(PING_INTERVAL_MS).collect { fix ->
                    val now = System.currentTimeMillis()
                    val last = lastPing
                    val moved = last == null || haversineKm(GeoPoint(last.first, last.second), GeoPoint(fix.lat, fix.lng)) > MOVED_KM
                    if (last == null || now - last.third > PING_MIN_GAP_MS || moved) {
                        lastPing = Triple(fix.lat, fix.lng, now)
                        repository.ping(fix)
                    }
                }
            }
        }
        if (pollJob?.isActive != true) {
            pollJob = viewModelScope.launch {
                while (true) {
                    pollLocations()
                    delay(POLL_MS)
                }
            }
        }
    }

    private suspend fun pollLocations() {
        val r = repository.liveLocations() as? AppResult.Success ?: return
        val snapshot = r.data
        val myTrail = if (snapshot.me?.online == true) (repository.trail(ofPartner = false) as? AppResult.Success)?.data.orEmpty() else emptyList()
        val theirTrail = if (snapshot.partner?.online == true) (repository.trail(ofPartner = true) as? AppResult.Success)?.data.orEmpty() else emptyList()
        _state.update { s ->
            if (!s.live.enabled) s else s.copy(live = s.live.copy(me = snapshot.me, partner = snapshot.partner, myTrail = myTrail, partnerTrail = theirTrail))
        }
    }

    override fun onCleared() {
        cancelLive()
        super.onCleared()
    }

    private companion object {
        const val EVENT_BUFFER = 8
        const val MIN_SEARCH_LETTERS = 3
        const val SEARCH_DELAY_MS = 400L
        const val UPLOAD_PARALLEL = 3
        const val SECONDS_PER_MINUTE = 60
        const val PING_INTERVAL_MS = 5_000L
        const val PING_MIN_GAP_MS = 8_000L
        const val MOVED_KM = 0.015
        const val POLL_MS = 10_000L
        const val RESET_LAT = 15.0
        const val RESET_LNG = 0.0
        const val RESET_ZOOM = 1.0
        const val MAP_WORLD_ZOOM = 1.8
        const val STREET_FALLBACK_GLOBE_ZOOM = 2.8
        const val PIN_GLOBE_ZOOM = 3.4
        const val PIN_MAP_ZOOM = 15.2
        const val SEARCH_GLOBE_ZOOM = 4.5
        const val SEARCH_MAP_ZOOM = 12.0
    }
}
