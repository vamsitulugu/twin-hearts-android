package com.twinhearts.app.feature.access.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Free daily uses + coin passes (us-app: routes/rewards.js, /access/*).
 * The server owns every number (free uses, price, hours). The app never sends an amount.
 */
interface AccessApi {
    /** Use one free try, ride a running pass (or Pro), or learn that a pass is needed. */
    @POST("api/rewards/access/use")
    suspend fun use(@Body body: AccessUseBody, @Header("x-user-id") header: String): AccessUseDto

    /** Buy a pass. [AccessPassBody.clientId] is one id per tap, so a retry is never charged twice. */
    @POST("api/rewards/access/pass")
    suspend fun pass(@Body body: AccessPassBody, @Header("x-user-id") header: String): AccessPassDto
}

@Serializable
data class AccessUseBody(val coupleId: String, val userId: String, val feature: String)

@Serializable
data class AccessUseDto(
    val allowed: Boolean = false,
    /** "free", "pass" or "pro" when allowed. */
    val via: String = "",
    val freeLeft: Int = 0,
    val freeTotal: Int = 0,
    val expiresAt: String = "",
    val needsCoins: Boolean = false,
    val feature: String = "",
    val price: Int = 0,
    val hours: Int = 0,
    val balance: Int = 0,
    val label: String = "",
    val emoji: String = "",
    val perk: String = "",
)

@Serializable
data class AccessPassBody(val coupleId: String, val userId: String, val feature: String, val clientId: String)

@Serializable
data class AccessPassDto(
    val ok: Boolean = false,
    val duplicate: Boolean = false,
    val expiresAt: String = "",
    val balance: Int = 0,
)
