package com.twinhearts.app.feature.memories.data

import android.net.Uri
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.data.StateCache
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.memories.data.remote.DeleteFileBody
import com.twinhearts.app.feature.memories.data.remote.MemoriesApi
import com.twinhearts.app.feature.memories.data.remote.MemoriesSaveBody
import com.twinhearts.app.feature.memories.domain.isUploadable
import com.twinhearts.app.feature.memories.domain.isVideoMime
import com.twinhearts.app.feature.memories.domain.model.Memory
import com.twinhearts.app.feature.memories.domain.model.UploadedMedia
import com.twinhearts.app.feature.memories.domain.newMemoryId
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random

/** What a delete did: the album now, and how many files could not be removed from storage. */
data class DeleteOutcome(val items: List<Memory>, val failed: Int)

interface MemoriesRepository {
    /** The last copy saved on this phone, or null. Used to paint at once and to work offline. */
    suspend fun cached(): List<Memory>?

    suspend fun load(): AppResult<List<Memory>>

    /** Sends one picked file to storage. A file type the server would refuse fails here, with a clear error. */
    suspend fun upload(uri: Uri): AppResult<UploadedMedia>

    /** Adds the uploaded files to the album in one save (web order: new ones go last). */
    suspend fun addToAlbum(media: List<UploadedMedia>): AppResult<List<Memory>>

    /** Removes the files from storage, then from the album. Items whose file cannot be removed stay. */
    suspend fun delete(ids: Set<String>): AppResult<DeleteOutcome>

    suspend fun toggleFavorite(id: String): AppResult<List<Memory>>
}

class MemoriesRepositoryImpl @Inject constructor(
    private val api: MemoriesApi,
    private val sessionStore: SessionStore,
    private val cache: StateCache,
    private val files: MemoryFiles,
) : MemoriesRepository {

    private val notSignedIn = AppResult.Failure(AppError.Unauthorized())

    override suspend fun cached(): List<Memory>? {
        val session = sessionStore.session.first() ?: return null
        val json = cache.read(cacheKey(session)) ?: return null
        return (runCatching { Json.parseToJsonElement(json) }.getOrNull() as? JsonArray)?.toMemories()
    }

    override suspend fun load(): AppResult<List<Memory>> {
        val session = sessionStore.session.first() ?: return notSignedIn
        return when (val state = readState(session)) {
            is AppResult.Success -> {
                val raw = state.data.rawPhotos()
                remember(session, raw)
                AppResult.Success(raw.toMemories())
            }
            is AppResult.Failure -> state
        }
    }

    override suspend fun upload(uri: Uri): AppResult<UploadedMedia> {
        val session = sessionStore.session.first() ?: return notSignedIn
        val file = files.describe(uri) ?: return AppResult.Failure(AppError.Unknown)
        // Same list the server accepts; refusing here gives a clear reason instead of a vague failure.
        if (!isUploadable(file.mime)) {
            return AppResult.Failure(AppError.Server(UNSUPPORTED, "That file type isn't supported — please choose a photo or video."))
        }
        val part = MultipartBody.Part.createFormData("file", file.name, files.body(file))
        val coupleId = session.coupleId.toString().toRequestBody("text/plain".toMediaType())
        return when (val r = safeApiCall { api.upload(part, coupleId) }) {
            is AppResult.Success -> AppResult.Success(UploadedMedia(r.data.url, r.data.path, file.name, isVideoMime(file.mime)))
            is AppResult.Failure -> r
        }
    }

    override suspend fun addToAlbum(media: List<UploadedMedia>): AppResult<List<Memory>> {
        if (media.isEmpty()) return AppResult.Success(cached().orEmpty())
        val session = sessionStore.session.first() ?: return notSignedIn
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return read
        }
        val now = System.currentTimeMillis()
        val today = LocalDate.now().toString()
        val added = media.map { m ->
            buildJsonObject {
                put("id", newMemoryId(now, Random::nextInt))
                put("url", m.url)
                put("name", m.name)
                put("type", if (m.isVideo) "video" else "image")
                put("date", today)
                put("path", m.path)
            }
        }
        return save(session, JsonArray(state.rawPhotos().toList() + added))
    }

    override suspend fun delete(ids: Set<String>): AppResult<DeleteOutcome> {
        val session = sessionStore.session.first() ?: return notSignedIn
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return read
        }
        val all = state.rawPhotos()
        val targets = all.filterIsInstance<JsonObject>().filter { rawId(it) in ids }
        // Files first (like the web): an item whose file could not be removed is kept, so nothing is orphaned.
        val removed = mutableSetOf<String>()
        var failed = 0
        for (target in targets) {
            val path = target["path"].text()
            val ok = path.isBlank() ||
                safeApiCall { api.deleteFile(DeleteFileBody(path, session.coupleId.toString())) } is AppResult.Success
            if (ok) removed += rawId(target) else failed++
        }
        val kept = JsonArray(all.filterNot { it is JsonObject && rawId(it) in removed })
        return when (val saved = save(session, kept)) {
            is AppResult.Success -> AppResult.Success(DeleteOutcome(saved.data, failed))
            is AppResult.Failure -> saved
        }
    }

    override suspend fun toggleFavorite(id: String): AppResult<List<Memory>> {
        val session = sessionStore.session.first() ?: return notSignedIn
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return read
        }
        // Copy the item so fields this app does not know about are kept.
        val next = JsonArray(
            state.rawPhotos().map { el ->
                val obj = el as? JsonObject
                if (obj != null && rawId(obj) == id) {
                    val now = (obj["favorite"] as? JsonPrimitive)?.booleanOrNull ?: false
                    JsonObject(obj + ("favorite" to JsonPrimitive(!now)))
                } else {
                    el
                }
            },
        )
        return save(session, next)
    }

    private suspend fun save(session: Session, photos: JsonArray): AppResult<List<Memory>> {
        val body = MemoriesSaveBody(session.coupleId, buildJsonObject { put("photos", photos) }, session.role, session.myName)
        return when (val result = safeApiCall { api.saveState(body) }) {
            is AppResult.Success -> {
                remember(session, photos)
                AppResult.Success(photos.toMemories())
            }
            is AppResult.Failure -> result
        }
    }

    private suspend fun readState(session: Session): AppResult<JsonObject> =
        when (val result = safeApiCall { api.getState(session.coupleId) }) {
            // The server answers the saved document itself (routes/data.js), or JSON null when there is
            // none yet. A { "state": {...} } wrapper is also accepted, so either shape reads the album.
            is AppResult.Success -> {
                val root = result.data as? JsonObject
                AppResult.Success((root?.get("state") as? JsonObject) ?: root ?: JsonObject(emptyMap()))
            }
            is AppResult.Failure -> result
        }

    private fun cacheKey(session: Session) = "memories:${session.coupleId}"

    /**
     * Saves the album for offline use. Very old items stored the picture itself as text (a "data:"
     * address); those are left out, and a list that is still too large is not saved at all, so one
     * big album can never break the local database.
     */
    private suspend fun remember(session: Session, photos: JsonArray) {
        val light = JsonArray(photos.filterNot { (it as? JsonObject)?.get("url").text().startsWith("data:") })
        val text = light.toString()
        if (text.length <= MAX_CACHE_CHARS) cache.write(cacheKey(session), text)
    }

    private fun JsonObject.rawPhotos(): JsonArray = this["photos"] as? JsonArray ?: JsonArray(emptyList())

    /** Same id rule as [toMemories], so a tap on a tile always finds its raw item. */
    private fun rawId(obj: JsonObject): String = obj["id"].text().ifBlank { obj["url"].text() }

    /** Skips items without a web address, so one bad item never hides the rest. */
    private fun JsonArray.toMemories(): List<Memory> = mapNotNull { item ->
        val obj = item as? JsonObject ?: return@mapNotNull null
        val url = obj["url"].text().takeIf { it.isNotBlank() } ?: return@mapNotNull null
        Memory(
            id = rawId(obj),
            url = url,
            name = obj["name"].text(),
            isVideo = obj["type"].text() == "video",
            date = runCatching { LocalDate.parse(obj["date"].text().take(ISO_DATE_LENGTH)) }.getOrNull(),
            path = obj["path"].text().takeIf { it.isNotBlank() },
            favorite = (obj["favorite"] as? JsonPrimitive)?.booleanOrNull ?: false,
        )
    }

    private fun JsonElement?.text(): String = (this as? JsonPrimitive)?.contentOrNull.orEmpty()

    private companion object {
        const val ISO_DATE_LENGTH = 10
        const val MAX_CACHE_CHARS = 1_000_000
        const val UNSUPPORTED = 415
    }
}
