package com.twinhearts.app.feature.profile.data

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.map
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.profile.data.mapper.parseIsoMs
import com.twinhearts.app.feature.profile.data.mapper.profileEntry
import com.twinhearts.app.feature.profile.data.mapper.profileKey
import com.twinhearts.app.feature.profile.data.mapper.toEventJson
import com.twinhearts.app.feature.profile.data.mapper.toJson
import com.twinhearts.app.feature.profile.data.mapper.toProfileData
import com.twinhearts.app.feature.profile.data.mapper.toReminderJson
import com.twinhearts.app.feature.profile.data.remote.ProfileApi
import com.twinhearts.app.feature.profile.data.remote.SaveStateBody
import com.twinhearts.app.feature.profile.data.remote.SendRequestBody
import com.twinhearts.app.feature.profile.data.remote.UnpairBody
import com.twinhearts.app.feature.profile.domain.model.ConnectionState
import com.twinhearts.app.feature.profile.domain.model.LoveLanguage
import com.twinhearts.app.feature.profile.domain.model.MyProfileEdit
import com.twinhearts.app.feature.profile.domain.model.NewMilestone
import com.twinhearts.app.feature.profile.domain.model.NewSpecialDate
import com.twinhearts.app.feature.profile.domain.model.PartnerEdit
import com.twinhearts.app.feature.profile.domain.model.ProfileData
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** Everything the Profile screens need from the network (R-40). */
interface ProfileRepository {
    suspend fun load(): AppResult<ProfileData>
    suspend fun saveMyProfile(edit: MyProfileEdit): AppResult<Unit>
    suspend fun savePartnerDetails(edit: PartnerEdit): AppResult<Unit>
    suspend fun saveStory(myBio: String, partnerBio: String): AppResult<Unit>
    suspend fun saveLoveLanguages(mine: LoveLanguage?, partner: LoveLanguage?): AppResult<Unit>
    suspend fun addDate(date: NewSpecialDate): AppResult<Unit>
    suspend fun addMilestone(milestone: NewMilestone): AppResult<Unit>
    suspend fun deleteMilestone(id: Long): AppResult<Unit>

    /** Uploads the picture and returns its new address. */
    suspend fun uploadAvatar(jpeg: ByteArray): AppResult<String>
    suspend fun sendPartnerRequest(phone: String): AppResult<Unit>
    suspend fun removePartner(): AppResult<Unit>

    /** The whole saved state as pretty JSON, for "Export My Data". */
    suspend fun exportJson(): AppResult<String>
}

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val api: ProfileApi,
    private val sessionStore: SessionStore,
) : ProfileRepository {

    private val prettyJson = Json { prettyPrint = true }

    private suspend fun <T> withSession(block: suspend (Session) -> AppResult<T>): AppResult<T> {
        val session = sessionStore.session.first() ?: return AppResult.Failure(AppError.Unauthorized())
        return block(session)
    }

    private suspend fun readState(session: Session): AppResult<JsonObject> =
        safeApiCall { api.getState(session.coupleId) }.map { it.state as? JsonObject ?: JsonObject(emptyMap()) }

    /** Reads the newest state, builds a patch from it, saves it. Reading first keeps the partner's newest data. */
    private suspend fun patchState(session: Session, build: (JsonObject) -> JsonObject): AppResult<Unit> {
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return read
        }
        return safeApiCall {
            api.saveState(SaveStateBody(session.coupleId, build(state), session.role, session.myName))
        }.map { }
    }

    override suspend fun load(): AppResult<ProfileData> = withSession { session ->
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return@withSession read
        }
        // The extras are best effort: a failure only means "no photo" or "not seen", never a blank page.
        coroutineScope {
            val mine = async { safeApiCall { api.getProfile(session.userId) } }
            val theirs = async { safeApiCall { api.getPartnerProfile(session.userId) } }
            val status = async { safeApiCall { api.partnerStatus(session.userId) } }
            val locations = async { safeApiCall { api.getLocations(session.coupleId) } }

            val connection = when (val r = status.await()) {
                is AppResult.Success -> when (r.data.status) {
                    "connected" -> ConnectionState.Connected
                    "waiting" -> ConnectionState.Waiting
                    else -> ConnectionState.NotConnected
                }
                is AppResult.Failure -> if (session.paired) ConnectionState.Connected else ConnectionState.NotConnected
            }
            val seen = (locations.await() as? AppResult.Success)?.data?.let {
                parseIsoMs((if (session.role == "user2") it.user1 else it.user2)?.updatedAt)
            }
            AppResult.Success(
                state.toProfileData(
                    role = session.role,
                    sessionName = session.myName,
                    sessionPartnerName = session.partnerName,
                    sessionAnniversary = session.anniversary,
                    sessionGender = session.gender,
                    connection = connection,
                    myAvatarUrl = (mine.await() as? AppResult.Success)?.data?.avatarUrl,
                    partnerAvatarUrl = (theirs.await() as? AppResult.Success)?.data?.profile?.avatarUrl,
                    partnerSeenAtMs = seen,
                ),
            )
        }
    }

    override suspend fun saveMyProfile(edit: MyProfileEdit): AppResult<Unit> = withSession { session ->
        val name = edit.name.trim().ifBlank { DEFAULT_NAME }
        val result = patchState(session) { state ->
            val key = profileKey(session.role)
            // The server replaces `profile.<me>` as a whole, so we send my full entry with the changes.
            val entry = state.profileEntry(key).toMutableMap().apply {
                put("name", JsonPrimitive(name))
                put("bday", JsonPrimitive(edit.birthday?.toString().orEmpty()))
                if (edit.gender != null) put("gender", JsonPrimitive(edit.gender.wire))
            }
            buildJsonObject {
                put("myName", name)
                put("myBday", edit.birthday?.toString().orEmpty())
                put("anniversary", edit.anniversary?.toString().orEmpty())
                put("myBio", edit.bio.trim())
                putJsonObject("profile") { put(key, JsonObject(entry)) }
            }
        }
        if (result is AppResult.Success) {
            sessionStore.save(
                session.copy(
                    myName = name,
                    anniversary = edit.anniversary?.toString().orEmpty(),
                    gender = edit.gender?.wire ?: session.gender,
                ),
            )
        }
        result
    }

    override suspend fun savePartnerDetails(edit: PartnerEdit): AppResult<Unit> = withSession { session ->
        val result = patchState(session) {
            buildJsonObject {
                put("partnerBio", edit.bio.trim())
                if (edit.includeIdentity) {
                    put("partnerName", edit.name.trim().ifBlank { DEFAULT_PARTNER })
                    put("ptBday", edit.birthday?.toString().orEmpty())
                }
            }
        }
        if (result is AppResult.Success && edit.includeIdentity) {
            sessionStore.save(session.copy(partnerName = edit.name.trim().ifBlank { DEFAULT_PARTNER }))
        }
        result
    }

    override suspend fun saveStory(myBio: String, partnerBio: String): AppResult<Unit> = withSession { session ->
        patchState(session) {
            buildJsonObject {
                put("myBio", myBio.trim())
                put("partnerBio", partnerBio.trim())
            }
        }
    }

    override suspend fun saveLoveLanguages(mine: LoveLanguage?, partner: LoveLanguage?): AppResult<Unit> = withSession { session ->
        patchState(session) {
            buildJsonObject {
                put("myLoveLang", mine?.wire.orEmpty())
                put("ptLoveLang", partner?.wire.orEmpty())
            }
        }
    }

    override suspend fun addDate(date: NewSpecialDate): AppResult<Unit> = withSession { session ->
        patchState(session) { state ->
            val now = System.currentTimeMillis()
            val events = (state["events"] as? JsonArray)?.toList().orEmpty() + date.toEventJson(now)
            // Same as the web: an alarm needs a time, and it also files a reminder.
            val addReminder = date.alarm.key.isNotEmpty() && date.time.isNotBlank()
            buildJsonObject {
                put("events", JsonArray(events))
                if (addReminder) {
                    val reminders = (state["reminders"] as? JsonArray)?.toList().orEmpty()
                    put("reminders", JsonArray(reminders + date.toReminderJson(now + 1)))
                }
            }
        }
    }

    override suspend fun addMilestone(milestone: NewMilestone): AppResult<Unit> = withSession { session ->
        patchState(session) { state ->
            val list = (state["milestones"] as? JsonArray)?.toList().orEmpty() + milestone.toJson(System.currentTimeMillis())
            buildJsonObject { put("milestones", JsonArray(list)) }
        }
    }

    override suspend fun deleteMilestone(id: Long): AppResult<Unit> = withSession { session ->
        patchState(session) { state ->
            // Work on the raw items so fields this app does not know about are kept.
            val kept = (state["milestones"] as? JsonArray).orEmpty().filterNot { item ->
                ((item as? JsonObject)?.get("id") as? JsonPrimitive)?.longOrNull == id
            }
            buildJsonObject { put("milestones", JsonArray(kept)) }
        }
    }

    override suspend fun uploadAvatar(jpeg: ByteArray): AppResult<String> = withSession { session ->
        val part = MultipartBody.Part.createFormData("avatar", "avatar.jpg", jpeg.toRequestBody("image/jpeg".toMediaType()))
        val owner = session.userId.toRequestBody("text/plain".toMediaType())
        val uploaded = safeApiCall { api.uploadAvatar(session.userId, part, owner) }
        when (uploaded) {
            is AppResult.Failure -> uploaded
            is AppResult.Success -> {
                val url = uploaded.data.avatarUrl
                if (url.isNullOrBlank()) {
                    AppResult.Failure(AppError.Unknown)
                } else {
                    // Best effort: the partner's phone also reads the picture from `profile.<me>.avatar`.
                    patchState(session) { state ->
                        val key = profileKey(session.role)
                        val entry = state.profileEntry(key).toMutableMap().apply { put("avatar", JsonPrimitive(url)) }
                        buildJsonObject { putJsonObject("profile") { put(key, JsonObject(entry)) } }
                    }
                    AppResult.Success(url)
                }
            }
        }
    }

    override suspend fun sendPartnerRequest(phone: String): AppResult<Unit> = withSession { session ->
        safeApiCall { api.sendRequest(SendRequestBody(session.userId, phone)) }.map { }
    }

    override suspend fun removePartner(): AppResult<Unit> = withSession { session ->
        val result = safeApiCall { api.unpair(UnpairBody(session.coupleId, session.role)) }
        if (result is AppResult.Success) {
            sessionStore.save(session.copy(paired = false, partnerName = DEFAULT_PARTNER))
        }
        result.map { }
    }

    override suspend fun exportJson(): AppResult<String> = withSession { session ->
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return@withSession read
        }
        val json = prettyJson.encodeToString(JsonElement.serializer(), state)
        // The web also remembers the time of the last backup in `settings.lastBackup`.
        patchState(session) { latest ->
            val settings = (latest["settings"] as? JsonObject)?.toMutableMap() ?: mutableMapOf()
            settings["lastBackup"] = JsonPrimitive(LocalDateTime.now().toString())
            buildJsonObject { put("settings", JsonObject(settings)) }
        }
        AppResult.Success(json)
    }

    private companion object {
        const val DEFAULT_NAME = "You"
        const val DEFAULT_PARTNER = "Partner"
    }
}
