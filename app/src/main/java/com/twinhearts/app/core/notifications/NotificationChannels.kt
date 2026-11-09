package com.twinhearts.app.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.provider.Settings

/** Creates every notification channel once at app start (safe to call again). */
object NotificationChannels {

    fun ensure(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // The first Touch channel kept its old (shorter) buzz forever, so it is replaced by v2.
        OLD_CHANNEL_IDS.forEach { manager.deleteNotificationChannel(it) }
        TwinChannel.entries.forEach { ch ->
            val channel = NotificationChannel(ch.id, context.getString(ch.title), ch.importance).apply {
                description = context.getString(ch.description)
                lightColor = ACCENT
                enableLights(ch.importance >= NotificationManager.IMPORTANCE_DEFAULT)
                setShowBadge(ch != TwinChannel.UPDATES)
                when (ch) {
                    TwinChannel.CALLS -> {
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 700, 500, 700, 500, 700)
                        setSound(
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build(),
                        )
                    }
                    TwinChannel.TOUCH -> {
                        setSound(Settings.System.DEFAULT_NOTIFICATION_URI, NOTIFICATION_AUDIO)
                        enableVibration(true)
                        // Long buzz (about 10 seconds, same as the web app) so a Touch is felt even in a pocket.
                        vibrationPattern = TOUCH_VIBRATION
                    }
                    TwinChannel.LOVE -> {
                        setSound(Settings.System.DEFAULT_NOTIFICATION_URI, NOTIFICATION_AUDIO)
                        enableVibration(true)
                        // Two soft pulses, like a heartbeat.
                        vibrationPattern = longArrayOf(0, 120, 90, 220)
                    }
                    TwinChannel.UPDATES -> {
                        setSound(Settings.System.DEFAULT_NOTIFICATION_URI, NOTIFICATION_AUDIO)
                        enableVibration(false)
                    }
                    else -> {
                        // The phone's own default notification sound for everybody.
                        setSound(Settings.System.DEFAULT_NOTIFICATION_URI, NOTIFICATION_AUDIO)
                        enableVibration(true)
                    }
                }
            }
            manager.createNotificationChannel(channel)
        }
    }

    /** Wait, buzz, pause, buzz ... in milliseconds (the first number is the wait before the first buzz). */
    val TOUCH_VIBRATION: LongArray = longArrayOf(0, 3000, 400, 3000, 400, 3000)

    /** Earlier channel ids. Their sound could not be changed, so they are removed and v3 replaces them. */
    private val OLD_CHANNEL_IDS = listOf(
        "twin_touch", "twin_touch_v2", "twin_messages", "twin_calls", "twin_love",
        "twin_location", "twin_reminders", "twin_activity", "twin_updates",
    )

    private val NOTIFICATION_AUDIO: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    /** The app's burgundy (same as TwinLook.Accent). */
    const val ACCENT: Int = 0xFFC2185B.toInt()

}