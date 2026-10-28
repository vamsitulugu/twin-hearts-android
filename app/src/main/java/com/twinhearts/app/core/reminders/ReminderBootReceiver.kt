package com.twinhearts.app.core.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Alarms are wiped when the phone restarts or the app updates; this puts the saved ones back. */
class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            ReminderScheduler(context.applicationContext).rescheduleFromStore()
        }
    }
}
