package com.twinhearts.app.core.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rings the phone for shared reminders even when the app is closed.
 * The whole schedule is replaced on every [sync], so edits and deletes on either phone are picked
 * up the next time the Calendar or Study page loads. The list is kept in SharedPreferences so
 * [rescheduleFromStore] can restore it after a reboot (alarms are cleared when the phone restarts).
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Replaces everything scheduled with [alarms]. */
    @Synchronized
    fun sync(alarms: List<PhoneAlarm>) {
        loadStored().forEach { cancel(it.id) }
        val now = System.currentTimeMillis()
        val future = alarms.filter { it.triggerAtMillis > now }
        future.forEach { schedule(it) }
        store(future)
    }

    /** Called after a reboot or an app update. */
    @Synchronized
    fun rescheduleFromStore() {
        val now = System.currentTimeMillis()
        val future = loadStored().filter { it.triggerAtMillis > now }
        future.forEach { schedule(it) }
        store(future)
    }

    private fun schedule(alarm: PhoneAlarm) {
        // Inexact-but-on-time alarm that also fires in Doze: needs no special permission.
        runCatching {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.triggerAtMillis, pending(alarm.id, alarm.title, alarm.note))
        }
    }

    private fun cancel(id: String) {
        alarmManager.cancel(pending(id, "", ""))
    }

    private fun pending(id: String, title: String, note: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(ReminderReceiver.EXTRA_ID, id)
            .putExtra(ReminderReceiver.EXTRA_TITLE, title)
            .putExtra(ReminderReceiver.EXTRA_NOTE, note)
        return PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun store(alarms: List<PhoneAlarm>) {
        val array = JSONArray()
        alarms.forEach {
            array.put(
                JSONObject().put("id", it.id).put("title", it.title).put("note", it.note).put("at", it.triggerAtMillis),
            )
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    private fun loadStored(): List<PhoneAlarm> = runCatching {
        val array = JSONArray(prefs.getString(KEY, "[]"))
        (0 until array.length()).map {
            val o = array.getJSONObject(it)
            PhoneAlarm(o.getString("id"), o.optString("title"), o.optString("note"), o.getLong("at"))
        }
    }.getOrDefault(emptyList())

    private companion object {
        const val PREFS = "twin_reminders"
        const val KEY = "scheduled"
    }
}
