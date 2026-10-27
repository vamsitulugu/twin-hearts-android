package com.twinhearts.app.feature.us.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    private val _state = MutableStateFlow(UsScreenState.Loading)
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

            val results = listOf(loveCounter, collection, dreamGoals)
            val failure = results.firstOrNull { it is AppResult.Failure<*> } as? AppResult.Failure<*>
            if (failure != null) {
                _state.update { UsScreenState.Error(failure.error) }
                return@launch
            }

            _state.update {
                UsScreenState.Content(
                    loveCounter = (loveCounter as AppResult.Success<*>).data,
                    collection = (collection as AppResult.Success<*>).data,
                    dreamGoals = (dreamGoals as AppResult.Success<*>).data,
                )
            }
        }
    }

    fun refresh() {
        load()
    }
}
