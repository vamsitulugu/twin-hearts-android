package com.twinhearts.app.feature.livemap.data.nav

import android.content.Context
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.livemap.data.LiveMapRepository
import com.twinhearts.app.feature.livemap.data.LocationSource
import com.twinhearts.app.feature.livemap.domain.nav.GeoPoint
import com.twinhearts.app.feature.livemap.domain.nav.NavFix
import com.twinhearts.app.feature.livemap.domain.nav.NavRoute
import com.twinhearts.app.feature.livemap.domain.nav.NavSession
import com.twinhearts.app.feature.livemap.domain.nav.NavStatus
import com.twinhearts.app.feature.livemap.domain.nav.NavTracker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs one trip of turn-by-turn navigation. It lives for the whole app (not the screen), so the trip
 * keeps going while the phone is locked: [NavigationService] holds the notification that keeps the
 * process alive. The screen only watches [session] and calls the small functions below.
 */
@Singleton
class NavEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationSource: LocationSource,
    private val repository: LiveMapRepository,
    private val voice: VoiceGuide,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val phrases by lazy { NavPhrases(context.resources) }

    private val _session = MutableStateFlow<NavSession?>(null)
    val session: StateFlow<NavSession?> = _session.asStateFlow()

    private var tracker: NavTracker? = null
    private var tripJob: Job? = null
    private var watchdogJob: Job? = null
    private var rerouteJob: Job? = null
    private var lastFixAt = 0L
    private var lastFix: NavFix? = null
    private var lastRerouteAt = 0L

    /** Begins a trip along [route]. A trip already running is replaced. */
    fun start(route: NavRoute, destinationName: String, destination: GeoPoint) {
        stopInternal(keepService = true)
        tracker = NavTracker(route)
        lastFixAt = System.currentTimeMillis()
        _session.value = NavSession(
            route = route,
            destinationName = destinationName,
            destination = destination,
            startedAtMs = System.currentTimeMillis(),
        )
        NavigationService.start(context)
        tripJob = scope.launch {
            locationSource.updates(FIX_INTERVAL_MS).collect { fix -> onFix(fix.toNavFix()) }
        }
        watchdogJob = scope.launch {
            while (true) {
                delay(WATCHDOG_MS)
                val lost = System.currentTimeMillis() - lastFixAt > GPS_LOST_MS
                _session.update { s -> if (s != null && s.gpsLost != lost) s.copy(gpsLost = lost) else s }
            }
        }
    }

    /** Ends the trip: stops voice, location and the notification. */
    fun stop() = stopInternal(keepService = false)

    private fun stopInternal(keepService: Boolean) {
        tripJob?.cancel()
        watchdogJob?.cancel()
        rerouteJob?.cancel()
        tripJob = null
        watchdogJob = null
        rerouteJob = null
        tracker = null
        lastFix = null
        voice.release()
        _session.value = null
        if (!keepService) NavigationService.stop(context)
    }

    fun setMuted(muted: Boolean) {
        if (muted) voice.stopSpeaking()
        _session.update { it?.copy(muted = muted) }
    }

    fun setFollowing(following: Boolean) = _session.update { it?.copy(following = following) }

    private fun onFix(fix: NavFix) {
        val t = tracker ?: return
        lastFixAt = System.currentTimeMillis()
        lastFix = fix
        val progress = t.update(fix)
        _session.update { it?.copy(progress = progress, gpsLost = false) }
        val muted = _session.value?.muted == true
        progress.cue?.let { cue -> if (!muted) voice.speak(phrases.spoken(cue)) }
        if (progress.status == NavStatus.OffRoute) maybeReroute(fix)
        // Arrived: no more GPS needed. The trip card stays until the person taps Done.
        if (progress.status == NavStatus.Arrived) {
            tripJob?.cancel()
            watchdogJob?.cancel()
        }
    }

    private fun maybeReroute(fix: NavFix) {
        val s = _session.value ?: return
        val now = System.currentTimeMillis()
        if (s.rerouting || now - lastRerouteAt < REROUTE_COOLDOWN_MS) return
        lastRerouteAt = now
        _session.update { it?.copy(rerouting = true, rerouteFailed = false) }
        if (!s.muted) voice.speak(context.getString(R.string.livemap_nav_say_rerouting))
        rerouteJob = scope.launch {
            val result = repository.navRoutes(GeoPoint(fix.lat, fix.lng), s.destination, s.route.mode)
            val route = (result as? AppResult.Success)?.data?.firstOrNull()
            if (route == null) {
                _session.update { it?.copy(rerouting = false, rerouteFailed = true) }
            } else {
                tracker = NavTracker(route)
                _session.update { it?.copy(route = route, rerouting = false, rerouteFailed = false, progress = null) }
                lastFix?.let { onFix(it) }
            }
        }
    }

    private fun com.twinhearts.app.feature.livemap.domain.model.DeviceFix.toNavFix() =
        NavFix(lat, lng, accuracyM, speedMps, headingDeg, atMs)

    private companion object {
        const val FIX_INTERVAL_MS = 1_000L
        const val WATCHDOG_MS = 2_000L
        const val GPS_LOST_MS = 10_000L
        const val REROUTE_COOLDOWN_MS = 12_000L
    }
}
