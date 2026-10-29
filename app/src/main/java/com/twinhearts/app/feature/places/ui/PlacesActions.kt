package com.twinhearts.app.feature.places.ui

import java.time.LocalDate

/** Why this file exists: every tap on the Places screens, in one list. Previews use [None]; the ViewModel implements it. */
interface PlacesActions {
    fun back() {}
    fun refresh() {}
    fun setQuery(text: String) {}
    fun toggleFavoritesOnly() {}
    fun quickFavorite(id: String) {}
    fun openAdd() {}
    fun openEdit(id: String) {}
    fun closeForm() {}
    fun setName(text: String) {}
    fun setLocation(text: String) {}
    fun setDate(date: LocalDate?) {}
    fun setDescription(text: String) {}
    fun setLat(text: String) {}
    fun setLng(text: String) {}
    fun setFormFavorite(on: Boolean) {}
    fun pickBanner(uri: String) {}
    fun removeBanner() {}
    fun addFormPhotos(uris: List<String>) {}
    fun removeFormPhoto(index: Int) {}
    fun useMyLocation() {}
    fun savePlace() {}
    fun openDetail(id: String) {}
    fun closeDetail() {}
    fun setTab(tab: DetailTab) {}
    fun toggleDetailFavorite() {}
    fun requestDelete() {}
    fun confirmDelete() {}
    fun cancelDelete() {}
    fun addGalleryPhotos(uris: List<String>) {}
    fun openLightbox(index: Int) {}
    fun closeLightbox() {}
    fun removeLightboxPhoto() {}
    fun setNoteText(text: String) {}
    fun setNoteDate(date: LocalDate?) {}
    fun setNoteFavorite(on: Boolean) {}
    fun addNote() {}
    fun toggleNoteFavorite(id: String) {}
    fun deleteNote(id: String) {}
    fun openMap() {}

    object None : PlacesActions
}
