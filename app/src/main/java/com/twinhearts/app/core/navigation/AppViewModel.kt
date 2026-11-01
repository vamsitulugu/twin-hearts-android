package com.twinhearts.app.core.navigation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.core.data.AppPrefKeys
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.notifications.NotificationNavigator
import com.twinhearts.app.core.presence.PartnerPresenceSource
import com.twinhearts.app.feature.chat.domain.model.PartnerPresence
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
    dataStore: DataStore<Preferences>,
    /** Hands a tapped notification's destination to the nav graph. */
    val notificationNavigator: NotificationNavigator,
    private val partnerPresenceSource: PartnerPresenceSource,
) : ViewModel() {

    /** Who is signed in, for the side menu's couple widget. Null until known / after sign out. */
    val session: StateFlow<Session?> = sessionStore.session
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Settings > Appearance > Reduce motion, saved on this phone. */
    val reduceMotion: StateFlow<Boolean> = dataStore.data
        .map { it[AppPrefKeys.REDUCE_MOTION] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _partnerPresence = MutableStateFlow<PartnerPresence?>(null)

    /** My partner's Online / Away / Last seen for the side menu. Null until the first answer. */
    val partnerPresence: StateFlow<PartnerPresence?> = _partnerPresence.asStateFlow()

    private var presenceJob: Job? = null

    /**
     * The side menu asks for presence only while it is open: no background polling, so the free
     * Render server is not woken or loaded for a line nobody is looking at.
     */
    fun setSidebarOpen(open: Boolean) {
        presenceJob?.cancel()
        presenceJob = null
        if (!open) return
        presenceJob = viewModelScope.launch {
            while (true) {
                partnerPresenceSource.fetch()?.let { _partnerPresence.value = it }
                delay(PRESENCE_POLL_MS)
            }
        }
    }

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

    private companion object {
        const val PRESENCE_POLL_MS = 10_000L
    }
}
