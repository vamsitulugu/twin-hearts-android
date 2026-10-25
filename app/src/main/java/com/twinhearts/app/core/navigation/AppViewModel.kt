package com.twinhearts.app.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.core.data.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Decides the first screen once at launch: Home when someone is signed in, Get Started when not.
 * Reading the session takes a few milliseconds, so [startRoute] is null until it is known.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    sessionStore: SessionStore,
) : ViewModel() {

    private val _startRoute = MutableStateFlow<Any?>(null)
    val startRoute: StateFlow<Any?> = _startRoute.asStateFlow()

    init {
        viewModelScope.launch {
            _startRoute.value = if (sessionStore.session.first() == null) Routes.GetStarted else Routes.Home
        }
    }
}
