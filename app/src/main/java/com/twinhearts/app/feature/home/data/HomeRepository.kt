package com.twinhearts.app.feature.home.data

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.home.data.mapper.asLong
import com.twinhearts.app.feature.home.data.mapper.asText
import com.twinhearts.app.feature.home.data.mapper.profileKey
import com.twinhearts.app.feature.home.data.mapper.toHomeOverview
import com.twinhearts.app.feature.home.data.remote.DeleteMediaBody
import com.twinhearts.app.feature.home.data.remote.HomeApi
import com.twinhearts.app.feature.home.data.remote.SaveStateBody
import com.twinhearts.app.feature.home.domain.model.CardPhotoSlot
import com.twinhearts.app.feature.home.domain.model.HomeOverview
import com.twinhearts.app.feature.home.domain.model.LoveKind
import com.twinhearts.app.feature.home.domain.model.MoodKind
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.time.LocalDate
import javax.inject.Inject

/**
 * Reads and writes what Home shows: the Stay Connected signals, "Coming up" and the moods.
 * The couple and role come from the saved Session.
 */
interface HomeRepository {
    /** One read of the shared state gives the love card, the upcoming items and both moods. */
    suspend fun overview(): AppResult<HomeOverview>
    suspend fun send(kind: LoveKind): AppResult<Unit>

    /** Saves MY mood for [date] (ISO "2026-10-26"); `null` clears it. Never touches my partner's. */
    suspend fun setMood(date: LocalDate, mood: MoodKind?): AppResult<Unit>

    /** Accepts the hug my partner sent. */
    suspend fun acceptHug(): AppResult<Unit>

    /** Uploads [jpeg] as the couple's own picture for [slot] and saves it in the shared state. */
    suspend fun saveCardPhoto(slot: CardPhotoSlot, jpeg: ByteArray): AppResult<Unit>

    /** Goes back to the built-in picture for [slot]. */
    suspend fun resetCardPhoto(slot: CardPhotoSlot): AppResult<Unit>
}

class HomeRepositoryImpl @Inject constructor(
    private val api: HomeApi,
    private val sessionStore: SessionStore,
) : HomeRepository {

    private suspend fun session(): Session? = sessionStore.session.first()
    private val notSignedIn = AppResult.Failure(AppError.Unauthorized())

    override suspend fun overview(): AppResult<HomeOverview> {
        val session = session() ?: return notSignedIn
        return when (val result = safeApiCall { api.getState(session.coupleId) }) {
            is AppResult.Success ->
                AppResult.Success(result.data.toHomeOverview(session.role, session.anniversary, LocalDate.now()))
            is AppResult.Failure -> result
        }
    }

    /**
     * The server replaces `profile.u1` as a whole (it only protects the OTHER person's entry), so we
     * send my full entry back with just `moods` changed. Like the web, we also keep the older
     * Wellness field of that day in step so both pages agree.
     */
    override suspend fun setMood(date: LocalDate, mood: MoodKind?): AppResult<Unit> {
        val session = session() ?: return notSignedIn
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return read
        }
        val day = date.toString()
        val myKey = profileKey(session.role)
        val profile = state["profile"] as? JsonObject
        val entry = profile?.get(myKey) as? JsonObject
        val moods = (entry?.get("moods") as? JsonObject)?.toMutableMap() ?: mutableMapOf()
        if (mood != null) moods[day] = JsonPrimitive(mood.label) else moods.remove(day)
        val newEntry = JsonObject((entry ?: JsonObject(emptyMap())) + ("moods" to JsonObject(moods)))

        val wellness = (state["wellness"] as? JsonObject)?.toMutableMap() ?: mutableMapOf()
        val wellnessDay = (wellness[day] as? JsonObject)?.toMutableMap() ?: mutableMapOf()
        if (mood != null) wellnessDay["mood"] = JsonPrimitive("${mood.emoji} ${mood.label}") else wellnessDay.remove("mood")
        wellness[day] = JsonObject(wellnessDay)

        val patch = buildJsonObject {
            putJsonObject("profile") { put(myKey, newEntry) }
            put("wellness", JsonObject(wellness))
            if (mood != null && date == LocalDate.now()) put("selectedMood", "${mood.emoji} ${mood.label}")
        }
        return save(session, patch)
    }

    override suspend fun send(kind: LoveKind): AppResult<Unit> {
        val session = session() ?: return notSignedIn
        val now = System.currentTimeMillis()
        val patch: JsonObject = when (kind) {
            LoveKind.Touch -> buildJsonObject {
                putJsonObject("touch") { put("from", session.role); put("ts", now) }
            }
            LoveKind.Hug -> buildJsonObject {
                putJsonObject("hug") {
                    put("id", now); put("from", session.role); put("ts", now); put("status", "pending")
                }
            }
            LoveKind.MissYou -> when (val read = readState(session)) {
                is AppResult.Success -> missYouPatch(read.data, session, now)
                is AppResult.Failure -> return read
            }
        }
        return save(session, patch)
    }

    override suspend fun acceptHug(): AppResult<Unit> {
        val session = session() ?: return notSignedIn
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return read
        }
        val hug = state["hug"] as? JsonObject
        val from = hug?.get("from").asText()
        val id = hug?.get("id").asLong()
        if (hug == null || from == null || id == null || hug["status"].asText() != "pending") {
            return AppResult.Failure(AppError.Unknown) // nothing to accept (already handled elsewhere)
        }
        val now = System.currentTimeMillis()
        val stats = state["hugStats"] as? JsonObject
        val user1 = (stats?.get("user1").asLong() ?: 0L) + if (from == "user1") 1 else 0
        val user2 = (stats?.get("user2").asLong() ?: 0L) + if (from == "user2") 1 else 0
        val total = (stats?.get("total").asLong() ?: 0L) + 1
        val patch = buildJsonObject {
            putJsonObject("hug") {
                put("id", id); put("from", from); put("ts", hug["ts"].asLong() ?: id)
                put("status", "accepted"); put("acceptedTs", now); put("acceptedBy", session.role)
            }
            putJsonObject("hugStats") { put("total", total); put("user1", user1); put("user2", user2) }
        }
        return save(session, patch)
    }

    /**
     * Same order as the web: upload, save `dashImgs`, then delete the old file (best effort).
     * The server merges only top-level keys, so `dashImgs` is sent as a whole object.
     */
    override suspend fun saveCardPhoto(slot: CardPhotoSlot, jpeg: ByteArray): AppResult<Unit> {
        val session = session() ?: return notSignedIn
        val filePart = MultipartBody.Part.createFormData(
            "file", "dashboard-${slot.key}.jpg", jpeg.toRequestBody("image/jpeg".toMediaType()),
        )
        val coupleId = session.coupleId.toRequestBody("text/plain".toMediaType())
        val uploaded = when (val result = safeApiCall { api.uploadMedia(filePart, coupleId) }) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> return result
        }
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return read
        }
        val images = (state["dashImgs"] as? JsonObject).orEmpty()
        val oldPath = (images[slot.key] as? JsonObject)?.get("path").asText().orEmpty()
        val entry = buildJsonObject { put("url", uploaded.url); put("path", uploaded.path.orEmpty()) }
        val saved = save(session, buildJsonObject { put("dashImgs", JsonObject(images + (slot.key to entry))) })
        if (saved is AppResult.Success) deleteOldPhoto(session, oldPath)
        return saved
    }

    override suspend fun resetCardPhoto(slot: CardPhotoSlot): AppResult<Unit> {
        val session = session() ?: return notSignedIn
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return read
        }
        val images = (state["dashImgs"] as? JsonObject).orEmpty()
        val old = images[slot.key] as? JsonObject ?: return AppResult.Success(Unit) // already default
        val saved = save(session, buildJsonObject { put("dashImgs", JsonObject(images - slot.key)) })
        if (saved is AppResult.Success) deleteOldPhoto(session, old["path"].asText().orEmpty())
        return saved
    }

    /** A failed clean-up only leaves an unused file in storage, so its result is ignored. */
    private suspend fun deleteOldPhoto(session: Session, path: String) {
        if (path.isBlank()) return
        safeApiCall { api.deleteMedia(DeleteMediaBody(path, session.coupleId)) }
    }

    private suspend fun readState(session: Session): AppResult<JsonObject> =
        when (val result = safeApiCall<JsonElement> { api.getState(session.coupleId) }) {
            is AppResult.Success -> AppResult.Success(result.data as? JsonObject ?: JsonObject(emptyMap()))
            is AppResult.Failure -> result
        }

    /** The web also counts how often each of you said "Miss You" (`missCounts`). */
    private fun missYouPatch(state: JsonObject, session: Session, now: Long): JsonObject {
        val counts = state["missCounts"] as? JsonObject
        var user1 = counts?.get("user1").asLong() ?: 0L
        var user2 = counts?.get("user2").asLong() ?: 0L
        if (session.role == "user2") user2++ else user1++
        return buildJsonObject {
            putJsonObject("missYou") {
                put("id", now); put("from", session.role); put("ts", now); put("status", "sent")
            }
            putJsonObject("missCounts") { put("user1", user1); put("user2", user2) }
        }
    }

    private suspend fun save(session: Session, patch: JsonObject): AppResult<Unit> {
        val body = SaveStateBody(session.coupleId, patch, session.role, session.myName)
        return when (val result = safeApiCall { api.saveState(body) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> result
        }
    }
}
