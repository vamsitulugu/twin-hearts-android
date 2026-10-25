package com.twinhearts.app.core.data

/**
 * Who is signed in on this phone. The old server has no tokens or cookies: after login it
 * returns these ids and every later request carries them (decided in Phase 0, R-64).
 */
data class Session(
    val userId: String,
    val coupleId: String,
    val myName: String,
    val email: String,
    val phoneNumber: String,
    val partnerName: String,
    val anniversary: String,
    /** "user1" or "user2": which side of the couple this account is. */
    val role: String,
    val paired: Boolean,
    /** "female" or "male", chosen at sign up. Null for accounts created on the web. */
    val gender: String?,
)
