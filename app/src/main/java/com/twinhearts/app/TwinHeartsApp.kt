package com.twinhearts.app

import android.app.Application
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.ServerWarmer
import com.twinhearts.app.core.notifications.AppForeground
import com.twinhearts.app.core.notifications.NotificationChannels
import com.twinhearts.app.core.notifications.PushTokenRegistrar
import com.twinhearts.app.core.presence.AppPresence
import com.twinhearts.app.core.telemetry.AppTelemetry
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application class. Starts Hilt (dependency injection) for the whole app, creates the
 * notification channels, and uploads the push token whenever someone is signed in.
 */
@HiltAndroidApp
class TwinHeartsApp : Application() {

    @Inject lateinit var sessionStore: SessionStore
    @Inject lateinit var pushTokens: PushTokenRegistrar
    @Inject lateinit var serverWarmer: ServerWarmer
    @Inject lateinit var telemetry: AppTelemetry
    @Inject lateinit var appPresence: AppPresence

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(AppForeground)
        // If the app crashes, save a short note (error type + code locations, no personal data)
        // and send it to the admin Health page on the next start.
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            telemetry.saveCrash(error)
            previousHandler?.uncaughtException(thread, error)
        }
        NotificationChannels.ensure(this)
        // Start waking the sleeping server before the first screen asks for anything.
        serverWarmer.warm()
        // Tell the server I am online on every screen (the web shows it as my partner's Online / Away).
        appPresence.start()
        // A new sign-in (or a new person on this phone) re-sends the token to the server.
        scope.launch {
            sessionStore.session
                .map { it?.userId }
                .distinctUntilChanged()
                .collect { userId -> if (userId != null) pushTokens.sync() }
        }
    }
}
