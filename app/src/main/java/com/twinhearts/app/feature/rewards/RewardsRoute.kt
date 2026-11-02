package com.twinhearts.app.feature.rewards

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.design.TwinNeutralPage
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.rewards.ui.RewardsScreen

/** Plugs the Twin Rewards page into the app (R-43). Needs `Routes.Rewards`. */
fun NavGraphBuilder.rewardsGraph(navController: NavHostController) {
    composable<Routes.Rewards> {
        TwinNeutralPage {
            RewardsScreen(navController)
        }
    }
}
