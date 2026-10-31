package com.twinhearts.app.feature.ads.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.twinhearts.app.feature.ads.domain.AdHistory
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Why this file exists: the ad counters (how many ads today, when the last one was) must survive
 * the app being closed, so they live in the shared DataStore. Only counters are stored, no personal data.
 */
@Singleton
class AdsPrefs @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val FIRST_SEEN = longPreferencesKey("ads_first_seen")
        val SESSIONS = intPreferencesKey("ads_sessions")
        val LAST_AD = longPreferencesKey("ads_last_interstitial")
        val COUNT_DAY = longPreferencesKey("ads_count_day")
        val COUNT_TODAY = intPreferencesKey("ads_count_today")
        val BREAKS = intPreferencesKey("ads_breaks")
    }

    suspend fun read(nowMillis: Long): AdHistory = dataStore.data.first().toHistory(nowMillis)

    /** One more app launch. The first launch also stamps "first seen" for the new-user grace. */
    suspend fun recordSession(nowMillis: Long) {
        dataStore.edit { p ->
            if (p[Keys.FIRST_SEEN] == null) p[Keys.FIRST_SEEN] = nowMillis
            p[Keys.SESSIONS] = (p[Keys.SESSIONS] ?: 0) + 1
        }
    }

    /** The person finished using a feature and came back Home: count one natural break. */
    suspend fun addBreak(nowMillis: Long): AdHistory {
        val updated = dataStore.edit { p -> p[Keys.BREAKS] = (p[Keys.BREAKS] ?: 0) + 1 }
        return updated.toHistory(nowMillis)
    }

    /** A full-screen ad was really shown: reset the break counter, bump today's count. */
    suspend fun recordShown(nowMillis: Long, epochDay: Long) {
        dataStore.edit { p ->
            val sameDay = p[Keys.COUNT_DAY] == epochDay
            p[Keys.LAST_AD] = nowMillis
            p[Keys.COUNT_DAY] = epochDay
            p[Keys.COUNT_TODAY] = if (sameDay) (p[Keys.COUNT_TODAY] ?: 0) + 1 else 1
            p[Keys.BREAKS] = 0
        }
    }

    private fun Preferences.toHistory(nowMillis: Long) = AdHistory(
        firstSeenAtMillis = this[Keys.FIRST_SEEN] ?: nowMillis,
        sessionCount = this[Keys.SESSIONS] ?: 0,
        lastInterstitialAtMillis = this[Keys.LAST_AD] ?: 0L,
        interstitialsToday = this[Keys.COUNT_TODAY] ?: 0,
        countDay = this[Keys.COUNT_DAY] ?: -1L,
        breaksSinceLastAd = this[Keys.BREAKS] ?: 0,
    )
}
