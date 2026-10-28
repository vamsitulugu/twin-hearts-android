package com.twinhearts.app.feature.us.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.us.data.UsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Us tab container: loads Love Counter, Collection and Dream Goals
 * from the shared couple state, then exposes one combined [UsScreenState].
 */
@HiltViewModel
class UsViewModel @Inject constructor(
    private val repository: UsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UsScreenState>(UsScreenState.Loading)
    val state: StateFlow<UsScreenState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { UsScreenState.Loading }
            val loveCounter = repository.loveCounter()
            val collection = repository.collection()
            val dreamGoals = repository.dreamGoals()

            val next: UsScreenState = when {
                loveCounter is AppResult.Failure -> UsScreenState.Error(loveCounter.error)
                collection is AppResult.Failure -> UsScreenState.Error(collection.error)
                dreamGoals is AppResult.Failure -> UsScreenState.Error(dreamGoals.error)
                loveCounter is AppResult.Success &&
                    collection is AppResult.Success &&
                    dreamGoals is AppResult.Success -> UsScreenState.Content(
                    loveCounter = loveCounter.data,
                    collection = collection.data,
                    dreamGoals = dreamGoals.data,
                )
                else -> UsScreenState.Error(AppError.Unknown)
            }
            _state.update { next }
        }
    }

    fun refresh() {
        load()
    }
}
