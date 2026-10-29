package com.twinhearts.app.feature.movie.data.upload

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.twinhearts.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Why this file exists: keeps a big cloud upload alive when the app is closed or the screen is off.
 * Android needs a visible notification for that (a "foreground service"), so the person sees the
 * progress and can cancel from it. The upload itself still runs in [CloudUploader]; this only
 * shows its progress and stops itself when the upload ends.
 */
@AndroidEntryPoint
class UploadService : Service() {

    @Inject lateinit var uploader: CloudUploader

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var watching: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) uploader.cancel()
        createChannel()
        // Must be called at once after startForegroundService, even if the upload already ended.
        val first = uploader.state.value
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, progressNotification(first as? UploadState.Running),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0,
        )
        if (watching == null) {
            watching = scope.launch {
                uploader.state.collect { state ->
                    when (state) {
                        is UploadState.Running -> notify(progressNotification(state), NOTIFICATION_ID)
                        is UploadState.Done -> finish(getString(R.string.movie_notif_done, state.title))
                        is UploadState.Failed -> finish(getString(R.string.movie_notif_failed))
                        UploadState.Idle -> finish(null)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    /** Ends the service and, when there is a result to tell, leaves one normal notification behind. */
    private fun finish(message: String?) {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        if (message != null) notify(resultNotification(message), RESULT_ID)
        stopSelf()
    }

    private fun notify(notification: Notification, id: Int) {
        getSystemService(NotificationManager::class.java)?.notify(id, notification)
    }

    private fun progressNotification(state: UploadState.Running?): Notification {
        val title = getString(R.string.movie_notif_uploading, state?.title.orEmpty())
        val percent = if (state != null && state.totalBytes > 0) (state.sentBytes * PERCENT / state.totalBytes).toInt() else 0
        val cancel = PendingIntent.getService(
            this, 0, Intent(this, UploadService::class.java).setAction(ACTION_CANCEL),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(title)
            .setProgress(PERCENT.toInt(), percent, state == null || state.sentBytes == 0L)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp())
            .addAction(0, getString(R.string.movie_notif_cancel), cancel)
            .build()
    }

    private fun resultNotification(message: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle(message)
            .setAutoCancel(true)
            .setContentIntent(openApp())
            .build()

    private fun openApp(): PendingIntent {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        return PendingIntent.getActivity(this, 1, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.movie_notif_channel), NotificationManager.IMPORTANCE_LOW),
        )
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_CANCEL = "com.twinhearts.app.movie.UPLOAD_CANCEL"
        private const val CHANNEL_ID = "movie_upload"
        private const val NOTIFICATION_ID = 7301
        private const val RESULT_ID = 7302
        private const val PERCENT = 100L
    }
}
