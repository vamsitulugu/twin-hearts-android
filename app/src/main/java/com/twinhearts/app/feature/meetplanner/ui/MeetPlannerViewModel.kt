package com.twinhearts.app.feature.meetplanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.feature.livemap.data.LiveMapRepository
import com.twinhearts.app.feature.livemap.data.nav.NavEngine
import com.twinhearts.app.feature.livemap.domain.nav.NavSession
import com.twinhearts.app.feature.livemap.ui.map.CameraRequest
import com.twinhearts.app.feature.livemap.ui.map.MapPoint
import com.twinhearts.app.feature.meetplanner.domain.haversineKm
import com.twinhearts.app.feature.livemap.domain.nav.GeoPoint as NavGeoPoint
import com.twinhearts.app.feature.livemap.domain.nav.TravelMode as NavMode
import com.twinhearts.app.feature.meetplanner.data.MeetPlannerRepository
import com.twinhearts.app.feature.meetplanner.domain.PlanDraft
import com.twinhearts.app.feature.meetplanner.domain.PopularDestination
import com.twinhearts.app.feature.meetplanner.domain.SavedFilter
import com.twinhearts.app.feature.meetplanner.domain.addChecklistItem
import com.twinhearts.app.feature.meetplanner.domain.directionsUrl
import com.twinhearts.app.feature.meetplanner.domain.meetupShareText
import com.twinhearts.app.feature.meetplanner.domain.model.City
import com.twinhearts.app.feature.meetplanner.domain.model.GeoPoint
import com.twinhearts.app.feature.meetplanner.domain.model.Meetup
import com.twinhearts.app.feature.meetplanner.domain.model.Place
import com.twinhearts.app.feature.meetplanner.domain.model.PlaceCategory
import com.twinhearts.app.feature.meetplanner.domain.model.TravelMode
import com.twinhearts.app.feature.meetplanner.domain.model.TripType
import com.twinhearts.app.feature.meetplanner.domain.moveStop
import com.twinhearts.app.feature.meetplanner.domain.optimizeOrder
import com.twinhearts.app.feature.meetplanner.domain.placeDirectionsUrl
import com.twinhearts.app.feature.meetplanner.domain.removeChecklist
import com.twinhearts.app.feature.meetplanner.domain.removeStopAt
import com.twinhearts.app.feature.meetplanner.domain.toggleChecklist
import com.twinhearts.app.feature.meetplanner.domain.toggleStop
import com.twinhearts.app.feature.meetplanner.domain.withCity
import com.twinhearts.app.feature.meetplanner.domain.withFrom
import com.twinhearts.app.feature.meetplanner.domain.withTo
import com.twinhearts.app.feature.meetplanner.domain.withTripType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Why this file exists: all Meet Planner behaviour in one place. The screens only show
 * [state] and call the [MeetActions] methods; nothing here touches Android views (R-42).
 */
@HiltViewModel
class MeetPlannerViewModel @Inject constructor(
    private val repo: MeetPlannerRepository,
    private val navRepo: LiveMapRepository,
    private val navEngine: NavEngine,
) : ViewModel(), MeetActions {

    private val _state = MutableStateFlow(MeetUiState(draft = PlanDraft.fresh(LocalDate.now())))
    val state: StateFlow<MeetUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<MeetEffect>(extraBufferCapacity = 8)
    val effects: SharedFlow<MeetEffect> = _effects.asSharedFlow()

    private var cityJob: Job? = null
    private var poiJob: Job? = null
    private var checklistClock = 0L

    private var cameraCounter = 0L
    private var switchingLeg = false

    init {
        loadPlans(silent = false)
        viewModelScope.launch { navEngine.session.collect { onNavSession(it) } }
    }

    override fun onCleared() {
        // Leaving Meet Planner ends a trip started from it; a trip started in Live Map is not ours to stop.
        if (_state.value.navActive) navEngine.stop()
        super.onCleared()
    }

    // ───────────────────────────── helpers ─────────────────────────────

    private fun toast(res: Int, vararg args: Any) {
        _effects.tryEmit(MeetEffect.Toast(res, args.toList()))
    }

    private fun editDraft(change: (PlanDraft) -> PlanDraft) = _state.update { it.copy(draft = change(it.draft)) }

    private fun push(screen: MeetScreen) {
        _state.update { if (it.screen == screen) it else it.copy(stack = it.stack + screen) }
        onEnter(screen)
    }

    private fun onEnter(screen: MeetScreen) {
        when (screen) {
            MeetScreen.Saved -> loadPlans(silent = true)
            MeetScreen.Search -> if (_state.value.poi == PoiState.Idle) loadPlaces()
            else -> Unit
        }
    }

    private fun resetDraft() = _state.update {
        it.copy(
            draft = PlanDraft.fresh(LocalDate.now()),
            cityQuery = "",
            citySuggest = CitySuggest.Idle,
            poiCategory = PlaceCategory.Hotel,
            poiQuery = "",
            poi = PoiState.Idle,
            startMinutes = MeetUiState.DEFAULT_START_MINUTES,
        )
    }

    // ───────────────────────────── loading plans ─────────────────────────────

    /** [silent] keeps the list on screen while it reloads (pull to refresh, coming back to Saved). */
    private fun loadPlans(silent: Boolean) {
        if (!silent || _state.value.plans !is UiState.Content) _state.update { it.copy(plans = UiState.Loading) }
        viewModelScope.launch {
            val who = _state.value.identity ?: repo.identity()
            if (who == null) {
                _state.update { it.copy(plans = UiState.Empty) }
                return@launch
            }
            val result = repo.plans(who)
            _state.update {
                it.copy(
                    identity = who,
                    plans = when (result) {
                        is AppResult.Success -> if (result.data.isEmpty()) UiState.Empty else UiState.Content(result.data)
                        // A silent refresh that fails keeps what we already show.
                        is AppResult.Failure -> if (silent && it.plans is UiState.Content) it.plans else UiState.Error(result.error)
                    },
                )
            }
        }
    }

    override fun refresh() = loadPlans(silent = true)

    // ───────────────────────────── moving around ─────────────────────────────

    override fun back() {
        _state.update { if (it.stack.size > 1) it.copy(stack = it.stack.dropLast(1)) else it }
        exitSelect()
        onEnter(_state.value.screen)
    }

    override fun startNew() {
        resetDraft()
        push(MeetScreen.Plan)
    }

    override fun openItinerary() = push(MeetScreen.Itinerary)

    override fun openSaved() = push(MeetScreen.Saved)

    override fun pickPopular(destination: PopularDestination) {
        resetDraft()
        _state.update {
            it.copy(draft = it.draft.withCity(City(destination.name, destination.name, destination.lat, destination.lng)))
        }
        push(MeetScreen.Plan)
    }

    override fun openPlan(id: String) {
        _state.update { it.copy(detailId = id) }
        push(MeetScreen.Detail)
    }

    // ───────────────────────────── Plan New ─────────────────────────────

    override fun setTitle(value: String) = editDraft { it.copy(title = value.take(MAX_TITLE)) }
    override fun setFrom(date: LocalDate) = editDraft { it.withFrom(date) }
    override fun setTo(date: LocalDate) = editDraft { it.withTo(date) }
    override fun setTripType(type: TripType) = editDraft { it.withTripType(type) }
    override fun setBudget(value: String) = editDraft { it.copy(budget = value.filter { c -> c.isDigit() || c == '.' }) }
    override fun setNotes(value: String) = editDraft { it.copy(notes = value) }

    override fun setMode(mode: TravelMode) {
        editDraft { it.copy(mode = mode) }
        val d = _state.value.draft
        if (d.routeBuilt) {
            viewModelScope.launch {
                val route = repo.buildRoute(d.stops, mode)
                editDraft { cur -> if (cur.stops == d.stops && cur.mode == mode) cur.copy(route = route) else cur }
            }
        }
    }

    override fun onCityQuery(value: String) {
        _state.update { it.copy(cityQuery = value) }
        cityJob?.cancel()
        if (value.trim().length < MIN_CITY_QUERY) {
            _state.update { it.copy(citySuggest = CitySuggest.Idle) }
            return
        }
        cityJob = viewModelScope.launch {
            delay(CITY_DEBOUNCE_MS)
            _state.update { it.copy(citySuggest = CitySuggest.Loading) }
            val result = repo.searchCities(value.trim())
            _state.update {
                it.copy(
                    citySuggest = when (result) {
                        is AppResult.Success -> if (result.data.isEmpty()) CitySuggest.Empty else CitySuggest.Results(result.data)
                        is AppResult.Failure -> CitySuggest.Failed
                    },
                )
            }
        }
    }

    override fun pickCity(city: City) {
        cityJob?.cancel()
        _state.update {
            it.copy(
                draft = it.draft.withCity(city),
                cityQuery = "",
                citySuggest = CitySuggest.Idle,
                poi = PoiState.Idle,
                poiCategory = it.poiCategory ?: PlaceCategory.Hotel,
            )
        }
    }

    override fun changeCity() {
        editDraft { it.withCity(null) }
        _state.update { it.copy(poi = PoiState.Idle) }
    }

    override fun useMyLocation() {
        if (!repo.hasLocationPermission()) {
            _effects.tryEmit(MeetEffect.NeedLocationPermission)
            return
        }
        if (_state.value.locating) return
        _state.update { it.copy(locating = true) }
        viewModelScope.launch {
            val point: GeoPoint? = repo.currentLocation()
            if (point == null) {
                toast(R.string.meetplanner_toast_no_location)
            } else {
                when (val r = repo.cityAt(point)) {
                    is AppResult.Success -> pickCity(r.data)
                    is AppResult.Failure -> toast(R.string.meetplanner_toast_city_failed)
                }
            }
            _state.update { it.copy(locating = false) }
        }
    }

    override fun createPlan() {
        if (_state.value.draft.city == null) {
            toast(R.string.meetplanner_toast_choose_city)
            return
        }
        push(MeetScreen.Search)
    }

    // ───────────────────────────── Find Places ─────────────────────────────

    override fun setCategory(category: PlaceCategory) {
        _state.update { it.copy(poiCategory = category, poiQuery = "") }
        loadPlaces()
    }

    override fun setQuery(value: String) = _state.update { it.copy(poiQuery = value) }

    override fun retryPlaces() {
        if (_state.value.poiCategory == null) submitSearch() else loadPlaces()
    }

    private fun loadPlaces() {
        val s = _state.value
        val city = s.draft.city ?: return
        val category = s.poiCategory ?: return
        poiJob?.cancel()
        _state.update { it.copy(poi = PoiState.Loading) }
        poiJob = viewModelScope.launch {
            val result = repo.nearby(category, city)
            _state.update {
                it.copy(
                    poi = when (result) {
                        is AppResult.Success -> PoiState.Results(result.data)
                        is AppResult.Failure -> PoiState.Failed(result.error)
                    },
                )
            }
        }
    }

    override fun submitSearch() {
        val s = _state.value
        val query = s.poiQuery.trim()
        if (query.isEmpty()) return
        val city = s.draft.city
        if (city == null) {
            toast(R.string.meetplanner_toast_choose_city_short)
            return
        }
        poiJob?.cancel()
        _state.update { it.copy(poiCategory = null, poi = PoiState.Loading) }
        poiJob = viewModelScope.launch {
            val result = repo.searchText(query, city)
            _state.update {
                it.copy(
                    poi = when (result) {
                        is AppResult.Success -> PoiState.Results(result.data)
                        is AppResult.Failure -> PoiState.Failed(result.error)
                    },
                )
            }
        }
    }

    override fun toggleStop(place: Place) = editDraft { it.toggleStop(place) }

    override fun openPlace(place: Place) = _state.update { it.copy(placeSheet = place) }

    override fun closePlace() = _state.update { it.copy(placeSheet = null) }

    override fun goStops() = push(MeetScreen.Stops)

    // ───────────────────────────── Your Stops ─────────────────────────────

    override fun moveStop(index: Int, direction: Int) = editDraft { it.moveStop(index, direction) }

    override fun removeStop(index: Int) = editDraft { it.removeStopAt(index) }

    override fun buildRoute() {
        val d = _state.value.draft
        if (d.stops.size < 2) {
            toast(R.string.meetplanner_toast_pick_two)
            return
        }
        if (_state.value.building) return
        _state.update { it.copy(building = true) }
        viewModelScope.launch {
            val ordered = optimizeOrder(d.stops)
            val route = repo.buildRoute(ordered, d.mode)
            _state.update { it.copy(draft = it.draft.copy(stops = ordered, route = route), building = false) }
            push(MeetScreen.Itinerary)
        }
    }

    // ───────────────────────────── Itinerary ─────────────────────────────

    override fun setStartTime(minutes: Int) = _state.update { it.copy(startMinutes = minutes) }

    override fun addChecklistItem(text: String) = editDraft { it.addChecklistItem(text, nextChecklistId()) }

    override fun toggleChecklist(id: Long) = editDraft { it.toggleChecklist(id) }

    override fun removeChecklist(id: Long) = editDraft { it.removeChecklist(id) }

    /** Time in milliseconds like the web app, bumped so two quick taps never share an id. */
    private fun nextChecklistId(): Long {
        checklistClock = maxOf(System.currentTimeMillis(), checklistClock + 1)
        return checklistClock
    }

    override fun openRouteDraft() {
        val d = _state.value.draft
        val route = d.route
        if (d.stops.size < 2 || route == null) {
            toast(R.string.meetplanner_toast_pick_two_first)
            return
        }
        _state.update {
            it.copy(
                routeView = RouteView(
                    title = d.city?.name ?: "",
                    stops = d.stops,
                    points = route.points,
                    distanceKm = route.distanceKm,
                    durationMin = route.durationMin,
                    mode = d.mode,
                ),
            )
        }
        push(MeetScreen.Route)
    }

    override fun savePlan() {
        val s = _state.value
        val who = s.identity
        if (who == null) {
            toast(R.string.meetplanner_toast_not_connected)
            return
        }
        if (s.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            when (val r = repo.save(who, s.draft)) {
                is AppResult.Success -> {
                    toast(R.string.meetplanner_toast_saved)
                    resetDraft()
                    _state.update { it.copy(saving = false, stack = listOf(MeetScreen.Home, MeetScreen.Saved)) }
                    loadPlans(silent = true)
                }
                is AppResult.Failure -> {
                    _state.update { it.copy(saving = false) }
                    _effects.tryEmit(MeetEffect.Failure(r.error))
                }
            }
        }
    }

    // ───────────────────────────── Route preview ─────────────────────────────

    override fun openRouteSaved(id: String) {
        val plan = _state.value.meetups.firstOrNull { it.id == id } ?: return
        if (plan.stops.isEmpty()) {
            toast(R.string.meetplanner_toast_no_stops)
            return
        }
        _state.update {
            it.copy(
                optionsId = null,
                routeView = RouteView(plan.title, plan.stops, plan.routePoints, plan.distanceKm, plan.durationMin, plan.travelMode),
            )
        }
        push(MeetScreen.Route)
    }

    /** The old behaviour, kept as "Open in Maps app" and as the fallback when in-app routing cannot work. */
    override fun openInMaps() {
        val view = _state.value.routeView ?: return
        directionsUrl(view.stops, view.mode)?.let { _effects.tryEmit(MeetEffect.OpenUrl(it)) }
    }

    /**
     * Real turn-by-turn inside the app, one leg at a time: from where you are to the first stop you are
     * not already at, then on to each next stop. Transit has no road router, so it opens the maps app.
     */
    override fun startNavigation() {
        val s = _state.value
        val view = s.routeView ?: return
        if (s.navLoading) return
        if (view.mode == TravelMode.Transit) {
            openInMaps()
            return
        }
        if (!repo.hasLocationPermission()) {
            _effects.tryEmit(MeetEffect.NeedNavPermission)
            return
        }
        _state.update { it.copy(navLoading = true) }
        viewModelScope.launch {
            val here = repo.currentLocation()
            if (here == null) {
                toast(R.string.meetplanner_toast_no_location)
                _state.update { it.copy(navLoading = false) }
                return@launch
            }
            // Already standing at a stop? Head for the one after it. Otherwise start with the first stop.
            val at = view.stops.indexOfFirst { haversineKm(here, GeoPoint(it.lat, it.lng)) * 1000 < AT_STOP_M }
            val target = if (at >= 0) at + 1 else 0
            if (target >= view.stops.size) {
                toast(R.string.meetplanner_toast_already_there)
                _state.update { it.copy(navLoading = false) }
                return@launch
            }
            beginLeg(here, view, target)
        }
    }

    /** "Next stop" after arriving: the stop we just reached is where the next leg starts. */
    override fun nextStop() {
        val s = _state.value
        val view = s.routeView ?: return
        val next = s.navStop + 1
        if (next >= view.stops.size || s.navLoading) return
        val from = view.stops[s.navStop]
        _state.update { it.copy(navLoading = true) }
        viewModelScope.launch { beginLeg(GeoPoint(from.lat, from.lng), view, next) }
    }

    private suspend fun beginLeg(from: GeoPoint, view: RouteView, index: Int) {
        val stop = view.stops[index]
        val to = NavGeoPoint(stop.lat, stop.lng)
        val result = navRepo.navRoutes(NavGeoPoint(from.lat, from.lng), to, view.mode.navMode())
        val route = (result as? AppResult.Success)?.data?.firstOrNull()
        if (route == null) {
            // No internet or the free router is busy: hand over to the maps app instead of leaving the button dead.
            toast(R.string.meetplanner_toast_route_failed)
            _state.update { it.copy(navLoading = false) }
            openInMaps()
            return
        }
        // Mark the trip as ours first, so the engine's first update is picked up by onNavSession.
        switchingLeg = true
        _state.update { it.copy(navActive = true, navStop = index, navLoading = false, navCamera = null) }
        navEngine.start(route, stop.name, to)
        switchingLeg = false
    }

    override fun endNavigation() {
        navEngine.stop()
        _state.update { it.copy(navActive = false, nav = null, navCamera = null, navStop = 0, navLoading = false) }
    }

    override fun toggleNavMute() {
        val muted = _state.value.nav?.muted ?: return
        navEngine.setMuted(!muted)
    }

    override fun recenterNavigation() = navEngine.setFollowing(true)

    /** The person dragged the map during navigation: stop following until they tap Re-center. */
    override fun onUserMovedMap() {
        if (_state.value.nav?.following == true) navEngine.setFollowing(false)
    }

    private fun onNavSession(session: NavSession?) {
        _state.update { s ->
            if (!s.navActive) return@update s
            // The trip ended on its own (the notification's End button): leave navigation mode.
            if (session == null) {
                return@update if (switchingLeg) s else s.copy(navActive = false, nav = null, navCamera = null)
            }
            val progress = session.progress
            val follow = session.following && progress != null
            s.copy(
                nav = session,
                navCamera = if (follow && progress != null) {
                    CameraRequest(
                        id = ++cameraCounter,
                        target = MapPoint(progress.snapped.lat, progress.snapped.lng),
                        zoom = navZoom(session) + if (progress.distanceToNextM < NAV_CLOSE_M) 1.0 else 0.0,
                        bearing = progress.roadBearingDeg,
                        tilt = NAV_TILT,
                        animMs = NAV_ANIM_MS,
                    )
                } else {
                    s.navCamera
                },
            )
        }
    }

    private fun navZoom(session: NavSession): Double = when (session.route.mode) {
        NavMode.Drive -> 16.0
        NavMode.Bike -> 17.0
        NavMode.Walk -> 18.0
    }

    private fun TravelMode.navMode(): NavMode = when (this) {
        TravelMode.Walk -> NavMode.Walk
        TravelMode.Bike, TravelMode.Cycle -> NavMode.Bike
        else -> NavMode.Drive
    }

    // ───────────────────────────── Saved meetups ─────────────────────────────

    override fun setFilter(filter: SavedFilter) {
        exitSelect()
        _state.update { it.copy(savedFilter = filter) }
    }

    override fun enterSelect(id: String) = _state.update { it.copy(selectMode = true, selected = it.selected + id) }

    override fun toggleSelect(id: String) {
        _state.update {
            val next = if (id in it.selected) it.selected - id else it.selected + id
            // Un-ticking the last one leaves selection mode, like the web app.
            it.copy(selected = next, selectMode = next.isNotEmpty())
        }
    }

    override fun exitSelect() = _state.update { it.copy(selectMode = false, selected = emptySet()) }

    override fun deleteSelected() {
        val ids = _state.value.selected.toList()
        if (ids.isNotEmpty()) _state.update { it.copy(deleteIds = ids) }
    }

    // ───────────────────────────── Detail, options, edit ─────────────────────────────

    override fun toggleSavedCheck(index: Int) {
        val s = _state.value
        val plan = s.detail ?: return
        val who = s.identity ?: return
        if (index !in plan.checklist.indices) return
        val list = plan.checklist.mapIndexed { i, c -> if (i == index) c.copy(done = !c.done) else c }
        replacePlan(plan.copy(checklist = list))
        viewModelScope.launch {
            if (repo.saveChecklist(who, plan.id, list) is AppResult.Failure) {
                toast(R.string.meetplanner_toast_checklist_failed)
                loadPlans(silent = true)
            }
        }
    }

    private fun replacePlan(updated: Meetup) = _state.update { s ->
        val data = s.meetups.map { if (it.id == updated.id) updated else it }
        s.copy(plans = if (data.isEmpty()) UiState.Empty else UiState.Content(data))
    }

    override fun openOptions(id: String) = _state.update { it.copy(optionsId = id) }
    override fun closeOptions() = _state.update { it.copy(optionsId = null) }

    override fun openEdit(id: String) = _state.update { it.copy(optionsId = null, editId = id) }
    override fun closeEdit() = _state.update { it.copy(editId = null) }

    override fun saveEdit(id: String, title: String, from: LocalDate?, to: LocalDate?, budget: String, notes: String) {
        val s = _state.value
        val who = s.identity ?: return
        val plan = s.meetups.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            when (val r = repo.edit(who, id, title, from, to, budget, notes, plan.tripType)) {
                is AppResult.Success -> {
                    _state.update { it.copy(editId = null) }
                    toast(R.string.meetplanner_toast_updated)
                    loadPlans(silent = true)
                }
                is AppResult.Failure -> _effects.tryEmit(MeetEffect.Failure(r.error))
            }
        }
    }

    // ───────────────────────────── Complete (Memory Globe) ─────────────────────────────

    override fun openComplete(id: String) =
        _state.update { it.copy(optionsId = null, completeId = id, completePhotos = emptyList(), uploadingCount = 0) }

    override fun closeComplete() = _state.update { it.copy(completeId = null, completePhotos = emptyList()) }

    override fun addPhotos(uris: List<String>) {
        val who = _state.value.identity ?: return
        val fresh = uris.filter { uri -> _state.value.completePhotos.none { it.uri == uri } }.distinct()
        if (fresh.isEmpty()) return
        _state.update { it.copy(completePhotos = it.completePhotos + fresh.map { u -> CompletePhoto(u) }, uploadingCount = it.uploadingCount + fresh.size) }
        fresh.forEach { uri ->
            viewModelScope.launch {
                val result = repo.uploadPhoto(who, uri)
                _state.update { s ->
                    val photos = when (result) {
                        is AppResult.Success -> s.completePhotos.map { if (it.uri == uri) it.copy(url = result.data) else it }
                        is AppResult.Failure -> s.completePhotos.filterNot { it.uri == uri }
                    }
                    s.copy(completePhotos = photos, uploadingCount = maxOf(0, s.uploadingCount - 1))
                }
                if (result is AppResult.Failure) toast(R.string.meetplanner_toast_photo_failed)
            }
        }
    }

    override fun removePhoto(index: Int) =
        _state.update { it.copy(completePhotos = it.completePhotos.filterIndexed { i, _ -> i != index }) }

    override fun confirmComplete(mood: String?, extraNotes: String) {
        val s = _state.value
        val who = s.identity ?: return
        val id = s.completeId ?: return
        if (s.uploadingCount > 0) {
            toast(R.string.meetplanner_toast_photos_uploading)
            return
        }
        if (s.completing) return
        _state.update { it.copy(completing = true) }
        viewModelScope.launch {
            val urls = s.completePhotos.mapNotNull { it.url }
            when (val r = repo.complete(who, id, mood, urls, extraNotes.trim().ifEmpty { null })) {
                is AppResult.Success -> {
                    _state.update { it.copy(completing = false, completeId = null, completePhotos = emptyList()) }
                    toast(R.string.meetplanner_toast_completed)
                    loadPlans(silent = true)
                }
                is AppResult.Failure -> {
                    _state.update { it.copy(completing = false) }
                    _effects.tryEmit(MeetEffect.Failure(r.error))
                }
            }
        }
    }

    override fun viewPhotos(id: String) {
        val s = _state.value
        val who = s.identity ?: return
        val memoryId = s.meetups.firstOrNull { it.id == id }?.globeMemoryId ?: return
        _state.update { it.copy(optionsId = null) }
        viewModelScope.launch {
            when (val r = repo.photos(who, memoryId)) {
                is AppResult.Success ->
                    if (r.data.isEmpty()) toast(R.string.meetplanner_toast_no_photos) else _effects.tryEmit(MeetEffect.ViewPhotos(r.data))
                is AppResult.Failure -> _effects.tryEmit(MeetEffect.Failure(r.error))
            }
        }
    }

    // ───────────────────────────── share, copy, delete ─────────────────────────────

    override fun share(id: String) {
        val plan = _state.value.meetups.firstOrNull { it.id == id } ?: return
        _state.update { it.copy(optionsId = null) }
        _effects.tryEmit(MeetEffect.Share(meetupShareText(plan)))
    }

    override fun copy(id: String) {
        val plan = _state.value.meetups.firstOrNull { it.id == id } ?: return
        _state.update { it.copy(optionsId = null) }
        _effects.tryEmit(MeetEffect.Copy(meetupShareText(plan)))
    }

    override fun requestDelete(id: String) = _state.update { it.copy(optionsId = null, deleteIds = listOf(id)) }

    override fun cancelDelete() = _state.update { it.copy(deleteIds = null) }

    override fun confirmDelete() {
        val s = _state.value
        val ids = s.deleteIds ?: return
        val who = s.identity ?: return
        _state.update { it.copy(deleteIds = null) }
        viewModelScope.launch {
            val summary = repo.delete(who, ids)
            exitSelect()
            if (s.screen == MeetScreen.Detail && summary.deleted > 0) back()
            loadPlans(silent = true)
            when {
                summary.failed > 0 -> toast(R.string.meetplanner_toast_delete_partial, summary.deleted, summary.failed)
                summary.deleted > 1 -> toast(R.string.meetplanner_toast_deleted_many, summary.deleted)
                else -> toast(R.string.meetplanner_toast_deleted)
            }
        }
    }

    /** "Get Directions" on the place sheet is a plain link, so the screen asks for it here. */
    fun placeLink(place: Place): String = placeDirectionsUrl(place)

    private companion object {
        const val MAX_TITLE = 80
        const val MIN_CITY_QUERY = 2
        const val CITY_DEBOUNCE_MS = 350L
    }
}

private const val AT_STOP_M = 150.0
private const val NAV_CLOSE_M = 150.0
private const val NAV_TILT = 55.0
private const val NAV_ANIM_MS = 900
