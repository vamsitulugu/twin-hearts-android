package com.twinhearts.app.feature.livemap.data

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.map
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.livemap.data.mapper.toAlertPrefs
import com.twinhearts.app.feature.livemap.data.mapper.toAlertsJson
import com.twinhearts.app.feature.livemap.data.mapper.toAreaName
import com.twinhearts.app.feature.livemap.data.mapper.toCouple
import com.twinhearts.app.feature.livemap.data.mapper.toDaySummary
import com.twinhearts.app.feature.livemap.data.mapper.toDrivingRoute
import com.twinhearts.app.feature.livemap.data.mapper.toFavorites
import com.twinhearts.app.feature.livemap.data.mapper.toFavoritesJson
import com.twinhearts.app.feature.livemap.data.mapper.toGeofenceEvents
import com.twinhearts.app.feature.livemap.data.mapper.toHits
import com.twinhearts.app.feature.livemap.data.mapper.toLoveNotes
import com.twinhearts.app.feature.livemap.data.mapper.toLoveNotesJson
import com.twinhearts.app.feature.livemap.data.mapper.toPlaces
import com.twinhearts.app.feature.livemap.data.mapper.toPlacesJson
import com.twinhearts.app.feature.livemap.data.mapper.toRouteDay
import com.twinhearts.app.feature.livemap.data.mapper.toSettings
import com.twinhearts.app.feature.livemap.data.mapper.toTimeline
import com.twinhearts.app.feature.livemap.data.mapper.toTrail
import com.twinhearts.app.feature.livemap.data.mapper.toWeather
import com.twinhearts.app.feature.livemap.data.remote.BatchBody
import com.twinhearts.app.feature.livemap.data.remote.BatchPointDto
import com.twinhearts.app.feature.livemap.data.remote.GeoServicesApi
import com.twinhearts.app.feature.livemap.data.remote.LiveMapApi
import com.twinhearts.app.feature.livemap.data.remote.PingBody
import com.twinhearts.app.feature.livemap.data.remote.RouteSettingsBody
import com.twinhearts.app.feature.livemap.data.remote.SafetyAlertBody
import com.twinhearts.app.feature.livemap.data.remote.SaveStateBody
import com.twinhearts.app.feature.livemap.data.remote.StatusBody
import com.twinhearts.app.feature.livemap.data.remote.StopBody
import com.twinhearts.app.feature.livemap.domain.model.AlertPrefs
import com.twinhearts.app.feature.livemap.domain.model.AreaName
import com.twinhearts.app.feature.livemap.domain.model.CouplePositions
import com.twinhearts.app.feature.livemap.domain.model.DeviceFix
import com.twinhearts.app.feature.livemap.domain.model.DaySummary
import com.twinhearts.app.feature.livemap.domain.model.DrivingRoute
import com.twinhearts.app.feature.livemap.domain.model.FavoritePlace
import com.twinhearts.app.feature.livemap.domain.model.GeofenceEvent
import com.twinhearts.app.feature.livemap.domain.model.LoveNote
import com.twinhearts.app.feature.livemap.domain.model.RouteDay
import com.twinhearts.app.feature.livemap.domain.model.RouteSettings
import com.twinhearts.app.feature.livemap.domain.model.SavedPlace
import com.twinhearts.app.feature.livemap.domain.model.SearchHit
import com.twinhearts.app.feature.livemap.domain.model.SharedMapState
import com.twinhearts.app.feature.livemap.domain.model.TimelineEvent
import com.twinhearts.app.feature.livemap.domain.model.Vehicle
import com.twinhearts.app.feature.livemap.domain.model.WeatherNow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import com.twinhearts.app.feature.livemap.data.mapper.toNavRoutes
import com.twinhearts.app.feature.livemap.domain.nav.GeoPoint
import com.twinhearts.app.feature.livemap.domain.nav.NavRoute
import com.twinhearts.app.feature.livemap.domain.nav.TravelMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.URLEncoder
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Everything the Live Map screens and the sharing service need from the network (R-40). */
interface LiveMapRepository {
    suspend fun positions(): AppResult<CouplePositions>
    suspend fun ping(fix: DeviceFix, vehicle: Vehicle, emergency: Boolean): AppResult<Unit>
    suspend fun sendBatch(fixes: List<DeviceFix>): AppResult<Unit>
    suspend fun pause(minutes: Int?): AppResult<Unit>
    suspend fun resume(): AppResult<Unit>
    suspend fun stopSharing(): AppResult<Unit>
    suspend fun trail(ofPartner: Boolean): AppResult<List<Pair<Double, Double>>>
    suspend fun safetyAlert(type: String): AppResult<Unit>

    suspend fun sharedState(): AppResult<SharedMapState>
    suspend fun updatePlaces(change: (List<SavedPlace>) -> List<SavedPlace>): AppResult<List<SavedPlace>>
    suspend fun updateFavorites(change: (List<FavoritePlace>) -> List<FavoritePlace>): AppResult<List<FavoritePlace>>
    suspend fun updateLoveNotes(change: (List<LoveNote>) -> List<LoveNote>): AppResult<List<LoveNote>>
    suspend fun saveAlertPrefs(prefs: AlertPrefs): AppResult<Unit>

    suspend fun routeDates(ofPartner: Boolean): AppResult<List<String>>
    suspend fun routeDay(ofPartner: Boolean, date: String): AppResult<RouteDay>
    suspend fun timeline(ofPartner: Boolean, date: String): AppResult<List<TimelineEvent>>
    suspend fun daySummary(ofPartner: Boolean, date: String): AppResult<DaySummary?>
    suspend fun routeSettings(): AppResult<RouteSettings>
    suspend fun saveRouteSettings(enabled: Boolean, retentionDays: Int?): AppResult<RouteSettings>
    suspend fun clearMyRouteHistory(): AppResult<Unit>
    suspend fun geofenceEvents(ofPartner: Boolean): AppResult<List<GeofenceEvent>>

    /** Routes with turn-by-turn steps, best first, up to three. Tries a second server when the first fails. */
    suspend fun navRoutes(from: GeoPoint, to: GeoPoint, mode: TravelMode): AppResult<List<NavRoute>>
    suspend fun drivingRoute(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): AppResult<DrivingRoute?>
    suspend fun search(query: String, nearLat: Double?, nearLng: Double?): AppResult<List<SearchHit>>
    suspend fun areaName(lat: Double, lng: Double): AppResult<AreaName>
    suspend fun weather(lat: Double, lng: Double): AppResult<WeatherNow?>
}

@Singleton
class LiveMapRepositoryImpl @Inject constructor(
    private val api: LiveMapApi,
    private val geo: GeoServicesApi,
    private val sessionStore: SessionStore,
) : LiveMapRepository {

    private suspend fun session(): Session? = sessionStore.session.first()

    /** Every call needs a signed-in session; without one it is the same as a 401. */
    private suspend fun <T> withSession(block: suspend (Session) -> AppResult<T>): AppResult<T> {
        val s = session() ?: return AppResult.Failure(AppError.Unauthorized())
        return block(s)
    }

    private fun Session.roleOf(partner: Boolean): String =
        if (partner) (if (role == "user1") "user2" else "user1") else role

    override suspend fun positions() = withSession { s ->
        safeApiCall { api.getLocations(s.coupleId) }.map { it.toCouple(s.role) }
    }

    override suspend fun ping(fix: DeviceFix, vehicle: Vehicle, emergency: Boolean) = withSession { s ->
        safeApiCall {
            api.ping(
                PingBody(
                    coupleId = s.coupleId,
                    role = s.role,
                    lat = fix.lat,
                    lng = fix.lng,
                    accuracy = fix.accuracyM,
                    heading = fix.headingDeg,
                    speed = fix.speedMps,
                    altitude = fix.altitudeM,
                    moving = (fix.speedMps ?: 0.0) > MOVING_MPS,
                    emergency = if (emergency) true else null,
                    vehicleType = vehicle.wire,
                    mockLocation = if (fix.mock) true else null,
                    localDate = LocalDate.now().toString(),
                    source = "foreground",
                ),
            )
        }.map { }
    }

    override suspend fun sendBatch(fixes: List<DeviceFix>) = withSession { s ->
        safeApiCall {
            // The server accepts at most 2000 points per call.
            fixes.chunked(BATCH_MAX).forEach { chunk ->
                api.sendBatch(
                    BatchBody(
                        coupleId = s.coupleId,
                        role = s.role,
                        points = chunk.map {
                            BatchPointDto(
                                lat = it.lat,
                                lng = it.lng,
                                accuracy = it.accuracyM,
                                heading = it.headingDeg,
                                speed = it.speedMps,
                                altitude = it.altitudeM,
                                moving = (it.speedMps ?: 0.0) > MOVING_MPS,
                                mockLocation = if (it.mock) true else null,
                                ts = it.atMs,
                            )
                        },
                    ),
                )
            }
        }
    }

    override suspend fun pause(minutes: Int?) = withSession { s ->
        safeApiCall { api.setStatus(StatusBody(s.coupleId, s.role, "paused", minutes)) }.map { }
    }

    override suspend fun resume() = withSession { s ->
        safeApiCall { api.setStatus(StatusBody(s.coupleId, s.role, "active")) }.map { }
    }

    override suspend fun stopSharing() = withSession { s ->
        safeApiCall { api.stop(StopBody(s.coupleId, s.role)) }.map { }
    }

    override suspend fun trail(ofPartner: Boolean) = withSession { s ->
        safeApiCall { api.getTrail(s.coupleId, s.roleOf(ofPartner)) }.map { it.toTrail() }
    }

    override suspend fun safetyAlert(type: String) = withSession { s ->
        safeApiCall { api.sendSafetyAlert(SafetyAlertBody(s.coupleId, s.role, type, s.myName.ifBlank { s.role })) }.map { }
    }

    // ── shared couple state ──────────────────────────────────────────────────────────────────

    /** The saved state, or an empty object when the couple has none yet (the server answers JSON null). */
    private suspend fun readState(s: Session): AppResult<JsonObject> =
        safeApiCall { api.getState(s.coupleId) }.map { element: JsonElement -> element as? JsonObject ?: JsonObject(emptyMap()) }

    private suspend fun writeKey(s: Session, key: String, value: JsonElement): AppResult<Unit> =
        safeApiCall {
            api.saveState(
                SaveStateBody(
                    coupleId = s.coupleId,
                    state = buildJsonObject { put(key, value) },
                    senderRole = s.role,
                    myName = s.myName,
                ),
            )
        }.map { }

    override suspend fun sharedState() = withSession { s ->
        readState(s).map { state ->
            SharedMapState(
                places = state.toPlaces(),
                favorites = state.toFavorites(),
                loveNotes = state.toLoveNotes(),
                alerts = state.toAlertPrefs(),
            )
        }
    }

    /**
     * Reads the newest list, applies [change], writes it back. Reading first keeps a place the
     * partner just added from being overwritten by an old copy on this phone.
     */
    override suspend fun updatePlaces(change: (List<SavedPlace>) -> List<SavedPlace>) = withSession { s ->
        when (val read = readState(s)) {
            is AppResult.Failure -> read
            is AppResult.Success -> {
                val next = change(read.data.toPlaces())
                writeKey(s, "placesList", next.toPlacesJson()).map { next }
            }
        }
    }

    override suspend fun updateFavorites(change: (List<FavoritePlace>) -> List<FavoritePlace>) = withSession { s ->
        when (val read = readState(s)) {
            is AppResult.Failure -> read
            is AppResult.Success -> {
                val next = change(read.data.toFavorites())
                writeKey(s, "favorites", next.toFavoritesJson()).map { next }
            }
        }
    }

    override suspend fun updateLoveNotes(change: (List<LoveNote>) -> List<LoveNote>) = withSession { s ->
        when (val read = readState(s)) {
            is AppResult.Failure -> read
            is AppResult.Success -> {
                val next = change(read.data.toLoveNotes())
                writeKey(s, "loveNotes", next.toLoveNotesJson()).map { next }
            }
        }
    }

    override suspend fun saveAlertPrefs(prefs: AlertPrefs) = withSession { s ->
        writeKey(s, "lmAlerts", prefs.toAlertsJson())
    }

    // ── route history ────────────────────────────────────────────────────────────────────────

    override suspend fun routeDates(ofPartner: Boolean) = withSession { s ->
        safeApiCall { api.getRouteDates(s.coupleId, s.roleOf(ofPartner)) }.map { it.dates }
    }

    override suspend fun routeDay(ofPartner: Boolean, date: String) = withSession { s ->
        safeApiCall { api.getRouteDay(s.coupleId, s.roleOf(ofPartner), date) }.map { it.toRouteDay(date) }
    }

    override suspend fun timeline(ofPartner: Boolean, date: String) = withSession { s ->
        safeApiCall { api.getTimeline(s.coupleId, s.roleOf(ofPartner), date) }.map { it.toTimeline() }
    }

    /** Totals of one day plus how many places were visited; null when the day has too few points. */
    override suspend fun daySummary(ofPartner: Boolean, date: String) = withSession { s ->
        val role = s.roleOf(ofPartner)
        val report = safeApiCall { api.getDailyReport(s.coupleId, role, date) }
        val places = when (val timeline = safeApiCall { api.getTimeline(s.coupleId, role, date) }) {
            is AppResult.Success -> timeline.data.events.size
            is AppResult.Failure -> 0
        }
        report.map { it.toDaySummary(date, places) }
    }

    override suspend fun routeSettings() = withSession { s ->
        safeApiCall { api.getRouteSettings(s.coupleId, s.role) }.map { it.toSettings() }
    }

    override suspend fun saveRouteSettings(enabled: Boolean, retentionDays: Int?) = withSession { s ->
        safeApiCall { api.saveRouteSettings(RouteSettingsBody(s.coupleId, s.role, enabled, retentionDays)) }
            .map { it.toSettings() }
    }

    /** Only ever deletes the signed-in person's own history, never the partner's. */
    override suspend fun clearMyRouteHistory() = withSession { s ->
        safeApiCall { api.deleteRouteHistory(s.coupleId, s.role) }.map { }
    }

    override suspend fun geofenceEvents(ofPartner: Boolean) = withSession { s ->
        safeApiCall { api.getGeofenceEvents(s.coupleId, s.roleOf(ofPartner)) }.map { it.toGeofenceEvents() }
    }

    // ── free public map services ─────────────────────────────────────────────────────────────

    private val routeGate = Mutex()
    private var lastRouteCallAt = 0L

    override suspend fun navRoutes(from: GeoPoint, to: GeoPoint, mode: TravelMode): AppResult<List<NavRoute>> {
        val coords = "${from.lng},${from.lat};${to.lng},${to.lat}"
        val query = "?overview=full&geometries=geojson&steps=true&alternatives=3"
        val urls = buildList {
            add("https://routing.openstreetmap.de/routed-${mode.profile}/route/v1/driving/$coords$query")
            // The public demo server only knows cars.
            if (mode == TravelMode.Drive) {
                add("https://router.project-osrm.org/route/v1/driving/$coords?overview=full&geometries=geojson&steps=true&alternatives=true")
            }
        }
        var failure: AppResult<List<NavRoute>> = AppResult.Failure(AppError.Unknown)
        for (url in urls) {
            // The free FOSSGIS servers allow one request a second: wait our turn.
            routeGate.withLock {
                val wait = ROUTE_GAP_MS - (System.currentTimeMillis() - lastRouteCallAt)
                if (wait > 0) delay(wait)
                lastRouteCallAt = System.currentTimeMillis()
            }
            val result = safeApiCall { geo.routeWithSteps(url) }.map { it.toNavRoutes(mode) }
            if (result is AppResult.Success && result.data.isNotEmpty()) return result
            failure = result
        }
        return failure
    }

    override suspend fun drivingRoute(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double) =
        safeApiCall {
            geo.route(
                "https://router.project-osrm.org/route/v1/driving/$fromLng,$fromLat;$toLng,$toLat" +
                    "?overview=full&geometries=geojson",
            )
        }.map { it.toDrivingRoute() }

    override suspend fun search(query: String, nearLat: Double?, nearLng: Double?) = safeApiCall {
        val bias = if (nearLat != null && nearLng != null) "&lat=$nearLat&lon=$nearLng" else ""
        geo.search("https://photon.komoot.io/api/?q=${URLEncoder.encode(query, "UTF-8")}&limit=10$bias")
    }.map { it.toHits() }

    override suspend fun areaName(lat: Double, lng: Double) = safeApiCall {
        geo.reverse("https://nominatim.openstreetmap.org/reverse?format=jsonv2&zoom=17&addressdetails=1&lat=$lat&lon=$lng")
    }.map { it.toAreaName() }

    override suspend fun weather(lat: Double, lng: Double) = safeApiCall {
        geo.weather(
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lng" +
                "&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code,precipitation" +
                "&daily=sunrise,sunset,uv_index_max&timezone=auto",
        )
    }.map { it.toWeather() }

    private companion object {
        /** Above this speed (about 3.6 km/h) a fix counts as "moving". */
        const val MOVING_MPS = 1.0
        const val BATCH_MAX = 2000
        const val ROUTE_GAP_MS = 1_100L
    }
}
