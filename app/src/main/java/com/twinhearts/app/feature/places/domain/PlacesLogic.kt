package com.twinhearts.app.feature.places.domain

import com.twinhearts.app.feature.places.domain.model.MemoryNote
import com.twinhearts.app.feature.places.domain.model.PlaceMemory
import com.twinhearts.app.feature.places.domain.model.PlaceStats
import java.time.LocalDate
import java.util.Locale

/*
 * Why this file exists: the page rules (stats, search, sort, ids, date text) kept apart from the
 * screen so they can be tested without a phone.
 */

fun placeStats(places: List<PlaceMemory>): PlaceStats = PlaceStats(
    places = places.size,
    favorites = places.count { it.favorite },
    photos = places.sumOf { it.photos.size },
    memories = places.sumOf { it.memories.size },
)

/** Newest visit first (places without a date go last), optional favourites only, optional text search on name or location. */
fun visiblePlaces(places: List<PlaceMemory>, query: String, favoritesOnly: Boolean): List<PlaceMemory> {
    val q = query.trim().lowercase(Locale.ROOT)
    return places
        .asSequence()
        .filter { !favoritesOnly || it.favorite }
        .filter { q.isEmpty() || it.name.lowercase(Locale.ROOT).contains(q) || it.location.lowercase(Locale.ROOT).contains(q) }
        .sortedWith(compareByDescending<PlaceMemory> { it.visitDate ?: LocalDate.MIN })
        .toList()
}

fun newestFirst(notes: List<MemoryNote>): List<MemoryNote> =
    notes.sortedWith(compareByDescending<MemoryNote> { it.date ?: LocalDate.MIN })

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** "5 Nov 2026", like the web app's en-IN date; empty when there is no date. */
fun formatVisitDate(date: LocalDate?): String =
    date?.let { "${it.dayOfMonth} ${MONTHS[it.monthValue - 1]} ${it.year}" }.orEmpty()

/** Same id style as the web app ("p" + time + 0-999) so both apps can read each other's places. */
fun newPlaceId(nowMillis: Long, random: Int): String = "p$nowMillis${(random % 1000 + 1000) % 1000}"

/** Parses "12.5" or "12,5". Empty or bad text means "no value". Out-of-range values are rejected. */
fun parseCoordinate(text: String, limit: Double): Double? {
    val value = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
    return value.takeIf { it in -limit..limit }
}

fun isValidPlaceName(name: String): Boolean = name.isNotBlank()

/** Link that opens the position in any maps app. */
fun mapsUrl(lat: Double, lng: Double, label: String): String =
    "geo:$lat,$lng?q=$lat,$lng(" + java.net.URLEncoder.encode(label, "UTF-8") + ")"

fun formatCoordinate(value: Double): String = String.format(Locale.US, "%.6f", value)
