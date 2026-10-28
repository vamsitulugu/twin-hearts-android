package com.twinhearts.app.feature.meetplanner.domain

import com.twinhearts.app.feature.meetplanner.domain.model.GeoPoint
import com.twinhearts.app.feature.meetplanner.domain.model.Place
import kotlin.math.max
import kotlin.math.min

/*
 * Why this file exists: text search asks two free geocoders (Nominatim and Photon). Their answers
 * are merged, de-duplicated and ranked here, with the same typo-tolerant scoring as the web app.
 */

/** How many single-letter edits turn [a] into [b] (so "Hotal" is close to "Hotel"). */
fun editDistance(a: String, b: String): Int {
    val s = a.lowercase()
    val t = b.lowercase()
    if (s.isEmpty()) return t.length
    if (t.isEmpty()) return s.length
    var previous = IntArray(t.length + 1) { it }
    for (i in 1..s.length) {
        val current = IntArray(t.length + 1)
        current[0] = i
        for (j in 1..t.length) {
            current[j] = if (s[i - 1] == t[j - 1]) {
                previous[j - 1]
            } else {
                1 + min(previous[j], min(current[j - 1], previous[j - 1]))
            }
        }
        previous = current
    }
    return previous[t.length]
}

/** 0..1 similarity (1 = the name contains the query). */
fun fuzzyScore(query: String, target: String): Double {
    if (query.isBlank() || target.isBlank()) return 0.0
    val q = query.lowercase().trim()
    val t = target.lowercase().trim()
    if (t.contains(q)) return 1.0
    val dist = editDistance(q, t.take(q.length + 3))
    val maxLen = max(q.length, 3)
    return max(0.0, 1.0 - dist.toDouble() / maxLen)
}

/** Drops a place that is within 50 m of an earlier one with a similar name. The first one found wins. */
fun dedupePlaces(places: List<Place>, thresholdKm: Double = 0.05): List<Place> {
    val out = mutableListOf<Place>()
    for (p in places) {
        val duplicate = out.any {
            haversineKm(GeoPoint(it.lat, it.lng), GeoPoint(p.lat, p.lng)) < thresholdKm &&
                fuzzyScore(it.name, p.name) > 0.6
        }
        if (!duplicate) out += p
    }
    return out
}

private fun score(place: Place, query: String): Double {
    var s = 50 * fuzzyScore(query, place.name)
    val d = place.distKm
    if (d != null) s += max(0.0, 20 - d)
    return s
}

/** Best match first: name similarity plus a small bonus for being close to the city centre. */
fun rankPlaces(places: List<Place>, query: String, limit: Int): List<Place> =
    places.sortedByDescending { score(it, query) }.take(limit)
