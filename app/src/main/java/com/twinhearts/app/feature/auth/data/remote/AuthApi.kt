package com.twinhearts.app.feature.auth.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

/** The four auth endpoints used by the entry screens. Errors come back as `{ "error": "..." }`. */
interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequestDto): SessionDto

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequestDto): SessionDto

    @POST("api/auth/verify-reset-identity")
    suspend fun verifyResetIdentity(@Body body: VerifyResetRequestDto): ResetTokenDto

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequestDto): MessageDto
}
