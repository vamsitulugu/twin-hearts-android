package com.twinhearts.app.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Decides the first screen once at launch:
 * not signed in -> Get Started, signed in -> Home.
 * (Home shows a "Connect with your partner" card while there is no partner yet.)
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    sessionStore: SessionStore,
) : ViewModel() {

    /** Who is signed in, for the side menu's couple widget. Null until known / after sign out. */
    val session: StateFlow<Session?> = sessionStore.session
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _startRoute = MutableStateFlow<Any?>(null)
    val startRoute: StateFlow<Any?> = _startRoute.asStateFlow()

    init {
        viewModelScope.launch {
            val session = sessionStore.session.first()
            _startRoute.value = when {
                session == null -> Routes.GetStarted
                else -> Routes.Home
            }
        }
    }
}
