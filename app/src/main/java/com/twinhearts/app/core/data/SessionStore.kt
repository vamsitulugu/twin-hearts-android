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
    val session: Flow<Session?> = dataStore.data.map { prefs -> prefs.toSession() }

    private fun Preferences.toSession(): Session? {
        val userId = this[USER_ID]
        val coupleId = this[COUPLE_ID]
        return if (userId.isNullOrBlank() || coupleId.isNullOrBlank()) {
            null
        } else {
            Session(
                userId = userId,
                coupleId = coupleId,
                myName = this[MY_NAME].orEmpty(),
                email = this[EMAIL].orEmpty(),
                phoneNumber = this[PHONE].orEmpty(),
                partnerName = this[PARTNER_NAME] ?: "Partner",
                anniversary = this[ANNIVERSARY].orEmpty(),
                role = this[ROLE] ?: "user1",
                paired = this[PAIRED] ?: false,
                gender = this[GENDER],
            )
        }
    }

    /**
     * Changes the saved session in ONE atomic read-modify-write. Prefer this over
     * `save(session.copy(...))`: a copy made from an older read can overwrite a newer change
     * (for example the pairing flag being set by a background check at the same moment).
     */
    suspend fun update(transform: (Session) -> Session) {
        dataStore.edit { prefs ->
            val current = prefs.toSession() ?: return@edit
            val next = transform(current)
            if (next == current) return@edit
            prefs[USER_ID] = next.userId
            prefs[COUPLE_ID] = next.coupleId
            prefs[MY_NAME] = next.myName
            prefs[EMAIL] = next.email
            prefs[PHONE] = next.phoneNumber
            prefs[PARTNER_NAME] = next.partnerName
            prefs[ANNIVERSARY] = next.anniversary
            prefs[ROLE] = next.role
            prefs[PAIRED] = next.paired
            if (next.gender != null) prefs[GENDER] = next.gender else prefs.remove(GENDER)
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
