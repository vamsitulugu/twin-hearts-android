package com.twinhearts.app.feature.collection.data

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.collection.data.remote.CollectionApi
import com.twinhearts.app.feature.collection.data.remote.CollectionSaveBody
import com.twinhearts.app.feature.collection.data.remote.CollectionStateResponse
import com.twinhearts.app.feature.collection.data.remote.DeleteRequest
import com.twinhearts.app.feature.collection.data.remote.StarRequest
import com.twinhearts.app.feature.collection.domain.model.CollectionData
import com.twinhearts.app.feature.collection.domain.model.VoiceItem
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject

interface CollectionRepository {
    suspend fun load(): AppResult<CollectionData>
    /** Toggles the shared star on a chat voice message (the same star Chat shows). */
    suspend fun toggleVoiceStar(id: String): AppResult<Unit>
    suspend fun deleteVoice(id: String): AppResult<Unit>
    suspend fun saveToLibrary(items: List<VoiceItem>): AppResult<Unit>
    suspend fun removeFromLibrary(ids: Set<String>): AppResult<Unit>
    fun photoFavorites(): Set<String>
    fun libraryFavorites(): Set<String>
    fun setPhotoFavorites(ids: Set<String>)
    fun setLibraryFavorites(ids: Set<String>)
}

class CollectionRepositoryImpl @Inject constructor(
    private val api: CollectionApi,
    private val sessionStore: SessionStore,
    private val prefs: CollectionPrefs,
) : CollectionRepository {

    private suspend fun session(): Session? = sessionStore.session.first()
    private val notSignedIn = AppResult.Failure(AppError.Unauthorized())

    override suspend fun load(): AppResult<CollectionData> {
        val session = session() ?: return notSignedIn
        val chat = when (val r = safeApiCall<JsonElement> { api.chat(session.coupleId) }) {
            is AppResult.Success -> r.data
            is AppResult.Failure -> return r
        }
        // The state document may be missing for a new couple; the chat part still works then.
        val state = when (val r = safeApiCall<CollectionStateResponse> { api.getState(session.coupleId) }) {
            is AppResult.Success -> r.data.state as? JsonObject ?: JsonObject(emptyMap())
            is AppResult.Failure -> JsonObject(emptyMap())
        }
        val rows = CollectionMapper.chat(chat, session.role)
        val albumPhotos = CollectionMapper.statePhotos(state)
        val knownAlbumIds = albumPhotos.map { it.id }.toSet()
        val photos = (albumPhotos + rows.photos.filter { it.id !in knownAlbumIds }).sortedByDescending { it.date }
        return AppResult.Success(
            CollectionData(
                voices = rows.voices,
                library = CollectionMapper.stateLibrary(state),
                photos = photos,
                role = session.role,
                myName = session.myName.ifBlank { "You" },
                partnerName = session.partnerName.ifBlank { "Partner" },
            ),
        )
    }

    override suspend fun toggleVoiceStar(id: String): AppResult<Unit> {
        val session = session() ?: return notSignedIn
        val number = id.toLongOrNull() ?: return AppResult.Failure(AppError.Unknown)
        return when (val r = safeApiCall<JsonElement> { api.toggleStar(number, StarRequest(session.coupleId, session.role)) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> r
        }
    }

    override suspend fun deleteVoice(id: String): AppResult<Unit> {
        val session = session() ?: return notSignedIn
        val number = id.toLongOrNull() ?: return AppResult.Failure(AppError.Unknown)
        // "me" hides it for this person only, as the web app's delete does for a received message.
        return when (val r = safeApiCall { api.deleteMessage(number, DeleteRequest(session.coupleId, session.role, "me")) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> r
        }
    }

    override suspend fun saveToLibrary(items: List<VoiceItem>): AppResult<Unit> {
        val session = session() ?: return notSignedIn
        val current = (load() as? AppResult.Success)?.data?.library.orEmpty()
        val merged = current + items.filter { n -> current.none { it.id == n.id } }
        return writeLibrary(session, merged)
    }

    override suspend fun removeFromLibrary(ids: Set<String>): AppResult<Unit> {
        val session = session() ?: return notSignedIn
        val current = (load() as? AppResult.Success)?.data?.library.orEmpty()
        return writeLibrary(session, current.filter { it.id !in ids })
    }

    private suspend fun writeLibrary(session: Session, items: List<VoiceItem>): AppResult<Unit> {
        val patch = JsonObject(mapOf("voiceLibrary" to CollectionMapper.libraryToJson(items)))
        val body = CollectionSaveBody(session.coupleId, patch, session.role, session.myName)
        return when (val r = safeApiCall { api.saveState(body) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> r
        }
    }

    override fun photoFavorites() = prefs.photoFavorites()
    override fun libraryFavorites() = prefs.libraryFavorites()
    override fun setPhotoFavorites(ids: Set<String>) = prefs.setPhotoFavorites(ids)
    override fun setLibraryFavorites(ids: Set<String>) = prefs.setLibraryFavorites(ids)
}
