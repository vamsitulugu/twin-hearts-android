package com.twinhearts.app.feature.search

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.design.TwinNeutralPage
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.search.domain.model.SearchDestination
import com.twinhearts.app.feature.search.ui.SearchScreen

/**
 * Plugs Search into the app (R-43). It is opened from the Home search bar and sits outside the
 * bottom bar, so it fills the whole screen. A tapped result opens the page it belongs to; going
 * back from that page returns to the same search.
 */
fun NavGraphBuilder.searchGraph(navController: NavHostController) {
    composable<Routes.Search> {
        TwinNeutralPage {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onOpen = { destination -> navController.openDestination(destination) },
            )
        }
    }
}

/** The five bottom-bar pages keep their own saved state, exactly like a tap on the bar. */
private fun NavHostController.openBottomBar(route: Any) = navigate(route) {
    popUpTo<Routes.Home> { saveState = true }
    launchSingleTop = true
    restoreState = true
}

private fun NavHostController.openDestination(destination: SearchDestination) {
    when (destination) {
        SearchDestination.Home -> openBottomBar(Routes.Home)
        SearchDestination.Chat -> openBottomBar(Routes.Chat)
        SearchDestination.TwinAi -> openBottomBar(Routes.TwinAi)
        SearchDestination.LiveMap -> openBottomBar(Routes.LiveMap)
        SearchDestination.Settings -> openBottomBar(Routes.Settings)
        SearchDestination.Profile -> navigate(Routes.Profile)
        SearchDestination.Pro -> navigate(Routes.Pro())
        SearchDestination.Movie -> navigate(Routes.Movie)
        SearchDestination.MeetPlanner -> navigate(Routes.MeetPlanner)
        SearchDestination.Fights -> navigate(Routes.Fights)
        SearchDestination.LoveCounter -> navigate(Routes.Us(tab = 0))
        SearchDestination.DreamGoals -> navigate(Routes.Us(tab = 2))
        SearchDestination.Collection -> navigate(Routes.Collection)
        SearchDestination.Money -> navigate(Routes.Money)
        SearchDestination.Calendar -> navigate(Routes.Calendar)
        SearchDestination.Music -> navigate(Routes.Music)
        SearchDestination.Study -> navigate(Routes.Study)
        SearchDestination.Level -> navigate(Routes.Level)
        SearchDestination.Places -> navigate(Routes.Places)
        SearchDestination.Memories -> navigate(Routes.Memories)
        SearchDestination.BucketList -> navigate(Routes.BucketList)
        SearchDestination.PeriodTracker -> navigate(Routes.PeriodTracker)
        SearchDestination.Help -> navigate(Routes.Help)
        SearchDestination.MySpace -> navigate(Routes.MySpace)
        SearchDestination.Vault -> navigate(Routes.Vault)
        SearchDestination.Surprises -> navigate(Routes.Surprises)
        SearchDestination.Globe -> navigate(Routes.Globe)
        SearchDestination.Games -> navigate(Routes.Games)
        SearchDestination.DailyRoutine -> navigate(Routes.DailyRoutine)
    }
}
