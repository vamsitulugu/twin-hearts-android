package com.twinhearts.app.feature.auth.ui

import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.auth.data.AuthRepository
import com.twinhearts.app.feature.auth.data.google.GoogleTokenResult
import com.twinhearts.app.feature.auth.domain.model.GoogleAuthOutcome

/** What a Google attempt means for the screen: navigate somewhere, or show a message. */
internal sealed interface GoogleStep {
    data class Go(val event: AuthEvent) : GoogleStep
    data class Problem(val message: AuthMessage) : GoogleStep
}

/**
 * Sends the Google ID token to the server and turns the answer into a [GoogleStep].
 * [existingAccountEvent] is what to do when the email already has an account (Sign In: go Home;
 * Sign Up: also go Home, not to Pairing, because that account may already be paired).
 */
internal suspend fun AuthRepository.googleStep(idToken: String, existingAccountEvent: AuthEvent): GoogleStep =
    when (val result = googleAuth(idToken)) {
        is AppResult.Success -> when (val outcome = result.data) {
            is GoogleAuthOutcome.SignedIn -> GoogleStep.Go(existingAccountEvent)
            is GoogleAuthOutcome.NeedsProfile ->
                GoogleStep.Go(AuthEvent.NeedsProfile(outcome.signupToken, outcome.email, outcome.name))
        }
        is AppResult.Failure -> GoogleStep.Problem(AuthMessage.Failure(result.error))
    }

/** The message for a Google sheet that did not give a token. Null = say nothing (the person just closed it). */
internal fun GoogleTokenResult.problemMessage(): AuthMessage? = when (this) {
    is GoogleTokenResult.Token, GoogleTokenResult.Cancelled -> null
    GoogleTokenResult.NoAccount -> AuthMessage.Resource(R.string.auth_err_google_no_account)
    GoogleTokenResult.Failed -> AuthMessage.Resource(R.string.auth_err_google_failed)
}
