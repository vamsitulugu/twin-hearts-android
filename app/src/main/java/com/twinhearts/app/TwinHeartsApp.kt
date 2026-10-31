package com.twinhearts.app

import android.app.Application
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.notifications.AppForeground
import com.twinhearts.app.core.notifications.NotificationChannels
import com.twinhearts.app.core.notifications.PushTokenRegistrar
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(AppForeground)
        NotificationChannels.ensure(this)
        // A new sign-in (or a new person on this phone) re-sends the token to the server.
        scope.launch {
            sessionStore.session
                .map { it?.userId }
                .distinctUntilChanged()
                .collect { userId -> if (userId != null) pushTokens.sync() }
        }
    }
}
