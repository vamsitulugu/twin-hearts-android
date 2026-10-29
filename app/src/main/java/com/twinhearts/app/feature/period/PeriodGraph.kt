package com.twinhearts.app.feature.period

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.period.ui.PeriodScreen

/** Plugs the Period Tracker page into the app (R-43). Opened from Home ("My Space › Period Tracker"). */
fun NavGraphBuilder.periodGraph(navController: NavHostController) {
    composable<Routes.PeriodTracker> {
        PeriodScreen(onBack = { navController.popBackStack() })
    }
}
