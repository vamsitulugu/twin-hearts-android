package com.twinhearts.app.feature.games.data

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.games.data.mapper.GameChatKeys
import com.twinhearts.app.feature.games.data.mapper.asStateObject
import com.twinhearts.app.feature.games.data.mapper.chatMessagesOf
import com.twinhearts.app.feature.games.data.mapper.toChatBox
import com.twinhearts.app.feature.games.data.remote.GamesApi
import com.twinhearts.app.feature.games.data.remote.SaveStateBody
import com.twinhearts.app.feature.games.domain.engine.GameChatRules
import com.twinhearts.app.feature.games.domain.model.GameChatIdentity
import com.twinhearts.app.feature.games.domain.model.GameChatMessage
import com.twinhearts.app.feature.games.domain.model.GameChatPull
import com.twinhearts.app.feature.games.domain.model.Side
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Why this file exists: the only door from the in-game chat to the server. Like the rest of Games it
 * has no routes of its own: messages live in the couple's shared state under two keys (one per
 * player, so the phones never overwrite each other) and voice messages use the existing
 * `/api/media/upload-voice` route. Every answer is an [AppResult] (R-46).
 */
interface GameChatRepository {
    /** `null` when nobody is signed in or not paired yet (the chat then says "connect with your partner"). */
    suspend fun identity(): GameChatIdentity?

    /** My partner's messages, and (when [includeMine]) my own saved ones too. */
    suspend fun pull(who: GameChatIdentity, includeMine: Boolean): AppResult<GameChatPull>

    /** Saves my last messages under my own key. */
    suspend fun saveMine(who: GameChatIdentity, messages: List<GameChatMessage>): AppResult<Unit>

    /** Uploads a recorded voice file and returns its web address. */
    suspend fun uploadVoice(who: GameChatIdentity, path: String): AppResult<String>
}

@Singleton
class GameChatRepositoryImpl @Inject constructor(
    private val api: GamesApi,
    private val sessionStore: SessionStore,
) : GameChatRepository {

    override suspend fun identity(): GameChatIdentity? =
        sessionStore.session.first()
            ?.takeIf { it.coupleId.isNotBlank() && it.paired }
            ?.let { GameChatIdentity(Side.fromRole(it.role), it.coupleId, it.role, it.myName, it.partnerName) }

    override suspend fun pull(who: GameChatIdentity, includeMine: Boolean): AppResult<GameChatPull> {
        val keys = buildList {
            if (includeMine) add(GameChatKeys.key(who.side))
            add(GameChatKeys.key(who.side.other))
        }.joinToString(",")
        // Small read first. An older server without that route answers 404: read the whole state then.
        val small = safeApiCall { api.getGames(who.coupleId, keys) }
        val read = if (small is AppResult.Success) small else safeApiCall { api.getState(who.coupleId) }
        return when (read) {
            is AppResult.Failure -> read
            is AppResult.Success -> {
                val state = read.data.asStateObject()
                AppResult.Success(
                    GameChatPull(
                        mine = if (includeMine) state.chatMessagesOf(who.side, mine = true) else emptyList(),
                        theirs = state.chatMessagesOf(who.side.other, mine = false),
                    ),
                )
            }
        }
    }

    override suspend fun saveMine(who: GameChatIdentity, messages: List<GameChatMessage>): AppResult<Unit> {
        val patch = buildJsonObject {
            put(GameChatKeys.key(who.side), GameChatRules.toSave(messages).toChatBox(System.currentTimeMillis()))
        }
        return when (val r = safeApiCall { api.saveState(SaveStateBody(who.coupleId, patch, who.role, who.myName)) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> r
        }
    }

    override suspend fun uploadVoice(who: GameChatIdentity, path: String): AppResult<String> = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists() || file.length() == 0L) return@withContext AppResult.Failure(AppError.Unknown)
        val type = "audio/mp4".toMediaTypeOrNull()
        val part = MultipartBody.Part.createFormData("file", "voice.m4a", file.asRequestBody(type))
        val couple = who.coupleId.toRequestBody("text/plain".toMediaTypeOrNull())
        val role = who.role.toRequestBody("text/plain".toMediaTypeOrNull())
        when (val r = safeApiCall { api.uploadVoice(part, couple, role) }) {
            is AppResult.Success -> AppResult.Success(r.data.url)
            is AppResult.Failure -> r
        }
    }
}
