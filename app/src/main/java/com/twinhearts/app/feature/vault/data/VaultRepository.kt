package com.twinhearts.app.feature.vault.data

import android.net.Uri
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.vault.data.remote.DeleteMediaBody
import com.twinhearts.app.feature.vault.data.remote.ResetPinBody
import com.twinhearts.app.feature.vault.data.remote.SetPinBody
import com.twinhearts.app.feature.vault.data.remote.VaultApi
import com.twinhearts.app.feature.vault.data.remote.VaultSaveBody
import com.twinhearts.app.feature.vault.data.remote.VerifyPinBody
import com.twinhearts.app.feature.vault.domain.model.VaultAddResult
import com.twinhearts.app.feature.vault.domain.model.VaultItem
import com.twinhearts.app.feature.vault.domain.model.VaultKind
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import javax.inject.Inject

interface VaultRepository {
    /** True when this couple still has to choose a vault PIN. */
    suspend fun needsSetup(): AppResult<Boolean>

    suspend fun verifyPin(pin: String): AppResult<Unit>
    suspend fun setPin(pin: String): AppResult<Unit>

    /** "Forgot PIN": proves who you are with the sign-in password, then sets a new PIN. */
    suspend fun resetPin(password: String, newPin: String): AppResult<Unit>

    suspend fun items(): AppResult<List<VaultItem>>

    /** Uploads each picked file to the private vault storage, then lists them in the shared state. */
    suspend fun add(uris: List<Uri>, kind: VaultKind): AppResult<VaultAddResult>

    /** Deletes the file from storage first (as the web does), then removes it from the list. */
    suspend fun delete(id: String): AppResult<List<VaultItem>>
}

class VaultRepositoryImpl @Inject constructor(
    private val api: VaultApi,
    private val sessionStore: SessionStore,
    private val files: VaultFiles,
) : VaultRepository {

    private val notSignedIn = AppResult.Failure(AppError.Unauthorized())

    override suspend fun needsSetup(): AppResult<Boolean> {
        val session = sessionStore.session.first() ?: return notSignedIn
        return when (val r = safeApiCall { api.status(session.coupleId) }) {
            is AppResult.Success -> AppResult.Success(r.data.needsSetup)
            is AppResult.Failure -> r
        }
    }

    override suspend fun verifyPin(pin: String): AppResult<Unit> {
        val session = sessionStore.session.first() ?: return notSignedIn
        return unit(safeApiCall { api.verifyPin(VerifyPinBody(session.coupleId, pin)) })
    }

    override suspend fun setPin(pin: String): AppResult<Unit> {
        val session = sessionStore.session.first() ?: return notSignedIn
        return unit(safeApiCall { api.setPin(SetPinBody(session.coupleId, pin)) })
    }

    override suspend fun resetPin(password: String, newPin: String): AppResult<Unit> {
        val session = sessionStore.session.first() ?: return notSignedIn
        return unit(safeApiCall { api.resetPin(ResetPinBody(session.userId, password, newPin)) })
    }

    override suspend fun items(): AppResult<List<VaultItem>> {
        val session = sessionStore.session.first() ?: return notSignedIn
        return when (val read = readState(session)) {
            is AppResult.Success -> AppResult.Success(read.data.rawItems().toItems())
            is AppResult.Failure -> read
        }
    }

    override suspend fun add(uris: List<Uri>, kind: VaultKind): AppResult<VaultAddResult> {
        val session = sessionStore.session.first() ?: return notSignedIn
        val fallbackMime = if (kind == VaultKind.Video) "video/mp4" else "image/jpeg"
        val text = "text/plain".toMediaType()

        val uploaded = mutableListOf<JsonObject>()
        var failed = 0
        var lastError: AppError? = null
        for ((index, uri) in uris.withIndex()) {
            val file = files.describe(uri, fallbackMime)
            if (file == null) {
                failed++
                continue
            }
            val part = MultipartBody.Part.createFormData("file", file.name, files.body(file))
            val result = safeApiCall {
                api.upload(
                    file = part,
                    coupleId = session.coupleId.toRequestBody(text),
                    type = "vault".toRequestBody(text),
                )
            }
            when (result) {
                is AppResult.Success -> uploaded += buildJsonObject {
                    // Same item the web adds: id, url, path, type, date.
                    put("id", System.currentTimeMillis() + index)
                    put("url", result.data.url)
                    put("path", result.data.path.orEmpty())
                    put("type", kind.key)
                    put("date", LocalDate.now().toString())
                }
                is AppResult.Failure -> {
                    failed++
                    lastError = result.error
                }
            }
        }
        if (uploaded.isEmpty()) return AppResult.Failure(lastError ?: AppError.Unknown)

        // Read the list only now, so an item the other phone added meanwhile is not lost.
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return read
        }
        val next = JsonArray(state.rawItems().toList() + uploaded)
        return when (val saved = save(session, next)) {
            is AppResult.Success -> AppResult.Success(VaultAddResult(next.toItems(), uploaded.size, failed))
            is AppResult.Failure -> saved
        }
    }

    override suspend fun delete(id: String): AppResult<List<VaultItem>> {
        val session = sessionStore.session.first() ?: return notSignedIn
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return read
        }
        val raw = state.rawItems()
        val target = raw.firstOrNull { (it as? JsonObject)?.get("id").text() == id } as? JsonObject
        val path = target?.get("path").text()
        if (path.isNotBlank()) {
            val removed = safeApiCall { api.deleteMedia(DeleteMediaBody(path, VAULT_BUCKET, session.coupleId)) }
            if (removed is AppResult.Failure) return removed
        }
        // Work on the raw items so fields this app does not know about are kept.
        val kept = JsonArray(raw.filterNot { (it as? JsonObject)?.get("id").text() == id })
        return when (val saved = save(session, kept)) {
            is AppResult.Success -> AppResult.Success(kept.toItems())
            is AppResult.Failure -> saved
        }
    }

    private suspend fun readState(session: Session): AppResult<JsonObject> =
        when (val r = safeApiCall { api.getState(session.coupleId) }) {
            is AppResult.Success -> AppResult.Success(r.data.state as? JsonObject ?: JsonObject(emptyMap()))
            is AppResult.Failure -> r
        }

    private suspend fun save(session: Session, items: JsonArray): AppResult<Unit> {
        val body = VaultSaveBody(session.coupleId, JsonObject(mapOf("vault" to items)), session.role, session.myName)
        return unit(safeApiCall { api.saveState(body) })
    }

    private fun <T> unit(result: AppResult<T>): AppResult<Unit> = when (result) {
        is AppResult.Success -> AppResult.Success(Unit)
        is AppResult.Failure -> result
    }

    private fun JsonObject.rawItems(): JsonArray = this["vault"] as? JsonArray ?: JsonArray(emptyList())

    /** Skips anything without a link, so one bad item never hides the rest. */
    private fun JsonArray.toItems(): List<VaultItem> = mapNotNull { el ->
        val o = el as? JsonObject ?: return@mapNotNull null
        val url = o["url"].text().takeIf { it.isNotBlank() } ?: return@mapNotNull null
        VaultItem(
            id = o["id"].text().ifBlank { url },
            url = url,
            path = o["path"].text(),
            kind = VaultKind.fromKey(o["type"].text()),
            date = o["date"].text(),
        )
    }

    private fun JsonElement?.text(): String = (this as? JsonPrimitive)?.contentOrNull.orEmpty()

    private companion object {
        const val VAULT_BUCKET = "vault-media"
    }
}
