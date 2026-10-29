package com.twinhearts.app.feature.globe.domain.model

/*
 * Why this file exists: the plain data classes every Globe screen uses (R-03).
 * Nothing here knows about Android, Compose or the network, so it is easy to test.
 */

/** The four kinds of things you can attach to a memory (same words as the server's `type` column). */
enum class MediaKind(val wire: String) {
    Photo("photo"),
    Video("video"),
    Voice("voice"),
    Journal("journal"),
    ;

    companion object {
        fun fromWire(value: String?): MediaKind? = entries.firstOrNull { it.wire == value }
    }
}

/** One photo, video, voice note or journal entry that belongs to a memory. */
data class MemoryMedia(
    val id: String,
    val kind: MediaKind,
    val url: String,
    /** Text shown under a voice note, for example "0:12". Empty when the server has none. */
    val duration: String,
    val journalText: String,
)

/** One pinned memory: a trip or a day in one place. */
data class Memory(
    val id: String,
    val city: String,
    val country: String,
    val state: String,
    val lat: Double,
    val lng: Double,
    val tripName: String,
    /** `yyyy-MM-dd` or null. */
    val dateFrom: String?,
    val dateTo: String?,
    /** Mood as saved, for example "🥰 Romantic". */
    val mood: String,
    val weather: String,
    val temperature: String,
    val tripCost: Double?,
    val currency: String,
    val notes: String,
    val favoriteMoment: String,
    val restaurants: List<String>,
    val hotels: List<String>,
    val gifts: List<String>,
    val media: List<MemoryMedia>,
) {
    /** Memories of the same city and country are shown together on one pin. */
    val cityKey: String get() = cityKeyOf(city, country)

    /** The mood's emoji, or a heart when no mood was picked. */
    val moodEmoji: String get() = moodEmojiOf(mood)

    fun mediaOf(kind: MediaKind): List<MemoryMedia> = media.filter { it.kind == kind }
}

const val HEART_EMOJI = "❤️"

fun cityKeyOf(city: String, country: String): String = "$city,$country"

fun moodEmojiOf(mood: String): String = mood.trim().substringBefore(' ').ifBlank { HEART_EMOJI }

/** A latitude / longitude pair in degrees. */
data class GeoPoint(val lat: Double, val lng: Double)

/** All memories of one city: one pin on the globe. */
data class CityGroup(
    val key: String,
    val city: String,
    val country: String,
    val lat: Double,
    val lng: Double,
    /** Mood of the first memory; it decides the emoji on the pin. */
    val mood: String,
    val memories: List<Memory>,
) {
    val count: Int get() = memories.size
    val emoji: String get() = moodEmojiOf(mood)
}

/** One trip leg: the line drawn from one memory to the next one in time. */
data class ArcLeg(
    val index: Int,
    val from: GeoPoint,
    val to: GeoPoint,
    val distanceKm: Double,
) {
    /** Short legs are drawn as real road routes, long ones (flights, ferries) as an arc. */
    val driveable: Boolean get() = distanceKm <= MAX_DRIVE_KM

    /** Stable cache key for the road route of this leg. */
    val routeKey: String get() = "%.4f,%.4f|%.4f,%.4f".format(java.util.Locale.ROOT, from.lat, from.lng, to.lat, to.lng)

    companion object {
        const val MAX_DRIVE_KM = 1200.0
    }
}

/** A soft glow over a country that has memories. */
data class CountryGlow(val country: String, val center: GeoPoint, val cities: Int, val memories: Int)

/** What the filter chips can select. */
sealed interface GlobeFilter {
    data object All : GlobeFilter
    data class Year(val year: String) : GlobeFilter
    data class Mood(val emoji: String) : GlobeFilter
}

/** The three views of the page. */
enum class GlobeViewMode { Globe, Map, Timeline }

/** Numbers in the bottom chips. */
data class GlobeSummary(val cities: Int, val countries: Int, val memories: Int, val totalCost: Double)

enum class AchievementId { FirstMemory, CityExplorer, WorldTraveler, BigSpender, Photographer, ContinentHopper, Romantics, GlobeMasters, Storytellers }

data class Achievement(val id: AchievementId, val unlocked: Boolean)

data class CityCount(val city: String, val count: Int)

data class MoodCount(val emoji: String, val count: Int)

/** One month of the activity strip. [level] is 0 to 4. */
data class HeatCell(val month: String, val count: Int, val level: Int)

/** Everything the Travel Stats page shows. */
data class TravelStats(
    val memories: Int,
    val cities: Int,
    val countries: Int,
    val totalCost: Double,
    val photos: Int,
    val topCities: List<CityCount>,
    val moods: List<MoodCount>,
    val heat: List<HeatCell>,
    val achievements: List<Achievement>,
)

/** A place found by the search box (Nominatim). */
data class PlaceHit(
    val city: String,
    val country: String,
    val state: String,
    val lat: Double,
    val lng: Double,
    /** Full address text from the search service. */
    val displayName: String,
)

/** Weather for a place, ready for the form. [option] is one of the form's weather choices, or null. */
data class WeatherNow(val option: String?, val temperatureC: Int)

/** What one person's last known position looks like on the globe. */
data class LivePerson(val point: GeoPoint, val online: Boolean)

data class LiveSnapshot(val me: LivePerson?, val partner: LivePerson?)

/** A file that is already uploaded and waits to be saved with a new memory. */
data class StagedMedia(
    val localId: Long,
    val kind: MediaKind,
    val url: String?,
    val duration: String,
    val uploading: Boolean,
)

/** Everything typed into the Pin a Memory form. */
data class MemoryDraft(
    val city: String = "",
    val country: String = "",
    val state: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val locationText: String = "",
    val tripName: String = "",
    val dateFrom: String = "",
    val dateTo: String = "",
    val mood: String = "",
    val weather: String = "",
    val temperature: String = "",
    val cost: String = "",
    val currency: String = "INR",
    val notes: String = "",
    val favoriteMoment: String = "",
    val restaurants: List<String> = emptyList(),
    val hotels: List<String> = emptyList(),
    val gifts: List<String> = emptyList(),
    val journal: String = "",
) {
    /** A memory can only be saved once a real place was picked from the search results. */
    val hasLocation: Boolean get() = city.isNotBlank() && country.isNotBlank() && lat != null && lng != null
}
