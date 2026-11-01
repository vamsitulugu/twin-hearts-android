package com.twinhearts.app.feature.auth.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

/** The auth endpoints used by the entry screens. Errors come back as `{ "error": "..." }`. */
interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequestDto): SessionDto

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequestDto): SessionDto

    /** Continue with Google, step 1: existing account signs in, new account gets a signup token. */
    @POST("api/auth/google")
    suspend fun googleAuth(@Body body: GoogleAuthRequestDto): GoogleAuthResponseDto

    /** Continue with Google, step 2 (new accounts only): the remaining sign-up details. */
    @POST("api/auth/google/complete")
    suspend fun googleComplete(@Body body: GoogleCompleteRequestDto): SessionDto

    @POST("api/auth/verify-reset-identity")
    suspend fun verifyResetIdentity(@Body body: VerifyResetRequestDto): ResetTokenDto

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequestDto): MessageDto
}
