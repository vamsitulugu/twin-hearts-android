package com.twinhearts.app.feature.livemap.ui

import androidx.annotation.StringRes
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.feature.livemap.data.LiveMapSettings
import com.twinhearts.app.feature.livemap.domain.model.AlertPrefs
import com.twinhearts.app.feature.livemap.domain.model.AreaName
import com.twinhearts.app.feature.livemap.domain.model.CouplePositions
import com.twinhearts.app.feature.livemap.domain.model.DeviceFix
import com.twinhearts.app.feature.livemap.domain.model.DrivingRoute
import com.twinhearts.app.feature.livemap.domain.model.GeofenceEvent
import com.twinhearts.app.feature.livemap.domain.model.MapStyleKind
import com.twinhearts.app.feature.livemap.domain.model.SearchHit
import com.twinhearts.app.feature.livemap.domain.model.SharedMapState
import com.twinhearts.app.feature.livemap.domain.model.Vehicle
import com.twinhearts.app.feature.livemap.domain.model.WeatherNow
import com.twinhearts.app.feature.livemap.ui.map.CameraRequest
import com.twinhearts.app.feature.livemap.ui.map.MapPoint

/** The eight sub-screens of the Live Map. The page keeps its own back stack, so Back returns to the screen before. */
enum class LmScreen { Map, Track, Partner, Places, More, Alerts, Privacy, Search }

/** Loading / error of the first load (R-41). After the first load the page stays on screen even when a refresh fails. */
sealed interface LiveMapPhase {
    data object Loading : LiveMapPhase
    data object Ready : LiveMapPhase
    data class Failed(val error: AppError) : LiveMapPhase
}

/** What the "destination" on the map is: a long-pressed spot, a search result, a saved place, the partner or the midpoint. */
enum class DestinationKind { Pin, Search, Place, Partner, Meeting }

/** A spot the person wants directions to, with its road route once known. */
data class Destination(
    val point: MapPoint,
    val name: String,
    val address: String = "",
    val kind: DestinationKind = DestinationKind.Pin,
    val route: DrivingRoute? = null,
    val routeLoading: Boolean = false,
    val routeFailed: Boolean = false,
)

/** State of the search screen. */
data class SearchUiState(
    val query: String = "",
    val loading: Boolean = false,
    val hits: List<SearchHit> = emptyList(),
    val error: AppError? = null,
    /** True once a search finished, so "no results" is not shown before the first search. */
    val searched: Boolean = false,
)

/** "Recent activity" on the Location Alerts screen. */
sealed interface AlertFeedState {
    data object Loading : AlertFeedState
    data class Content(val events: List<GeofenceEvent>) : AlertFeedState
    data object Empty : AlertFeedState
    data class Error(val error: AppError) : AlertFeedState
}

data class LiveMapUiState(
    val stack: List<LmScreen> = listOf(LmScreen.Map),
    val phase: LiveMapPhase = LiveMapPhase.Loading,
    val myName: String = "",
    val partnerName: String = "Partner",
    val myRole: String = "user1",
    val paired: Boolean = true,
    val positions: CouplePositions = CouplePositions(null, null),
    /** This phone's own latest GPS fix. Shown instantly, before the server round trip. */
    val myFix: DeviceFix? = null,
    val settings: LiveMapSettings = LiveMapSettings(
        sharing = null,
        invisible = false,
        approximate = false,
        pausedUntilMs = 0L,
        vehicle = Vehicle.Walking,
        mapStyle = MapStyleKind.Street,
        threeD = false,
        followPartner = false,
        showRouteLine = true,
        showPlaceLabels = true,
        showWeather = false,
        tellBattery = true,
        tellNetwork = true,
        tellGps = true,
    ),
    val shared: SharedMapState = SharedMapState(emptyList(), emptyList(), emptyList(), AlertPrefs()),
    val myTrail: List<MapPoint> = emptyList(),
    val partnerTrail: List<MapPoint> = emptyList(),
    val partnerArea: AreaName? = null,
    val destination: Destination? = null,
    val weather: WeatherNow? = null,
    val weatherLoading: Boolean = false,
    val search: SearchUiState = SearchUiState(),
    val feed: AlertFeedState = AlertFeedState.Loading,
    val camera: CameraRequest? = null,
    /** True while refreshes fail for lack of internet: the old positions stay on screen with a banner. */
    val offline: Boolean = false,
    /** Emergency banner the person already closed (the partner's update time it was closed at). */
    val dismissedEmergencyAt: Long = 0L,
    val savingPlace: Boolean = false,
    /** Last map position the person looked at, so the map comes back where they left it. */
    val lastCenter: MapPoint? = null,
    val lastZoom: Double = 14.0,
    /** Wall clock used for "ago" texts; bumped on every refresh. */
    val nowMs: Long = 0L,
) {
    val screen: LmScreen get() = stack.last()
}

/** One-off things the screen should show once (R-42): a short message or a partner alert. */
sealed interface LiveMapEvent {
    data class Message(@StringRes val res: Int, val arg: String? = null) : LiveMapEvent
    data class PartnerArrived(val place: String) : LiveMapEvent
    data class PartnerLeft(val place: String) : LiveMapEvent
    data object PartnerOffline : LiveMapEvent
}
