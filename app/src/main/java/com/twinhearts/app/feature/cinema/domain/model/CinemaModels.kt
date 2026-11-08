package com.twinhearts.app.feature.cinema.domain.model

/** Who is asking: the couple, which side of it this phone is, and the avatar gender chosen at sign up. */
data class CinemaIdentity(
    val coupleId: String,
    /** "user1" or "user2". */
    val role: String,
    /** "female", "male" or null (accounts made on the web). */
    val gender: String?,
)

/** A movie-night invite waiting for me: my partner is in the cinema lobby. */
data class CinemaInvite(
    val id: String,
    val hostName: String,
    val movie: String?,
)
