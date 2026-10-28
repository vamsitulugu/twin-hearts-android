package com.twinhearts.app.feature.meetplanner.domain

import com.twinhearts.app.feature.meetplanner.domain.model.Meetup
import com.twinhearts.app.feature.meetplanner.domain.model.Place
import com.twinhearts.app.feature.meetplanner.domain.model.TravelMode
import java.net.URLEncoder
import java.time.LocalDate

/*
 * Why this file exists: small questions the screens ask about the saved plans (which one is next,
 * what is in each tab) and the two texts we hand to other apps (share text, maps link).
 */

enum class SavedFilter { All, Upcoming, Completed }

data class SplitPlans(val planned: List<Meetup>, val done: List<Meetup>) {
    fun forFilter(filter: SavedFilter): List<Meetup> = when (filter) {
        SavedFilter.All -> planned + done
        SavedFilter.Upcoming -> planned
        SavedFilter.Completed -> done
    }
}

/** Planned plans by date (no date last); completed plans newest first. */
fun splitPlans(all: List<Meetup>): SplitPlans = SplitPlans(
    planned = all.filter { !it.isCompleted }.sortedBy { it.meetDate ?: LocalDate.MAX },
    done = all.filter { it.isCompleted }.sortedByDescending { it.meetDate ?: LocalDate.MIN },
)

/** The card under "Upcoming Meetup": the soonest planned one that is not in the past, else one with no date. */
fun upcomingPlan(all: List<Meetup>, today: LocalDate): Meetup? {
    val planned = all.filter { !it.isCompleted }
    val dated = planned
        .filter { val d = it.meetDate; d != null && !d.isBefore(today) }
        .sortedBy { it.meetDate }
    return dated.firstOrNull() ?: planned.firstOrNull { it.meetDate == null }
}

/** The plain text that "Share Plan" and "Copy Itinerary" send. */
fun meetupShareText(m: Meetup): String {
    val range = formatRange(m.meetDate, m.endDate) ?: "Date not set"
    val lines = mutableListOf("💕 ${m.title}", "📅 $range${m.cityName?.let { " · $it" }.orEmpty()}", "")
    m.stops.forEachIndexed { i, s -> lines += "${i + 1}. ${s.emoji} ${s.name}" }
    m.distanceKm?.takeIf { it > 0 }?.let { km ->
        lines += "\n📏 ${formatKm(km)}${m.durationMin?.takeIf { it > 0 }?.let { " · ${formatDuration(it)}" }.orEmpty()}"
    }
    m.budget?.takeIf { it > 0 }?.let { lines += "💰 Budget ₹${formatMoney(it)}" }
    if (m.notes.isNotBlank()) lines += "\n${m.notes.trim()}"
    return lines.filter { it.isNotEmpty() }.joinToString("\n")
}

/** 5000.0 shows as "5000"; 49.5 stays "49.5". */
fun formatMoney(value: Double): String =
    if (value == Math.floor(value)) value.toLong().toString() else value.toString()

/** A Google Maps directions link: first stop to last stop, up to 9 stops in between. */
fun directionsUrl(stops: List<Place>, mode: TravelMode): String? {
    if (stops.isEmpty()) return null
    fun pt(p: Place) = "${p.lat},${p.lng}"
    val base = "https://www.google.com/maps/dir/?api=1&origin=${pt(stops.first())}&destination=${pt(stops.last())}&travelmode=${mode.googleMode}"
    val middle = stops.drop(1).dropLast(1).take(9).joinToString("|") { pt(it) }
    return if (middle.isEmpty()) base else "$base&waypoints=${URLEncoder.encode(middle, "UTF-8")}"
}

/** A single-place link (the "Get Directions" button on the place sheet). */
fun placeDirectionsUrl(place: Place): String =
    "https://www.google.com/maps/dir/?api=1&destination=${place.lat},${place.lng}"
