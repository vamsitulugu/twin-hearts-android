package com.twinhearts.app.feature.settings.data

import android.net.Uri
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.settings.data.mapper.backupPatch
import com.twinhearts.app.feature.settings.data.mapper.imageUrls
import com.twinhearts.app.feature.settings.data.mapper.profilePatch
import com.twinhearts.app.feature.settings.data.mapper.toDomain
import com.twinhearts.app.feature.settings.data.mapper.toProfileInfo
import com.twinhearts.app.feature.settings.data.mapper.toStorageSummary
import com.twinhearts.app.feature.settings.data.remote.ChangePinBody
import com.twinhearts.app.feature.settings.data.remote.RequestActionBody
import com.twinhearts.app.feature.settings.data.remote.SaveStateBody
import com.twinhearts.app.feature.settings.data.remote.SendRequestBody
import com.twinhearts.app.feature.settings.data.remote.SettingsApi
import com.twinhearts.app.feature.settings.data.remote.UnpairBody
import com.twinhearts.app.feature.settings.domain.model.ImportOutcome
import com.twinhearts.app.feature.settings.domain.model.PartnerOverview
import com.twinhearts.app.feature.settings.domain.model.PartnerStatus
import com.twinhearts.app.feature.settings.domain.model.ProfileEdit
import com.twinhearts.app.feature.settings.domain.model.ProfileInfo
import com.twinhearts.app.feature.settings.domain.model.RemoveOutcome
import com.twinhearts.app.feature.settings.domain.model.StorageSummary
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

/** Everything the Settings pages ask the server for. The signed-in user comes from the saved Session. */
interface SettingsRepository {
    suspend fun profile(): AppResult<ProfileInfo>
    suspend fun saveProfile(edit: ProfileEdit): AppResult<Unit>

    /** Uploads the chosen picture and returns its new web address. */
    suspend fun changeAvatar(photo: Uri): AppResult<String>

    suspend fun partner(): AppResult<PartnerOverview>
    suspend fun sendRequest(phone: String): AppResult<Unit>
    suspend fun cancelRequest(requestId: String): AppResult<Unit>

    /** Accepts an invitation and returns the new partner's name. */
    suspend fun accept(requestId: String): AppResult<String>
    suspend fun decline(requestId: String): AppResult<Unit>
    suspend fun removePartner(): AppResult<RemoveOutcome>

    suspend fun changePin(current: String, new: String): AppResult<Unit>

    suspend fun storage(): AppResult<StorageSummary>
    suspend fun exportBackup(target: Uri): AppResult<Unit>
    suspend fun importBackup(source: Uri): AppResult<ImportOutcome>

    /** Saves every image of the memories into [folder]. Returns how many were saved (0 = none to export). */
    suspend fun exportPhotos(folder: Uri): AppResult<Int>
    suspend fun clearCache()
    suspend fun signOut()
}

class SettingsRepositoryImpl @Inject constructor(
    private val api: SettingsApi,
    private val sessionStore: SessionStore,
    private val avatars: AvatarCompressor,
    private val files: BackupFiles,
    private val cache: CacheCleaner,
) : SettingsRepository {

    private val prettyJson = Json { prettyPrint = true }
    private val notSignedIn = AppResult.Failure(AppError.Unauthorized())

    private suspend fun session(): Session? = sessionStore.session.first()

    private suspend fun readState(session: Session): AppResult<JsonObject> =
        when (val result = safeApiCall<JsonElement> { api.getState(session.coupleId) }) {
            is AppResult.Success -> AppResult.Success(result.data as? JsonObject ?: JsonObject(emptyMap()))
            is AppResult.Failure -> result
        }

    private suspend fun saveState(session: Session, patch: JsonObject, myName: String): AppResult<Unit> {
        val body = SaveStateBody(session.coupleId, patch, session.role, myName)
        return when (val result = safeApiCall { api.saveState(body) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> result
        }
    }

    // ───────────── profile ─────────────

    override suspend fun profile(): AppResult<ProfileInfo> {
        val session = session() ?: return notSignedIn
        return coroutineScope {
            val stateCall = async { readState(session) }
            // Photos are a nice-to-have: when they fail the screen shows the first letters, like the web.
            val mineCall = async { safeApiCall { api.myProfile(session.userId) } }
            val partnerCall = async { safeApiCall { api.partnerProfile(session.userId) } }
            val state = when (val read = stateCall.await()) {
                is AppResult.Success -> read.data
                is AppResult.Failure -> return@coroutineScope read
            }
            val mine = (mineCall.await() as? AppResult.Success)?.data?.avatarUrl
            val partner = (partnerCall.await() as? AppResult.Success)?.data?.profile?.avatarUrl
            AppResult.Success(state.toProfileInfo(session, mine, partner))
        }
    }

    override suspend fun saveProfile(edit: ProfileEdit): AppResult<Unit> {
        val session = session() ?: return notSignedIn
        val clean = edit.normalized()
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return read
        }
        val saved = saveState(session, profilePatch(state, session.role, clean), clean.myName)
        if (saved is AppResult.Success) {
            sessionStore.save(
                session.copy(
                    myName = clean.myName,
                    partnerName = clean.partnerName,
                    anniversary = clean.anniversary?.toString().orEmpty(),
                    gender = clean.gender?.wire ?: session.gender,
                ),
            )
        }
        return saved
    }

    override suspend fun changeAvatar(photo: Uri): AppResult<String> {
        val session = session() ?: return notSignedIn
        val jpeg = avatars.toJpeg(photo) ?: return AppResult.Failure(AppError.Unknown)
        val part = MultipartBody.Part.createFormData("avatar", "avatar.jpg", jpeg.toRequestBody("image/jpeg".toMediaType()))
        val requester = session.userId.toRequestBody("text/plain".toMediaType())
        return when (val result = safeApiCall { api.uploadAvatar(session.userId, part, requester) }) {
            is AppResult.Success -> result.data.avatarUrl?.let { AppResult.Success(it) } ?: AppResult.Failure(AppError.Unknown)
            is AppResult.Failure -> result
        }
    }

    // ───────────── partner ─────────────

    override suspend fun partner(): AppResult<PartnerOverview> {
        val session = session() ?: return notSignedIn
        val status = when (val result = safeApiCall { api.partnerStatus(session.userId) }) {
            is AppResult.Success -> result.data.toDomain()
            is AppResult.Failure -> return result
        }
        // The server is the truth: keep the phone's "paired" flag and partner name in step with it.
        val connected = status as? PartnerStatus.Connected
        if (connected != null && (!session.paired || session.partnerName != connected.partnerName)) {
            sessionStore.save(session.copy(paired = true, partnerName = connected.partnerName))
        } else if (connected == null && session.paired) {
            sessionStore.save(session.copy(paired = false))
        }
        if (connected != null) return AppResult.Success(PartnerOverview(status, emptyList()))
        val incoming = when (val result = safeApiCall { api.pendingList(session.userId) }) {
            is AppResult.Success -> result.data.requests.map { it.toDomain() }
            is AppResult.Failure -> return result
        }
        return AppResult.Success(PartnerOverview(status, incoming))
    }

    override suspend fun sendRequest(phone: String): AppResult<Unit> {
        val session = session() ?: return notSignedIn
        return when (val result = safeApiCall { api.sendRequest(SendRequestBody(session.userId, phone)) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> result
        }
    }

    override suspend fun cancelRequest(requestId: String): AppResult<Unit> {
        val session = session() ?: return notSignedIn
        return when (val result = safeApiCall { api.cancel(RequestActionBody(requestId, session.userId)) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> result
        }
    }

    override suspend fun accept(requestId: String): AppResult<String> {
        val session = session() ?: return notSignedIn
        return when (val result = safeApiCall { api.accept(RequestActionBody(requestId, session.userId)) }) {
            is AppResult.Success -> {
                val name = result.data.partnerName?.takeIf { it.isNotBlank() } ?: "Partner"
                sessionStore.save(
                    session.copy(
                        coupleId = result.data.coupleId ?: session.coupleId,
                        role = result.data.role ?: "user2",
                        partnerName = name,
                        paired = true,
                    ),
                )
                AppResult.Success(name)
            }
            is AppResult.Failure -> result
        }
    }

    override suspend fun decline(requestId: String): AppResult<Unit> {
        val session = session() ?: return notSignedIn
        return when (val result = safeApiCall { api.reject(RequestActionBody(requestId, session.userId)) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> result
        }
    }

    override suspend fun removePartner(): AppResult<RemoveOutcome> {
        val session = session() ?: return notSignedIn
        return when (val result = safeApiCall { api.unpair(UnpairBody(session.coupleId, session.role)) }) {
            is AppResult.Success -> {
                sessionStore.save(session.copy(paired = false, partnerName = "Partner"))
                AppResult.Success(if (session.role == "user2") RemoveOutcome.MustSignOut else RemoveOutcome.StaySignedIn)
            }
            is AppResult.Failure -> result
        }
    }

    // ───────────── vault PIN ─────────────

    override suspend fun changePin(current: String, new: String): AppResult<Unit> {
        val session = session() ?: return notSignedIn
        return when (val result = safeApiCall { api.changePin(ChangePinBody(session.coupleId, current, new)) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> result
        }
    }

    // ───────────── storage & backup ─────────────

    override suspend fun storage(): AppResult<StorageSummary> {
        val session = session() ?: return notSignedIn
        return when (val read = readState(session)) {
            is AppResult.Success -> AppResult.Success(read.data.toStorageSummary())
            is AppResult.Failure -> read
        }
    }

    override suspend fun exportBackup(target: Uri): AppResult<Unit> {
        val session = session() ?: return notSignedIn
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return read
        }
        val text = prettyJson.encodeToString(JsonElement.serializer(), state)
        return if (files.write(target, text)) AppResult.Success(Unit) else AppResult.Failure(AppError.Unknown)
    }

    override suspend fun importBackup(source: Uri): AppResult<ImportOutcome> {
        val session = session() ?: return notSignedIn
        val text = files.read(source) ?: return AppResult.Success(ImportOutcome.InvalidFile)
        val imported = runCatching { Json.parseToJsonElement(text) }.getOrNull() as? JsonObject
            ?: return AppResult.Success(ImportOutcome.InvalidFile)
        return when (val saved = saveState(session, backupPatch(imported), session.myName)) {
            is AppResult.Success -> AppResult.Success(ImportOutcome.Restored)
            is AppResult.Failure -> saved
        }
    }

    override suspend fun exportPhotos(folder: Uri): AppResult<Int> {
        val session = session() ?: return notSignedIn
        val urls = when (val read = readState(session)) {
            is AppResult.Success -> read.data.imageUrls()
            is AppResult.Failure -> return read
        }
        if (urls.isEmpty()) return AppResult.Success(0)
        val saved = files.savePhotos(folder, urls)
        return if (saved > 0) AppResult.Success(saved) else AppResult.Failure(AppError.Unknown)
    }

    override suspend fun clearCache() = cache.clear()

    override suspend fun signOut() = sessionStore.clear()
}
