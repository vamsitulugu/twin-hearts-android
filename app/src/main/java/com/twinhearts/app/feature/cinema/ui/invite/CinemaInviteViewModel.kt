package com.twinhearts.app.feature.cinema.ui.invite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.cinema.data.CinemaRepository
import com.twinhearts.app.feature.cinema.domain.model.CinemaInvite
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Why this file exists: holds the invite my partner sent, so the dialog survives rotation and the same invite
 * is never shown twice after I answered it. The polling itself runs in [CinemaInviteWatcher].
 */
@HiltViewModel
class CinemaInviteViewModel @Inject constructor(
    private val repository: CinemaRepository,
) : ViewModel() {

    private val _invite = MutableStateFlow<CinemaInvite?>(null)
    val invite: StateFlow<CinemaInvite?> = _invite.asStateFlow()

    /** Invites already answered in this app run (the server keeps an invite open for 10 minutes). */
    private val answered = mutableSetOf<String>()

    /** Asks the server once whether an invite is waiting. A failed check is simply tried again next time. */
    suspend fun check() {
        if (_invite.value != null) return
        val who = repository.identity() ?: return
        val result = repository.pendingInvite(who)
        if (result is AppResult.Success) {
            result.data?.takeIf { it.id !in answered }?.let { _invite.value = it }
        }
    }

    /** "Accept": returns the session id to open (the Cinema screen calls the server's accept itself). */
    fun accept(): String? {
        val invite = _invite.value ?: return null
        answered += invite.id
        _invite.value = null
        return invite.id
    }

    /** "Not now": tells the host and closes the dialog. */
    fun notNow() {
        val invite = _invite.value ?: return
        answered += invite.id
        _invite.value = null
        viewModelScope.launch {
            repository.identity()?.let { repository.decline(it, invite.id) }
        }
    }
}
