package com.twinhearts.app.feature.places.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.util.Consumer
import com.twinhearts.app.feature.places.domain.model.GeoPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Why this file exists: "Use My Current Location" on the Add Place form needs one position,
 * once. It uses the phone's own LocationManager (no extra library). Only asked after the
 * person tapped the button and allowed the permission; nothing is tracked or stored.
 */
@Singleton
class LocationFinder @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** A fresh fix if the phone can give one in a few seconds, otherwise the last known place. Null if none. */
    @SuppressLint("MissingPermission")
    suspend fun current(): GeoPoint? {
        if (!hasPermission()) return null
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val providers = manager.getProviders(true).filter { it != LocationManager.PASSIVE_PROVIDER }
        for (provider in providers) {
            val fix = withTimeoutOrNull(FIX_TIMEOUT_MS) { freshFix(manager, provider) }
            if (fix != null) return GeoPoint(fix.latitude, fix.longitude)
        }
        val last = providers.mapNotNull { manager.getLastKnownLocation(it) }.maxByOrNull { it.time }
        return last?.let { GeoPoint(it.latitude, it.longitude) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun freshFix(manager: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { cont ->
            val signal = CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            LocationManagerCompat.getCurrentLocation(
                manager,
                provider,
                signal,
                ContextCompat.getMainExecutor(context),
                Consumer<Location> { location -> if (cont.isActive) cont.resume(location) },
            )
        }

    private companion object {
        const val FIX_TIMEOUT_MS = 8_000L
    }
}
