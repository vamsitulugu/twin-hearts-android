package com.twinhearts.app.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What the app currently knows about the connection, readable from anywhere (also from places that
 * cannot be injected, like [RetryInterceptor]). Kept up to date by [ConnectivityObserver].
 */
object NetworkState {
    /** `true` while the phone has a network that reaches the internet. Starts optimistic. */
    @Volatile var online: Boolean = true
        internal set
}

/**
 * Why this file exists: one place that always knows if the phone has internet, for the whole app.
 * It drives the "You're offline" strip, lets the retry code stop waiting when there is clearly no
 * network, and tells the chat to send waiting messages the moment the connection returns.
 *
 * Uses only ACCESS_NETWORK_STATE, which the app already has. It starts when the app starts and
 * lives as long as the app does (one small system callback).
 */
@Singleton
class ConnectivityObserver @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val manager: ConnectivityManager? = context.getSystemService(ConnectivityManager::class.java)

    private val _online = MutableStateFlow(checkNow())

    /** `true` while the phone has a network that reaches the internet. */
    val online: StateFlow<Boolean> = _online.asStateFlow()

    init {
        NetworkState.online = _online.value
        val m = manager
        if (m != null) {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = publish(true)
                override fun onLost(network: Network) = publish(checkNow())
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) =
                    publish(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            }
            runCatching { m.registerDefaultNetworkCallback(callback) }
        }
    }

    private fun publish(value: Boolean) {
        NetworkState.online = value
        _online.value = value
    }

    private fun checkNow(): Boolean {
        val m = manager ?: return true
        val caps = m.getNetworkCapabilities(m.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
