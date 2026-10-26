package com.twinhearts.app.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.home.data.CardPhotoProcessor
import com.twinhearts.app.feature.home.data.HomeRepository
import com.twinhearts.app.feature.home.data.LoadedPhoto
import com.twinhearts.app.feature.home.data.PhotoLoadResult
import com.twinhearts.app.feature.home.domain.CropMath
import com.twinhearts.app.feature.home.domain.CropState
import com.twinhearts.app.feature.home.domain.model.CardPhotoSlot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A photo picked for one slot and which part of it is shown. Not saved until "Save". */
data class PendingPhoto(val photo: LoadedPhoto, val crop: CropState)

data class CardPhotoEditorUiState(
    val slot: CardPhotoSlot = CardPhotoSlot.Idle,
    /** Picked but not yet saved photos; each slot keeps its own (like the web editor). */
    val pending: Map<CardPhotoSlot, PendingPhoto> = emptyMap(),
    val isBusy: Boolean = false,
    val message: HomeMessage? = null,
    /** Goes up after every successful save or reset, so Home knows to read the new pictures. */
    val changeCount: Int = 0,
)

/**
 * Logic of the "Card photos" editor (web: `#dwPhotoOv`): choose a slot, pick a photo, move and
 * zoom it inside the 16:9 frame, save or go back to the default picture.
 */
@HiltViewModel
class CardPhotoEditorViewModel @Inject constructor(
    private val repository: HomeRepository,
    private val processor: CardPhotoProcessor,
) : ViewModel() {

    private val _state = MutableStateFlow(CardPhotoEditorUiState())
    val state: StateFlow<CardPhotoEditorUiState> = _state.asStateFlow()

    /** Opens the editor on [slot] (the picture the card is showing now). */
    fun open(slot: CardPhotoSlot) = _state.update { it.copy(slot = slot, message = null) }

    fun selectSlot(slot: CardPhotoSlot) {
        if (_state.value.isBusy) return
        _state.update { it.copy(slot = slot, message = null) }
    }

    fun onPhotoPicked(uri: String?) {
        val slot = _state.value.slot
        if (uri == null || _state.value.isBusy) return
        _state.update { it.copy(isBusy = true, message = null) }
        viewModelScope.launch {
            when (val result = processor.load(uri)) {
                is PhotoLoadResult.Loaded -> {
                    val photo = result.photo
                    val pending = PendingPhoto(photo, CropMath.initial(photo.width, photo.height))
                    _state.update { it.copy(isBusy = false, pending = it.pending + (slot to pending)) }
                }
                PhotoLoadResult.NotAnImage -> problem(R.string.home_photo_not_image)
                PhotoLoadResult.TooLarge -> problem(R.string.home_photo_too_large)
                PhotoLoadResult.Unreadable -> problem(R.string.home_photo_cant_open)
            }
        }
    }

    /** Dragging inside the frame. [stageWidthPx] is the frame's width on screen. */
    fun pan(dx: Float, dy: Float, stageWidthPx: Float) = updateCrop { photo, crop ->
        CropMath.pan(crop, dx, dy, stageWidthPx, photo.width, photo.height)
    }

    /** Pinching ([focusX]/[focusY] = 0..1 inside the frame) or the slider (0.5, 0.5). */
    fun zoomTo(zoom: Float, focusX: Float, focusY: Float) = updateCrop { photo, crop ->
        CropMath.zoomTo(crop, zoom, focusX, focusY, photo.width, photo.height)
    }

    fun save() {
        val current = _state.value
        val slot = current.slot
        val pending = current.pending[slot] ?: return
        if (current.isBusy) return
        _state.update { it.copy(isBusy = true, message = null) }
        viewModelScope.launch {
            val rect = CropMath.rect(pending.crop, pending.photo.width, pending.photo.height)
            val jpeg = processor.render(pending.photo, rect)
            if (jpeg == null) {
                problem(R.string.home_photo_save_failed)
                return@launch
            }
            when (val result = repository.saveCardPhoto(slot, jpeg)) {
                is AppResult.Success -> _state.update {
                    it.copy(
                        isBusy = false,
                        pending = it.pending - slot,
                        message = HomeMessage.Info(R.string.home_photo_saved),
                        changeCount = it.changeCount + 1,
                    )
                }
                is AppResult.Failure -> _state.update {
                    it.copy(isBusy = false, message = HomeMessage.Failure(result.error))
                }
            }
        }
    }

    /** "Use default picture": drops my own photo for the current slot. */
    fun useDefault() {
        val current = _state.value
        if (current.isBusy) return
        _state.update { it.copy(isBusy = true, message = null) }
        viewModelScope.launch {
            when (val result = repository.resetCardPhoto(current.slot)) {
                is AppResult.Success -> _state.update {
                    it.copy(
                        isBusy = false,
                        message = HomeMessage.Info(R.string.home_photo_back_to_default),
                        changeCount = it.changeCount + 1,
                    )
                }
                is AppResult.Failure -> _state.update {
                    it.copy(isBusy = false, message = HomeMessage.Failure(result.error))
                }
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    private fun problem(id: Int) = _state.update { it.copy(isBusy = false, message = HomeMessage.Problem(id)) }

    private fun updateCrop(change: (LoadedPhoto, CropState) -> CropState) = _state.update { state ->
        val pending = state.pending[state.slot] ?: return@update state
        val crop = change(pending.photo, pending.crop)
        state.copy(pending = state.pending + (state.slot to pending.copy(crop = crop)))
    }
}
