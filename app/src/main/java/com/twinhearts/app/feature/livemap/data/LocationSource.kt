package com.twinhearts.app.feature.livemap.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import com.twinhearts.app.feature.livemap.domain.model.DeviceFix
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The phone's location as a Flow. It uses Android's own LocationManager (GPS + network), so no
 * Google Play Services library or billing account is needed (R-21). Callers must hold the
 * location permission; without it the Flow simply ends.
 */
@Singleton
class LocationSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager: LocationManager? = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** True when the GPS or network location switch in the phone settings is on. */
    fun isLocationEnabled(): Boolean {
        val m = manager ?: return false
        return runCatching {
            m.isProviderEnabled(LocationManager.GPS_PROVIDER) || m.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }.getOrDefault(false)
    }

    /** A fresh stream of fixes about every [intervalMs]. Starts with the last known fix when there is one. */
    fun updates(intervalMs: Long): Flow<DeviceFix> = callbackFlow {
        val m = manager
        if (m == null || !hasPermission()) {
            close()
            return@callbackFlow
        }
        val listener = LocationListener { location -> trySend(location.toFix()) }
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        var started = false
        providers.forEach { provider ->
            try {
                if (m.isProviderEnabled(provider)) {
                    m.getLastKnownLocation(provider)?.let { trySend(it.toFix()) }
                    m.requestLocationUpdates(provider, intervalMs, 0f, listener, Looper.getMainLooper())
                    started = true
                }
            } catch (e: SecurityException) {
                // Permission was taken away while we were starting: end quietly, the caller notices.
            } catch (e: IllegalArgumentException) {
                // This phone has no such provider.
            }
        }
        if (!started) close()
        awaitClose { m.removeUpdates(listener) }
    }

    /** One fix, or null when none arrives within [timeoutMs]. Used by "Locate me" and Emergency share. */
    suspend fun currentFix(timeoutMs: Long = 15_000L): DeviceFix? =
        withTimeoutOrNull(timeoutMs) { updates(1_000L).first() }

    private fun Location.toFix() = DeviceFix(
        lat = latitude,
        lng = longitude,
        accuracyM = if (hasAccuracy()) accuracy.toDouble() else null,
        headingDeg = if (hasBearing()) bearing.toDouble() else null,
        speedMps = if (hasSpeed()) speed.toDouble() else null,
        altitudeM = if (hasAltitude()) altitude else null,
        mock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) isMock else @Suppress("DEPRECATION") isFromMockProvider,
        atMs = time,
    )
}
