package com.twinhearts.app.feature.profile.highlights.data

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.map
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.profile.highlights.data.mapper.toHighlights
import com.twinhearts.app.feature.profile.highlights.data.remote.HighlightsApi
import com.twinhearts.app.feature.profile.highlights.data.remote.OwnerBody
import com.twinhearts.app.feature.profile.highlights.data.remote.UpdateHighlightBody
import com.twinhearts.app.feature.profile.highlights.domain.HighlightCategory
import com.twinhearts.app.feature.profile.highlights.domain.HighlightsSnapshot
import com.twinhearts.app.feature.profile.highlights.domain.PickedClip
import com.twinhearts.app.feature.profile.highlights.domain.UploadTarget
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/** Everything the Highlights row needs from the network and the phone (R-40). */
interface HighlightsRepository {
    suspend fun load(): AppResult<HighlightsSnapshot>

    /** Reads a chosen file's name, size and type. Null when it cannot be opened. */
    suspend fun describe(uri: String): PickedClip?

    /** Sends one clip. Returns the id of the highlight it landed in (new or existing). */
    suspend fun uploadClip(clip: PickedClip, target: UploadTarget, onProgress: (Float) -> Unit): AppResult<String>

    suspend fun update(highlightId: String, title: String, category: HighlightCategory): AppResult<Unit>

    /** Returns true when that was the last clip, so the whole highlight is gone. */
    suspend fun deleteClip(clipId: String): AppResult<Boolean>

    suspend fun deleteHighlight(highlightId: String): AppResult<Unit>
}

@Singleton
class HighlightsRepositoryImpl @Inject constructor(
    private val api: HighlightsApi,
    private val sessionStore: SessionStore,
    private val files: ClipFiles,
) : HighlightsRepository {

    private suspend fun <T> withSession(block: suspend (Session) -> AppResult<T>): AppResult<T> {
        val session = sessionStore.session.first() ?: return AppResult.Failure(AppError.Unauthorized())
        return block(session)
    }

    override suspend fun load(): AppResult<HighlightsSnapshot> = withSession { session ->
        safeApiCall { api.list(session.coupleId) }.map {
            HighlightsSnapshot(it.toHighlights(session.role), session.myName, session.partnerName)
        }
    }

    override suspend fun describe(uri: String): PickedClip? = files.describe(uri)

    override suspend fun uploadClip(clip: PickedClip, target: UploadTarget, onProgress: (Float) -> Unit): AppResult<String> =
        withSession { session ->
            val part = MultipartBody.Part.createFormData("file", clip.name, files.body(clip, onProgress))
            val duration = files.durationSeconds(clip).toString().toTextBody()
            val existing = (target as? UploadTarget.Existing)?.highlightId
            val fresh = target as? UploadTarget.NewHighlight
            safeApiCall {
                api.upload(
                    file = part,
                    coupleId = session.coupleId.toTextBody(),
                    userId = session.role.toTextBody(),
                    durationSec = duration,
                    highlightId = existing?.toTextBody(),
                    title = fresh?.title?.toTextBody(),
                    category = fresh?.category?.wire?.toTextBody(),
                )
            }.map { it.highlight.id }
        }

    override suspend fun update(highlightId: String, title: String, category: HighlightCategory): AppResult<Unit> =
        withSession { session ->
            safeApiCall { api.update(highlightId, UpdateHighlightBody(session.coupleId, session.role, title, category.wire)) }.map { }
        }

    override suspend fun deleteClip(clipId: String): AppResult<Boolean> = withSession { session ->
        safeApiCall { api.deleteClip(clipId, OwnerBody(session.coupleId, session.role)) }.map { it.highlightDeleted == true }
    }

    override suspend fun deleteHighlight(highlightId: String): AppResult<Unit> = withSession { session ->
        safeApiCall { api.deleteHighlight(highlightId, OwnerBody(session.coupleId, session.role)) }.map { }
    }

    private fun String.toTextBody(): RequestBody = toRequestBody("text/plain".toMediaType())
}
