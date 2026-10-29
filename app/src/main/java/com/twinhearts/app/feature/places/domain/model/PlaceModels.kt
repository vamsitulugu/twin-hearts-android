package com.twinhearts.app.feature.places.domain.model

import java.time.LocalDate

/*
 * Why this file exists: the plain Kotlin shapes the Places screens use. They know nothing about
 * Retrofit or Compose. Field meanings match the web app's "Places Memory" page.
 */

/** One note on a place's timeline ("Timeline & Notes" tab). */
data class MemoryNote(
    val id: String,
    val date: LocalDate?,
    val text: String,
    val favorite: Boolean = false,
)

/** A place the couple visited together. */
data class PlaceMemory(
    val id: String,
    val name: String,
    val location: String = "",
    val visitDate: LocalDate? = null,
    val description: String = "",
    /** Picture shown at the top of the card. */
    val banner: String? = null,
    val photos: List<String> = emptyList(),
    val lat: Double? = null,
    val lng: Double? = null,
    val favorite: Boolean = false,
    val memories: List<MemoryNote> = emptyList(),
    val createdAt: Long = 0L,
) {
    val hasCoordinates: Boolean get() = lat != null && lng != null
}

/** The four numbers at the top of the page. */
data class PlaceStats(val places: Int, val favorites: Int, val photos: Int, val memories: Int)

/** Who is signed in, as far as this feature needs. */
data class PlacesIdentity(val coupleId: String, val role: String, val myName: String)

data class GeoPoint(val lat: Double, val lng: Double)
