package com.twinhearts.app.feature.meetplanner.domain.model

import java.time.LocalDate

/*
 * Why this file exists: the plain Kotlin shapes the Meet Planner screens use. They know nothing
 * about Retrofit or Compose, so they are easy to test and the UI never sees server field names.
 */

/** One OSM tag group we ask Overpass for, plus the emoji and fallback name the web app uses for it. */
data class OsmKind(val label: String, val emoji: String, val filters: List<String>)

/**
 * The place types in the "Find Places" chips (same order as the web app).
 * [key] is the value stored in the saved plan's `stops[].cat`, so old web plans still open.
 */
enum class PlaceCategory(
    val key: String,
    val emoji: String,
    val stayMinutes: Int,
    val osmKinds: List<OsmKind>,
) {
    Hotel(
        "hotel", "🏨", 45,
        listOf(OsmKind("Hotels", "🏨", listOf("[\"tourism\"=\"hotel\"]", "[\"tourism\"=\"guest_house\"]", "[\"tourism\"=\"hostel\"]"))),
    ),
    Tourist(
        "tourist", "🗿", 90,
        listOf(OsmKind("Tourist Places", "🗺️", listOf("[\"tourism\"=\"attraction\"]", "[\"tourism\"=\"viewpoint\"]"))),
    ),
    Cafe("cafe", "☕", 45, listOf(OsmKind("Cafes", "☕", listOf("[\"amenity\"=\"cafe\"]")))),
    Restaurant("restaurant", "🍽️", 60, listOf(OsmKind("Restaurants", "🍽️", listOf("[\"amenity\"=\"restaurant\"]")))),
    Mall("mall", "🛍️", 90, listOf(OsmKind("Shopping Malls", "🛍️", listOf("[\"shop\"=\"mall\"]")))),
    Park("park", "🌳", 60, listOf(OsmKind("Parks", "🌳", listOf("[\"leisure\"=\"park\"]")))),
    Museum("museum", "🏛️", 90, listOf(OsmKind("Museums", "🏛️", listOf("[\"tourism\"=\"museum\"]")))),
    Temple(
        "temple", "🛕", 45,
        listOf(
            OsmKind(
                "Temples", "🛕",
                listOf(
                    "[\"amenity\"=\"place_of_worship\"][\"religion\"=\"hindu\"]",
                    "[\"amenity\"=\"place_of_worship\"][\"religion\"=\"buddhist\"]",
                ),
            ),
            OsmKind("Mosques", "🕌", listOf("[\"amenity\"=\"place_of_worship\"][\"religion\"=\"muslim\"]")),
            OsmKind("Churches", "⛪", listOf("[\"amenity\"=\"place_of_worship\"][\"religion\"=\"christian\"]")),
        ),
    ),
    Entertainment("entertainment", "🎬", 120, listOf(OsmKind("Movie Theatres", "🎬", listOf("[\"amenity\"=\"cinema\"]")))),

    /** Anything found with the search box. Has no OSM tags of its own. */
    Custom("custom", "📍", 60, emptyList());

    companion object {
        /** The chips shown on Find Places: every category except [Custom]. */
        val chips: List<PlaceCategory> = entries.filter { it != Custom }

        fun fromKey(key: String?): PlaceCategory = entries.firstOrNull { it.key == key } ?: Custom
    }
}

/** A place found on the map. Once the couple adds it to the plan it is a "stop". */
data class Place(
    val id: String,
    val name: String,
    val category: PlaceCategory,
    val emoji: String,
    val lat: Double,
    val lng: Double,
    val address: String? = null,
    /** Distance from the chosen city centre, when we know it. */
    val distKm: Double? = null,
    /** The first hotel picked is the "Start/End" stop of the route. */
    val isAnchor: Boolean = false,
)

data class GeoPoint(val lat: Double, val lng: Double)

/** The city the couple is meeting in. */
data class City(val name: String, val display: String, val lat: Double, val lng: Double)

/** How the couple travels. [kmh] null means "use the road time OSRM gives us" (car). */
enum class TravelMode(val key: String, val emoji: String, val kmh: Double?, val googleMode: String) {
    Car("car", "🚗", null, "driving"),
    Bike("bike", "🏍️", 32.0, "two-wheeler"),
    Cycle("cycle", "🚲", 15.0, "bicycling"),
    Walk("walk", "🚶", 5.0, "walking"),
    Transit("transit", "🚌", 22.0, "transit");

    companion object {
        fun fromKey(key: String?): TravelMode = entries.firstOrNull { it.key == key } ?: Car
    }
}

/** Day Trip / Weekend / Long Trip. [extraDays] is added to the start date to get the end date. */
enum class TripType(val key: String, val extraDays: Long) {
    Day("day", 0),
    Weekend("weekend", 2),
    Long("long", 5);

    companion object {
        fun fromKey(key: String?): TripType? = entries.firstOrNull { it.key == key }
    }
}

data class ChecklistItem(val id: Long, val text: String, val done: Boolean = false)

data class RouteLeg(val distanceKm: Double, val durationMin: Double)

/** A built route: the line to draw, the totals, and one leg between each pair of stops. */
data class RoutePlan(
    val points: List<GeoPoint>,
    val distanceKm: Double,
    val durationMin: Double,
    val legs: List<RouteLeg>,
)

enum class MeetStatus { Planned, Completed }

/** A saved meetup, as read from the server. */
data class Meetup(
    val id: String,
    val title: String,
    val meetDate: LocalDate?,
    val endDate: LocalDate?,
    val tripType: TripType?,
    /** The notes the couple typed, with the hidden header line already removed. */
    val notes: String,
    val budget: Double?,
    val cityName: String?,
    val stops: List<Place>,
    val routePoints: List<GeoPoint>,
    val distanceKm: Double?,
    val durationMin: Double?,
    val travelMode: TravelMode,
    val checklist: List<ChecklistItem>,
    val status: MeetStatus,
    val globeMemoryId: String?,
) {
    val isCompleted: Boolean get() = status == MeetStatus.Completed
}

/** Who is signed in, as far as this feature needs. */
data class MeetIdentity(
    val coupleId: String,
    val role: String,
    val myName: String,
    val partnerName: String,
)

/** The mood choices on the "Mark Meetup Complete" form (same wording as the web app). */
val MeetMoods: List<String> = listOf("😍 Amazing", "🥰 Sweet", "😂 Fun", "😌 Relaxing", "🥹 Emotional", "🎉 Exciting")
