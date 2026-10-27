package com.twinhearts.app.feature.movie.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
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

    private fun favKey(coupleId: String) = stringSetPreferencesKey("movie_fav_$coupleId")

    private companion object {
        val KEEP_AWAKE = booleanPreferencesKey("movie_keep_awake")
    }
}
