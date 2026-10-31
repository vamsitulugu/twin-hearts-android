package com.twinhearts.app.core.navigation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.R
import com.twinhearts.app.feature.auth.authGraph
import com.twinhearts.app.feature.calendar.calendarGraph
import com.twinhearts.app.feature.calls.callsGraph
import com.twinhearts.app.feature.chat.chatGraph
import com.twinhearts.app.feature.collection.collectionGraph
import com.twinhearts.app.feature.fights.fightsGraph
import com.twinhearts.app.feature.getstarted.getStartedGraph
import com.twinhearts.app.feature.home.homeGraph
import com.twinhearts.app.feature.livemap.liveMapGraph
import com.twinhearts.app.feature.meetplanner.ui.meetPlannerGraph
import com.twinhearts.app.feature.money.moneyGraph
import com.twinhearts.app.feature.movie.movieGraph
import com.twinhearts.app.feature.music.musicGraph
import com.twinhearts.app.feature.pairing.pairingGraph
import com.twinhearts.app.feature.study.studyGraph
import com.twinhearts.app.feature.twinai.twinAiGraph
import com.twinhearts.app.feature.places.ui.placesGraph
import com.twinhearts.app.feature.level.levelGraph
import com.twinhearts.app.feature.profile.profileGraph
import com.twinhearts.app.feature.pro.proGraph
import com.twinhearts.app.feature.settings.settingsGraph
import com.twinhearts.app.feature.memories.memoriesGraph
import com.twinhearts.app.feature.bucket.bucketGraph
import com.twinhearts.app.feature.period.periodGraph
import com.twinhearts.app.feature.help.helpGraph
import com.twinhearts.app.feature.myspace.mySpaceGraph
import com.twinhearts.app.feature.vault.vaultGraph
import com.twinhearts.app.feature.surprises.surprisesGraph
import com.twinhearts.app.feature.globe.globeGraph
import com.twinhearts.app.feature.games.gamesGraph
import com.twinhearts.app.feature.dailyroutine.dailyRoutineGraph
import com.twinhearts.app.feature.us.UsRoute.usGraph
import com.twinhearts.app.feature.search.searchGraph

/**
 * THE one place where finished features are plugged in (R-43).
 * When a feature is delivered, add its single line here, then delete that feature's
 * route from [pendingGraphs] below. Nothing else in core changes.
 */
internal fun NavGraphBuilder.featureGraphs(navController: NavHostController) {
    getStartedGraph(navController) // 1.1 Get Started
    authGraph(navController) // 1.2 Sign In, Sign Up, Forgot Password
    pairingGraph(navController) // 1.3 Partner pairing
    homeGraph(navController) // 2.1 Home
    chatGraph(navController) // 2.2 Chat
    twinAiGraph(navController) // 2.3 Twin AI
    movieGraph(navController) // 5.2 Watch Together (movies)
    meetPlannerGraph(navController) // 4.x Meet Planner
    fightsGraph(navController) // 3.x Fight Log
    callsGraph(navController) // Calls (voice and video)
    calendarGraph(navController) // Calendar
    collectionGraph(navController) // Collection
    moneyGraph(navController) // Money
    musicGraph(navController) // Music
    studyGraph(navController) // Study
    liveMapGraph(navController) // Live Map
    usGraph(navController) // 3.1 Us
    levelGraph(navController) // 3.2 Couple Level
    placesGraph(navController) // Places
    profileGraph(navController) // 2.6 Our Profile
    proGraph(navController) // Pro (Razorpay)
    settingsGraph(navController) // 2.5 Settings
    memoriesGraph(navController) // Memories
    bucketGraph(navController) // 4.x Bucket List
    periodGraph(navController) // 4.x Period Tracker
    helpGraph(navController) // 4.x Help & Customer Care
    mySpaceGraph(navController) // My Space
    vaultGraph(navController) // Secret Vault
    surprisesGraph(navController) // Surprises + Love Capsule
    globeGraph(navController) // 6.x Memory Globe
    gamesGraph(navController) // Games
    dailyRoutineGraph(navController) // 4.x Daily Routine
    searchGraph(navController) // Global search (Home search bar)
}

/**
 * Screens whose feature is not built yet. Each shows a small "built in a later step" page,
 * so the bottom bar and start flow never point at a missing screen.
 * Remove a line when the real feature is added above.
 */
internal fun NavGraphBuilder.pendingGraphs(navController: NavHostController) {
    val openGallery = { navController.navigate(Routes.DesignGallery) }
}

private inline fun <reified T : Any> NavGraphBuilder.pending(
    @StringRes title: Int,
    noinline openGallery: () -> Unit,
) {
    composable<T> {
        PendingFeatureScreen(title = stringResource(title), onOpenGallery = openGallery)
    }
}
