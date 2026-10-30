package com.twinhearts.app.feature.home

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.R
import com.twinhearts.app.core.design.LocalOpenSidebar
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.home.ui.HomeScreen
import com.twinhearts.app.feature.home.ui.HomeShortcut

/**
 * Plugs Home into the app (R-43). Our Profile already has a (stand-in) screen, so it navigates.
 * Every other shortcut opens a feature that is built in a later step, so it shows a short note
 * instead of a dead tap. When a feature arrives, its line moves into the `when` below.
 */
fun NavGraphBuilder.homeGraph(navController: NavHostController) {
    composable<Routes.Home> {
        val context = LocalContext.current
        val openSidebar = LocalOpenSidebar.current
        HomeScreen(
            onShortcut = { shortcut ->
                when (shortcut) {
                    HomeShortcut.Menu -> openSidebar()
                    HomeShortcut.OurProfile -> navController.navigate(Routes.Profile)
                    HomeShortcut.Movie -> navController.navigate(Routes.Movie)
                    HomeShortcut.MeetPlanner -> navController.navigate(Routes.MeetPlanner)
                    HomeShortcut.FightLog -> navController.navigate(Routes.Fights)
                    HomeShortcut.LoveCounter -> navController.navigate(Routes.Us(tab = 0))
                    HomeShortcut.DreamGoals -> navController.navigate(Routes.Us(tab = 2))
                    HomeShortcut.Places -> navController.navigate(Routes.Places)
                    HomeShortcut.Level -> navController.navigate(Routes.Level)
                    HomeShortcut.Collection -> navController.navigate(Routes.Collection)
                    HomeShortcut.Music -> navController.navigate(Routes.Music)
                    HomeShortcut.Study -> navController.navigate(Routes.Study)
                    HomeShortcut.Money -> navController.navigate(Routes.Money)
                    HomeShortcut.Calendar -> navController.navigate(Routes.Calendar)
                    HomeShortcut.Memories -> navController.navigate(Routes.Memories)
                    HomeShortcut.BucketList -> navController.navigate(Routes.BucketList)
                    HomeShortcut.PeriodTracker -> navController.navigate(Routes.PeriodTracker)
                    HomeShortcut.Help -> navController.navigate(Routes.Help)
                    HomeShortcut.MySpace -> navController.navigate(Routes.MySpace)
                    HomeShortcut.Vault -> navController.navigate(Routes.Vault)
                    HomeShortcut.Surprises -> navController.navigate(Routes.Surprises)
                    HomeShortcut.Globe -> navController.navigate(Routes.Globe)
                    HomeShortcut.Games -> navController.navigate(Routes.Games)
                    HomeShortcut.DailyRoutine -> navController.navigate(Routes.DailyRoutine)
                    else -> Toast.makeText(context, R.string.core_pending_message, Toast.LENGTH_SHORT).show()
                }
            },
            onConnectPartner = { navController.navigate(Routes.Pairing) },
        )
    }
}
