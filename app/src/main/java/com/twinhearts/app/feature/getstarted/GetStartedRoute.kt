package com.twinhearts.app.feature.getstarted

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.getstarted.ui.GetStartedScreen

/** Plugs the Get Started screen into the app (R-43). After the slide, the user goes to Sign In. */
fun NavGraphBuilder.getStartedGraph(navController: NavHostController) {
    composable<Routes.GetStarted> {
        GetStartedScreen(onSlideComplete = { navController.navigate(Routes.SignIn) })
    }
}
