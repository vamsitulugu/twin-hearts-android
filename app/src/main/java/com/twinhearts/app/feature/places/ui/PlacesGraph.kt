package com.twinhearts.app.feature.places.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes

/** Why this file exists: registers the Places screen in the app's navigation (one line in your nav graph file). */
fun NavGraphBuilder.placesGraph(navController: NavHostController) {
    composable<Routes.Places> { PlacesScreen(onExit = { navController.popBackStack() }) }
}
