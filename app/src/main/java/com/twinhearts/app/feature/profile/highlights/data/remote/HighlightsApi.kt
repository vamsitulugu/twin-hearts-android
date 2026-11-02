package com.twinhearts.app.feature.profile.highlights.data.remote

import com.twinhearts.app.feature.profile.data.remote.OkResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/** routes/highlights.js of the old server (the same one the web Profile page uses). */
interface HighlightsApi {

    /** Every highlight of the couple (both partners) with its clips, oldest clip first. */
    @GET("api/highlights/{coupleId}")
    suspend fun list(@Path("coupleId") coupleId: String): List<HighlightDto>

    /** One clip per call. Send [highlightId] to add to a highlight, or [title] + [category] to start one. */
    @Multipart
    @POST("api/highlights/upload")
    suspend fun upload(
        @Part file: MultipartBody.Part,
        @Part("coupleId") coupleId: RequestBody,
        @Part("userId") userId: RequestBody,
        @Part("durationSec") durationSec: RequestBody,
        @Part("highlightId") highlightId: RequestBody?,
        @Part("title") title: RequestBody?,
        @Part("category") category: RequestBody?,
    ): UploadClipDto

    @PATCH("api/highlights/{id}")
    suspend fun update(@Path("id") id: String, @Body body: UpdateHighlightBody): HighlightDto

    @HTTP(method = "DELETE", path = "api/highlights/item/{itemId}", hasBody = true)
    suspend fun deleteClip(@Path("itemId") itemId: String, @Body body: OwnerBody): DeleteClipDto

    @HTTP(method = "DELETE", path = "api/highlights/{id}", hasBody = true)
    suspend fun deleteHighlight(@Path("id") id: String, @Body body: OwnerBody): OkResponse
}

@Serializable
data class HighlightDto(
    val id: String,
    @SerialName("user_id") val userId: String = "",
    val title: String = "",
    val category: String = "other",
    val items: List<HighlightItemDto> = emptyList(),
)

@Serializable
data class HighlightItemDto(
    val id: String,
    @SerialName("media_type") val mediaType: String = "",
    @SerialName("media_url") val mediaUrl: String = "",
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class UploadClipDto(val highlight: HighlightDto, val item: HighlightItemDto)

@Serializable
data class UpdateHighlightBody(val coupleId: String, val userId: String, val title: String, val category: String)

@Serializable
data class OwnerBody(val coupleId: String, val userId: String)

@Serializable
data class DeleteClipDto(val ok: Boolean? = null, val highlightDeleted: Boolean? = null)
