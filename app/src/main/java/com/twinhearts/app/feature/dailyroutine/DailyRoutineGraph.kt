package com.twinhearts.app.feature.dailyroutine

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.dailyroutine.ui.RoutineScreen

/*
 * Why this file exists: plugs Daily Routine into the app navigation (R-43).
 */

/** Opened from the Home "Daily Routine" chip. */
fun NavGraphBuilder.dailyRoutineGraph(navController: NavHostController) {
    composable<Routes.DailyRoutine> {
        RoutineScreen(onBack = { navController.popBackStack() })
    }
}
