package com.twinhearts.app.core.notifications

import com.twinhearts.app.core.data.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SENDING side. Any feature calls this after something happens that the partner should hear
 * about ("I sent a hug", "I added a surprise", "I started a game"). The server turns it into a
 * push to the partner's phone, which [TwinMessagingService] shows with the app's look.
 *
 * It never throws and never blocks the screen: a failed push must not break the feature.
 */
@Singleton
class NotificationSender @Inject constructor(
    private val api: NotificationApi,
    private val sessionStore: SessionStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Fire and forget. [title] / [body] are optional: the receiving phone has good defaults per type. */
    fun toPartner(
        type: NotificationType,
        title: String? = null,
        body: String? = null,
        data: Map<String, String> = emptyMap(),
    ) {
        scope.launch { sendNow(type, title, body, data) }
    }

    suspend fun sendNow(
        type: NotificationType,
        title: String? = null,
        body: String? = null,
        data: Map<String, String> = emptyMap(),
    ): Boolean = runCatching {
        val session = sessionStore.session.first() ?: return false
        if (!session.paired) return false
        val payload = data + mapOf("fromName" to session.myName)
        api.send(SendNotificationBody(session.coupleId, session.role, type.key, title, body, payload))
        true
    }.getOrDefault(false)
}
