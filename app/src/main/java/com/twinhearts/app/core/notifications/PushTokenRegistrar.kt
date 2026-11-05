package com.twinhearts.app.core.notifications

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.twinhearts.app.core.data.SessionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Uploads this phone's push token to the server for the signed-in person.
 * Does nothing (and never crashes) while Firebase is not set up: no google-services.json yet.
 */
@Singleton
class PushTokenRegistrar @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: NotificationApi,
    private val sessionStore: SessionStore,
    private val prefs: NotificationPrefs,
) {
    /** True when the Firebase config file was added to the app. */
    val isConfigured: Boolean
        get() = runCatching { FirebaseApp.getApps(context).isNotEmpty() }.getOrDefault(false)

    /**
     * [force] uploads again even when this token was already sent. The server keeps one token per
     * person and can lose it (another install of the app took it, or the server dropped it), and the
     * "already sent" note on this phone would then hide the problem forever.
     */
    suspend fun sync(knownToken: String? = null, force: Boolean = false) {
        if (!isConfigured) return
        val session = sessionStore.session.first() ?: return
        val token = knownToken ?: fetchToken() ?: return
        // Re-upload when the token changed or a different person signed in on this phone.
        val marker = "${session.userId}|$token"
        if (!force && prefs.lastUploadedToken() == marker) return
        runCatching {
            api.registerToken(RegisterTokenBody(session.userId, session.coupleId, session.role, token))
        }.onSuccess { prefs.saveUploadedToken(marker) }
    }

    /** Call just before sign-out, while the session still exists. */
    suspend fun unregister() {
        if (!isConfigured) return
        val session = sessionStore.session.first() ?: return
        val token = fetchToken() ?: return
        runCatching { api.removeToken(RemoveTokenBody(session.userId, token)) }
        prefs.saveUploadedToken(null)
    }

    private suspend fun fetchToken(): String? = runCatching {
        suspendCancellableCoroutine<String?> { cont ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (cont.isActive) cont.resume(if (task.isSuccessful) task.result else null)
            }
        }
    }.getOrNull()
}
