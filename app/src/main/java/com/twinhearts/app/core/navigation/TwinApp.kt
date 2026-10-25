package com.twinhearts.app.core.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.twinhearts.app.R
import com.twinhearts.app.core.design.BottomBarItem
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinBottomBar
import com.twinhearts.app.core.design.TwinOrbIcon

/** The five bottom-bar tabs, left to right (R-52). The centre one is the Twin orb. */
private val bottomBarItems = listOf(
    BottomBarItem(Routes.Home, R.string.core_nav_home, Icons.Rounded.Home),
    BottomBarItem(Routes.Chat, R.string.core_nav_chat, Icons.Rounded.ChatBubble),
    BottomBarItem(Routes.TwinAi, R.string.core_nav_twin, TwinOrbIcon, isOrb = true),
    BottomBarItem(Routes.LiveMap, R.string.core_nav_map, Icons.Rounded.Place),
    BottomBarItem(Routes.Settings, R.string.core_nav_settings, Icons.Rounded.Settings),
)

/** Root composable: background, bottom bar (only on the five tab screens) and the nav graph. */
@Composable
fun TwinApp(viewModel: AppViewModel = hiltViewModel()) {
    val startRoute by viewModel.startRoute.collectAsStateWithLifecycle()
    TwinBackground {
        // Until the start screen is known we show only the background (a few milliseconds).
        startRoute?.let { start -> TwinNavigation(startDestination = start) }
    }
}

@Composable
private fun TwinNavigation(startDestination: Any) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val selectedItem = bottomBarItems.firstOrNull { item ->
        destination?.hierarchy?.any { it.hasRoute(item.key::class) } == true
    }
    val showBottomBar = selectedItem != null

    Scaffold(
        containerColor = Color.Transparent,
        // Bars and screens handle their own insets (see TwinTopBar and TwinBottomBar).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                TwinBottomBar(
                    items = bottomBarItems,
                    selectedKey = selectedItem?.key,
                    onItemClick = { item ->
                        navController.navigate(item.key) {
                            popUpTo<Routes.Home> { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        // Without the bottom bar, the screen itself must stay above the gesture bar.
        val insets = if (showBottomBar) Modifier else Modifier.navigationBarsPadding()
        AppNavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .padding(innerPadding)
                .then(insets),
        )
    }
}
