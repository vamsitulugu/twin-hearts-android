package com.twinhearts.app.feature.ads.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.ads.di.rememberAdsManager

/**
 * Why this file exists: tells AdsManager which screen the person is on, so ads are always kind:
 *  - a due (timed) ad waits while the person chats, calls, plays games, listens / sings or watches a movie,
 *    and plays when they are back on a calm page;
 *  - opening a Movie plays ONE ad at the start (Sing Together does the same from its own overlay);
 *  - staying 5+ minutes on any page (chat and the map included) earns one ad for when the person leaves it.
 * One watcher in TwinApp covers every feature, so no feature screen needs ad code.
 */
@Composable
fun AdBreakWatcher(navController: NavController) {
    val ads = rememberAdsManager()
    val activity = LocalContext.current.findActivity()

    DisposableEffect(navController, ads, activity) {
        var wasMovie = false
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            ads.onScreenChanged(canCoverWithAd(destination), countsForLeavingAd(destination))
            if (destination.hasRoute<Routes.Home>() && activity != null) ads.onHomeVisible(activity)

            val isMovie = destination.hasRoute<Routes.Movie>()
            if (isMovie && !wasMovie && activity != null) ads.onSessionStart(activity)
            wasMovie = isMovie
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }
}

/**
 * Screens a timed ad must NEVER cover: Chat and Twin AI (typing), calls, Live Map (navigating), Games,
 * Music (playback and Sing Together), Movie (watching together), payment, sign-in and pairing.
 * On these the ad waits; it plays on the first other page once it has been on screen for a moment.
 */
private fun canCoverWithAd(d: NavDestination): Boolean = !(
    d.hasRoute<Routes.Chat>() ||
        d.hasRoute<Routes.TwinAi>() ||
        d.hasRoute<Routes.Call>() ||
        d.hasRoute<Routes.LiveMap>() ||
        d.hasRoute<Routes.Games>() ||
        d.hasRoute<Routes.Music>() ||
        d.hasRoute<Routes.Movie>() ||
        d.hasRoute<Routes.Pro>() ||
        d.hasRoute<Routes.GetStarted>() ||
        d.hasRoute<Routes.SignIn>() ||
        d.hasRoute<Routes.SignUp>() ||
        d.hasRoute<Routes.ForgotPassword>() ||
        d.hasRoute<Routes.CompleteProfile>() ||
        d.hasRoute<Routes.Pairing>()
    )

/**
 * Pages whose long stay earns an ad on the way out. Everything counts (chat, the map, a movie, any planner)
 * except calls (a call ending must not be followed by an ad), payment and the sign-in / pairing screens.
 */
private fun countsForLeavingAd(d: NavDestination): Boolean = !(
    d.hasRoute<Routes.Call>() ||
        d.hasRoute<Routes.Pro>() ||
        d.hasRoute<Routes.GetStarted>() ||
        d.hasRoute<Routes.SignIn>() ||
        d.hasRoute<Routes.SignUp>() ||
        d.hasRoute<Routes.ForgotPassword>() ||
        d.hasRoute<Routes.CompleteProfile>() ||
        d.hasRoute<Routes.Pairing>()
    )
