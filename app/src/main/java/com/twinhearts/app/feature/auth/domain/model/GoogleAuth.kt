package com.twinhearts.app.feature.auth.domain.model

import com.twinhearts.app.core.data.Session

/** What the server decided after a Google sign-in. */
sealed interface GoogleAuthOutcome {
    /** The email already had an account: the session is saved, go straight in. */
    data class SignedIn(val session: Session) : GoogleAuthOutcome

    /** A new email: the app must ask for the rest of the sign-up details first. */
    data class NeedsProfile(val signupToken: String, val email: String, val name: String) : GoogleAuthOutcome
}

/** The extra details a new Google account fills in (everything the normal sign-up form has, except the email). */
data class GoogleCompleteRequest(
    val signupToken: String,
    /** Only used to fill the saved session if the server leaves the email out. Never sent. */
    val email: String,
    val name: String,
    val phone: String,
    val password: String,
    val vaultPin: String,
    val gender: Gender,
)
