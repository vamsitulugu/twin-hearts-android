package com.twinhearts.app.feature.music.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Why this file exists: Android only lets music keep playing with the screen off if a foreground
 * service holds it. This service wraps the app's single player in a media session, which gives the
 * lock-screen and notification controls (play, pause, next, previous, seek) for free.
 */
@AndroidEntryPoint
class MusicPlaybackService : MediaSessionService() {

    @Inject lateinit var controller: MusicPlayerController

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val builder = MediaSession.Builder(this, controller.player)
        // Tapping the notification brings the app back.
        packageManager.getLaunchIntentForPackage(packageName)?.let { launch ->
            builder.setSessionActivity(
                PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
            )
        }
        session = builder.build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /** Swiping the app away stops the service unless music is really playing. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        // The player itself belongs to MusicPlayerController, so only the session is released.
        session?.release()
        session = null
        super.onDestroy()
    }
}
