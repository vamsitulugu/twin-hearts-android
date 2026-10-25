package com.twinhearts.app.feature.auth.domain.model

/** Everything the sign-up form collects, already trimmed and checked by the ViewModel. */
data class SignUpRequest(
    val email: String,
    val password: String,
    val name: String,
    val phone: String,
    val vaultPin: String,
    val gender: Gender,
)
