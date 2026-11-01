package com.twinhearts.app.core.presence

import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.notifications.AppForeground
import com.twinhearts.app.feature.chat.data.remote.ChatApi
import com.twinhearts.app.feature.chat.data.remote.PresenceBody
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tells the server "I am online" for the WHOLE app, not only while the Chat screen is open.
 * The web shows my partner's Online / Away / Last seen from this signal, so before this the web
 * showed "Offline" whenever I was on any screen except Chat.
 *
 * Every [BEAT_MS] while any screen of the app is visible: "online". The moment the app goes to the
 * background: one "away". Needs no screen, so it is started once from [com.twinhearts.app.TwinHeartsApp].
 */
@Singleton
class AppPresence @Inject constructor(
    private val api: ChatApi,
    private val sessionStore: SessionStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            var wasVisible = false
            var lastBeat = 0L
            while (true) {
                val visible = AppForeground.isVisible
                val now = System.currentTimeMillis()
                when {
                    visible && (!wasVisible || now - lastBeat >= BEAT_MS) -> {
                        send("online")
                        lastBeat = now
                    }
                    !visible && wasVisible -> send("away")
                }
                wasVisible = visible
                delay(CHECK_MS)
            }
        }
    }

    private suspend fun send(status: String) {
        try {
            val session = sessionStore.session.first() ?: return
            if (session.coupleId.isBlank() || session.role.isBlank()) return
            api.setPresence(session.coupleId, PresenceBody(session.role, status))
        } catch (e: CancellationException) {
            throw e
        } catch (ignored: Exception) {
            // A missed beat is fine: the next one comes in a few seconds.
        }
    }

    private companion object {
        const val BEAT_MS = 15_000L
        const val CHECK_MS = 3_000L
    }
}
