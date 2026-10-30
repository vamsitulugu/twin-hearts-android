package com.twinhearts.app.feature.livemap.data.nav

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
import com.twinhearts.app.feature.livemap.domain.nav.NavSession
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Keeps navigation alive with the screen off or another app in front. Android needs a visible
 * notification for that; ours shows the next turn and has an "End" button. All the real work is in [NavEngine].
 */
@AndroidEntryPoint
class NavigationService : Service() {

    @Inject lateinit var engine: NavEngine

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watching = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_END) {
            engine.stop()
            return START_NOT_STICKY
        }
        createChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            build(engine.session.value),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
        )
        if (!watching) {
            watching = true
            scope.launch {
                engine.session.collect { s ->
                    if (s == null) {
                        ServiceCompat.stopForeground(this@NavigationService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    } else {
                        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, build(s))
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun build(s: NavSession?): Notification {
        val phrases = NavPhrases(resources)
        val progress = s?.progress
        val next = progress?.next
        val title = when {
            s == null -> getString(R.string.livemap_nav_notification_title)
            progress?.status == com.twinhearts.app.feature.livemap.domain.nav.NavStatus.Arrived -> getString(R.string.livemap_nav_arrived_title)
            next != null && progress != null ->
                getString(R.string.livemap_nav_notification_next, phrases.distance(progress.distanceToNextM), phrases.instruction(next.kind, next.road, next.exit, next.side))
            else -> getString(R.string.livemap_nav_notification_title)
        }
        val text = s?.let { getString(R.string.livemap_nav_notification_to, it.destinationName.ifBlank { getString(R.string.livemap_dropped_pin) }) }
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val end = PendingIntent.getService(
            this, 2, Intent(this, NavigationService::class.java).setAction(ACTION_END),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .addAction(0, getString(R.string.livemap_nav_end), end)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .build()
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.livemap_nav_channel_name), NotificationManager.IMPORTANCE_LOW),
        )
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val ACTION_END = "com.twinhearts.app.livemap.NAV_END"
        private const val CHANNEL_ID = "livemap_navigation"
        private const val NOTIFICATION_ID = 4102

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, NavigationService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, NavigationService::class.java))
        }
    }
}
