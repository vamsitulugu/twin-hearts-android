package com.twinhearts.app.feature.surprises.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.surprises.data.SurprisesRepository
import com.twinhearts.app.feature.surprises.domain.isLocked
import com.twinhearts.app.feature.surprises.domain.model.NewCapsule
import com.twinhearts.app.feature.surprises.domain.model.NewSurprise
import com.twinhearts.app.feature.surprises.domain.model.PickedMedia
import com.twinhearts.app.feature.surprises.domain.model.SurpriseKind
import com.twinhearts.app.feature.surprises.domain.model.SurprisesData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Why this file exists: runs the Surprises page. Checks the form like the web does ("Title and unlock
 * date required", "Write a message and pick a date"), asks the repository to save, and turns the
 * answer into a screen state. It never shows a half-saved item: the lists change only after the
 * server accepted the change.
 */
@HiltViewModel
class SurprisesViewModel @Inject constructor(
    private val repository: SurprisesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SurprisesUiState())
    val state: StateFlow<SurprisesUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    /** Loads the page. Also used by the Sync button and when the screen comes back to the front. */
    fun refresh() {
        viewModelScope.launch {
            val hadData = _state.value.let { !it.loading && it.loadError == null }
            if (!hadData) _state.update { it.copy(loading = true, loadError = null) }
            when (val result = repository.load()) {
                is AppResult.Success -> _state.update {
                    it.copy(loading = false, loadError = null, data = result.data, today = LocalDate.now())
                }
                is AppResult.Failure -> _state.update {
                    if (hadData) {
                        it.copy(message = SurprisesMessage.Failed(result.error))
                    } else {
                        it.copy(loading = false, loadError = result.error)
                    }
                }
            }
        }
    }

    fun selectTab(tab: SurprisesTab) = _state.update { it.copy(tab = tab) }

    fun addSurprise(kind: SurpriseKind, title: String, message: String, media: PickedMedia?, unlock: LocalDate?) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty() || unlock == null) {
            _state.update { it.copy(message = SurprisesMessage.NeedTitleAndDate) }
            return
        }
        save(SurprisesMessage.Hidden) { repository.addSurprise(NewSurprise(kind, cleanTitle, message.trim(), media, unlock)) }
    }

    fun deleteSurprise(id: String) = save(SurprisesMessage.SurpriseDeleted) { repository.deleteSurprise(id) }

    fun addCapsule(message: String, unlock: LocalDate?) {
        val clean = message.trim()
        if (clean.isEmpty() || unlock == null) {
            _state.update { it.copy(message = SurprisesMessage.NeedMessageAndDate) }
            return
        }
        save(SurprisesMessage.Sealed) { repository.addCapsule(NewCapsule(clean, unlock)) }
    }

    /** Same guard as the web's `openCapsule()`: a capsule cannot be opened before its date. */
    fun openCapsule(id: String, unlock: LocalDate) {
        if (isLocked(unlock, _state.value.today)) {
            _state.update { it.copy(message = SurprisesMessage.StillLocked(unlock)) }
            return
        }
        save(success = null) { repository.openCapsule(id) }
    }

    fun deleteCapsule(id: String) = save(SurprisesMessage.CapsuleDeleted) { repository.deleteCapsule(id) }

    fun messageShown() = _state.update { it.copy(message = null) }

    private fun save(success: SurprisesMessage?, block: suspend () -> AppResult<SurprisesData>) {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            when (val result = block()) {
                is AppResult.Success -> _state.update {
                    it.copy(saving = false, data = result.data, today = LocalDate.now(), message = success)
                }
                is AppResult.Failure -> _state.update {
                    it.copy(saving = false, message = SurprisesMessage.Failed(result.error))
                }
            }
        }
    }
}
