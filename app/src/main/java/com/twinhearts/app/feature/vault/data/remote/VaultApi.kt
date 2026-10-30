package com.twinhearts.app.feature.vault.data.remote

import com.twinhearts.app.core.network.StateEnvelopeSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * Everything the Secret Vault needs from the existing server. No new backend route:
 * - PIN: `routes/auth.js` (vault-status, verify-pin, set-vault-pin, reset-vault-pin)
 * - files: `routes/media.js` (`/upload` with `type=vault`, `/delete` with `bucket=vault-media`)
 * - the list of items: the shared state document (`vault` key), as the web app saves it
 */
interface VaultApi {
    @GET("api/auth/vault-status/{coupleId}")
    suspend fun status(@Path("coupleId") coupleId: String): VaultStatusDto

    @POST("api/auth/verify-pin")
    suspend fun verifyPin(@Body body: VerifyPinBody): VaultOkDto

    @POST("api/auth/set-vault-pin")
    suspend fun setPin(@Body body: SetPinBody): VaultOkDto

    @POST("api/auth/reset-vault-pin")
    suspend fun resetPin(@Body body: ResetPinBody): VaultOkDto

    @GET("api/data/state/{coupleId}")
    suspend fun getState(@Path("coupleId") coupleId: String): VaultStateResponse

    @POST("api/data/state")
    suspend fun saveState(@Body body: VaultSaveBody): VaultOkDto

    @Multipart
    @POST("api/media/upload")
    suspend fun upload(
        @Part file: MultipartBody.Part,
        @Part("coupleId") coupleId: RequestBody,
        @Part("type") type: RequestBody,
    ): VaultUploadDto

    /** DELETE with a JSON body, which is how `routes/media.js` expects it. */
    @HTTP(method = "DELETE", path = "api/media/delete", hasBody = true)
    suspend fun deleteMedia(@Body body: DeleteMediaBody): VaultOkDto
}

@Serializable
data class VaultStatusDto(val needsSetup: Boolean = false)

@Serializable
data class VaultOkDto(val ok: Boolean? = null)

@Serializable
data class VerifyPinBody(val coupleId: String, val pin: String)

@Serializable
data class SetPinBody(val coupleId: String, val newPin: String)

@Serializable
data class ResetPinBody(val userId: String, val password: String, val newPin: String)

@Serializable(with = VaultStateResponseSerializer::class)
data class VaultStateResponse(val state: JsonElement? = null)

/** Reads the saved document in either shape (see [unwrapState]). */
object VaultStateResponseSerializer : StateEnvelopeSerializer<VaultStateResponse>("VaultStateResponse", { VaultStateResponse(it) }, { it.state })

@Serializable
data class VaultSaveBody(
    val coupleId: String,
    val state: JsonObject,
    val senderRole: String,
    val myName: String,
)

@Serializable
data class VaultUploadDto(val url: String, val path: String? = null)

@Serializable
data class DeleteMediaBody(val path: String, val bucket: String, val coupleId: String)
