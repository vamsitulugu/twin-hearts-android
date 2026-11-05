package com.twinhearts.app.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager

/** Creates every notification channel once at app start (safe to call again). */
object NotificationChannels {

    fun ensure(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
                        enableVibration(true)
                        // Long buzz (about 7 seconds) so a Touch is felt even in a pocket.
                        vibrationPattern = TOUCH_VIBRATION
                    }
                    TwinChannel.LOVE -> {
                        enableVibration(true)
                        // Two soft pulses, like a heartbeat.
                        vibrationPattern = longArrayOf(0, 120, 90, 220)
                    }
                    TwinChannel.UPDATES -> enableVibration(false)
                    else -> enableVibration(true)
                }
            }
            manager.createNotificationChannel(channel)
        }
    }

    /** Wait, buzz, pause, buzz ... in milliseconds (the first number is the wait before the first buzz). */
    val TOUCH_VIBRATION: LongArray = longArrayOf(0, 2000, 400, 2000, 400, 2000)

    /** The app's burgundy (same as TwinLook.Accent). */
    const val ACCENT: Int = 0xFFC2185B.toInt()

}