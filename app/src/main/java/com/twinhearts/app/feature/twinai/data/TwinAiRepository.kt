package com.twinhearts.app.feature.twinai.data

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.map
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.twinai.data.mapper.toDomain
import com.twinhearts.app.feature.twinai.data.remote.AiChatBody
import com.twinhearts.app.feature.twinai.data.remote.CoupleBody
import com.twinhearts.app.feature.twinai.data.remote.NewSessionBody
import com.twinhearts.app.feature.twinai.data.remote.RoleBody
import com.twinhearts.app.feature.twinai.data.remote.SseChunkDto
import com.twinhearts.app.feature.twinai.data.remote.SubmitBody
import com.twinhearts.app.feature.twinai.data.remote.TwinAiApi
import com.twinhearts.app.feature.twinai.domain.model.AiIdentity
import com.twinhearts.app.feature.twinai.domain.model.AiStreamEvent
import com.twinhearts.app.feature.twinai.domain.model.BothDiscussion
import com.twinhearts.app.feature.twinai.domain.model.BothRound
import com.twinhearts.app.feature.twinai.domain.model.BothSession
import com.twinhearts.app.feature.twinai.domain.model.RoundStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject

/**
 * Why this file exists: the only place Twin AI talks to the old server. Every call returns an
 * [AppResult] (or a Flow of events) so the ViewModels never see an exception (R-46).
 */
interface TwinAiRepository {
    /** Who is talking to Twin; `null` when nobody is signed in. */
    suspend fun identity(): AiIdentity?

    /** Twin's answer to [question], piece by piece. Ends with [AiStreamEvent.Done] or [AiStreamEvent.Failure]. */
    fun reply(who: AiIdentity, question: String): Flow<AiStreamEvent>

    /** True when this couple has Pro (the web only lets Pro couples open BOTH). */
    suspend fun isPro(who: AiIdentity): AppResult<Boolean>
    suspend fun bothSessions(who: AiIdentity): AppResult<List<BothSession>>
    suspend fun startDiscussion(who: AiIdentity): AppResult<BothDiscussion>
    suspend fun discussion(who: AiIdentity, sessionId: String): AppResult<BothDiscussion>
    suspend fun round(who: AiIdentity, roundId: String): AppResult<BothRound>
    suspend fun submit(who: AiIdentity, roundId: String, text: String): AppResult<RoundStatus>
    suspend fun edit(who: AiIdentity, roundId: String, text: String): AppResult<Unit>
    /** `true` when the partner was really notified, `false` when they have no push set up. */
    suspend fun nudge(who: AiIdentity, roundId: String): AppResult<Boolean>
    suspend fun nextRound(sessionId: String): AppResult<BothRound>
    suspend fun retry(who: AiIdentity, roundId: String): AppResult<Unit>
    suspend fun delete(who: AiIdentity, sessionId: String): AppResult<Unit>
}

class TwinAiRepositoryImpl @Inject constructor(
    private val api: TwinAiApi,
    private val sessionStore: SessionStore,
    private val json: Json,
) : TwinAiRepository {

    override suspend fun identity(): AiIdentity? = sessionStore.session.first()?.let {
        AiIdentity(it.coupleId, it.role, it.myName, it.partnerName, it.anniversary)
    }

    override fun reply(who: AiIdentity, question: String): Flow<AiStreamEvent> = flow {
        // The web app sends the latest journal mood too; the journal is not in this app yet, so it is left empty.
        val body = AiChatBody(question, who.myName, who.partnerName, who.anniversary)
        var gotText = false
        var failure: AppError? = null

        when (val opened = safeApiCall { api.chatStream(body) }) {
            is AppResult.Failure -> failure = opened.error
            is AppResult.Success -> try {
                opened.data.use { response ->
                    val source = response.source()
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val line = source.readUtf8Line() ?: break
                        val payload = line.trim().takeIf { it.startsWith("data:") }?.removePrefix("data:")?.trim()
                            ?: continue
                        if (payload == "[DONE]") continue
                        val chunk = runCatching { json.decodeFromString<SseChunkDto>(payload) }.getOrNull() ?: continue
                        if (chunk.error != null) {
                            failure = AppError.Server(500, chunk.error)
                            break
                        }
                        if (!chunk.delta.isNullOrEmpty()) {
                            gotText = true
                            emit(AiStreamEvent.Delta(chunk.delta))
                        }
                    }
                }
            } catch (e: IOException) {
                failure = AppError.Network
            }
        }

        if (gotText) {
            // Text already reached the screen; a half answer is shown as failed so Retry is offered.
            val problem = failure
            emit(if (problem == null) AiStreamEvent.Done else AiStreamEvent.Failure(problem))
            return@flow
        }
        // Same as the web app: when the stream gives nothing, ask once for the whole answer.
        when (val whole = safeApiCall { api.chat(body) }) {
            is AppResult.Success -> {
                val text = whole.data.reply?.takeIf { it.isNotBlank() }
                if (text == null) {
                    emit(AiStreamEvent.Failure(failure ?: AppError.Unknown))
                } else {
                    emit(AiStreamEvent.Delta(text))
                    emit(AiStreamEvent.Done)
                }
            }
            is AppResult.Failure -> emit(AiStreamEvent.Failure(whole.error))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun isPro(who: AiIdentity) =
        safeApiCall { api.proStatus(who.coupleId) }.map { it.isPro }

    override suspend fun bothSessions(who: AiIdentity) =
        safeApiCall { api.sessions(who.coupleId) }.map { list -> list.map { it.toDomain() } }

    override suspend fun startDiscussion(who: AiIdentity) =
        safeApiCall { api.startSession(NewSessionBody(who.coupleId, who.role)) }
            .map { BothDiscussion(it.session.toDomain(), listOf(it.round.toDomain())) }

    override suspend fun discussion(who: AiIdentity, sessionId: String) =
        safeApiCall { api.session(sessionId, who.role) }.map { it.toDomain() }

    override suspend fun round(who: AiIdentity, roundId: String) =
        safeApiCall { api.round(roundId, who.role) }.map { it.toDomain() }

    override suspend fun submit(who: AiIdentity, roundId: String, text: String) =
        safeApiCall { api.submit(roundId, SubmitBody(who.coupleId, who.role, text)) }
            .map { RoundStatus.from(it.status) }

    override suspend fun edit(who: AiIdentity, roundId: String, text: String) =
        safeApiCall { api.edit(roundId, SubmitBody(who.coupleId, who.role, text)) }.map { }

    override suspend fun nudge(who: AiIdentity, roundId: String) =
        safeApiCall { api.nudge(roundId, RoleBody(who.coupleId, who.role)) }.map { it.sent }

    override suspend fun nextRound(sessionId: String) =
        safeApiCall { api.nextRound(sessionId) }.map { it.round.toDomain() }

    override suspend fun retry(who: AiIdentity, roundId: String) =
        safeApiCall { api.retry(roundId, CoupleBody(who.coupleId)) }.map { }

    override suspend fun delete(who: AiIdentity, sessionId: String) =
        safeApiCall { api.deleteSession(sessionId, CoupleBody(who.coupleId)) }.map { }
}
