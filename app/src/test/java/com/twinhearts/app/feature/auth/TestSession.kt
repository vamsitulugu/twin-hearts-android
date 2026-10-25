package com.twinhearts.app.feature.auth

import com.twinhearts.app.core.data.Session

/** A ready-made signed-in user for tests. */
fun testSession() = Session(
    userId = "u1",
    coupleId = "c1",
    myName = "Vamsi",
    email = "a@b.com",
    phoneNumber = "1234567890",
    partnerName = "Partner",
    anniversary = "",
    role = "user1",
    paired = false,
    gender = null,
)
