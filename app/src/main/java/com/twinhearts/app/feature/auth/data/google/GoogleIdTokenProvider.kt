package com.twinhearts.app.feature.auth.data.google

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.twinhearts.app.BuildConfig
import kotlinx.coroutines.CancellationException

/** What the Google account chooser gave back. */
sealed interface GoogleTokenResult {
    /** The ID token the server checks. Never logged or stored. */
    data class Token(val idToken: String) : GoogleTokenResult

    /** The person closed the Google sheet. Nothing to show. */
    data object Cancelled : GoogleTokenResult

    /** There is no Google account on this phone yet. */
    data object NoAccount : GoogleTokenResult

    data object Failed : GoogleTokenResult
}

/**
 * Opens Google's account chooser (Credential Manager, "Sign in with Google") and returns an ID token.
 * [BuildConfig.GOOGLE_WEB_CLIENT_ID] is the *Web* client id: the token's audience must be the same id
 * the server checks against.
 */
object GoogleIdTokenProvider {

    /** False when GOOGLE_WEB_CLIENT_ID is missing from local.properties: the button is then hidden. */
    val isConfigured: Boolean get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    /** [context] must be an Activity context so Google's sheet can open on top of the app. */
    suspend fun requestIdToken(context: Context): GoogleTokenResult {
        if (!isConfigured) return GoogleTokenResult.Failed
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build())
            .build()
        return try {
            val credential = CredentialManager.create(context).getCredential(context, request).credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                GoogleTokenResult.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                GoogleTokenResult.Failed
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            GoogleTokenResult.Cancelled
        } catch (e: NoCredentialException) {
            GoogleTokenResult.NoAccount
        } catch (e: GetCredentialException) {
            // Type only (never the message): the usual cause is a wrong SHA-1 / client id in Google Cloud.
            Log.w("GoogleSignIn", "Credential error: ${e.type}")
            GoogleTokenResult.Failed
        } catch (e: GoogleIdTokenParsingException) {
            Log.w("GoogleSignIn", "Could not read the Google token")
            GoogleTokenResult.Failed
        }
    }
}
