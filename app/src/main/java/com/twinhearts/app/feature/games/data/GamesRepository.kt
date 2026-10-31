package com.twinhearts.app.feature.games.data

import android.content.Context
import android.net.Uri
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.games.data.mapper.GamesKeys
import com.twinhearts.app.feature.games.data.mapper.asStateObject
import com.twinhearts.app.feature.games.data.mapper.gameDoc
import com.twinhearts.app.feature.games.data.mapper.presenceOf
import com.twinhearts.app.feature.games.data.mapper.profileOf
import com.twinhearts.app.feature.games.data.remote.GamesApi
import com.twinhearts.app.feature.games.data.remote.SaveStateBody
import com.twinhearts.app.feature.games.domain.model.GameId
import com.twinhearts.app.feature.games.domain.model.PlayerProfile
import com.twinhearts.app.feature.games.domain.model.Players
import com.twinhearts.app.feature.games.domain.model.Presence
import com.twinhearts.app.feature.games.domain.model.Side
import com.twinhearts.app.feature.games.domain.engine.TextMatch
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/** One read of the shared state: both players, plus the saved document of one game (if asked). */
data class GamesSnapshot(
    val players: Players,
    val doc: JsonElement?,
)

interface GamesRepository {
    /** Reads names, progress, presence and (when [game] is given) that game's saved document. */
    suspend fun snapshot(game: GameId?): AppResult<GamesSnapshot>

    /** Loads both profile photos once. Never fails: a missing photo just shows a letter. */
    suspend fun loadAvatars()

    suspend fun saveDoc(game: GameId, doc: JsonElement): AppResult<Unit>

    /** Changes only MY profile (XP, coins, favourites...) and saves it. */
    suspend fun updateMyProfile(transform: (PlayerProfile) -> PlayerProfile): AppResult<PlayerProfile>

    /** Tells the partner what I am playing (null = just on the hub). */
    suspend fun setPresence(game: GameId?): AppResult<Unit>

    /** Uploads the Picture Puzzle photo at [uri] and returns where it now lives. */
    suspend fun uploadPhoto(uri: String): AppResult<String>

    /**
     * Words that mean about the same as [word] (Draw & Guess uses them to accept "boy" for
     * "human"). Empty when the phone is offline: the other checks still work.
     */
    suspend fun relatedWords(word: String): Set<String>
}

/**
 * Singleton on purpose: it remembers my profile and the two photos, so every game and the hub
 * see the same numbers without re-reading the whole state after each reward.
 */
@Singleton
class GamesRepositoryImpl @Inject constructor(
    private val api: GamesApi,
    private val sessionStore: SessionStore,
    private val json: Json,
    @ApplicationContext private val context: Context,
) : GamesRepository {

    private val relatedCache = HashMap<String, Set<String>>()

    private val profileLock = Mutex()
    private var myProfile: PlayerProfile? = null
    private var myAvatar: String? = null
    private var partnerAvatar: String? = null
    private var avatarsLoadedFor: String? = null

    private suspend fun session(): Session? = sessionStore.session.first()

    override suspend fun snapshot(game: GameId?): AppResult<GamesSnapshot> {
        val session = session() ?: return AppResult.Failure(AppError.Unauthorized())
        return when (val result = safeApiCall { api.getState(session.coupleId) }) {
            is AppResult.Failure -> result
            is AppResult.Success -> {
                val state = result.data.asStateObject()
                AppResult.Success(GamesSnapshot(players(session, state), game?.let { state.gameDoc(it) }))
            }
        }
    }

    private suspend fun players(session: Session, state: JsonObject): Players {
        val side = Side.fromRole(session.role)
        // My own progress is only ever written by this phone, so the copy in memory wins.
        val mine = profileLock.withLock {
            myProfile ?: (state.profileOf(side, json) ?: PlayerProfile()).also { myProfile = it }
        }
        val presence: Presence = state.presenceOf(side.other, json)
        return Players(
            side = side,
            myName = session.myName.ifBlank { "You" },
            partnerName = session.partnerName.ifBlank { "Partner" },
            myAvatar = myAvatar,
            partnerAvatar = partnerAvatar,
            me = mine,
            partner = state.profileOf(side.other, json) ?: PlayerProfile(),
            partnerPresence = presence,
            partnerOnline = presence.isOnline(System.currentTimeMillis()),
        )
    }

    override suspend fun loadAvatars() {
        val session = session() ?: return
        if (avatarsLoadedFor == session.userId) return
        val mine = safeApiCall { api.getMyProfile(session.userId) }
        val partner = safeApiCall { api.getPartnerProfile(session.userId) }
        if (mine is AppResult.Success) myAvatar = mine.data.avatarUrl?.takeIf { it.isNotBlank() }
        if (partner is AppResult.Success) partnerAvatar = partner.data.profile?.avatarUrl?.takeIf { it.isNotBlank() }
        // Only remember the attempt when at least one call worked, so a bad network retries later.
        if (mine is AppResult.Success || partner is AppResult.Success) avatarsLoadedFor = session.userId
    }

    override suspend fun saveDoc(game: GameId, doc: JsonElement): AppResult<Unit> =
        save(buildJsonObject { put(GamesKeys.doc(game), doc) })

    override suspend fun updateMyProfile(
        transform: (PlayerProfile) -> PlayerProfile,
    ): AppResult<PlayerProfile> {
        val session = session() ?: return AppResult.Failure(AppError.Unauthorized())
        val side = Side.fromRole(session.role)
        val updated = profileLock.withLock {
            transform(myProfile ?: PlayerProfile()).also { myProfile = it }
        }
        val patch = buildJsonObject {
            put(GamesKeys.profile(side), json.encodeToJsonElement(PlayerProfile.serializer(), updated))
        }
        return when (val saved = save(patch)) {
            is AppResult.Success -> AppResult.Success(updated)
            is AppResult.Failure -> saved
        }
    }

    override suspend fun setPresence(game: GameId?): AppResult<Unit> {
        val session = session() ?: return AppResult.Failure(AppError.Unauthorized())
        val presence = Presence(game = game?.key, ts = System.currentTimeMillis())
        val patch = buildJsonObject {
            put(
                GamesKeys.presence(Side.fromRole(session.role)),
                json.encodeToJsonElement(Presence.serializer(), presence),
            )
        }
        return save(patch)
    }

    private suspend fun save(patch: JsonObject): AppResult<Unit> {
        val session = session() ?: return AppResult.Failure(AppError.Unauthorized())
        val body = SaveStateBody(session.coupleId, patch, session.role, session.myName)
        return when (val result = safeApiCall { api.saveState(body) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> result
        }
    }

    override suspend fun uploadPhoto(uri: String): AppResult<String> = withContext(Dispatchers.IO) {
        val session = session() ?: return@withContext AppResult.Failure(AppError.Unauthorized())
        val parsed = Uri.parse(uri)
        val resolver = context.contentResolver
        val mime = resolver.getType(parsed) ?: DEFAULT_MIME
        val bytes = try {
            resolver.openInputStream(parsed)?.use { it.readBytes() }
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        }
        val type = mime.toMediaTypeOrNull()
        if (bytes == null || type == null) {
            AppResult.Failure(AppError.Unknown)
        } else {
            val ext = mime.substringAfter('/').let { if (it == "jpeg") "jpg" else it }
            val part = MultipartBody.Part.createFormData("file", "puzzle_${System.currentTimeMillis()}.$ext", bytes.toRequestBody(type))
            val couple = session.coupleId.toRequestBody("text/plain".toMediaTypeOrNull())
            when (val result = safeApiCall { api.upload(part, couple) }) {
                is AppResult.Success -> AppResult.Success(result.data.url)
                is AppResult.Failure -> result
            }
        }
    }

    // Datamuse is a public word service on another host, so it is called directly (not through the
    // app's own client) and the couple's sign-in token is never sent to it.
    override suspend fun relatedWords(word: String): Set<String> = withContext(Dispatchers.IO) {
        val key = TextMatch.tight(word)
        if (key.isEmpty()) return@withContext emptySet()
        synchronized(relatedCache) { relatedCache[key] }?.let { return@withContext it }
        val encoded = URLEncoder.encode(key, "UTF-8")
        val found = runCatching {
            fetchWords("https://api.datamuse.com/words?rel_syn=$encoded&max=25") +
                fetchWords("https://api.datamuse.com/words?ml=$encoded&max=15")
        }.getOrDefault(emptySet())
        // Only remember real answers, so a bad network is retried next time.
        if (found.isNotEmpty()) synchronized(relatedCache) { relatedCache[key] = found }
        found
    }

    private fun fetchWords(address: String): Set<String> {
        val connection = URL(address).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = WORD_TIMEOUT_MS
            connection.readTimeout = WORD_TIMEOUT_MS
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return emptySet()
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            (json.parseToJsonElement(text) as? JsonArray).orEmpty()
                .mapNotNull { (it as? JsonObject)?.get("word")?.jsonPrimitive?.contentOrNull }
                .map { TextMatch.tight(it) }
                .filter { it.isNotEmpty() }
                .toSet()
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val DEFAULT_MIME = "image/jpeg"
        const val WORD_TIMEOUT_MS = 4_000
    }
}
