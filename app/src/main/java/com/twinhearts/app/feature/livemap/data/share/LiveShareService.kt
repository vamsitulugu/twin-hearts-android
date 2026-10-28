package com.twinhearts.app.feature.livemap.data.share

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.twinhearts.app.MainActivity
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.livemap.data.LiveMapPrefs
import com.twinhearts.app.feature.livemap.data.LiveMapRepository
import com.twinhearts.app.feature.livemap.data.LocationSource
import com.twinhearts.app.feature.livemap.domain.model.DeviceFix
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Keeps sharing the phone's location while the app is closed or the screen is off.
 * Android requires a visible notification for this (a "foreground service"), so the person always
 * sees that sharing is on and can stop it from the notification.
 *
 * What it does, the same as the web app: a ping about every 8 seconds (while not paused or
 * invisible), points saved while offline are sent in one batch when the internet is back, and the
 * partner gets a short heads-up for low battery, lost internet or GPS/permission turned off
 * (only when those switches are on in Location Alerts).
 */
@AndroidEntryPoint
class LiveShareService : Service() {

    @Inject lateinit var locationSource: LocationSource
    @Inject lateinit var repository: LiveMapRepository
    @Inject lateinit var prefs: LiveMapPrefs

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var sharingJob: Job? = null
    private var healthJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            scope.launch {
                prefs.setSharing(false)
                repository.stopSharing()
                shutDown()
            }
            return START_NOT_STICKY
        }
        createChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
        )
        if (sharingJob == null) {
            sharingJob = scope.launch { shareLoop() }
            healthJob = scope.launch { healthLoop() }
        }
        // Not sticky: Android 12+ will not let a killed service restart itself and use location
        // from the background. The Map screen starts it again the next time the app opens.
        return START_NOT_STICKY
    }

    private suspend fun shareLoop() {
        val offlineQueue = ArrayDeque<DeviceFix>()
        var lastPingAt = 0L
        var lostInternet = false
        locationSource.updates(PING_INTERVAL_MS).collect { fix ->
            val settings = prefs.snapshot()
            if (settings.sharing != true) {
                shutDown()
                return@collect
            }
            val now = System.currentTimeMillis()
            // Invisible mode must stay undetectable, so it just stops pinging without telling the server.
            if (settings.invisible || settings.isPaused || now - lastPingAt < MIN_GAP_MS) return@collect
            lastPingAt = now

            val shared = if (settings.approximate) fix.roundedToAbout1Km() else fix
            val result = repository.ping(shared, settings.vehicle, emergency = false)
            if (result is AppResult.Success) {
                if (offlineQueue.isNotEmpty()) {
                    val batch = offlineQueue.toList()
                    if (repository.sendBatch(batch) is AppResult.Success) offlineQueue.clear()
                }
                if (lostInternet) {
                    lostInternet = false
                    if (settings.tellNetwork) repository.safetyAlert(ALERT_INTERNET)
                }
            } else if (result is AppResult.Failure && result.error == AppError.Network) {
                lostInternet = true
                offlineQueue.addLast(shared)
                while (offlineQueue.size > MAX_OFFLINE_POINTS) offlineQueue.removeFirst()
            }
        }
    }

    /** Every 30 seconds: low battery, location switch off, permission taken away. One alert per problem. */
    private suspend fun healthLoop() {
        var batteryLatched = false
        var gpsLatched = false
        var permissionLatched = false
        while (true) {
            delay(HEALTH_CHECK_MS)
            val settings = prefs.snapshot()
            if (settings.sharing != true) return
            val level = batteryPercent()
            if (level in 0..LOW_BATTERY_PERCENT) {
                if (!batteryLatched && settings.tellBattery) repository.safetyAlert(ALERT_BATTERY)
                batteryLatched = true
            } else {
                batteryLatched = false
            }
            if (!locationSource.hasPermission()) {
                if (!permissionLatched && settings.tellGps) repository.safetyAlert(ALERT_PERMISSION)
                permissionLatched = true
            } else {
                permissionLatched = false
                if (!locationSource.isLocationEnabled()) {
                    if (!gpsLatched && settings.tellGps) repository.safetyAlert(ALERT_GPS)
                    gpsLatched = true
                } else {
                    gpsLatched = false
                }
            }
        }
    }

    private fun batteryPercent(): Int {
        val manager = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return -1
        return manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun shutDown() {
        sharingJob?.cancel()
        healthJob?.cancel()
        sharingJob = null
        healthJob = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.livemap_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = getString(R.string.livemap_channel_description) }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, LiveShareService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(getString(R.string.livemap_notification_title))
            .setContentText(getString(R.string.livemap_notification_text))
            .setContentIntent(open)
            .addAction(0, getString(R.string.livemap_notification_stop), stop)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val ACTION_STOP = "com.twinhearts.app.livemap.STOP"
        private const val CHANNEL_ID = "livemap_sharing"
        private const val NOTIFICATION_ID = 4101
        private const val PING_INTERVAL_MS = 8_000L
        private const val MIN_GAP_MS = 6_000L
        private const val HEALTH_CHECK_MS = 30_000L
        private const val LOW_BATTERY_PERCENT = 15
        private const val MAX_OFFLINE_POINTS = 500
        private const val ALERT_BATTERY = "battery_low"
        private const val ALERT_GPS = "gps_disabled"
        private const val ALERT_PERMISSION = "permission_revoked"
        private const val ALERT_INTERNET = "internet_lost"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, LiveShareService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LiveShareService::class.java))
        }
    }
}

/** Two decimals of latitude/longitude is about 1 km. The accuracy is set to match, so the map shows a wide circle. */
private fun DeviceFix.roundedToAbout1Km(): DeviceFix = copy(
    lat = Math.round(lat * 100.0) / 100.0,
    lng = Math.round(lng * 100.0) / 100.0,
    accuracyM = 1000.0,
    altitudeM = null,
    headingDeg = null,
)
