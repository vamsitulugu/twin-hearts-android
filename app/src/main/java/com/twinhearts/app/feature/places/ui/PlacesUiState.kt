package com.twinhearts.app.feature.places.ui

import androidx.annotation.StringRes
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.feature.places.domain.model.PlaceMemory
import com.twinhearts.app.feature.places.domain.model.PlacesIdentity
import java.time.LocalDate

/** The three tabs of the place detail page. */
enum class DetailTab { Gallery, Timeline, Map }

/** The Add / Edit Place form. [editId] is null when adding. Photos still uploading are counted, not listed. */
data class PlaceForm(
    val editId: String? = null,
    val name: String = "",
    val location: String = "",
    val date: LocalDate? = null,
    val description: String = "",
    val banner: String? = null,
    val bannerUploading: Boolean = false,
    val photos: List<String> = emptyList(),
    val photosUploading: Int = 0,
    val lat: String = "",
    val lng: String = "",
    val favorite: Boolean = false,
    val locating: Boolean = false,
    val nameError: Boolean = false,
) {
    val busy: Boolean get() = bannerUploading || photosUploading > 0
}

/** The note being typed on the Timeline tab. */
data class NoteDraft(val date: LocalDate? = null, val text: String = "", val favorite: Boolean = false)

data class PlacesUiState(
    val identity: PlacesIdentity? = null,
    val places: UiState<List<PlaceMemory>> = UiState.Loading,
    val query: String = "",
    val favoritesOnly: Boolean = false,
    val detailId: String? = null,
    val tab: DetailTab = DetailTab.Gallery,
    val form: PlaceForm? = null,
    val saving: Boolean = false,
    val note: NoteDraft = NoteDraft(),
    val galleryUploading: Int = 0,
    val lightboxIndex: Int? = null,
    val deleteId: String? = null,
) {
    val all: List<PlaceMemory> get() = (places as? UiState.Content)?.data.orEmpty()
    val detail: PlaceMemory? get() = all.firstOrNull { it.id == detailId }
}

/** One-off things the screen must do once (toast, permission, open maps). */
sealed interface PlacesEffect {
    data class Toast(@StringRes val res: Int) : PlacesEffect
    data class Failure(val error: AppError) : PlacesEffect
    data class OpenUrl(val url: String) : PlacesEffect
    data object NeedLocationPermission : PlacesEffect
}
