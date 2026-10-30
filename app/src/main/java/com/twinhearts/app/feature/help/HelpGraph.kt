package com.twinhearts.app.feature.help

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.help.ui.HelpScreen
import com.twinhearts.app.core.design.TwinNeutralPage

/** Plugs the Help & Customer Care page into the app (R-43). Opened from Home (My Space list). */
fun NavGraphBuilder.helpGraph(navController: NavHostController) {
    composable<Routes.Help> { TwinNeutralPage {
        HelpScreen(onBack = { navController.popBackStack() })
    }
    }
}
