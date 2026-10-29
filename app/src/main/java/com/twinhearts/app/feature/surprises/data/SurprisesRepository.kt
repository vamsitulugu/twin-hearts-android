package com.twinhearts.app.feature.surprises.data

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.surprises.data.mapper.KEY_CAPSULES
import com.twinhearts.app.feature.surprises.data.mapper.KEY_SURPRISES
import com.twinhearts.app.feature.surprises.data.mapper.KEY_XP
import com.twinhearts.app.feature.surprises.data.mapper.XP_CAPSULE
import com.twinhearts.app.feature.surprises.data.mapper.XP_SURPRISE
import com.twinhearts.app.feature.surprises.data.mapper.capsuleJson
import com.twinhearts.app.feature.surprises.data.mapper.idOrEmpty
import com.twinhearts.app.feature.surprises.data.mapper.rawList
import com.twinhearts.app.feature.surprises.data.mapper.surpriseJson
import com.twinhearts.app.feature.surprises.data.mapper.text
import com.twinhearts.app.feature.surprises.data.mapper.toSurprisesData
import com.twinhearts.app.feature.surprises.data.mapper.xp
import com.twinhearts.app.feature.surprises.data.remote.SaveStateBody
import com.twinhearts.app.feature.surprises.data.remote.SurprisesApi
import com.twinhearts.app.feature.surprises.domain.model.MediaKind
import com.twinhearts.app.feature.surprises.domain.model.NewCapsule
import com.twinhearts.app.feature.surprises.domain.model.NewSurprise
import com.twinhearts.app.feature.surprises.domain.model.SurprisesData
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import javax.inject.Inject

/** Every call returns the whole refreshed page, so the screen never has to merge changes itself. */
interface SurprisesRepository {
    suspend fun load(): AppResult<SurprisesData>
    suspend fun addSurprise(new: NewSurprise): AppResult<SurprisesData>
    suspend fun deleteSurprise(id: String): AppResult<SurprisesData>
    suspend fun addCapsule(new: NewCapsule): AppResult<SurprisesData>
    suspend fun openCapsule(id: String): AppResult<SurprisesData>
    suspend fun deleteCapsule(id: String): AppResult<SurprisesData>
}

/**
 * Why this file exists: reads and writes `surprises` / `capsules` in the couple's shared state.
 * The server replaces a whole top-level key on save, so each change reads the newest state first,
 * changes only its own items, and sends back only the keys it touched.
 *
 * New items are appended (not put first like the web does). The server's push-notification check
 * looks at items added at the END of a list, so this is the order that reliably tells the partner.
 * The screen sorts newest first anyway.
 */
class SurprisesRepositoryImpl @Inject constructor(
    private val api: SurprisesApi,
    private val uploader: MediaUploader,
    private val sessionStore: SessionStore,
) : SurprisesRepository {

    private val notSignedIn = AppResult.Failure(AppError.Unauthorized())

    override suspend fun load(): AppResult<SurprisesData> {
        val session = sessionStore.session.first() ?: return notSignedIn
        return when (val state = readState(session)) {
            is AppResult.Success -> AppResult.Success(state.data.toSurprisesData(session.role))
            is AppResult.Failure -> state
        }
    }

    override suspend fun addSurprise(new: NewSurprise): AppResult<SurprisesData> {
        val session = sessionStore.session.first() ?: return notSignedIn

        // Upload first: a failed upload must not leave a surprise with a missing file.
        val media = new.media
        val uploaded: UploadedMedia? = if (media == null) {
            null
        } else {
            when (val result = uploader.upload(media, session)) {
                is AppResult.Success -> result.data
                is AppResult.Failure -> return result
            }
        }
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> {
                discard(uploaded, session)
                return read
            }
        }
        val item = surpriseJson(
            id = System.currentTimeMillis(),
            emoji = new.kind.emoji,
            title = new.title,
            message = new.message,
            unlock = new.unlock,
            role = session.role,
            mediaUrl = uploaded?.url.orEmpty(),
            mediaKind = uploaded?.kind,
            mediaPath = uploaded?.path,
        )
        val patch = buildJsonObject {
            put(KEY_SURPRISES, JsonArray(state.rawList(KEY_SURPRISES) + item))
            put(KEY_XP, state.xp() + XP_SURPRISE)
        }
        return saveAndMap(session, state, patch) { discard(uploaded, session) }
    }

    override suspend fun deleteSurprise(id: String): AppResult<SurprisesData> {
        val session = sessionStore.session.first() ?: return notSignedIn
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return read
        }
        val all = state.rawList(KEY_SURPRISES)
        val removed = all.firstOrNull { it.idOrEmpty() == id } as? JsonObject
        val patch = buildJsonObject { put(KEY_SURPRISES, JsonArray(all.filterNot { it.idOrEmpty() == id })) }
        val result = saveAndMap(session, state, patch) {}
        if (result is AppResult.Success && removed != null) {
            val path = removed["mediaPath"].text()
            val kind = MediaKind.entries.firstOrNull { it.key == removed["mediaKind"].text() }
            if (path.isNotBlank() && kind != null) uploader.discard(path, kind, session.coupleId)
        }
        return result
    }

    override suspend fun addCapsule(new: NewCapsule): AppResult<SurprisesData> {
        val session = sessionStore.session.first() ?: return notSignedIn
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return read
        }
        val item = capsuleJson(System.currentTimeMillis(), new.message, new.unlock, session.role, LocalDate.now())
        val patch = buildJsonObject {
            put(KEY_CAPSULES, JsonArray(state.rawList(KEY_CAPSULES) + item))
            put(KEY_XP, state.xp() + XP_CAPSULE)
        }
        return saveAndMap(session, state, patch) {}
    }

    override suspend fun openCapsule(id: String): AppResult<SurprisesData> {
        val session = sessionStore.session.first() ?: return notSignedIn
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return read
        }
        // Only the `opened` flag changes; every other field of the item is kept as it was.
        val updated = state.rawList(KEY_CAPSULES).map { item ->
            if (item.idOrEmpty() == id && item is JsonObject) JsonObject(item + ("opened" to JsonPrimitive(true))) else item
        }
        val patch = buildJsonObject { put(KEY_CAPSULES, JsonArray(updated)) }
        return saveAndMap(session, state, patch) {}
    }

    override suspend fun deleteCapsule(id: String): AppResult<SurprisesData> {
        val session = sessionStore.session.first() ?: return notSignedIn
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return read
        }
        val kept = state.rawList(KEY_CAPSULES).filterNot { it.idOrEmpty() == id }
        val patch = buildJsonObject { put(KEY_CAPSULES, JsonArray(kept)) }
        return saveAndMap(session, state, patch) {}
    }

    private suspend fun readState(session: Session): AppResult<JsonObject> =
        when (val result = safeApiCall { api.getState(session.coupleId) }) {
            is AppResult.Success -> AppResult.Success(result.data as? JsonObject ?: JsonObject(emptyMap()))
            is AppResult.Failure -> result
        }

    /** Saves [patch]; on success returns the page as it now is, on failure runs [onFailure] (e.g. removes an upload). */
    private suspend fun saveAndMap(
        session: Session,
        state: JsonObject,
        patch: JsonObject,
        onFailure: suspend () -> Unit,
    ): AppResult<SurprisesData> {
        val body = SaveStateBody(session.coupleId, patch, session.role, session.myName)
        return when (val saved = safeApiCall { api.saveState(body) }) {
            is AppResult.Success -> AppResult.Success(JsonObject(state + patch).toSurprisesData(session.role))
            is AppResult.Failure -> {
                onFailure()
                saved
            }
        }
    }

    private suspend fun discard(uploaded: UploadedMedia?, session: Session) {
        val path = uploaded?.path ?: return
        uploader.discard(path, uploaded.kind, session.coupleId)
    }
}
