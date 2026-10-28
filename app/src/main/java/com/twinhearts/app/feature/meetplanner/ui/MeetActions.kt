package com.twinhearts.app.feature.meetplanner.ui

import com.twinhearts.app.feature.meetplanner.domain.PopularDestination
import com.twinhearts.app.feature.meetplanner.domain.SavedFilter
import com.twinhearts.app.feature.meetplanner.domain.model.City
import com.twinhearts.app.feature.meetplanner.domain.model.Place
import com.twinhearts.app.feature.meetplanner.domain.model.PlaceCategory
import com.twinhearts.app.feature.meetplanner.domain.model.TravelMode
import com.twinhearts.app.feature.meetplanner.domain.model.TripType
import java.time.LocalDate

/**
 * Why this file exists: every tap the screens can make. The ViewModel implements this, and
 * previews pass an empty object, so the screens stay stateless and previewable (R-42, R-57).
 * All methods do nothing by default.
 */
interface MeetActions {
    // Moving around
    fun back() {}
    fun startNew() {}
    fun openItinerary() {}
    fun openSaved() {}
    fun pickPopular(destination: PopularDestination) {}
    fun openPlan(id: String) {}
    fun refresh() {}

    // Plan New
    fun setTitle(value: String) {}
    fun setFrom(date: LocalDate) {}
    fun setTo(date: LocalDate) {}
    fun setTripType(type: TripType) {}
    fun setBudget(value: String) {}
    fun setNotes(value: String) {}
    fun setMode(mode: TravelMode) {}
    fun onCityQuery(value: String) {}
    fun pickCity(city: City) {}
    fun changeCity() {}
    fun useMyLocation() {}
    fun createPlan() {}

    // Find Places
    fun setCategory(category: PlaceCategory) {}
    fun setQuery(value: String) {}
    fun submitSearch() {}
    fun retryPlaces() {}
    fun toggleStop(place: Place) {}
    fun openPlace(place: Place) {}
    fun closePlace() {}
    fun goStops() {}

    // Your Stops
    fun moveStop(index: Int, direction: Int) {}
    fun removeStop(index: Int) {}
    fun buildRoute() {}

    // Itinerary
    fun setStartTime(minutes: Int) {}
    fun addChecklistItem(text: String) {}
    fun toggleChecklist(id: Long) {}
    fun removeChecklist(id: Long) {}
    fun openRouteDraft() {}
    fun savePlan() {}

    // Route preview
    fun startNavigation() {}

    // Saved meetups
    fun setFilter(filter: SavedFilter) {}
    fun toggleSelect(id: String) {}
    fun enterSelect(id: String) {}
    fun exitSelect() {}
    fun deleteSelected() {}

    // Detail and the sheets and dialogs on top of it
    fun toggleSavedCheck(index: Int) {}
    fun openRouteSaved(id: String) {}
    fun openOptions(id: String) {}
    fun closeOptions() {}
    fun openEdit(id: String) {}
    fun closeEdit() {}
    fun saveEdit(id: String, title: String, from: LocalDate?, to: LocalDate?, budget: String, notes: String) {}
    fun openComplete(id: String) {}
    fun closeComplete() {}
    fun addPhotos(uris: List<String>) {}
    fun removePhoto(index: Int) {}
    fun confirmComplete(mood: String?, extraNotes: String) {}
    fun viewPhotos(id: String) {}
    fun share(id: String) {}
    fun copy(id: String) {}
    fun requestDelete(id: String) {}
    fun confirmDelete() {}
    fun cancelDelete() {}

    /** Used by previews: every tap does nothing. */
    object None : MeetActions
}
