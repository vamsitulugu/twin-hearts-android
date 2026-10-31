package com.twinhearts.app.feature.search.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Why this file exists: the last few things the person searched, kept on this phone only
 * (DataStore). Stored as one text with a line per search, newest first.
 */
@Singleton
class SearchHistory @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val recents: Flow<List<String>> = dataStore.data.map { prefs ->
        prefs[KEY].orEmpty().split('\n').filter { it.isNotBlank() }
    }

    suspend fun remember(query: String) {
        val clean = query.trim().replace('\n', ' ')
        if (clean.length < MIN_LENGTH) return
        dataStore.edit { prefs ->
            val old = prefs[KEY].orEmpty().split('\n').filter { it.isNotBlank() && !it.equals(clean, ignoreCase = true) }
            prefs[KEY] = (listOf(clean) + old).take(MAX_ITEMS).joinToString("\n")
        }
    }

    suspend fun forget(query: String) {
        dataStore.edit { prefs ->
            prefs[KEY] = prefs[KEY].orEmpty().split('\n').filter { it.isNotBlank() && it != query }.joinToString("\n")
        }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(KEY) }
    }

    private companion object {
        val KEY = stringPreferencesKey("search_recents")
        const val MAX_ITEMS = 8
        const val MIN_LENGTH = 2
    }
}
