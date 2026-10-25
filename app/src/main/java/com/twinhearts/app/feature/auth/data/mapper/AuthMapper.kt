package com.twinhearts.app.feature.auth.data.mapper

import com.twinhearts.app.core.data.Session
import com.twinhearts.app.feature.auth.data.remote.SessionDto

/**
 * Turns the server answer into the app's [Session]. Same defaults as the web app:
 * partner "Partner", role "user1", not paired. Register does not send the email back, so the
 * email the user typed is used ([fallbackEmail]).
 */
fun SessionDto.toSession(fallbackEmail: String, gender: String?): Session = Session(
    userId = userId,
    coupleId = coupleId.orEmpty(),
    myName = myName,
    email = email ?: fallbackEmail,
    phoneNumber = phoneNumber.orEmpty(),
    partnerName = partnerName?.takeIf { it.isNotBlank() } ?: "Partner",
    anniversary = anniversary.orEmpty(),
    role = role ?: "user1",
    paired = paired ?: false,
    gender = gender,
)
