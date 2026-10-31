package com.twinhearts.app.core.pro

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/** The server's routes/pro.js. Prices and the Razorpay secret never reach the phone. */
interface ProApi {
    @GET("api/pro/status")
    suspend fun status(@Query("coupleId") coupleId: String): ProStatusDto

    @POST("api/pro/create-order")
    suspend fun createOrder(@Header("x-user-id") userId: String, @Body body: CreateOrderBody): OrderDto

    @POST("api/pro/verify")
    suspend fun verify(@Header("x-user-id") userId: String, @Body body: VerifyBody): ProStatusDto

    @GET("api/pro/history")
    suspend fun history(@Header("x-user-id") userId: String, @Query("coupleId") coupleId: String): HistoryDto
}

@Serializable
data class ProStatusDto(val isPro: Boolean = false, val plan: String? = null, val expiresAt: String? = null)

@Serializable
data class CreateOrderBody(val coupleId: String, val userId: String, val plan: String)

@Serializable
data class PrefillDto(val name: String = "", val contact: String = "")

@Serializable
data class OrderDto(
    val keyId: String,
    val orderId: String,
    val amount: Long,
    val currency: String = "INR",
    val plan: String,
    val planLabel: String = "",
    val prefill: PrefillDto? = null,
)

@Serializable
data class VerifyBody(
    val coupleId: String,
    val userId: String,
    @SerialName("razorpay_order_id") val orderId: String,
    @SerialName("razorpay_payment_id") val paymentId: String,
    @SerialName("razorpay_signature") val signature: String,
)

@Serializable
data class PaymentDto(val plan: String = "", val amount: Double = 0.0, val paymentId: String? = null, val paidAt: String? = null)

@Serializable
data class HistoryDto(val payments: List<PaymentDto> = emptyList())
