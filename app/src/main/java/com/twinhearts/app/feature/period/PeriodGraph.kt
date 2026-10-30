package com.twinhearts.app.feature.period

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.period.ui.PeriodScreen
import com.twinhearts.app.core.design.TwinNeutralPage

/** Plugs the Period Tracker page into the app (R-43). Opened from Home ("My Space › Period Tracker"). */
fun NavGraphBuilder.periodGraph(navController: NavHostController) {
    composable<Routes.PeriodTracker> { TwinNeutralPage {
        PeriodScreen(onBack = { navController.popBackStack() })
    }
    }
}
