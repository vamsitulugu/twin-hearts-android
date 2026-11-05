package com.twinhearts.app.core.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.twinhearts.app.MainActivity
import com.twinhearts.app.R
import com.twinhearts.app.core.data.SessionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/** One notification to show. Title / body are optional: [NotificationDefaults] fill the gaps. */
data class TwinNotification(
    val type: NotificationType,
    val title: String? = null,
    val body: String? = null,
    val data: Map<String, String> = emptyMap(),
)

/**
 * Why this file exists: the ONE place that draws a Twin Hearts notification. Every feature, the
 * push service and the reminder alarms go through [show], so they all look the same:
 * the app logo (status-bar icon + large icon), the burgundy accent, grouped by feature,
 * with the right buttons (Reply, Answer, Send back …), quiet hours and lock-screen privacy.
 */
// Every notify() below is behind canPost(), which checks the POST_NOTIFICATIONS permission.
@SuppressLint("MissingPermission")
@Singleton
class TwinNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: NotificationPrefs,
    private val sessionStore: SessionStore,
) {
    private val manager = NotificationManagerCompat.from(context)

    private val logo: Bitmap by lazy {
        val raw = BitmapFactory.decodeResource(context.resources, R.drawable.core_logo)
        Bitmap.createScaledBitmap(raw, LOGO_PX, LOGO_PX, true)
    }

    /** Round version of the logo for the sender's picture in chat notifications. */
    private val roundLogo: Bitmap by lazy {
        val out = Bitmap.createBitmap(LOGO_PX, LOGO_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawCircle(LOGO_PX / 2f, LOGO_PX / 2f, LOGO_PX / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        // Logo on a soft dark disc so it reads on any wallpaper.
        canvas.drawColor(0xFF1A1014.toInt(), PorterDuff.Mode.SRC_IN)
        val pad = LOGO_PX * 0.12f
        canvas.drawBitmap(Bitmap.createScaledBitmap(logo, (LOGO_PX - 2 * pad).toInt(), (LOGO_PX - 2 * pad).toInt(), true), pad, pad, Paint(Paint.ANTI_ALIAS_FLAG))
        out
    }

    fun canPost(): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && manager.areNotificationsEnabled()
    }

    /** Shows the notification. Returns false when it was skipped (switched off, app open, no permission). */
    suspend fun show(event: TwinNotification): Boolean {
        val type = event.type
        if (!canPost()) return false
        if (!prefs.isEnabled(type)) return false
        if (AppForeground.isVisible && !type.showInForeground) return false

        NotificationChannels.ensure(context)
        val session = sessionStore.session.first()
        val partner = event.data["fromName"]?.takeIf { it.isNotBlank() }
            ?: session?.partnerName?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.notif_partner_fallback)
        val title = event.title?.takeIf { it.isNotBlank() } ?: titleFor(event, partner)
        val body = event.body?.takeIf { it.isNotBlank() } ?: bodyFor(event, partner)

        val silent = prefs.isQuietNow() && !type.breaksQuietHours
        val showDetails = prefs.previewAllowed() && !type.sensitive
        val id = idFor(event)

        // The partner's profile photo (sent by the server as senderAvatar); falls back to the round logo.
        val avatar = if (type == NotificationType.CHAT_MESSAGE || type == NotificationType.CALL_INCOMING) {
            loadAvatar(event.data["senderAvatar"]) ?: roundLogo
        } else roundLogo

        val builder = base(type, id, event)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSilent(silent)
        // Phones older than Android 8 have no channels, so the long buzz is set on the notification.
        if (type == NotificationType.PARTNER_TOUCH && !silent) builder.setVibrate(NotificationChannels.TOUCH_VIBRATION)
        if (!showDetails) builder.setPublicVersion(publicVersion(type))

        when (type) {
            NotificationType.CHAT_MESSAGE -> applyChat(builder, partner, body, showDetails, avatar)
            NotificationType.CALL_INCOMING -> applyIncomingCall(builder, partner, event, avatar)
            NotificationType.PARTNER_HUG, NotificationType.PARTNER_MISS, NotificationType.PARTNER_TOUCH ->
                builder.addAction(0, context.getString(R.string.notif_action_send_back), actionIntent(NotificationActionReceiver.ACTION_SEND_BACK, id, type))
            NotificationType.LOCATION_SOS -> {
                builder.setPriority(NotificationCompat.PRIORITY_MAX).setOngoing(false)
                    .addAction(0, context.getString(R.string.notif_action_open_map), openIntent(NotifScreen.LIVEMAP, id + 1))
                    .addAction(0, context.getString(R.string.notif_action_call), openIntent(NotifScreen.CALL, id + 2, video = false))
            }
            else -> if (type.channel == TwinChannel.REMINDERS) {
                builder.addAction(0, context.getString(R.string.notif_action_done), actionIntent(NotificationActionReceiver.ACTION_DISMISS, id, type))
            }
        }

        manager.notify(id, builder.build())
        if (type != NotificationType.CHAT_MESSAGE && type != NotificationType.CALL_INCOMING) postSummary(type.channel)
        return true
    }

    /** Removes the chat / call notifications. Called when the person opens the app. */
    fun onAppOpened() {
        manager.cancel(ID_CHAT)
        manager.cancel(ID_CALL)
    }

    fun cancel(id: Int) = manager.cancel(id)

    /** After a reply from the notification: show it in the thread so the spinner goes away. */
    fun showReplySent(text: String) {
        val active = activeNotification(ID_CHAT) ?: return
        // Same look as the notification on screen: copy its channel, group, tap action and buttons.
        val builder = NotificationCompat.Builder(context, NotificationCompat.getChannelId(active) ?: TwinChannel.ACTIVITY.id)
            .setSmallIcon(R.drawable.ic_stat_twin)
            .setColor(NotificationChannels.ACCENT)
            .setLargeIcon(logo)
            .setAutoCancel(true)
            .setContentIntent(active.contentIntent)
            .setGroup(NotificationCompat.getGroup(active))
        for (i in 0 until NotificationCompat.getActionCount(active)) {
            NotificationCompat.getAction(active, i)?.let { builder.addAction(it) }
        }
        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(active) ?: return
        style.addMessage(text, System.currentTimeMillis(), null as Person?)
        builder.setStyle(style).setOnlyAlertOnce(true).setSilent(true)
        manager.notify(ID_CHAT, builder.build())
    }

    // ───────────── building blocks ─────────────

    private fun base(type: NotificationType, id: Int, event: TwinNotification): NotificationCompat.Builder {
        val priority = when (type.channel) {
            TwinChannel.UPDATES -> NotificationCompat.PRIORITY_LOW
            TwinChannel.ACTIVITY -> NotificationCompat.PRIORITY_DEFAULT
            else -> NotificationCompat.PRIORITY_HIGH
        }
        val category = when (type) {
            NotificationType.CHAT_MESSAGE, NotificationType.CHAT_REACTION -> NotificationCompat.CATEGORY_MESSAGE
            NotificationType.CALL_INCOMING -> NotificationCompat.CATEGORY_CALL
            NotificationType.CALL_MISSED -> NotificationCompat.CATEGORY_MISSED_CALL
            NotificationType.LOCATION_SOS -> NotificationCompat.CATEGORY_ALARM
            else -> when (type.channel) {
                TwinChannel.REMINDERS -> NotificationCompat.CATEGORY_REMINDER
                TwinChannel.UPDATES -> NotificationCompat.CATEGORY_STATUS
                else -> NotificationCompat.CATEGORY_SOCIAL
            }
        }
        val screen = if (type == NotificationType.CALL_MISSED) NotifScreen.CHAT else type.screen
        return NotificationCompat.Builder(context, type.channel.id)
            .setSmallIcon(R.drawable.ic_stat_twin)
            .setColor(NotificationChannels.ACCENT)
            .setLargeIcon(logo)
            .setPriority(priority)
            .setCategory(category)
            .setAutoCancel(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setGroup(type.channel.id)
            .setContentIntent(openIntent(screen, id, video = event.data["video"] == "true"))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
    }

    /** Downloads the partner's photo and crops it round. Null (so the logo is used) on any problem. */
    private suspend fun loadAvatar(url: String?): Bitmap? {
        if (url.isNullOrBlank() || !url.startsWith("http")) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4000
                    readTimeout = 4000
                }
                try {
                    val src = conn.inputStream.use { BitmapFactory.decodeStream(it) } ?: return@runCatching null
                    val side = minOf(src.width, src.height)
                    val square = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
                    val scaled = Bitmap.createScaledBitmap(square, LOGO_PX, LOGO_PX, true)
                    val out = Bitmap.createBitmap(LOGO_PX, LOGO_PX, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(out)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                    canvas.drawCircle(LOGO_PX / 2f, LOGO_PX / 2f, LOGO_PX / 2f, paint)
                    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                    canvas.drawBitmap(scaled, 0f, 0f, paint)
                    out
                } finally {
                    conn.disconnect()
                }
            }.getOrNull()
        }
    }

    private fun applyChat(builder: NotificationCompat.Builder, partner: String, text: String, showDetails: Boolean, avatar: Bitmap) {
        val shown = if (showDetails) text else context.getString(R.string.notif_private_body)
        val partnerPerson = Person.Builder().setName(partner).setIcon(IconCompat.createWithBitmap(avatar)).setKey("partner").build()
        val me = Person.Builder().setName(context.getString(R.string.notif_you)).setKey("me").build()
        val existing = activeNotification(ID_CHAT)
            ?.let { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it) }
        val style = existing ?: NotificationCompat.MessagingStyle(me)
        style.addMessage(shown, System.currentTimeMillis(), partnerPerson)
        builder.setStyle(style).setContentTitle(null).setContentText(null).setNumber(style.messages.size)

        val remote = RemoteInput.Builder(NotificationActionReceiver.KEY_REPLY).setLabel(context.getString(R.string.notif_reply_hint)).build()
        val reply = NotificationCompat.Action.Builder(0, context.getString(R.string.notif_action_reply), mutableActionIntent(NotificationActionReceiver.ACTION_REPLY, ID_CHAT))
            .addRemoteInput(remote)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .setShowsUserInterface(false)
            .setAllowGeneratedReplies(true)
            .build()
        val read = NotificationCompat.Action.Builder(0, context.getString(R.string.notif_action_mark_read), actionIntent(NotificationActionReceiver.ACTION_MARK_READ, ID_CHAT, NotificationType.CHAT_MESSAGE))
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
            .setShowsUserInterface(false)
            .build()
        builder.addAction(reply).addAction(read)
    }

    private fun applyIncomingCall(builder: NotificationCompat.Builder, partner: String, event: TwinNotification, avatar: Bitmap) {
        val video = event.data["video"] == "true"
        val answer = openIntent(NotifScreen.CALL, ID_CALL + 1, video = video)
        val decline = actionIntent(NotificationActionReceiver.ACTION_DECLINE_CALL, ID_CALL, NotificationType.CALL_INCOMING, extra = if (video) "video" else "voice")
        builder.setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .setTimeoutAfter(CALL_RING_MS)
            .setFullScreenIntent(answer, true)
            .setColorized(false)
        runCatching {
            val caller = Person.Builder().setName(partner).setIcon(IconCompat.createWithBitmap(avatar)).setImportant(true).build()
            builder.setStyle(NotificationCompat.CallStyle.forIncomingCall(caller, decline, answer))
        }.onFailure {
            builder.addAction(0, context.getString(R.string.notif_action_decline), decline)
                .addAction(0, context.getString(R.string.notif_action_answer), answer)
        }
    }

    /** A short "N new" card that stacks the notifications of one feature together. */
    private fun postSummary(channel: TwinChannel) {
        val children = manager.activeNotifications.filter { it.notification.group == channel.id && it.id != summaryId(channel) }
        if (children.size < 2) return
        val lines = NotificationCompat.InboxStyle()
        children.take(6).forEach { sbn ->
            val extras = sbn.notification.extras
            val line = listOfNotNull(extras.getCharSequence(Notification.EXTRA_TITLE), extras.getCharSequence(Notification.EXTRA_TEXT)).joinToString(" · ")
            if (line.isNotBlank()) lines.addLine(line)
        }
        lines.setSummaryText(context.getString(R.string.notif_summary_count, children.size))
        val summary = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(R.drawable.ic_stat_twin)
            .setColor(NotificationChannels.ACCENT)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.notif_summary_count, children.size))
            .setStyle(lines)
            .setGroup(channel.id)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setContentIntent(openIntent(NotifScreen.HOME, summaryId(channel)))
            .build()
        manager.notify(summaryId(channel), summary)
    }

    /** What the lock screen shows when details are hidden. */
    private fun publicVersion(type: NotificationType): Notification =
        NotificationCompat.Builder(context, type.channel.id)
            .setSmallIcon(R.drawable.ic_stat_twin)
            .setColor(NotificationChannels.ACCENT)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.notif_public_body))
            .build()

    private fun titleFor(event: TwinNotification, partner: String): String {
        val res = NotificationDefaults.title(event.type)
        return context.getString(res, partner)
    }

    private fun bodyFor(event: TwinNotification, partner: String): String {
        if (event.type == NotificationType.CALL_INCOMING && event.data["video"] == "true") {
            return context.getString(R.string.notif_call_video_body)
        }
        return context.getString(NotificationDefaults.body(event.type), partner)
    }

    private fun idFor(event: TwinNotification): Int = when (event.type) {
        NotificationType.CHAT_MESSAGE -> ID_CHAT
        NotificationType.CALL_INCOMING -> ID_CALL
        else -> (event.type.key + (event.data["id"] ?: "")).hashCode().let { if (it in 0..SUMMARY_BASE) it + SUMMARY_BASE else it }
    }

    private fun summaryId(channel: TwinChannel): Int = ("summary_" + channel.id).hashCode()

    private fun activeNotification(id: Int): Notification? =
        manager.activeNotifications.firstOrNull { it.id == id }?.notification

    // ───────────── intents ─────────────

    private fun openIntent(screen: NotifScreen, requestCode: Int, video: Boolean = false): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .setAction("twin.open.$requestCode") // a different action keeps each PendingIntent separate
            .putExtra(NotificationTarget.EXTRA_SCREEN, screen.name)
            .putExtra(NotificationTarget.EXTRA_VIDEO, video)
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun actionIntent(action: String, id: Int, type: NotificationType, extra: String? = null): PendingIntent =
        PendingIntent.getBroadcast(context, id + action.hashCode(), receiverIntent(action, id, type, extra), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    /** Reply needs a mutable PendingIntent so Android can add the typed text. */
    private fun mutableActionIntent(action: String, id: Int): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        return PendingIntent.getBroadcast(context, id + action.hashCode(), receiverIntent(action, id, NotificationType.CHAT_MESSAGE, null), flags)
    }

    private fun receiverIntent(action: String, id: Int, type: NotificationType, extra: String?) =
        Intent(context, NotificationActionReceiver::class.java)
            .setAction(action)
            .putExtra(NotificationActionReceiver.EXTRA_ID, id)
            .putExtra(NotificationActionReceiver.EXTRA_TYPE, type.key)
            .putExtra(NotificationActionReceiver.EXTRA_EXTRA, extra)

    companion object {
        const val ID_CHAT = 1001
        const val ID_CALL = 1002
        private const val SUMMARY_BASE = 100_000
        private const val CALL_RING_MS = 45_000L
        private const val LOGO_PX = 192
    }
}