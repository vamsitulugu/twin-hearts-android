package com.twinhearts.app.core.appconfig

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/** The server's public routes/releases.js and routes/flags.js (what the admin dashboard controls). */
interface AppConfigApi {
    @GET("api/releases/check")
    suspend fun checkRelease(@Query("version") version: String, @Query("platform") platform: String): ReleaseCheckDto

    @GET("api/flags")
    suspend fun flags(@Query("userId") userId: String?): FlagsDto
}

@Serializable
data class ReleaseCheckDto(
    val updateAvailable: Boolean = false,
    val updateType: String? = null,
    val release: ReleaseDto? = null,
)

@Serializable
data class ReleaseDto(
    val version: String = "",
    val title: String? = null,
    val message: String? = null,
    val updateUrl: String? = null,
)

@Serializable
data class FlagsDto(
    val flags: Map<String, Boolean> = emptyMap(),
    val degraded: Boolean = false,
)
