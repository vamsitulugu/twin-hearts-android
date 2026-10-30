package com.twinhearts.app.feature.calls.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.twinhearts.app.MainActivity
import com.twinhearts.app.R
import com.twinhearts.app.feature.calls.domain.CallPhase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Keeps a call alive when the app goes to the background or the screen turns off. Android only lets
 * an app keep using the microphone (and camera) in the background while it shows a notification like
 * this one. It does no call work itself: [CallEngine] does, and starts and stops this service.
 */
@AndroidEntryPoint
class CallService : Service() {

    @Inject lateinit var engine: CallEngine

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watching = false
    private var lastVideo: Boolean? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_HANGUP) {
            engine.hangUp()
            return START_NOT_STICKY
        }
        createChannel()
        publish(engine.state.value.video, engine.state.value.partnerName)
        if (!watching) {
            watching = true
            scope.launch {
                engine.state.collect { s ->
                    val active = s.phase == CallPhase.Calling || s.phase == CallPhase.Connecting || s.phase == CallPhase.Connected
                    if (!active) {
                        ServiceCompat.stopForeground(this@CallService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    } else if (lastVideo != s.video) {
                        // A voice call became a video call: the service must now also declare the camera.
                        publish(s.video, s.partnerName)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun publish(video: Boolean, partner: String) {
        lastVideo = video
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (video) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            else ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, build(partner), type)
        } catch (e: Exception) {
            // Android refused (the app was not allowed to start it right now). The call itself still runs while the app is open.
            stopSelf()
        }
    }

    private fun build(partner: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val hangUp = PendingIntent.getService(
            this, 3, Intent(this, CallService::class.java).setAction(ACTION_HANGUP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle(getString(R.string.calls_notification_title, partner))
            .setContentText(getString(R.string.calls_notification_text))
            .setContentIntent(open)
            .addAction(0, getString(R.string.calls_notification_hangup), hangUp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .build()
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.calls_notification_channel), NotificationManager.IMPORTANCE_LOW),
        )
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val ACTION_HANGUP = "com.twinhearts.app.calls.HANGUP"
        private const val CHANNEL_ID = "calls_ongoing"
        private const val NOTIFICATION_ID = 4103

        fun start(context: Context) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, CallService::class.java)) }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CallService::class.java))
        }
    }
}
