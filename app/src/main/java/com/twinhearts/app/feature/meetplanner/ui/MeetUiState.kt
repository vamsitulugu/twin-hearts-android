package com.twinhearts.app.feature.meetplanner.ui

import androidx.annotation.StringRes
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.feature.livemap.domain.nav.NavSession
import com.twinhearts.app.feature.livemap.ui.map.CameraRequest
import com.twinhearts.app.feature.meetplanner.domain.PlanDraft
import com.twinhearts.app.feature.meetplanner.domain.SavedFilter
import com.twinhearts.app.feature.meetplanner.domain.model.City
import com.twinhearts.app.feature.meetplanner.domain.model.GeoPoint
import com.twinhearts.app.feature.meetplanner.domain.model.MeetIdentity
import com.twinhearts.app.feature.meetplanner.domain.model.Meetup
import com.twinhearts.app.feature.meetplanner.domain.model.Place
import com.twinhearts.app.feature.meetplanner.domain.model.PlaceCategory
import com.twinhearts.app.feature.meetplanner.domain.model.TravelMode

/** Why this file exists: everything the Meet Planner screens can show, in one place (R-41, R-42). */

/** The in-page screens, like the web app's views. Only the top of the stack is visible. */
enum class MeetScreen { Home, Plan, Search, Stops, Itinerary, Route, Saved, Detail }

/** The city search box under "Where are you meeting?". */
sealed interface CitySuggest {
    data object Idle : CitySuggest
    data object Loading : CitySuggest
    data object Empty : CitySuggest
    data object Failed : CitySuggest
    data class Results(val cities: List<City>) : CitySuggest
}

/** The list on Find Places. */
sealed interface PoiState {
    data object Idle : PoiState
    data object Loading : PoiState
    data class Failed(val error: AppError) : PoiState
    data class Results(val places: List<Place>) : PoiState
}

/** A route being looked at on the Route Preview screen (a draft or a saved plan). */
data class RouteView(
    val title: String,
    val stops: List<Place>,
    val points: List<GeoPoint>,
    val distanceKm: Double?,
    val durationMin: Double?,
    val mode: TravelMode,
)

/** A photo picked for "Mark Meetup Complete". [url] is filled once the upload finished. */
data class CompletePhoto(val uri: String, val url: String? = null)

data class MeetUiState(
    val identity: MeetIdentity? = null,
    val plans: UiState<List<Meetup>> = UiState.Loading,
    val stack: List<MeetScreen> = listOf(MeetScreen.Home),
    val draft: PlanDraft,
    val cityQuery: String = "",
    val citySuggest: CitySuggest = CitySuggest.Idle,
    /** Null while the last search was typed text instead of a category chip. */
    val poiCategory: PlaceCategory? = PlaceCategory.Hotel,
    val poiQuery: String = "",
    val poi: PoiState = PoiState.Idle,
    val startMinutes: Int = DEFAULT_START_MINUTES,
    val locating: Boolean = false,
    val building: Boolean = false,
    val saving: Boolean = false,
    val savedFilter: SavedFilter = SavedFilter.All,
    val selectMode: Boolean = false,
    val selected: Set<String> = emptySet(),
    val detailId: String? = null,
    val routeView: RouteView? = null,
    val placeSheet: Place? = null,
    val optionsId: String? = null,
    val editId: String? = null,
    val completeId: String? = null,
    val completePhotos: List<CompletePhoto> = emptyList(),
    val uploadingCount: Int = 0,
    val completing: Boolean = false,
    val deleteIds: List<String>? = null,
    /** In-app turn-by-turn: true while a trip started from Route Preview is running. */
    val navActive: Boolean = false,
    val nav: NavSession? = null,
    /** Index (in the route's stops) of the stop we are driving to right now. */
    val navStop: Int = 0,
    val navLoading: Boolean = false,
    val navCamera: CameraRequest? = null,
) {
    val screen: MeetScreen get() = stack.last()
    val meetups: List<Meetup> get() = (plans as? UiState.Content)?.data.orEmpty()
    val detail: Meetup? get() = meetups.firstOrNull { it.id == detailId }

    companion object {
        const val DEFAULT_START_MINUTES = 9 * 60
    }
}

/** One-off things the screen must do once: a toast, an error, or opening another app. */
sealed interface MeetEffect {
    data class Toast(@StringRes val res: Int, val args: List<Any> = emptyList()) : MeetEffect
    data class Failure(val error: AppError) : MeetEffect
    data class Share(val text: String) : MeetEffect
    data class Copy(val text: String) : MeetEffect
    data class OpenUrl(val url: String) : MeetEffect
    data class ViewPhotos(val urls: List<String>) : MeetEffect
    data object NeedLocationPermission : MeetEffect

    /** Location permission for turn-by-turn; the screen calls startNavigation again once answered. */
    data object NeedNavPermission : MeetEffect
}
