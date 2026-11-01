package com.twinhearts.app.feature.auth.data

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.auth.data.mapper.toSession
import com.twinhearts.app.feature.auth.data.remote.AuthApi
import com.twinhearts.app.feature.auth.data.remote.GoogleAuthRequestDto
import com.twinhearts.app.feature.auth.data.remote.GoogleCompleteRequestDto
import com.twinhearts.app.feature.auth.data.remote.LoginRequestDto
import com.twinhearts.app.feature.auth.data.remote.RegisterRequestDto
import com.twinhearts.app.feature.auth.data.remote.ResetPasswordRequestDto
import com.twinhearts.app.feature.auth.data.remote.SessionDto
import com.twinhearts.app.feature.auth.data.remote.VerifyResetRequestDto
import com.twinhearts.app.feature.auth.domain.model.GoogleAuthOutcome
import com.twinhearts.app.feature.auth.domain.model.GoogleCompleteRequest
import com.twinhearts.app.feature.auth.domain.model.SignUpRequest
import javax.inject.Inject

/** What the auth screens can ask for. A successful sign in / sign up also saves the [Session]. */
interface AuthRepository {
    suspend fun signIn(email: String, password: String): AppResult<Session>
    suspend fun signUp(request: SignUpRequest): AppResult<Session>

    /**
     * Continue with Google, step 1. An existing account is signed in (session saved); a new email comes back
     * as [GoogleAuthOutcome.NeedsProfile] so the app can ask for the remaining sign-up details.
     */
    suspend fun googleAuth(idToken: String): AppResult<GoogleAuthOutcome>

    /** Continue with Google, step 2 (new accounts only). Creates the account and saves the session. */
    suspend fun completeGoogleSignUp(request: GoogleCompleteRequest): AppResult<Session>

    /** Step 1 of password reset. Returns the one-time reset token (kept in memory only). */
    suspend fun verifyResetIdentity(email: String, phone: String): AppResult<String>

    /** Step 2 of password reset. */
    suspend fun resetPassword(token: String, newPassword: String): AppResult<Unit>
}

class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val sessionStore: SessionStore,
) : AuthRepository {

    override suspend fun signIn(email: String, password: String): AppResult<Session> {
        val result = safeApiCall { api.login(LoginRequestDto(email = email, password = password)) }
        return persist(result, fallbackEmail = email, gender = null)
    }

    override suspend fun signUp(request: SignUpRequest): AppResult<Session> {
        val body = RegisterRequestDto(
            email = request.email,
            password = request.password,
            myName = request.name,
            phoneNumber = request.phone,
            vaultPin = request.vaultPin,
        )
        val result = safeApiCall { api.register(body) }
        return persist(result, fallbackEmail = request.email.lowercase(), gender = request.gender.apiValue)
    }

    override suspend fun googleAuth(idToken: String): AppResult<GoogleAuthOutcome> =
        when (val result = safeApiCall { api.googleAuth(GoogleAuthRequestDto(idToken = idToken)) }) {
            is AppResult.Failure -> result
            is AppResult.Success -> {
                val dto = result.data
                if (dto.needsProfile) {
                    val token = dto.signupToken
                    if (token.isNullOrBlank()) {
                        AppResult.Failure(AppError.Unknown)
                    } else {
                        AppResult.Success(GoogleAuthOutcome.NeedsProfile(token, dto.email.orEmpty(), dto.name.orEmpty()))
                    }
                } else {
                    val userId = dto.userId
                    if (userId.isNullOrBlank()) {
                        AppResult.Failure(AppError.Unknown)
                    } else {
                        val session = SessionDto(
                            userId = userId,
                            coupleId = dto.coupleId,
                            myName = dto.myName,
                            email = dto.email,
                            phoneNumber = dto.phoneNumber,
                            partnerName = dto.partnerName,
                            anniversary = dto.anniversary,
                            role = dto.role,
                            paired = dto.paired,
                        ).toSession(fallbackEmail = dto.email.orEmpty(), gender = null)
                        sessionStore.save(session)
                        AppResult.Success(GoogleAuthOutcome.SignedIn(session))
                    }
                }
            }
        }

    override suspend fun completeGoogleSignUp(request: GoogleCompleteRequest): AppResult<Session> {
        val body = GoogleCompleteRequestDto(
            signupToken = request.signupToken,
            myName = request.name,
            phoneNumber = request.phone,
            password = request.password,
            vaultPin = request.vaultPin,
        )
        val result = safeApiCall { api.googleComplete(body) }
        return persist(result, fallbackEmail = request.email.lowercase(), gender = request.gender.apiValue)
    }

    override suspend fun verifyResetIdentity(email: String, phone: String): AppResult<String> =
        when (val result = safeApiCall { api.verifyResetIdentity(VerifyResetRequestDto(email = email, phone = phone)) }) {
            is AppResult.Success -> result.data.resetToken?.let { AppResult.Success(it) }
                ?: AppResult.Failure(AppError.Unknown)
            is AppResult.Failure -> result
        }

    override suspend fun resetPassword(token: String, newPassword: String): AppResult<Unit> =
        when (val result = safeApiCall { api.resetPassword(ResetPasswordRequestDto(token = token, newPassword = newPassword)) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> result
        }

    private suspend fun persist(
        result: AppResult<SessionDto>,
        fallbackEmail: String,
        gender: String?,
    ): AppResult<Session> = when (result) {
        is AppResult.Success -> {
            val session = result.data.toSession(fallbackEmail = fallbackEmail, gender = gender)
            sessionStore.save(session)
            AppResult.Success(session)
        }
        is AppResult.Failure -> result
    }
}
