package com.twinhearts.app.core.telemetry

import android.content.Context
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reports to the admin dashboard:
 *  - [ping]: "this person used the app today" (counts Android daily/weekly/monthly users)
 *  - crashes: saved to disk the moment the app dies (no network is possible then),
 *    sent on the next start by [flushPendingCrash].
 * Never sends chat text, names or phone numbers. The server also scrubs what it receives.
 */
@Singleton
class AppTelemetry @Inject constructor(
    private val api: TelemetryApi,
    private val sessionStore: SessionStore,
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** At most once every 10 minutes, and only while someone is signed in. */
    suspend fun ping() {
        val now = System.currentTimeMillis()
        if (now - prefs.getLong(KEY_LAST_PING, 0L) < PING_EVERY_MILLIS) return
        val session = sessionStore.session.first() ?: return
        prefs.edit().putLong(KEY_LAST_PING, now).apply()
        safeApiCall { api.ping(session.userId, PingBody()) }
    }

    /** Called from the crash handler: must be synchronous and must never throw. */
    fun saveCrash(e: Throwable) {
        try {
            val frames = e.stackTrace.take(8).joinToString("\n") { "${it.className}.${it.methodName}:${it.lineNumber}" }
            prefs.edit()
                .putString(KEY_CRASH_MESSAGE, "${e::class.java.simpleName}: ${e.message.orEmpty()}".take(400))
                .putString(KEY_CRASH_STACK, frames)
                .commit()
        } catch (ignored: Throwable) {
            // nothing more we can do while the app is dying
        }
    }

    /** Sends a crash saved by the previous run, once. */
    suspend fun flushPendingCrash() {
        val message = prefs.getString(KEY_CRASH_MESSAGE, null) ?: return
        val stack = prefs.getString(KEY_CRASH_STACK, null)
        val userId = sessionStore.session.first()?.userId
        val r = safeApiCall { api.error(userId, ErrorBody("app-crash", message, stack, fatal = true)) }
        if (r is com.twinhearts.app.core.common.AppResult.Success) {
            prefs.edit().remove(KEY_CRASH_MESSAGE).remove(KEY_CRASH_STACK).apply()
        }
    }

    private companion object {
        const val PREFS = "telemetry"
        const val KEY_LAST_PING = "last_ping"
        const val KEY_CRASH_MESSAGE = "crash_message"
        const val KEY_CRASH_STACK = "crash_stack"
        const val PING_EVERY_MILLIS = 10 * 60 * 1000L
    }
}
