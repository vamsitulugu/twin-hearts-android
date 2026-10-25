package com.twinhearts.app.core.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

/** The single navigation graph. Simple fades keep transitions cheap on mid-range phones. */
@Composable
internal fun AppNavHost(
    navController: NavHostController,
    startDestination: Any,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(160)) },
    ) {
        featureGraphs(navController)
        pendingGraphs(navController)
        debugGraph(navController)
    }
}
