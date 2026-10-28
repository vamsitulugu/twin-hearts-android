package com.twinhearts.app.feature.meetplanner.domain

/**
 * Why this file exists: builds the Overpass query text for "places near this city". The old
 * server forwards the text as-is to the public OpenStreetMap mirrors and caches the answer,
 * so we build it exactly like the web app does (same text = same cache hit).
 */
fun buildOverpassQuery(filters: List<String>, lat: Double, lng: Double, radiusM: Int, limit: Int): String? {
    if (filters.isEmpty()) return null
    val clauses = filters.joinToString("") { f ->
        "node$f(around:$radiusM,$lat,$lng);way$f(around:$radiusM,$lat,$lng);"
    }
    return "[out:json][timeout:20];($clauses);out center $limit;"
}
