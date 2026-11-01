package com.twinhearts.app.core.network

import com.twinhearts.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wakes the free Render server early. It sleeps after ~15 minutes without traffic and the first
 * real request then waits up to a minute. Pinging /api/health the moment the app opens means the
 * server is already waking while the screen paints its saved copy.
 *
 * Fire-and-forget: the result is ignored, and it never pings more than once every 4 minutes.
 */
@Singleton
class ServerWarmer @Inject constructor(client: OkHttpClient) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pinger = client.newBuilder()
        .callTimeout(70, TimeUnit.SECONDS)
        .readTimeout(70, TimeUnit.SECONDS)
        .build()

    @Volatile private var lastPingMillis = 0L

    fun warm() {
        val now = System.currentTimeMillis()
        if (now - lastPingMillis < MIN_GAP_MILLIS) return
        lastPingMillis = now
        scope.launch {
            val base = BuildConfig.API_BASE_URL.let { if (it.endsWith("/")) it else "$it/" }
            runCatching {
                pinger.newCall(Request.Builder().url(base + "api/health").get().build()).execute().close()
            }
        }
    }

    private companion object {
        const val MIN_GAP_MILLIS = 4 * 60 * 1000L
    }
}
