package com.twinhearts.app.feature.level.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.level.data.LevelRepository
import com.twinhearts.app.feature.level.domain.LevelState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LevelUiState {
    data object Loading : LevelUiState
    data class Error(val error: AppError) : LevelUiState
    data class Content(val level: LevelState) : LevelUiState
}

@HiltViewModel
class LevelViewModel @Inject constructor(
    private val repository: LevelRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<LevelUiState>(LevelUiState.Loading)
    val state: StateFlow<LevelUiState> = _state.asStateFlow()

    init {
        load()
    }

    /** Loads on open and again from the Sync button. An older result stays on screen while it reloads. */
    fun load() {
        viewModelScope.launch {
            if (_state.value is LevelUiState.Error) _state.value = LevelUiState.Loading
            _state.value = when (val r = repository.level()) {
                is AppResult.Success -> LevelUiState.Content(r.data)
                is AppResult.Failure -> LevelUiState.Error(r.error)
            }
        }
    }
}
