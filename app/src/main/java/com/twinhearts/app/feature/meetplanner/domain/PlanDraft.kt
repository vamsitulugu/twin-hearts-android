package com.twinhearts.app.feature.meetplanner.domain

import com.twinhearts.app.feature.meetplanner.domain.model.ChecklistItem
import com.twinhearts.app.feature.meetplanner.domain.model.City
import com.twinhearts.app.feature.meetplanner.domain.model.Place
import com.twinhearts.app.feature.meetplanner.domain.model.PlaceCategory
import com.twinhearts.app.feature.meetplanner.domain.model.RoutePlan
import com.twinhearts.app.feature.meetplanner.domain.model.TravelMode
import com.twinhearts.app.feature.meetplanner.domain.model.TripType
import java.time.LocalDate

/**
 * Why this file exists: the plan the couple is building right now (before it is saved).
 * Every change is a small pure function, so the rules (first hotel is the anchor, any change
 * throws away the built route) can be tested without Android.
 */
data class PlanDraft(
    val title: String = "",
    val city: City? = null,
    val from: LocalDate,
    val to: LocalDate,
    /** Null once the couple edits the end date by hand (nothing is "selected" in the tabs then). */
    val tripType: TripType? = TripType.Weekend,
    val budget: String = "",
    val mode: TravelMode = TravelMode.Car,
    val notes: String = "",
    val stops: List<Place> = emptyList(),
    val route: RoutePlan? = null,
    val checklist: List<ChecklistItem> = emptyList(),
) {
    /** The route is only "built" while it matches the current stops and there are at least two. */
    val routeBuilt: Boolean get() = route != null && stops.size >= 2

    companion object {
        /** A fresh draft that starts today as a Weekend trip, like the web "Plan New". */
        fun fresh(today: LocalDate): PlanDraft =
            PlanDraft(from = today, to = today.plusDays(TripType.Weekend.extraDays))
    }
}

/** Picks a trip type: the end date follows the start date. */
fun PlanDraft.withTripType(type: TripType): PlanDraft = copy(tripType = type, to = from.plusDays(type.extraDays))

/** Changes the start date; with a trip type selected, the end date follows. */
fun PlanDraft.withFrom(date: LocalDate): PlanDraft {
    val type = tripType
    return when {
        type != null -> copy(from = date, to = date.plusDays(type.extraDays))
        to.isBefore(date) -> copy(from = date, to = date)
        else -> copy(from = date)
    }
}

/** Changes the end date by hand: it can never be before the start date, and no trip type stays selected. */
fun PlanDraft.withTo(date: LocalDate): PlanDraft = copy(tripType = null, to = if (date.isBefore(from)) from else date)

/** Choosing a new city throws away the old stops and route (they belong to the old city). */
fun PlanDraft.withCity(city: City?): PlanDraft = copy(city = city, stops = emptyList(), route = null)

fun PlanDraft.isSelected(place: Place): Boolean = stops.any { it.id == place.id }

/** Adds the place, or removes it when it is already a stop. The first hotel becomes the Start/End stop. */
fun PlanDraft.toggleStop(place: Place): PlanDraft {
    val index = stops.indexOfFirst { it.id == place.id }
    val next = if (index >= 0) {
        stops.filterIndexed { i, _ -> i != index }
    } else {
        val anchor = place.category == PlaceCategory.Hotel && stops.none { it.isAnchor }
        stops + place.copy(isAnchor = anchor)
    }
    return copy(stops = next, route = null)
}

fun PlanDraft.moveStop(index: Int, direction: Int): PlanDraft {
    val target = index + direction
    if (index !in stops.indices || target !in stops.indices) return this
    val list = stops.toMutableList()
    val moved = list.removeAt(index)
    list.add(target, moved)
    return copy(stops = list, route = null)
}

fun PlanDraft.removeStopAt(index: Int): PlanDraft {
    if (index !in stops.indices) return this
    return copy(stops = stops.filterIndexed { i, _ -> i != index }, route = null)
}

fun PlanDraft.addChecklistItem(text: String, id: Long): PlanDraft {
    val clean = text.trim()
    return if (clean.isEmpty()) this else copy(checklist = checklist + ChecklistItem(id, clean))
}

fun PlanDraft.toggleChecklist(id: Long): PlanDraft =
    copy(checklist = checklist.map { if (it.id == id) it.copy(done = !it.done) else it })

fun PlanDraft.removeChecklist(id: Long): PlanDraft = copy(checklist = checklist.filterNot { it.id == id })
