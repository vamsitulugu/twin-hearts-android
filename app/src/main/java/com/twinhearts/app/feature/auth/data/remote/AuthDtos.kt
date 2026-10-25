package com.twinhearts.app.feature.auth.data.remote

import kotlinx.serialization.Serializable

// Shapes read from routes/auth.js of the old server (R-65).

/** POST /api/auth/login body. */
@Serializable
data class LoginRequestDto(val email: String, val password: String)

/** POST /api/auth/register body. The server also needs `phoneNumber` and `vaultPin`. */
@Serializable
data class RegisterRequestDto(
    val email: String,
    val password: String,
    val myName: String,
    val phoneNumber: String,
    val vaultPin: String,
)

/** Answer of both login and register. Register leaves out email/anniversary, so they are optional. */
@Serializable
data class SessionDto(
    val userId: String,
    val coupleId: String? = null,
    val myName: String = "",
    val email: String? = null,
    val phoneNumber: String? = null,
    val partnerName: String? = null,
    val anniversary: String? = null,
    val role: String? = null,
    val paired: Boolean? = null,
)

/** POST /api/auth/verify-reset-identity body. */
@Serializable
data class VerifyResetRequestDto(val email: String, val phone: String)

/** Answer: a one-time token, valid 10 minutes. */
@Serializable
data class ResetTokenDto(val resetToken: String? = null)

/** POST /api/auth/reset-password body. */
@Serializable
data class ResetPasswordRequestDto(val token: String, val newPassword: String)

/** Answer is `{ "message": "Password updated" }`. We only need to know it succeeded. */
@Serializable
data class MessageDto(val message: String? = null)
