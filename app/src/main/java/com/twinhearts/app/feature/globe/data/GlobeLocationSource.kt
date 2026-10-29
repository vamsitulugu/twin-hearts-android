package com.twinhearts.app.feature.globe.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/** One reading of the phone's position. */
data class DeviceFix(
    val lat: Double,
    val lng: Double,
    val accuracyM: Double?,
    val headingDeg: Double?,
    val speedMps: Double?,
)

/**
 * Why this file exists: the Globe's "Live Location" button needs the phone's position while the page is open.
 * It uses Android's own LocationManager, so no Google library or billing is needed (R-21).
 * Without the permission the Flow simply ends.
 */
@Singleton
class GlobeLocationSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager: LocationManager? = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Fixes about every [intervalMs], starting with the last known one. */
    fun updates(intervalMs: Long): Flow<DeviceFix> = callbackFlow {
        val m = manager
        if (m == null || !hasPermission()) {
            close()
            return@callbackFlow
        }
        val listener = LocationListener { location -> trySend(location.toFix()) }
        var started = false
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
            try {
                if (m.isProviderEnabled(provider)) {
                    m.getLastKnownLocation(provider)?.let { trySend(it.toFix()) }
                    m.requestLocationUpdates(provider, intervalMs, 0f, listener, Looper.getMainLooper())
                    started = true
                }
            } catch (e: SecurityException) {
                // Permission was withdrawn while running: end quietly.
            } catch (e: IllegalArgumentException) {
                // This device has no such provider.
            }
        }
        if (!started) close()
        awaitClose { m.removeUpdates(listener) }
    }

    private fun Location.toFix() = DeviceFix(
        lat = latitude,
        lng = longitude,
        accuracyM = if (hasAccuracy()) accuracy.toDouble() else null,
        headingDeg = if (hasBearing()) bearing.toDouble() else null,
        speedMps = if (hasSpeed()) speed.toDouble() else null,
    )
}
