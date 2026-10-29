package com.twinhearts.app.feature.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.twinhearts.app.core.data.AppPrefKeys
import com.twinhearts.app.feature.settings.domain.model.AppPrefs
import com.twinhearts.app.feature.settings.domain.model.SettingsFlag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** What Settings saves on this phone (DataStore, R-07). Other features read the same keys from `AppPrefKeys`. */
interface SettingsPrefs {
    val prefs: Flow<AppPrefs>
    suspend fun setFlag(flag: SettingsFlag, on: Boolean)
    suspend fun setQuietHours(start: LocalTime, end: LocalTime)
    suspend fun setLastBackup(text: String)
}

class SettingsPrefsImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsPrefs {

    override val prefs: Flow<AppPrefs> = dataStore.data.map { p ->
        AppPrefs(
            reduceMotion = p[AppPrefKeys.REDUCE_MOTION] ?: false,
            autoSavePhotos = p[AppPrefKeys.AUTO_SAVE_PHOTOS] ?: false,
            autoPlayVoice = p[AppPrefKeys.AUTO_PLAY_VOICE] ?: false,
            aiUseContext = p[AppPrefKeys.AI_USE_CONTEXT] ?: true,
            appLock = p[AppPrefKeys.APP_LOCK] ?: false,
            quietStart = parseTime(p[AppPrefKeys.QUIET_START]) ?: AppPrefs.DEFAULT_QUIET_START,
            quietEnd = parseTime(p[AppPrefKeys.QUIET_END]) ?: AppPrefs.DEFAULT_QUIET_END,
            lastBackup = p[AppPrefKeys.LAST_BACKUP],
        )
    }

    override suspend fun setFlag(flag: SettingsFlag, on: Boolean) {
        dataStore.edit { it[keyOf(flag)] = on }
    }

    override suspend fun setQuietHours(start: LocalTime, end: LocalTime) {
        dataStore.edit {
            it[AppPrefKeys.QUIET_START] = start.format(Clock)
            it[AppPrefKeys.QUIET_END] = end.format(Clock)
        }
    }

    override suspend fun setLastBackup(text: String) {
        dataStore.edit { it[AppPrefKeys.LAST_BACKUP] = text }
    }

    private fun keyOf(flag: SettingsFlag): Preferences.Key<Boolean> = when (flag) {
        SettingsFlag.ReduceMotion -> AppPrefKeys.REDUCE_MOTION
        SettingsFlag.AutoSavePhotos -> AppPrefKeys.AUTO_SAVE_PHOTOS
        SettingsFlag.AutoPlayVoice -> AppPrefKeys.AUTO_PLAY_VOICE
        SettingsFlag.AiUseContext -> AppPrefKeys.AI_USE_CONTEXT
        SettingsFlag.AppLock -> AppPrefKeys.APP_LOCK
    }

    private fun parseTime(text: String?): LocalTime? =
        text?.let { runCatching { LocalTime.parse(it) }.getOrNull() }

    private companion object {
        val Clock: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
