package com.twinhearts.app.feature.rewards

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.design.TwinNeutralPage
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.rewards.ui.ScratchCardsScreen

/** Plugs the My Scratch Cards page (cards won from couple games) into the app. Needs `Routes.ScratchCards`. */
fun NavGraphBuilder.scratchCardsGraph(navController: NavHostController) {
    composable<Routes.ScratchCards> {
        TwinNeutralPage {
            ScratchCardsScreen(onPlayGames = { navController.navigate(Routes.Games) })
        }
    }
}
