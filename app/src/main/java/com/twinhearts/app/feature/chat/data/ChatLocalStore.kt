package com.twinhearts.app.feature.chat.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.twinhearts.app.feature.chat.domain.model.ChatSettings
import com.twinhearts.app.feature.chat.domain.model.Wallpaper
import com.twinhearts.app.feature.chat.domain.model.WallpaperMode
import com.twinhearts.app.feature.chat.domain.pushRecentEmoji
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Why this file exists: the things the web chat keeps in localStorage (mute, pet name,
 * do-not-disturb, recent emoji, the unsent draft, this phone's wallpaper) kept in DataStore (R-07).
 * Every key carries the couple id, so two accounts on one phone never mix.
 */
interface ChatLocalStore {
    fun settings(coupleId: String): Flow<ChatSettings>
    suspend fun setMuted(coupleId: String, muted: Boolean)
    suspend fun setNickname(coupleId: String, nickname: String)

    /** Both null turns do-not-disturb off. */
    suspend fun setDnd(coupleId: String, start: String?, end: String?)

    /** Minutes of no touching before the chat locks; 0 = never. */
    suspend fun setAutoLock(coupleId: String, minutes: Int)

    fun recentEmoji(): Flow<List<String>>
    suspend fun useEmoji(emoji: String)

    suspend fun draft(coupleId: String): String
    suspend fun saveDraft(coupleId: String, text: String)

    /** "Shared with partner" switch of the wallpaper screen. */
    fun wallpaperShared(coupleId: String): Flow<Boolean>
    suspend fun setWallpaperShared(coupleId: String, shared: Boolean)

    /** The wallpaper kept on this phone only (used while sharing is off). */
    fun localWallpaper(coupleId: String): Flow<Wallpaper>
    suspend fun setLocalWallpaper(coupleId: String, wallpaper: Wallpaper)
}

@Singleton
class DataStoreChatLocalStore @Inject constructor(
    private val store: DataStore<Preferences>,
) : ChatLocalStore {

    private fun key(name: String, coupleId: String) = "chat_${name}_$coupleId"

    override fun settings(coupleId: String): Flow<ChatSettings> = store.data.map { p ->
        ChatSettings(
            muted = p[booleanPreferencesKey(key("muted", coupleId))] ?: false,
            nickname = p[stringPreferencesKey(key("nickname", coupleId))].orEmpty(),
            dndStart = p[stringPreferencesKey(key("dnd_start", coupleId))],
            dndEnd = p[stringPreferencesKey(key("dnd_end", coupleId))],
            autoLockMinutes = p[intPreferencesKey(key("autolock", coupleId))] ?: 0,
        )
    }

    override suspend fun setMuted(coupleId: String, muted: Boolean) {
        store.edit { it[booleanPreferencesKey(key("muted", coupleId))] = muted }
    }

    override suspend fun setNickname(coupleId: String, nickname: String) {
        store.edit {
            val k = stringPreferencesKey(key("nickname", coupleId))
            if (nickname.isBlank()) it.remove(k) else it[k] = nickname.trim()
        }
    }

    override suspend fun setDnd(coupleId: String, start: String?, end: String?) {
        store.edit {
            val s = stringPreferencesKey(key("dnd_start", coupleId))
            val e = stringPreferencesKey(key("dnd_end", coupleId))
            if (start == null || end == null) {
                it.remove(s)
                it.remove(e)
            } else {
                it[s] = start
                it[e] = end
            }
        }
    }

    override suspend fun setAutoLock(coupleId: String, minutes: Int) {
        store.edit { it[intPreferencesKey(key("autolock", coupleId))] = minutes.coerceAtLeast(0) }
    }

    private val recentKey = stringPreferencesKey("chat_recent_emoji")

    override fun recentEmoji(): Flow<List<String>> = store.data.map { p ->
        p[recentKey]?.split(SEPARATOR)?.filter { it.isNotEmpty() }.orEmpty()
    }

    override suspend fun useEmoji(emoji: String) {
        store.edit { p ->
            val current = p[recentKey]?.split(SEPARATOR)?.filter { it.isNotEmpty() }.orEmpty()
            p[recentKey] = pushRecentEmoji(current, emoji).joinToString(SEPARATOR)
        }
    }

    override suspend fun draft(coupleId: String): String =
        store.data.first()[stringPreferencesKey(key("draft", coupleId))].orEmpty()

    override suspend fun saveDraft(coupleId: String, text: String) {
        store.edit {
            val k = stringPreferencesKey(key("draft", coupleId))
            if (text.isBlank()) it.remove(k) else it[k] = text
        }
    }

    override fun wallpaperShared(coupleId: String): Flow<Boolean> =
        store.data.map { it[booleanPreferencesKey(key("wp_shared", coupleId))] ?: false }

    override suspend fun setWallpaperShared(coupleId: String, shared: Boolean) {
        store.edit { it[booleanPreferencesKey(key("wp_shared", coupleId))] = shared }
    }

    override fun localWallpaper(coupleId: String): Flow<Wallpaper> = store.data.map { p ->
        Wallpaper(
            mode = WallpaperMode.fromApi(p[stringPreferencesKey(key("wp_mode", coupleId))]),
            swatch = p[stringPreferencesKey(key("wp_swatch", coupleId))],
            image = p[stringPreferencesKey(key("wp_image", coupleId))],
            dim = p[intPreferencesKey(key("wp_dim", coupleId))] ?: 0,
        )
    }

    override suspend fun setLocalWallpaper(coupleId: String, wallpaper: Wallpaper) {
        store.edit {
            it[stringPreferencesKey(key("wp_mode", coupleId))] = wallpaper.mode.apiValue
            val swatch = stringPreferencesKey(key("wp_swatch", coupleId))
            val image = stringPreferencesKey(key("wp_image", coupleId))
            if (wallpaper.swatch == null) it.remove(swatch) else it[swatch] = wallpaper.swatch
            if (wallpaper.image == null) it.remove(image) else it[image] = wallpaper.image
            it[intPreferencesKey(key("wp_dim", coupleId))] = wallpaper.dim
        }
    }

    private companion object {
        /** A character no emoji uses, so the list can be stored as one string. */
        const val SEPARATOR = "\u001F"
    }
}
