package com.twinhearts.app.feature.meetplanner.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes

/** Why this file exists: registers the Meet Planner screen in the app's navigation (one line in FeatureGraphs.kt). */
fun NavGraphBuilder.meetPlannerGraph(navController: NavHostController) {
    composable<Routes.MeetPlanner> { MeetPlannerScreen(onExit = { navController.popBackStack() }) }
}
