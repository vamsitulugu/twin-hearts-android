package com.twinhearts.app.core.navigation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.R
import com.twinhearts.app.feature.auth.authGraph
import com.twinhearts.app.feature.getstarted.getStartedGraph
import com.twinhearts.app.feature.home.homeGraph
import com.twinhearts.app.feature.pairing.pairingGraph

/**
 * THE one place where finished features are plugged in (R-43).
 * When a feature is delivered, add its single line here, then delete that feature's
 * route from [pendingGraphs] below. Nothing else in core changes.
 */
internal fun NavGraphBuilder.featureGraphs(navController: NavHostController) {
    getStartedGraph(navController) // 1.1 Get Started
    authGraph(navController) // 1.2 Sign In, Sign Up, Forgot Password
    pairingGraph(navController) // 1.3 Partner pairing
    homeGraph(navController) // 2.1 Home
}

/**
 * Screens whose feature is not built yet. Each shows a small "built in a later step" page,
 * so the bottom bar and start flow never point at a missing screen.
 * Remove a line when the real feature is added above.
 */
internal fun NavGraphBuilder.pendingGraphs(navController: NavHostController) {
    val openGallery = { navController.navigate(Routes.DesignGallery) }
    pending<Routes.Chat>(R.string.core_nav_chat, openGallery)
    pending<Routes.TwinAi>(R.string.core_nav_twin, openGallery)
    pending<Routes.LiveMap>(R.string.core_nav_map, openGallery)
    pending<Routes.Settings>(R.string.core_nav_settings, openGallery)
    pending<Routes.Profile>(R.string.app_name, openGallery)
}

private inline fun <reified T : Any> NavGraphBuilder.pending(
    @StringRes title: Int,
    noinline openGallery: () -> Unit,
) {
    composable<T> {
        PendingFeatureScreen(title = stringResource(title), onOpenGallery = openGallery)
    }
}
