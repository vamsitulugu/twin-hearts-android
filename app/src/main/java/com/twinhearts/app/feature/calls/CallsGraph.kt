package com.twinhearts.app.feature.calls

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.calls.ui.CallScreen
import com.twinhearts.app.core.design.TwinNeutralPage

/*
 * Why this file exists: plugs Calls into the app navigation (R-43).
 */

/** The full-screen voice / video call page. It closes itself when the call is over. */
fun NavGraphBuilder.callsGraph(navController: NavHostController) {
    composable<Routes.Call> { entry -> TwinNeutralPage {
        val route = entry.toRoute<Routes.Call>()
        CallScreen(
            video = route.video,
            incoming = route.incoming,
            onClose = { navController.popBackStack() },
        )
    }
    }
}
