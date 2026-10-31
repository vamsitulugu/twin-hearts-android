package com.twinhearts.app.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.feature.calls.data.remote.CallApi
import com.twinhearts.app.feature.calls.data.remote.CallLogBody
import com.twinhearts.app.feature.chat.data.remote.ChatApi
import com.twinhearts.app.feature.chat.data.remote.RoleBody
import com.twinhearts.app.feature.chat.data.remote.SendMessageBody
import com.twinhearts.app.feature.home.data.HomeRepository
import com.twinhearts.app.feature.home.domain.model.LoveKind
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Buttons on a notification that work without opening the app:
 * Reply and Mark as read (chat), Decline (call), Send back (hug / miss you / touch), Done.
 */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var notifier: TwinNotifier
    @Inject lateinit var homeRepository: HomeRepository
    @Inject lateinit var sessionStore: SessionStore
    @Inject lateinit var chatApi: ChatApi
    @Inject lateinit var callApi: CallApi

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_ID, 0)
        val type = NotificationType.fromKey(intent.getStringExtra(EXTRA_TYPE))
        val action = intent.action ?: return
        val pending = goAsync()
        scope.launch {
            try {
                when (action) {
                    ACTION_REPLY -> reply(intent)
                    ACTION_MARK_READ -> {
                        val session = sessionStore.session.first()
                        if (session != null) runCatching { chatApi.markRead(session.coupleId, RoleBody(session.role)) }
                        notifier.cancel(id)
                    }
                    ACTION_DECLINE_CALL -> {
                        notifier.cancel(id)
                        val session = sessionStore.session.first()
                        if (session != null) {
                            val caller = if (session.role == "user1") "user2" else "user1"
                            val kind = intent.getStringExtra(EXTRA_EXTRA) ?: "voice"
                            runCatching { callApi.log(CallLogBody(session.coupleId, caller, kind, "declined", 0)) }
                        }
                    }
                    ACTION_SEND_BACK -> {
                        val kind = when (type) {
                            NotificationType.PARTNER_HUG -> LoveKind.Hug
                            NotificationType.PARTNER_TOUCH -> LoveKind.Touch
                            NotificationType.PARTNER_MISS -> LoveKind.MissYou
                            else -> null
                        }
                        if (kind != null) runCatching { homeRepository.send(kind) }
                        notifier.cancel(id)
                    }
                    ACTION_DISMISS -> notifier.cancel(id)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun reply(intent: Intent) {
        val text = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(KEY_REPLY)?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return
        val session = sessionStore.session.first() ?: return
        runCatching {
            chatApi.send(SendMessageBody(session.coupleId, UUID.randomUUID().toString(), session.role, type = "text", text = text))
        }
        notifier.showReplySent(text)
    }

    companion object {
        const val ACTION_REPLY = "com.twinhearts.app.notif.REPLY"
        const val ACTION_MARK_READ = "com.twinhearts.app.notif.MARK_READ"
        const val ACTION_DECLINE_CALL = "com.twinhearts.app.notif.DECLINE_CALL"
        const val ACTION_SEND_BACK = "com.twinhearts.app.notif.SEND_BACK"
        const val ACTION_DISMISS = "com.twinhearts.app.notif.DISMISS"
        const val KEY_REPLY = "twin_reply_text"
        const val EXTRA_ID = "notif_id"
        const val EXTRA_TYPE = "notif_type"
        const val EXTRA_EXTRA = "notif_extra"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
