package com.twinhearts.app.feature.movie.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Why this file exists: the two things the web page keeps only in the browser, so they stay on this
 * phone too: the ⭐ favourites (per couple) and the "Keep Screen On" switch of the player.
 */
@Singleton
class MovieLocalStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    fun favoriteIds(coupleId: String): Flow<Set<String>> =
        dataStore.data.map { it[favKey(coupleId)] ?: emptySet() }

    suspend fun toggleFavorite(coupleId: String, movieId: String) {
        dataStore.edit { prefs ->
            val now = prefs[favKey(coupleId)] ?: emptySet()
            prefs[favKey(coupleId)] = if (movieId in now) now - movieId else now + movieId
        }
    }

    /** On by default, like the web switch. */
    val keepScreenOn: Flow<Boolean> = dataStore.data.map { it[KEEP_AWAKE] ?: true }

    suspend fun setKeepScreenOn(on: Boolean) {
        dataStore.edit { it[KEEP_AWAKE] = on }
    }

    /**
     * The file this phone picked for Direct Device Video. The server only knows its name and length, so without
     * this the phone forgets the file when the app restarts and the movie cannot start (the partner sees nothing).
     */
    suspend fun savedLocalMovie(coupleId: String): SavedLocalMovie? {
        val prefs = dataStore.data.first()
        val uri = prefs[localUriKey(coupleId)] ?: return null
        val title = prefs[localTitleKey(coupleId)] ?: return null
        return SavedLocalMovie(uri, title, prefs[localSecKey(coupleId)] ?: 0)
    }

    suspend fun saveLocalMovie(coupleId: String, movie: SavedLocalMovie) {
        dataStore.edit {
            it[localUriKey(coupleId)] = movie.uri
            it[localTitleKey(coupleId)] = movie.title
            it[localSecKey(coupleId)] = movie.durationSec
        }
    }

    suspend fun clearLocalMovie(coupleId: String) {
        dataStore.edit {
            it.remove(localUriKey(coupleId))
            it.remove(localTitleKey(coupleId))
            it.remove(localSecKey(coupleId))
        }
    }

    private fun favKey(coupleId: String) = stringSetPreferencesKey("movie_fav_$coupleId")
    private fun localUriKey(coupleId: String) = stringPreferencesKey("movie_local_uri_$coupleId")
    private fun localTitleKey(coupleId: String) = stringPreferencesKey("movie_local_title_$coupleId")
    private fun localSecKey(coupleId: String) = intPreferencesKey("movie_local_sec_$coupleId")

    private companion object {
        val KEEP_AWAKE = booleanPreferencesKey("movie_keep_awake")
    }
}

/** A phone file picked for Direct Device Video, remembered between app starts. */
data class SavedLocalMovie(val uri: String, val title: String, val durationSec: Int)
