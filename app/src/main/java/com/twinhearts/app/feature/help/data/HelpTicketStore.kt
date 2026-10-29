package com.twinhearts.app.feature.help.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers the id of my support conversation on this phone (web: localStorage
 * `th_support_ticket_id`), so reopening Help continues the same chat.
 * It lives in the shared DataStore, so signing out (which clears it) also forgets the
 * conversation: the next person on this phone never sees it.
 */
@Singleton
class HelpTicketStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    suspend fun savedId(): String? = dataStore.data.first()[TICKET_ID]?.takeIf { it.isNotBlank() }

    suspend fun save(id: String) {
        dataStore.edit { it[TICKET_ID] = id }
    }

    private companion object {
        val TICKET_ID = stringPreferencesKey("help_support_ticket_id")
    }
}
