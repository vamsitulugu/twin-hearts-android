package com.twinhearts.app.feature.livemap.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.feature.livemap.data.LiveMapPrefs
import com.twinhearts.app.feature.livemap.data.LiveMapRepository
import com.twinhearts.app.feature.livemap.data.LiveMapSettings
import com.twinhearts.app.feature.livemap.data.LocationSource
import com.twinhearts.app.feature.livemap.data.share.ShareLauncher
import com.twinhearts.app.feature.livemap.domain.GeoMath
import com.twinhearts.app.feature.livemap.domain.model.AlertKey
import com.twinhearts.app.feature.livemap.domain.model.CouplePositions
import com.twinhearts.app.feature.livemap.domain.model.FavoritePlace
import com.twinhearts.app.feature.livemap.domain.model.LivePosition
import com.twinhearts.app.feature.livemap.domain.model.LoveNote
import com.twinhearts.app.feature.livemap.domain.model.MapStyleKind
import com.twinhearts.app.feature.livemap.domain.model.SavedPlace
import com.twinhearts.app.feature.livemap.domain.model.SearchHit
import com.twinhearts.app.feature.livemap.domain.model.Vehicle
import com.twinhearts.app.feature.livemap.domain.nav.GeoPoint
import com.twinhearts.app.feature.livemap.domain.nav.NavSession
import com.twinhearts.app.feature.livemap.domain.nav.sampled
import com.twinhearts.app.feature.livemap.domain.nav.TravelMode
import com.twinhearts.app.feature.livemap.data.nav.NavEngine
import com.twinhearts.app.feature.livemap.domain.spotKey
import com.twinhearts.app.feature.livemap.ui.map.CameraRequest
import com.twinhearts.app.feature.livemap.ui.map.MapPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Brain of the Live Map page: who is where, sharing on/off and pause, saved places, favorites,
 * love notes, alerts, search and directions. The route history screen has its own [TrackViewModel].
 */
@HiltViewModel
class LiveMapViewModel @Inject constructor(
    private val repository: LiveMapRepository,
    private val sessionStore: SessionStore,
    private val prefs: LiveMapPrefs,
    private val locationSource: LocationSource,
    private val shareLauncher: ShareLauncher,
    private val navEngine: NavEngine,
) : ViewModel() {

    private val _state = MutableStateFlow(LiveMapUiState())
    val state: StateFlow<LiveMapUiState> = _state.asStateFlow()

    private val _events = Channel<LiveMapEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var pollJob: Job? = null
    private var fixJob: Job? = null
    private var searchJob: Job? = null
    private var cameraCounter = 0L
    private var pollCount = 0
    private var partnerWasOnline: Boolean? = null
    private val partnerInside = mutableMapOf<String, Boolean>()
    private var lastAreaKey: String? = null
    private var lastAreaAt = 0L
    private var weatherKey: String? = null
    private var weatherAt = 0L

    init {
        viewModelScope.launch {
            sessionStore.session.collect { s ->
                if (s != null) {
                    _state.update { it.copy(myName = s.myName, partnerName = s.partnerName, myRole = s.role, paired = s.paired) }
                }
            }
        }
        viewModelScope.launch {
            prefs.settings.collect { settings -> _state.update { it.copy(settings = settings) } }
        }
        viewModelScope.launch {
            navEngine.session.collect { session -> onNavSession(session) }
        }
        load()
    }

    // ── loading and polling ──────────────────────────────────────────────────────────────────────

    /** First load, and the "Try again" button. Reads positions and the shared state at the same time. */
    fun load() {
        _state.update { it.copy(phase = LiveMapPhase.Loading) }
        viewModelScope.launch {
            val sharedCall = async { repository.sharedState() }
            val positionsCall = async { repository.positions() }
            val shared = sharedCall.await()
            val positions = positionsCall.await()
            if (shared is AppResult.Failure && positions is AppResult.Failure) {
                _state.update { it.copy(phase = LiveMapPhase.Failed(positions.error)) }
                return@launch
            }
            _state.update { s ->
                s.copy(
                    phase = LiveMapPhase.Ready,
                    shared = (shared as? AppResult.Success)?.data ?: s.shared,
                    offline = shared is AppResult.Failure || positions is AppResult.Failure,
                )
            }
            if (positions is AppResult.Success) applyPositions(positions.data)
            refreshTrails()
            loadWeatherIfNeeded()
        }
    }

    /** The page is on screen: start polling and listening to this phone's GPS (R-66: nothing runs in the background). */
    fun onVisible() {
        if (_state.value.settings.sharing == true && locationSource.hasPermission()) shareLauncher.start()
        startFixes()
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (true) {
                refreshPositions()
                delay(POLL_MS)
            }
        }
    }

    /** The page left the screen: stop polling. The sharing service keeps going on its own if sharing is on. */
    fun onHidden() {
        pollJob?.cancel()
        pollJob = null
        fixJob?.cancel()
        fixJob = null
    }

    private fun startFixes() {
        if (fixJob?.isActive == true || !locationSource.hasPermission()) return
        fixJob = viewModelScope.launch {
            locationSource.updates(FIX_MS).collect { fix -> _state.update { it.copy(myFix = fix) } }
        }
    }

    private suspend fun refreshPositions() {
        when (val result = repository.positions()) {
            is AppResult.Success -> {
                _state.update { it.copy(offline = false) }
                applyPositions(result.data)
            }
            is AppResult.Failure -> if (result.error == AppError.Network) _state.update { it.copy(offline = true) }
        }
        pollCount++
        if (pollCount % TRAIL_EVERY == 0) refreshTrails()
    }

    private fun applyPositions(couple: CouplePositions) {
        val before = _state.value.positions.partner
        _state.update { it.copy(positions = couple, nowMs = System.currentTimeMillis()) }
        checkPartnerAlerts(couple.partner)
        val partner = couple.partner
        if (partner != null) {
            val moved = before == null || GeoMath.distanceM(before.lat, before.lng, partner.lat, partner.lng) > FOLLOW_MIN_M
            if (moved && _state.value.settings.followPartner && _state.value.screen == LmScreen.Map) {
                cameraTo(MapPoint(partner.lat, partner.lng))
            }
            lookupArea(partner)
        }
    }

    private suspend fun refreshTrails() {
        if (!_state.value.settings.showRouteLine) return
        val mine = repository.trail(ofPartner = false)
        val theirs = repository.trail(ofPartner = true)
        _state.update { s ->
            s.copy(
                myTrail = (mine as? AppResult.Success)?.data?.map { MapPoint(it.first, it.second) } ?: s.myTrail,
                partnerTrail = (theirs as? AppResult.Success)?.data?.map { MapPoint(it.first, it.second) } ?: s.partnerTrail,
            )
        }
    }

    /** Looks up "Near <area>" for the partner, at most once every 15 seconds and once per ~100 m grid cell. */
    private fun lookupArea(partner: LivePosition) {
        val key = String.format(java.util.Locale.US, "%.3f,%.3f", partner.lat, partner.lng)
        val now = System.currentTimeMillis()
        if (key == lastAreaKey || now - lastAreaAt < AREA_GAP_MS) return
        if (GeoMath.nearestSaved(partner.lat, partner.lng, _state.value.shared.places) != null) return
        lastAreaKey = key
        lastAreaAt = now
        viewModelScope.launch {
            val result = repository.areaName(partner.lat, partner.lng)
            if (result is AppResult.Success) _state.update { it.copy(partnerArea = result.data) }
        }
    }

    private fun checkPartnerAlerts(partner: LivePosition?) {
        if (partner == null) return
        val s = _state.value
        val alerts = s.shared.alerts
        val wasOnline = partnerWasOnline
        partnerWasOnline = partner.online
        if (wasOnline == true && !partner.online && alerts.offline) emit(LiveMapEvent.PartnerOffline)
        s.shared.places.forEach { place ->
            val inside = GeoMath.distanceM(partner.lat, partner.lng, place.lat, place.lng) <= GEOFENCE_RADIUS_M
            val previous = partnerInside.put(place.id, inside)
            // The first time we see a place only records where the partner is: no alert (same as the web app).
            if (previous == null || previous == inside) return@forEach
            val label = place.name.ifBlank { place.category }
            if (inside && alerts.arrive) emit(LiveMapEvent.PartnerArrived(label))
            if (!inside && alerts.leave) emit(LiveMapEvent.PartnerLeft(label))
        }
    }

    // ── navigation between the sub-screens ───────────────────────────────────────────────────────

    fun open(screen: LmScreen) {
        if (screen == LmScreen.Map) {
            goToMap()
            return
        }
        _state.update { it.copy(stack = it.stack + screen) }
        if (screen == LmScreen.Alerts) loadFeed()
    }

    /** Tab strip: Map / Track / Partner / Places replace each other, so Back always returns to the Map. */
    fun switchTab(screen: LmScreen) {
        _state.update { it.copy(stack = if (screen == LmScreen.Map) listOf(LmScreen.Map) else listOf(LmScreen.Map, screen)) }
    }

    fun goToMap() = _state.update { it.copy(stack = listOf(LmScreen.Map)) }

    /** Goes back one sub-screen. Returns false when already on the Map screen, so the page itself should close. */
    fun back(): Boolean {
        if (_state.value.stack.size <= 1) return false
        _state.update { it.copy(stack = it.stack.dropLast(1)) }
        return true
    }

    fun onCameraIdle(center: MapPoint, zoom: Double) = _state.update { it.copy(lastCenter = center, lastZoom = zoom) }

    // ── sharing, pause, privacy ──────────────────────────────────────────────────────────────────

    fun setSharing(on: Boolean) {
        viewModelScope.launch {
            if (on) {
                prefs.setSharing(true)
                prefs.setPausedUntil(0L)
                shareLauncher.start()
                emit(LiveMapEvent.Message(R.string.livemap_msg_sharing_on))
            } else {
                prefs.setSharing(false)
                shareLauncher.stop()
                repository.stopSharing()
                emit(LiveMapEvent.Message(R.string.livemap_msg_sharing_off))
            }
        }
    }

    /** Called with the result of the location permission dialog. The first grant turns sharing on, like the web app. */
    fun onPermissionResult(granted: Boolean) {
        if (!granted) return
        startFixes()
        viewModelScope.launch {
            if (prefs.snapshot().sharing == null) setSharingNow() else if (prefs.snapshot().sharing == true) shareLauncher.start()
        }
    }

    private suspend fun setSharingNow() {
        prefs.setSharing(true)
        shareLauncher.start()
    }

    /** [minutes] null means "until I resume it myself". */
    fun pause(minutes: Int?) {
        viewModelScope.launch {
            val until = if (minutes == null) LiveMapSettings.MANUAL_PAUSE else System.currentTimeMillis() + minutes * 60_000L
            prefs.setPausedUntil(until)
            val result = repository.pause(minutes)
            emit(
                if (result is AppResult.Success) LiveMapEvent.Message(R.string.livemap_msg_paused)
                else LiveMapEvent.Message(R.string.livemap_msg_could_not_save),
            )
        }
    }

    fun resume() {
        viewModelScope.launch {
            prefs.setPausedUntil(0L)
            repository.resume()
            emit(LiveMapEvent.Message(R.string.livemap_msg_resumed))
        }
    }

    fun setInvisible(on: Boolean) {
        viewModelScope.launch {
            prefs.setInvisible(on)
            emit(LiveMapEvent.Message(if (on) R.string.livemap_msg_invisible_on else R.string.livemap_msg_invisible_off))
        }
    }

    fun setApproximate(on: Boolean) {
        viewModelScope.launch {
            prefs.setApproximate(on)
            emit(LiveMapEvent.Message(if (on) R.string.livemap_msg_approx_on else R.string.livemap_msg_approx_off))
        }
    }

    /** Sends the exact position right now, marked as an emergency. The partner gets a banner for 10 minutes. */
    fun emergencyShare() {
        viewModelScope.launch {
            val fix = locationSource.currentFix() ?: _state.value.myFix
            if (fix == null) {
                emit(LiveMapEvent.Message(R.string.livemap_msg_no_my_location))
                return@launch
            }
            val result = repository.ping(fix, _state.value.settings.vehicle, emergency = true)
            emit(
                LiveMapEvent.Message(
                    if (result is AppResult.Success) R.string.livemap_msg_emergency_sent else R.string.livemap_msg_could_not_save,
                ),
            )
        }
    }

    fun dismissEmergencyBanner() {
        val at = _state.value.positions.partner?.updatedAtMs ?: return
        _state.update { it.copy(dismissedEmergencyAt = at) }
    }

    fun setVehicle(vehicle: Vehicle) {
        viewModelScope.launch { prefs.setVehicle(vehicle) }
    }

    // ── map options ──────────────────────────────────────────────────────────────────────────────

    fun setMapStyle(style: MapStyleKind) {
        viewModelScope.launch { prefs.setMapStyle(style) }
    }

    fun setThreeD(on: Boolean) {
        viewModelScope.launch { prefs.setThreeD(on) }
    }

    fun setFollowPartner(on: Boolean) {
        viewModelScope.launch { prefs.setFollowPartner(on) }
    }

    fun setShowRouteLine(on: Boolean) {
        viewModelScope.launch {
            prefs.setShowRouteLine(on)
            if (on) refreshTrails()
        }
    }

    fun setShowPlaceLabels(on: Boolean) {
        viewModelScope.launch { prefs.setShowPlaceLabels(on) }
    }

    fun setShowWeather(on: Boolean) {
        viewModelScope.launch {
            prefs.setShowWeather(on)
            if (on) {
                weatherKey = null
                loadWeatherIfNeeded()
            } else {
                _state.update { it.copy(weather = null) }
            }
        }
    }

    private suspend fun loadWeatherIfNeeded() {
        val s = _state.value
        if (!s.settings.showWeather) return
        val here = s.myFix?.let { MapPoint(it.lat, it.lng) } ?: s.positions.me?.let { MapPoint(it.lat, it.lng) } ?: return
        val key = String.format(java.util.Locale.US, "%.2f,%.2f", here.lat, here.lng)
        if (key == weatherKey && System.currentTimeMillis() - weatherAt < WEATHER_CACHE_MS) return
        _state.update { it.copy(weatherLoading = true) }
        val result = repository.weather(here.lat, here.lng)
        if (result is AppResult.Success) {
            weatherKey = key
            weatherAt = System.currentTimeMillis()
        }
        _state.update { it.copy(weather = (result as? AppResult.Success)?.data ?: it.weather, weatherLoading = false) }
    }

    // ── camera and directions ────────────────────────────────────────────────────────────────────

    private fun cameraTo(point: MapPoint, zoom: Double? = null) =
        _state.update { it.copy(camera = CameraRequest(++cameraCounter, target = point, zoom = zoom)) }

    private fun cameraFit(points: List<MapPoint>) =
        _state.update { it.copy(camera = CameraRequest(++cameraCounter, fit = points)) }

    private fun myPoint(): MapPoint? {
        val s = _state.value
        return s.myFix?.let { MapPoint(it.lat, it.lng) } ?: s.positions.me?.let { MapPoint(it.lat, it.lng) }
    }

    fun locateMe() {
        val point = myPoint()
        if (point != null) {
            cameraTo(point)
            return
        }
        viewModelScope.launch {
            val fix = locationSource.currentFix()
            if (fix == null) emit(LiveMapEvent.Message(R.string.livemap_msg_no_my_location))
            else cameraTo(MapPoint(fix.lat, fix.lng))
        }
    }

    fun locatePartner() {
        val partner = _state.value.positions.partner
        if (partner == null) emit(LiveMapEvent.Message(R.string.livemap_msg_no_partner_location))
        else cameraTo(MapPoint(partner.lat, partner.lng))
    }

    fun flyTo(point: MapPoint) {
        goToMap()
        cameraTo(point, zoom = PLACE_ZOOM)
    }

    /** Long-press on the map drops a pin and asks for directions to it. */
    fun onLongPress(point: MapPoint) {
        setDestination(Destination(point, name = "", kind = DestinationKind.Pin))
        viewModelScope.launch {
            val area = repository.areaName(point.lat, point.lng)
            if (area is AppResult.Success) {
                _state.update { s ->
                    val current = s.destination
                    if (current != null && current.point == point) {
                        s.copy(destination = current.copy(name = area.data.main, address = area.data.sub))
                    } else {
                        s
                    }
                }
            }
        }
    }

    fun navigateToPartner() {
        val partner = _state.value.positions.partner
        if (partner == null) {
            emit(LiveMapEvent.Message(R.string.livemap_msg_no_partner_location))
            return
        }
        goToMap()
        setDestination(
            Destination(MapPoint(partner.lat, partner.lng), name = _state.value.partnerName, kind = DestinationKind.Partner),
        )
    }

    /** Midpoint between both people. Only possible when both have a position. */
    fun showMeetingPoint() {
        val me = myPoint()
        val partner = _state.value.positions.partner
        if (me == null || partner == null) {
            emit(LiveMapEvent.Message(R.string.livemap_msg_need_both_locations))
            return
        }
        val mid = MapPoint((me.lat + partner.lat) / 2, (me.lng + partner.lng) / 2)
        setDestination(Destination(mid, name = "", kind = DestinationKind.Meeting))
        cameraFit(listOf(me, MapPoint(partner.lat, partner.lng), mid))
        viewModelScope.launch {
            val area = repository.areaName(mid.lat, mid.lng)
            if (area is AppResult.Success) {
                _state.update { s ->
                    val current = s.destination
                    if (current != null && current.point == mid) s.copy(destination = current.copy(address = area.data.main)) else s
                }
            }
        }
    }

    fun directionsToPlace(place: SavedPlace) {
        goToMap()
        setDestination(
            Destination(
                MapPoint(place.lat, place.lng),
                name = place.name.ifBlank { place.category },
                address = place.address,
                kind = DestinationKind.Place,
            ),
        )
    }

    fun clearDestination() {
        if (_state.value.nav != null) return
        routeJob?.cancel()
        _state.update { it.copy(destination = null) }
    }

    /** Puts a destination on the map and asks for road routes from here, in the person's usual way of travelling. */
    private fun setDestination(destination: Destination) {
        if (_state.value.nav != null) return
        val mode = when (_state.value.settings.vehicle) {
            Vehicle.Walking -> TravelMode.Walk
            Vehicle.Bike -> TravelMode.Bike
            else -> TravelMode.Drive
        }
        _state.update { it.copy(destination = destination.copy(mode = mode)) }
        loadRoutes(destination.point, mode)
    }

    private var routeJob: Job? = null

    private fun loadRoutes(point: MapPoint, mode: TravelMode) {
        val origin = myPoint()
        routeJob?.cancel()
        _state.update { s ->
            s.copy(
                destination = s.destination?.copy(
                    mode = mode, routes = emptyList(), selected = 0,
                    routeLoading = origin != null, routeFailed = origin == null,
                ),
            )
        }
        if (origin == null) {
            cameraTo(point, PLACE_ZOOM)
            emit(LiveMapEvent.Message(R.string.livemap_msg_no_my_location))
            return
        }
        routeJob = viewModelScope.launch {
            val result = repository.navRoutes(GeoPoint(origin.lat, origin.lng), GeoPoint(point.lat, point.lng), mode)
            val routes = (result as? AppResult.Success)?.data.orEmpty()
            _state.update { s ->
                val current = s.destination
                if (current == null || current.point != point || current.mode != mode) {
                    s
                } else {
                    s.copy(destination = current.copy(routes = routes, selected = 0, routeLoading = false, routeFailed = routes.isEmpty()))
                }
            }
            val line = routes.firstOrNull()?.line
            cameraFit(if (line.isNullOrEmpty()) listOf(origin, point) else line.sampled(FIT_SAMPLES).map { MapPoint(it.lat, it.lng) })
        }
    }

    /** Drive / Walk / Bike chips of the directions card. */
    fun setTravelMode(mode: TravelMode) {
        val d = _state.value.destination ?: return
        if (_state.value.nav != null || d.mode == mode) return
        loadRoutes(d.point, mode)
    }

    fun retryRoutes() {
        val d = _state.value.destination ?: return
        loadRoutes(d.point, d.mode)
    }

    fun selectRoute(index: Int) {
        _state.update { s ->
            val d = s.destination ?: return@update s
            if (index !in d.routes.indices) s else s.copy(destination = d.copy(selected = index))
        }
    }

    // ── turn-by-turn navigation ──────────────────────────────────────────────────────────────────

    fun startNavigation() {
        val d = _state.value.destination ?: return
        val route = d.route ?: return
        goToMap()
        navEngine.start(
            route = route,
            destinationName = d.name.ifBlank { d.address },
            destination = GeoPoint(d.point.lat, d.point.lng),
        )
    }

    /** "End" button, the notification's End action, or "Done" after arriving. */
    fun endNavigation() {
        val arrived = _state.value.nav?.arrived == true
        navEngine.stop()
        _state.update { it.copy(nav = null, destination = if (arrived) null else it.destination, camera = resetCamera()) }
    }

    fun toggleNavMute() {
        val muted = _state.value.nav?.muted ?: return
        navEngine.setMuted(!muted)
    }

    fun recenterNavigation() = navEngine.setFollowing(true)

    /** The person dragged the map during navigation: stop following until they tap Re-center. */
    fun onUserMovedMap() {
        if (_state.value.nav?.following == true) navEngine.setFollowing(false)
    }

    private fun resetCamera(): CameraRequest {
        val target = myPoint()
        return CameraRequest(++cameraCounter, target = target, zoom = PLACE_ZOOM, bearing = 0.0, tilt = 0.0)
    }

    private fun onNavSession(session: NavSession?) {
        _state.update { s ->
            // The engine can end on its own (the notification's End button): drop the trip and flatten the map.
            if (session == null && s.nav != null) return@update s.copy(nav = null, camera = resetCamera())
            val progress = session?.progress
            val follow = session != null && session.following && progress != null
            s.copy(
                nav = session,
                camera = if (follow && progress != null) {
                    val close = progress.distanceToNextM < NAV_CLOSE_M
                    CameraRequest(
                        id = ++cameraCounter,
                        target = MapPoint(progress.snapped.lat, progress.snapped.lng),
                        zoom = navZoom(session.route.mode) + if (close) 1.0 else 0.0,
                        bearing = progress.roadBearingDeg,
                        tilt = NAV_TILT,
                        animMs = NAV_ANIM_MS,
                    )
                } else {
                    s.camera
                },
            )
        }
    }

    private fun navZoom(mode: TravelMode): Double = when (mode) {
        TravelMode.Drive -> 16.0
        TravelMode.Bike -> 17.0
        TravelMode.Walk -> 18.0
    }

    // ── places, favorites, love notes ────────────────────────────────────────────────────────────

    fun addPlace(category: String, customName: String, lat: Double, lng: Double, address: String) {
        val role = _state.value.myRole
        val name = if (category == CUSTOM_CATEGORY) customName.trim() else category
        val place = SavedPlace(
            id = "p_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
            ownerRole = role,
            category = category,
            name = name,
            lat = lat,
            lng = lng,
            address = address.trim(),
            createdAtMs = System.currentTimeMillis(),
        )
        savePlace(place)
    }

    /** "Save" on a search result or a dropped pin. */
    fun saveDestinationAsPlace() {
        val d = _state.value.destination ?: return
        savePlace(
            SavedPlace(
                id = "p_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
                ownerRole = _state.value.myRole,
                category = CUSTOM_CATEGORY,
                name = d.name,
                lat = d.point.lat,
                lng = d.point.lng,
                address = d.address,
                createdAtMs = System.currentTimeMillis(),
            ),
        )
    }

    private fun savePlace(place: SavedPlace) {
        _state.update { it.copy(savingPlace = true) }
        viewModelScope.launch {
            when (val result = repository.updatePlaces { it + place }) {
                is AppResult.Success -> {
                    _state.update { s -> s.copy(savingPlace = false, shared = s.shared.copy(places = result.data)) }
                    emit(LiveMapEvent.Message(R.string.livemap_msg_place_saved))
                }
                is AppResult.Failure -> {
                    _state.update { it.copy(savingPlace = false) }
                    emit(LiveMapEvent.Message(R.string.livemap_msg_could_not_save))
                }
            }
        }
    }

    /** Only the person who added a place can delete it. */
    fun deletePlace(id: String) {
        val place = _state.value.shared.places.firstOrNull { it.id == id } ?: return
        if (place.ownerRole != _state.value.myRole) return
        viewModelScope.launch {
            when (val result = repository.updatePlaces { list -> list.filterNot { it.id == id } }) {
                is AppResult.Success -> {
                    partnerInside.remove(id)
                    _state.update { s -> s.copy(shared = s.shared.copy(places = result.data)) }
                    emit(LiveMapEvent.Message(R.string.livemap_msg_place_deleted))
                }
                is AppResult.Failure -> emit(LiveMapEvent.Message(R.string.livemap_msg_could_not_save))
            }
        }
    }

    fun toggleFavoriteDestination() {
        val d = _state.value.destination ?: return
        val key = spotKey(d.point.lat, d.point.lng)
        val exists = _state.value.shared.favorites.any { spotKey(it.lat, it.lng) == key }
        updateFavorites(if (exists) R.string.livemap_msg_favorite_removed else R.string.livemap_msg_favorite_added) { list ->
            if (exists) list.filterNot { spotKey(it.lat, it.lng) == key }
            else listOf(FavoritePlace(d.name.ifBlank { "Saved spot" }, d.address, d.point.lat, d.point.lng)) + list
        }
    }

    fun renameFavorite(index: Int, name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        updateFavorites(null) { list -> list.mapIndexed { i, f -> if (i == index) f.copy(name = clean) else f } }
    }

    fun moveFavorite(index: Int, direction: Int) = updateFavorites(null) { list ->
        val j = index + direction
        if (index !in list.indices || j !in list.indices) {
            list
        } else {
            list.toMutableList().also { m -> m[index] = list[j]; m[j] = list[index] }
        }
    }

    fun removeFavorite(index: Int) = updateFavorites(R.string.livemap_msg_favorite_removed) { list ->
        list.filterIndexed { i, _ -> i != index }
    }

    fun openFavorite(favorite: FavoritePlace) {
        goToMap()
        setDestination(Destination(MapPoint(favorite.lat, favorite.lng), favorite.name, favorite.address, DestinationKind.Search))
    }

    private fun updateFavorites(message: Int?, change: (List<FavoritePlace>) -> List<FavoritePlace>) {
        viewModelScope.launch {
            when (val result = repository.updateFavorites(change)) {
                is AppResult.Success -> {
                    _state.update { s -> s.copy(shared = s.shared.copy(favorites = result.data)) }
                    if (message != null) emit(LiveMapEvent.Message(message))
                }
                is AppResult.Failure -> emit(LiveMapEvent.Message(R.string.livemap_msg_could_not_save))
            }
        }
    }

    /** Leaves a note at the spot where you are now. Needs this phone's position. */
    fun sendLoveNote(text: String) {
        val message = text.trim()
        val here = myPoint()
        if (message.isEmpty()) return
        if (here == null) {
            emit(LiveMapEvent.Message(R.string.livemap_msg_no_my_location))
            return
        }
        val note = LoveNote(
            id = "ln_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
            fromRole = _state.value.myRole,
            message = message,
            lat = here.lat,
            lng = here.lng,
            createdAtMs = System.currentTimeMillis(),
        )
        updateNotes(R.string.livemap_msg_note_sent) { it + note }
    }

    fun deleteLoveNote(id: String) {
        val note = _state.value.shared.loveNotes.firstOrNull { it.id == id } ?: return
        if (note.fromRole != _state.value.myRole) return
        updateNotes(R.string.livemap_msg_note_deleted) { list -> list.filterNot { it.id == id } }
    }

    private fun updateNotes(message: Int, change: (List<LoveNote>) -> List<LoveNote>) {
        viewModelScope.launch {
            when (val result = repository.updateLoveNotes(change)) {
                is AppResult.Success -> {
                    _state.update { s -> s.copy(shared = s.shared.copy(loveNotes = result.data)) }
                    emit(LiveMapEvent.Message(message))
                }
                is AppResult.Failure -> emit(LiveMapEvent.Message(R.string.livemap_msg_could_not_save))
            }
        }
    }

    // ── location alerts ──────────────────────────────────────────────────────────────────────────

    fun setAlert(key: AlertKey, on: Boolean) {
        val before = _state.value.shared.alerts
        val after = when (key) {
            AlertKey.Arrive -> before.copy(arrive = on)
            AlertKey.Leave -> before.copy(leave = on)
            AlertKey.Offline -> before.copy(offline = on)
            AlertKey.Battery -> before.copy(battery = on)
            AlertKey.Network -> before.copy(network = on)
            AlertKey.Gps -> before.copy(gps = on)
        }
        _state.update { it.copy(shared = it.shared.copy(alerts = after)) }
        viewModelScope.launch {
            // The background service reads the three "tell my partner" switches from this phone's settings.
            prefs.setTellAlerts(after.battery, after.network, after.gps)
            if (repository.saveAlertPrefs(after) is AppResult.Failure) {
                _state.update { it.copy(shared = it.shared.copy(alerts = before)) }
                prefs.setTellAlerts(before.battery, before.network, before.gps)
                emit(LiveMapEvent.Message(R.string.livemap_msg_could_not_save))
            }
        }
    }

    fun loadFeed() {
        _state.update { it.copy(feed = AlertFeedState.Loading) }
        viewModelScope.launch {
            val result = repository.geofenceEvents(ofPartner = true)
            _state.update {
                it.copy(
                    feed = when (result) {
                        is AppResult.Success ->
                            if (result.data.isEmpty()) AlertFeedState.Empty else AlertFeedState.Content(result.data)
                        is AppResult.Failure -> AlertFeedState.Error(result.error)
                    },
                )
            }
        }
    }

    // ── search ───────────────────────────────────────────────────────────────────────────────────

    fun onQueryChange(query: String) {
        _state.update { it.copy(search = it.search.copy(query = query, error = null)) }
        searchJob?.cancel()
        if (query.trim().length < MIN_QUERY) {
            _state.update { it.copy(search = it.search.copy(hits = emptyList(), loading = false, searched = false)) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            runSearch(query.trim())
        }
    }

    /** A chip like "Cafes" searches straight away, without the typing delay. */
    fun searchChip(query: String) {
        _state.update { it.copy(search = it.search.copy(query = query, error = null)) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch { runSearch(query) }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _state.update { it.copy(search = SearchUiState()) }
    }

    private suspend fun runSearch(query: String) {
        _state.update { it.copy(search = it.search.copy(loading = true, error = null)) }
        val near = myPoint()
        val result = repository.search(query, near?.lat, near?.lng)
        _state.update {
            it.copy(
                search = when (result) {
                    is AppResult.Success -> it.search.copy(loading = false, hits = result.data, searched = true)
                    is AppResult.Failure -> it.search.copy(loading = false, hits = emptyList(), error = result.error, searched = true)
                },
            )
        }
    }

    fun openHit(hit: SearchHit) {
        goToMap()
        setDestination(Destination(MapPoint(hit.lat, hit.lng), hit.name, hit.address, DestinationKind.Search))
    }

    // ── misc ─────────────────────────────────────────────────────────────────────────────────────

    fun showMessage(res: Int) = emit(LiveMapEvent.Message(res))

    private fun emit(event: LiveMapEvent) {
        _events.trySend(event)
    }

    override fun onCleared() {
        onHidden()
        super.onCleared()
    }

    private companion object {
        const val POLL_MS = 8_000L
        const val FIX_MS = 5_000L
        const val TRAIL_EVERY = 3
        const val FOLLOW_MIN_M = 5.0
        const val AREA_GAP_MS = 15_000L
        const val GEOFENCE_RADIUS_M = 100.0
        const val WEATHER_CACHE_MS = 600_000L
        const val PLACE_ZOOM = 16.0
        const val FIT_SAMPLES = 40
        const val NAV_CLOSE_M = 150.0
        const val NAV_TILT = 55.0
        const val NAV_ANIM_MS = 900
        const val MIN_QUERY = 2
        const val SEARCH_DEBOUNCE_MS = 400L
        const val CUSTOM_CATEGORY = "Custom"
    }
}
