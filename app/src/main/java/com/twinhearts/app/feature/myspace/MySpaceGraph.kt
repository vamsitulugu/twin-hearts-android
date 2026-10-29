package com.twinhearts.app.feature.myspace

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.myspace.ui.MySpaceScreen

/** Plugs the My Space page into the app (R-43). Opened from the Home "My Space" tile. */
fun NavGraphBuilder.mySpaceGraph(navController: NavHostController) {
    composable<Routes.MySpace> {
        MySpaceScreen(onBack = { navController.popBackStack() })
    }
}
