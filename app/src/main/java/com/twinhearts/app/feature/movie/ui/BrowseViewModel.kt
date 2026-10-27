package com.twinhearts.app.feature.movie.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.feature.movie.data.MovieRepository
import com.twinhearts.app.feature.movie.data.local.MovieLocalStore
import com.twinhearts.app.feature.movie.domain.model.MovieIdentity
import com.twinhearts.app.feature.movie.domain.model.MovieItem
import com.twinhearts.app.feature.movie.domain.model.MovieOwner
import com.twinhearts.app.feature.movie.domain.model.WatchStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The online (TMDB) search box inside the Library tab. */
sealed interface OnlineSearch {
    data object Idle : OnlineSearch
    data object Loading : OnlineSearch
    data class Results(val items: List<MovieItem>) : OnlineSearch
    data object Failed : OnlineSearch
}

/**
 * Why this file exists: it holds the couple's movie list (Home / Library / Genres tabs) and the
 * online search. It is separate from [WatchViewModel] because browsing needs no clock or polling.
 */
@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val repo: MovieRepository,
    private val store: MovieLocalStore,
) : ViewModel() {

    private val _movies = MutableStateFlow<UiState<List<MovieItem>>>(UiState.Loading)
    val movies: StateFlow<UiState<List<MovieItem>>> = _movies.asStateFlow()

    private val _online = MutableStateFlow<OnlineSearch>(OnlineSearch.Idle)
    val online: StateFlow<OnlineSearch> = _online.asStateFlow()

    private val _messages = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    val messages: SharedFlow<Int> = _messages.asSharedFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())

    /** The ⭐ movies of this phone (the web keeps them in the browser, not on the server). */
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _role = MutableStateFlow<String?>(null)

    /** "user1" or "user2": needed to tell "mine" from "partner's" in the owner chips. */
    val role: StateFlow<String?> = _role.asStateFlow()

    private var who: MovieIdentity? = null
    private var favoritesJob: Job? = null

    init { load() }

    fun load() {
        _movies.value = UiState.Loading
        viewModelScope.launch {
            val me = repo.identity()
            who = me
            if (me == null) {
                _movies.value = UiState.Empty
                return@launch
            }
            _role.value = me.role
            if (favoritesJob == null) {
                favoritesJob = viewModelScope.launch { store.favoriteIds(me.coupleId).collect { _favorites.value = it } }
            }
            _movies.value = when (val r = repo.favorites(me)) {
                is AppResult.Success -> if (r.data.isEmpty()) UiState.Empty else UiState.Content(r.data)
                is AppResult.Failure -> UiState.Error(r.error)
            }
        }
    }

    fun searchOnline(query: String) {
        if (query.isBlank()) {
            _online.value = OnlineSearch.Idle
            return
        }
        _online.value = OnlineSearch.Loading
        viewModelScope.launch {
            _online.value = when (val r = repo.search(query.trim())) {
                is AppResult.Success -> OnlineSearch.Results(r.data)
                is AppResult.Failure -> OnlineSearch.Failed
            }
        }
    }

    fun clearSearch() { _online.value = OnlineSearch.Idle }

    fun add(movie: MovieItem) {
        val me = who ?: return
        viewModelScope.launch {
            when (val r = repo.addFavorite(me, movie.copy(status = WatchStatus.Watchlist))) {
                is AppResult.Success -> {
                    _messages.tryEmit(R.string.movie_toast_added)
                    reloadQuietly(me)
                }
                is AppResult.Failure -> _messages.tryEmit(R.string.movie_toast_add_failed)
            }
        }
    }

    fun toggleFavorite(movie: MovieItem) {
        val me = who ?: return
        if (movie.id.isBlank()) return
        viewModelScope.launch { store.toggleFavorite(me.coupleId, movie.id) }
    }

    /** The "Add Movie" form of the web page. It saves into the same shared list as "Search online". */
    fun addManual(title: String, year: String, genre: String?, status: WatchStatus, stars: Int, owner: MovieOwner, onDone: () -> Unit) {
        val me = who ?: return
        val partner = if (me.role == "user1") "user2" else "user1"
        val movie = MovieItem(
            id = "", tmdbId = null, title = title.trim(), year = year.trim().ifEmpty { null }, genre = genre,
            posterUrl = null, backdropUrl = null, overview = null, rating = stars.takeIf { it > 0 }?.toDouble(), status = status,
            addedBy = when (owner) {
                MovieOwner.Mine -> me.role
                MovieOwner.Partner -> partner
                MovieOwner.Together -> "both"
            },
        )
        viewModelScope.launch {
            when (repo.addFavorite(me, movie)) {
                is AppResult.Success -> {
                    _messages.tryEmit(R.string.movie_toast_added)
                    reloadQuietly(me)
                    onDone()
                }
                is AppResult.Failure -> _messages.tryEmit(R.string.movie_toast_add_failed)
            }
        }
    }

    fun setStatus(movie: MovieItem, status: WatchStatus) = update(movie, status, null)

    fun setRating(movie: MovieItem, stars: Int) = update(movie, null, stars.toDouble())

    private fun update(movie: MovieItem, status: WatchStatus?, rating: Double?) {
        val me = who ?: return
        viewModelScope.launch {
            when (repo.updateFavorite(movie.id, status, rating)) {
                is AppResult.Success -> reloadQuietly(me)
                is AppResult.Failure -> _messages.tryEmit(R.string.movie_toast_update_failed)
            }
        }
    }

    fun remove(movie: MovieItem) {
        val me = who ?: return
        viewModelScope.launch {
            when (repo.deleteFavorite(movie.id)) {
                is AppResult.Success -> {
                    _messages.tryEmit(R.string.movie_toast_removed)
                    reloadQuietly(me)
                }
                is AppResult.Failure -> _messages.tryEmit(R.string.movie_toast_delete_failed)
            }
        }
    }

    /** Refresh the list without flashing the loading skeleton. */
    private suspend fun reloadQuietly(me: MovieIdentity) {
        val r = repo.favorites(me)
        if (r is AppResult.Success) _movies.update { if (r.data.isEmpty()) UiState.Empty else UiState.Content(r.data) }
    }
}
