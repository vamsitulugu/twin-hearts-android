package com.twinhearts.app.core.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.twinhearts.app.R
import com.twinhearts.app.core.notifications.NotificationType
import com.twinhearts.app.core.notifications.TwinNotification
import com.twinhearts.app.core.notifications.TwinNotifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shows the notification when a scheduled reminder time arrives (Calendar and Study alarms).
 * It uses the shared [TwinNotifier], so reminders look like every other Twin Hearts notification
 * and follow the Settings switches and quiet hours. Tapping it opens the right page.
 */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var notifier: TwinNotifier

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { context.getString(R.string.reminders_default_title) }
        val note = intent.getStringExtra(EXTRA_NOTE).orEmpty().ifBlank { context.getString(R.string.reminders_default_body) }
        val id = intent.getStringExtra(EXTRA_ID).orEmpty()
        val pending = goAsync()
        scope.launch {
            try {
                notifier.show(TwinNotification(NotificationType.CALENDAR_REMINDER, title, note, mapOf("id" to id)))
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_NOTE = "note"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
