package com.twinhearts.app.feature.us

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.us.ui.UsScreen

/** Route object and navigation registration for the Us feature (Phase 3 — Us). */
object UsRoute {

    /** Adds the Us tab container to the navigation graph. Called from [FeatureGraphs.featureGraphs]. */
    fun NavGraphBuilder.usGraph(navController: NavHostController) {
        composable<Routes.Us> { entry ->
            val route = entry.toRoute<Routes.Us>()
            UsScreen(
                initialTab = route.tab.coerceIn(0, 2),
                onDreamGoalAdded = { navController.navigate(Routes.Us(tab = 2)) },
            )
        }
    }
}
