package com.twinhearts.app.core.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Why this file exists: the Settings switches are saved on this phone in the shared DataStore.
 * Keeping the key names here (core) lets other features read a switch without importing the
 * Settings feature, e.g. Chat reads [AUTO_PLAY_VOICE] and Twin AI reads [AI_USE_CONTEXT].
 */
object AppPrefKeys {
    /** Settings > Appearance > Reduce motion. Read by TwinApp and applied to every animation. */
    val REDUCE_MOTION = booleanPreferencesKey("settings_reduce_motion")

    /** Settings > Chat & Media > Auto-save photos. */
    val AUTO_SAVE_PHOTOS = booleanPreferencesKey("settings_auto_save_photos")

    /** Settings > Chat & Media > Voice message playback (auto-play on open). */
    val AUTO_PLAY_VOICE = booleanPreferencesKey("settings_auto_play_voice")

    /** Settings > Twin AI > Use relationship context. On by default, like the web app. */
    val AI_USE_CONTEXT = booleanPreferencesKey("settings_ai_use_context")

    /** Settings > Privacy & Security > Lock app on background. */
    val APP_LOCK = booleanPreferencesKey("settings_app_lock")

    /** Settings > Notifications > Quiet hours, saved as "HH:mm". */
    val QUIET_START = stringPreferencesKey("settings_quiet_start")
    val QUIET_END = stringPreferencesKey("settings_quiet_end")

    /** Settings > Storage & Data: when the last backup file was saved. */
    val LAST_BACKUP = stringPreferencesKey("settings_last_backup")
}
