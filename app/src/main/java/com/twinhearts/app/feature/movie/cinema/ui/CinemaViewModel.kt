package com.twinhearts.app.feature.movie.cinema.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.movie.cinema.data.CinemaRepository
import com.twinhearts.app.feature.movie.cinema.domain.CinemaInvite
import com.twinhearts.app.feature.movie.cinema.domain.CinemaLaunch
import com.twinhearts.app.feature.movie.cinema.domain.CinemaWho
import com.twinhearts.app.feature.movie.cinema.domain.LocalMovieRef
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CinemaUiState(
    /** The "Movie night" sheet (opened from the Twin Cinema 3D card). */
    val sheet: Boolean = false,
    val partnerName: String = "",
    val busy: Boolean = false,
    /** Not null = the 3D theatre is open on top of Watch Together. */
    val launch: CinemaLaunch? = null,
    /** My partner is waiting for me in a theatre. */
    val invite: CinemaInvite? = null,
)

/**
 * Why this file exists: everything the 3D theatre needs from Watch Together, same as the web app:
 * open the lobby (card), 3D MOVIE (open + invite), follow my partner in when both are ready, and the invite card.
 */
@HiltViewModel
class CinemaViewModel @Inject constructor(
    private val repo: CinemaRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CinemaUiState())
    val state: StateFlow<CinemaUiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    val messages: SharedFlow<Int> = _messages.asSharedFlow()

    /** Sentences the server wrote for the user are shown as they are. */
    private val _notes = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val notes: SharedFlow<String> = _notes.asSharedFlow()

    private var who: CinemaWho? = null
    private var following = false
    private val dismissed = mutableSetOf<String>()

    private suspend fun me(): CinemaWho? = who ?: repo.who().also { who = it }

    private fun fail(error: AppError, @StringRes fallback: Int) {
        val text = (error as? AppError.Server)?.message
        if (text != null) _notes.tryEmit(text) else _messages.tryEmit(fallback)
    }

    // ── The card and its sheet ──

    fun openSheet() {
        viewModelScope.launch {
            val w = me()
            if (w == null) _messages.tryEmit(R.string.movie_cinema_connect_first)
            else _state.update { it.copy(sheet = true, partnerName = w.partnerName) }
        }
    }

    fun closeSheet() = _state.update { it.copy(sheet = false) }

    /** "Open cinema lobby": the host enters the lobby first and invites from there. */
    fun openLobby(title: String, local: LocalMovieRef?) = open(title, local, inviteNow = false)

    /** "3D MOVIE": open the theatre, invite the partner, go in. The partner's phone follows by itself. */
    fun start3D(title: String, local: LocalMovieRef?) = open(title, local, inviteNow = true)

    private fun open(title: String, local: LocalMovieRef?, inviteNow: Boolean) {
        if (_state.value.busy) return
        viewModelScope.launch {
            val w = me()
            if (w == null) {
                _messages.tryEmit(R.string.movie_cinema_connect_first)
                return@launch
            }
            _state.update { it.copy(busy = true) }
            when (val r = repo.open(w, title, inviteNow)) {
                is AppResult.Success -> _state.update { it.copy(busy = false, sheet = false, launch = CinemaLaunch(r.data, local)) }
                is AppResult.Failure -> {
                    _state.update { it.copy(busy = false) }
                    fail(r.error, R.string.movie_cinema_err_open)
                }
            }
        }
    }

    fun closeTheatre() = _state.update { it.copy(launch = null) }

    // ── My partner invited me ──

    /** The invite card: checks every 6 seconds while Watch Together is on screen (same as the web app). */
    suspend fun pollInvites() {
        while (true) {
            val s = _state.value
            if (!following && s.launch == null && s.invite == null && !s.busy) {
                val w = me()
                val inv = w?.let { (repo.pending(it) as? AppResult.Success)?.data }
                if (inv != null && inv.id !in dismissed) _state.update { it.copy(invite = inv) }
            }
            delay(INVITE_POLL_MS)
        }
    }

    /**
     * Both of us are READY on Watch Together: when my partner opens the 3D theatre, follow straight in
     * (no card), exactly like the web app. Both phones then land on the avatar page together.
     */
    suspend fun followPartner(local: () -> LocalMovieRef?) {
        following = true
        try {
            while (true) {
                val s = _state.value
                if (s.launch == null && !s.busy) {
                    val w = me()
                    val inv = w?.let { (repo.pending(it) as? AppResult.Success)?.data }
                    if (w != null && inv != null) {
                        _state.update { it.copy(invite = null) }
                        join(w, inv, local())
                    }
                }
                delay(FOLLOW_POLL_MS)
            }
        } finally {
            following = false
        }
    }

    fun accept(inv: CinemaInvite, local: LocalMovieRef?) {
        viewModelScope.launch {
            val w = me() ?: return@launch
            join(w, inv, local)
        }
    }

    fun decline(inv: CinemaInvite) {
        dismissed += inv.id
        _state.update { it.copy(invite = null) }
        viewModelScope.launch { me()?.let { repo.decline(it, inv.id) } }
    }

    private suspend fun join(w: CinemaWho, inv: CinemaInvite, local: LocalMovieRef?) {
        dismissed += inv.id
        _state.update { it.copy(busy = true, invite = null) }
        when (val r = repo.accept(w, inv.id)) {
            is AppResult.Success -> _state.update { it.copy(busy = false, launch = CinemaLaunch(r.data, local)) }
            is AppResult.Failure -> {
                _state.update { it.copy(busy = false) }
                fail(r.error, R.string.movie_cinema_err_join)
            }
        }
    }

    private companion object {
        const val INVITE_POLL_MS = 6_000L
        const val FOLLOW_POLL_MS = 2_000L
    }
}
