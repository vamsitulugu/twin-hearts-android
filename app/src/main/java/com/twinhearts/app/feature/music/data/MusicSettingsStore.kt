package com.twinhearts.app.feature.music.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.twinhearts.app.feature.music.domain.model.FxPreset
import com.twinhearts.app.feature.music.domain.model.MusicSettings
import com.twinhearts.app.feature.music.domain.model.SoundFx
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Why this file exists: remembers the Music settings on the phone (DataStore, R-07). The web app keeps
 * the same values in localStorage `mx_settings_v1`; they are per device there too.
 */
@Singleton
class MusicSettingsStore @Inject constructor(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<MusicSettings> = dataStore.data.map { p ->
        MusicSettings(
            crossfadeSec = (p[CROSSFADE] ?: 0).coerceIn(0, MAX_CROSSFADE),
            autoLyrics = p[AUTO_LYRICS] ?: true,
            normalize = p[NORMALIZE] ?: false,
            vocalReduce = p[VOCAL_REDUCE] ?: false,
            preset = FxPreset.entries.firstOrNull { it.name == p[PRESET] } ?: FxPreset.Flat,
            fx = SoundFx(
                reverb = (p[REVERB] ?: 0f).coerceIn(0f, 1f),
                echo = (p[ECHO] ?: 0f).coerceIn(0f, 1f),
                bass = (p[BASS] ?: 0).coerceIn(-MAX_DB, MAX_DB),
                treble = (p[TREBLE] ?: 0).coerceIn(-MAX_DB, MAX_DB),
            ),
        )
    }

    suspend fun setCrossfade(seconds: Int) = dataStore.edit { it[CROSSFADE] = seconds.coerceIn(0, MAX_CROSSFADE) }

    suspend fun setAutoLyrics(on: Boolean) = dataStore.edit { it[AUTO_LYRICS] = on }

    suspend fun setNormalize(on: Boolean) = dataStore.edit { it[NORMALIZE] = on }

    suspend fun setVocalReduce(on: Boolean) = dataStore.edit { it[VOCAL_REDUCE] = on }

    /** Picks a ready-made look. [FxPreset.Custom] has no values of its own, so it changes nothing. */
    suspend fun setPreset(preset: FxPreset) {
        val fx = preset.fx ?: return
        dataStore.edit {
            it[PRESET] = preset.name
            writeFx(it, fx)
        }
    }

    /** A slider was moved by hand, so the look becomes "Custom". */
    suspend fun setFx(fx: SoundFx) = dataStore.edit {
        it[PRESET] = FxPreset.Custom.name
        writeFx(it, fx)
    }

    /** "Reset" in the Sound effects sheet: flat sound, no vocal reduction, no normalising (same as the web). */
    suspend fun resetSound() = dataStore.edit {
        it[PRESET] = FxPreset.Flat.name
        writeFx(it, SoundFx())
        it[VOCAL_REDUCE] = false
        it[NORMALIZE] = false
    }

    private fun writeFx(p: androidx.datastore.preferences.core.MutablePreferences, fx: SoundFx) {
        p[REVERB] = fx.reverb
        p[ECHO] = fx.echo
        p[BASS] = fx.bass
        p[TREBLE] = fx.treble
    }

    private companion object {
        const val MAX_CROSSFADE = 12
        const val MAX_DB = 10
        val CROSSFADE = intPreferencesKey("music_crossfade_sec")
        val AUTO_LYRICS = booleanPreferencesKey("music_auto_lyrics")
        val NORMALIZE = booleanPreferencesKey("music_normalize")
        val VOCAL_REDUCE = booleanPreferencesKey("music_vocal_reduce")
        val PRESET = stringPreferencesKey("music_fx_preset")
        val REVERB = floatPreferencesKey("music_fx_reverb")
        val ECHO = floatPreferencesKey("music_fx_echo")
        val BASS = intPreferencesKey("music_fx_bass")
        val TREBLE = intPreferencesKey("music_fx_treble")
    }
}
