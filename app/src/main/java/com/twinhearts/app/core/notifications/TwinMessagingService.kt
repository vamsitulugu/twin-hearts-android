package com.twinhearts.app.core.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import com.twinhearts.app.feature.chat.data.ChatRepository
import javax.inject.Inject

/**
 * RECEIVING side. The server sends "data" messages (no ready-made text), so this app always
 * draws the notification itself with [TwinNotifier]: same look whether the app is open, in the
 * background or closed.
 *
 * Message data (sent by routes/auth.js): tag, title, body, senderName, senderAvatar, type, url, …
 */
@AndroidEntryPoint
class TwinMessagingService : FirebaseMessagingService() {

    @Inject lateinit var notifier: TwinNotifier
    @Inject lateinit var registrar: PushTokenRegistrar
    @Inject lateinit var chat: ChatRepository

    override fun onNewToken(token: String) {
        scope.launch { registrar.sync(knownToken = token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val raw = message.data
        // The server's format: title, body, tag, senderName, type ("video"/"voice" for calls), url …
        // A newer/unknown tag still shows as a plain app notification.
        val type = NotificationType.fromKey(raw["type"]) ?: NotificationType.fromServerTag(raw["tag"])
        val isVideo = raw["type"] == "video" || raw["title"].orEmpty().contains("video", ignoreCase = true)
        val data = raw + mapOf(
            "fromName" to (raw["senderName"] ?: raw["fromName"].orEmpty()),
            "video" to isVideo.toString(),
        )
        val event = TwinNotification(type, raw["title"], raw["body"], data)
        // FCM gives this method about 20 seconds; showing is quick, so wait for it here.
        // Hug / Touch / Miss You: let an open app show its Accept popup right now.
        if (type == NotificationType.PARTNER_HUG || type == NotificationType.PARTNER_TOUCH || type == NotificationType.PARTNER_MISS) {
            NotificationEvents.loveArrived()
        }
        runBlocking {
            notifier.show(event)
            // A chat message reached this phone: tell the server now, so my partner's single tick
            // becomes a double tick even though the app is closed (their tick stays single only
            // when this phone has no internet).
            if (raw["tag"] == "chat-msg") {
                runCatching { withTimeoutOrNull(ACK_TIMEOUT_MS) { chat.identity()?.let { chat.ackDelivered(it) } } }
            }
        }
    }

    private companion object {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        const val ACK_TIMEOUT_MS = 6_000L
    }
}
