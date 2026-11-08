package com.twinhearts.app.feature.cinema.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.feature.cinema.data.CinemaRepository
import com.twinhearts.app.feature.cinema.domain.CinemaLinks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Why this file exists: finds the cinema link for this screen.
 * - No `sessionId` in the route: I am the host, so the server opens a session for me.
 * - A `sessionId`: I am the partner and just accepted the invite for that session.
 * The state is the link to load (already carrying the back link), or an error with retry (R-41).
 */
@HiltViewModel
class CinemaViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: CinemaRepository,
) : ViewModel() {

    // Route arguments arrive in the SavedStateHandle under their own names (see Routes.Cinema).
    private val sessionId: String? = savedState.get<String>("sessionId")

    private val _state = MutableStateFlow<UiState<String>>(UiState.Loading)
    val state: StateFlow<UiState<String>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            val who = repository.identity()
            if (who == null) {
                _state.value = UiState.Error(AppError.Unauthorized())
                return@launch
            }
            val result = if (sessionId == null) repository.open(who) else repository.accept(who, sessionId)
            _state.value = when (result) {
                is AppResult.Failure -> UiState.Error(result.error)
                is AppResult.Success ->
                    if (CinemaLinks.isSecure(result.data)) UiState.Content(CinemaLinks.withBackLink(result.data))
                    else UiState.Error(AppError.Unknown)
            }
        }
    }
}
