package com.twinhearts.app.feature.ads.ui

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.ads.di.rememberAdsManager

/**
 * Why this file exists: spots a "natural break" — the person used a feature for a while and pressed back
 * to Home — and tells AdsManager. AdsManager (and AdPolicy) then decide whether an ad is fair.
 * One watcher in TwinApp covers every feature, so no feature screen needs ad code.
 */
@Composable
fun AdBreakWatcher(navController: NavController) {
    val ads = rememberAdsManager()
    val activity = LocalContext.current.findActivity()

    DisposableEffect(navController, ads, activity) {
        var inFeature = false
        var enteredAt = 0L
        val listener = NavController.OnDestinationChangedListener { controller, destination, _ ->
            when {
                isBreakFeature(destination) -> {
                    if (!inFeature) {
                        inFeature = true
                        enteredAt = SystemClock.elapsedRealtime()
                    }
                }
                destination.hasRoute<Routes.Home>() && inFeature -> {
                    inFeature = false
                    val spent = SystemClock.elapsedRealtime() - enteredAt
                    if (activity != null) {
                        ads.onFeatureClosed(activity, spent) {
                            controller.currentDestination?.hasRoute<Routes.Home>() == true
                        }
                    }
                }
                else -> inFeature = false
            }
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }
}

/**
 * Screens where leaving is a calm, finished moment: games and everyday planning tools.
 * Deliberately NOT here: Chat, Calls, Live Map, Twin AI, Settings, Pro, sign-in, Music and Movie
 * (audio/sync), and the personal pages (Memories, Collection, Globe, Love Counter, Fight Log,
 * Surprises, Vault, Period Tracker, My Space). An ad should never interrupt those.
 */
private fun isBreakFeature(d: NavDestination): Boolean =
    d.hasRoute<Routes.Games>() ||
        d.hasRoute<Routes.Money>() ||
        d.hasRoute<Routes.Calendar>() ||
        d.hasRoute<Routes.Study>() ||
        d.hasRoute<Routes.Places>() ||
        d.hasRoute<Routes.MeetPlanner>() ||
        d.hasRoute<Routes.DailyRoutine>() ||
        d.hasRoute<Routes.BucketList>() ||
        d.hasRoute<Routes.Level>()
