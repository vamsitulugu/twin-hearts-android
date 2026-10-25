package com.twinhearts.app.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Saves and reads the signed-in [Session] in DataStore. One place, used by every feature. */
@Singleton
class SessionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    /** Emits the session, or null when nobody is signed in. */
    val session: Flow<Session?> = dataStore.data.map { prefs ->
        val userId = prefs[USER_ID]
        val coupleId = prefs[COUPLE_ID]
        if (userId.isNullOrBlank() || coupleId.isNullOrBlank()) {
            null
        } else {
            Session(
                userId = userId,
                coupleId = coupleId,
                myName = prefs[MY_NAME].orEmpty(),
                email = prefs[EMAIL].orEmpty(),
                phoneNumber = prefs[PHONE].orEmpty(),
                partnerName = prefs[PARTNER_NAME] ?: "Partner",
                anniversary = prefs[ANNIVERSARY].orEmpty(),
                role = prefs[ROLE] ?: "user1",
                paired = prefs[PAIRED] ?: false,
                gender = prefs[GENDER],
            )
        }
    }

    suspend fun save(session: Session) {
        dataStore.edit { prefs ->
            prefs[USER_ID] = session.userId
            prefs[COUPLE_ID] = session.coupleId
            prefs[MY_NAME] = session.myName
            prefs[EMAIL] = session.email
            prefs[PHONE] = session.phoneNumber
            prefs[PARTNER_NAME] = session.partnerName
            prefs[ANNIVERSARY] = session.anniversary
            prefs[ROLE] = session.role
            prefs[PAIRED] = session.paired
            if (session.gender != null) prefs[GENDER] = session.gender else prefs.remove(GENDER)
        }
    }

    /** Sign out: forget everything about the account on this phone. */
    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        val USER_ID = stringPreferencesKey("session_user_id")
        val COUPLE_ID = stringPreferencesKey("session_couple_id")
        val MY_NAME = stringPreferencesKey("session_my_name")
        val EMAIL = stringPreferencesKey("session_email")
        val PHONE = stringPreferencesKey("session_phone")
        val PARTNER_NAME = stringPreferencesKey("session_partner_name")
        val ANNIVERSARY = stringPreferencesKey("session_anniversary")
        val ROLE = stringPreferencesKey("session_role")
        val PAIRED = booleanPreferencesKey("session_paired")
        val GENDER = stringPreferencesKey("session_gender")
    }
}
