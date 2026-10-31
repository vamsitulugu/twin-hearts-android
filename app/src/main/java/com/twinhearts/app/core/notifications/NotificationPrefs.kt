package com.twinhearts.app.core.notifications

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.twinhearts.app.core.data.AppPrefKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Why this file exists: the person's notification choices, saved on this phone (DataStore).
 * Master switch, one switch per [NotificationType], lock-screen preview, and the quiet hours that
 * Settings > Notifications already saves under [AppPrefKeys.QUIET_START] / [AppPrefKeys.QUIET_END].
 */
@Singleton
class NotificationPrefs @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private fun typeKey(type: NotificationType) = booleanPreferencesKey("notif_type_${type.key}")

    /** Every type's on/off, for the Settings list. */
    val enabledMap: Flow<Map<NotificationType, Boolean>> = dataStore.data.map { p ->
        NotificationType.entries.associateWith { p[typeKey(it)] ?: it.defaultOn }
    }

    val masterOn: Flow<Boolean> = dataStore.data.map { it[MASTER] ?: true }
    val showPreview: Flow<Boolean> = dataStore.data.map { it[PREVIEW] ?: true }

    suspend fun isEnabled(type: NotificationType): Boolean {
        val p = dataStore.data.first()
        return (p[MASTER] ?: true) && (p[typeKey(type)] ?: type.defaultOn)
    }

    suspend fun previewAllowed(): Boolean = dataStore.data.first()[PREVIEW] ?: true

    suspend fun setType(type: NotificationType, on: Boolean) {
        dataStore.edit { it[typeKey(type)] = on }
    }

    suspend fun setMaster(on: Boolean) {
        dataStore.edit { it[MASTER] = on }
    }

    suspend fun setPreview(on: Boolean) {
        dataStore.edit { it[PREVIEW] = on }
    }

    /** True inside the person's quiet hours (default 22:00 to 08:00, may cross midnight). */
    suspend fun isQuietNow(now: LocalTime = LocalTime.now()): Boolean {
        val p = dataStore.data.first()
        val start = parse(p[AppPrefKeys.QUIET_START]) ?: LocalTime.of(22, 0)
        val end = parse(p[AppPrefKeys.QUIET_END]) ?: LocalTime.of(8, 0)
        return when {
            start == end -> false
            start.isBefore(end) -> !now.isBefore(start) && now.isBefore(end)
            else -> !now.isBefore(start) || now.isBefore(end)
        }
    }

    /** The last token the server accepted, so we only upload when it changes. */
    suspend fun lastUploadedToken(): String? = dataStore.data.first()[TOKEN]

    suspend fun saveUploadedToken(value: String?) {
        dataStore.edit { if (value == null) it.remove(TOKEN) else it[TOKEN] = value }
    }

    private fun parse(text: String?): LocalTime? = runCatching { LocalTime.parse(text) }.getOrNull()

    private companion object {
        val MASTER = booleanPreferencesKey("notif_master")
        val PREVIEW = booleanPreferencesKey("notif_preview")
        val TOKEN = stringPreferencesKey("notif_uploaded_token")
    }
}
