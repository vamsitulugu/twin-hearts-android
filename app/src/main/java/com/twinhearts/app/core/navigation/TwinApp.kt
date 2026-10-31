package com.twinhearts.app.core.navigation

import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.twinhearts.app.R
import com.twinhearts.app.core.pro.rememberProGate
import com.twinhearts.app.feature.calls.ui.CallBannerHost
import com.twinhearts.app.feature.calls.ui.IncomingCallWatcher
import com.twinhearts.app.core.design.BottomBarItem
import com.twinhearts.app.core.common.LocalReduceMotionSetting
import com.twinhearts.app.core.design.LocalOpenSidebar
import com.twinhearts.app.core.design.TwinBackground
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import com.twinhearts.app.core.design.TwinBottomBar
import com.twinhearts.app.core.design.TwinOrbIcon
import kotlinx.coroutines.launch

/** The five bottom-bar tabs, left to right (R-52). The centre one is the Twin orb. */
private val bottomBarItems = listOf(
    BottomBarItem(Routes.Home, R.string.core_nav_home, Icons.Outlined.Home, selectedIcon = Icons.Rounded.Home),
    BottomBarItem(Routes.Chat, R.string.core_nav_chat, Icons.Outlined.ChatBubbleOutline, selectedIcon = Icons.Rounded.ChatBubble),
    BottomBarItem(Routes.TwinAi, R.string.core_nav_twin, TwinOrbIcon, isOrb = true),
    BottomBarItem(Routes.LiveMap, R.string.core_nav_map, Icons.Outlined.Place, selectedIcon = Icons.Rounded.Place),
    BottomBarItem(Routes.Settings, R.string.core_nav_settings, Icons.Outlined.Settings, selectedIcon = Icons.Rounded.Settings),
)

/** The tab routes: moving between them keeps each tab's state (same rule the bottom bar uses). */
private val tabRoutes: List<Any> = bottomBarItems.map { it.key }

/** Root composable: background, side menu, bottom bar (only on the five tab screens) and the nav graph. */
@Composable
fun TwinApp(viewModel: AppViewModel = hiltViewModel()) {
    val startRoute by viewModel.startRoute.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val reduceMotion by viewModel.reduceMotion.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalReduceMotionSetting provides reduceMotion) {
        TwinBackground {
            // Until the start screen is known we show only the background (a few milliseconds).
            startRoute?.let { start -> TwinNavigation(startDestination = start, session = session) }
        }
    }
}

@Composable
private fun TwinNavigation(startDestination: Any, session: com.twinhearts.app.core.data.Session?) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val openSidebar: () -> Unit = remember(drawerState) { { scope.launch { drawerState.open() } } }

    // A free couple tapped a Pro feature anywhere in the app: open the Pro page for it.
    val proGate = rememberProGate()
    LaunchedEffect(proGate) {
        proGate.paywall.collect { feature ->
            navController.navigate(Routes.Pro(feature)) { launchSingleTop = true }
        }
    }

    // A call from my partner opens the call page on any screen (the app must be open).
    IncomingCallWatcher(
        onIncoming = { video ->
            if (navController.currentDestination?.hasRoute<Routes.Call>() != true) {
                navController.navigate(Routes.Call(video = video, incoming = true)) { launchSingleTop = true }
            }
        },
    )
    val destination = backStackEntry?.destination
    val selectedItem = bottomBarItems.firstOrNull { item ->
        destination?.hierarchy?.any { it.hasRoute(item.key::class) } == true
    }
    // Which side-menu row is active: the first menu route the current screen belongs to.
    val sidebarKey = sidebarRouteKeys.firstOrNull { key ->
        if (key is Routes.Us) {
            // Love Counter and Dream Goals are the same page on different tabs: match the open tab.
            destination?.hasRoute<Routes.Us>() == true &&
                (backStackEntry?.toRoute<Routes.Us>()?.tab == 2) == (key.tab == 2)
        } else {
            destination?.hierarchy?.any { it.hasRoute(key::class) } == true
        }
    }
    // Twin has its own input dock with the orb, and Chat gives the whole bottom to the typing bar
    // (like WhatsApp), so the bar is hidden on both. Leave Chat with the back arrow.
    // Fight Log keeps the bottom bar (as in the web reference), with no tab highlighted.
    val onFights = destination?.hasRoute<Routes.Fights>() == true
    // My Space and the Secret Vault also keep the bottom bar, as in the reference screens.
    val onMySpace = destination?.hasRoute<Routes.MySpace>() == true
    val onVault = destination?.hasRoute<Routes.Vault>() == true
    val showBottomBar = (selectedItem != null &&
        selectedItem.key != Routes.TwinAi &&
        selectedItem.key != Routes.Chat) || onFights || onMySpace || onVault

    // Back closes the menu first.
    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    CompositionLocalProvider(LocalOpenSidebar provides openSidebar) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            // Opened with the hamburger / Home grid button; the swipe is only for closing, so it
            // never fights the map, chat lists or sliders.
            gesturesEnabled = drawerState.isOpen,
            scrimColor = Color.Black.copy(alpha = 0.55f),
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(300.dp),
                    drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
                    drawerContainerColor = Color.Transparent,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                ) {
                    TwinSidebar(
                        session = session,
                        selectedKey = sidebarKey,
                        onNavigate = { entry ->
                            scope.launch { drawerState.close() }
                            val key = entry.key
                            if (key == null) {
                                Toast.makeText(context, R.string.core_pending_message, Toast.LENGTH_SHORT).show()
                            } else if (key in tabRoutes) {
                                navController.navigate(key) {
                                    popUpTo<Routes.Home> { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } else {
                                navController.navigate(key) { launchSingleTop = true }
                            }
                        },
                        onOpenProfile = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Routes.Profile) { launchSingleTop = true }
                        },
                    )
                }
            },
        ) {
            // On Home the bar FLOATS over the page (no reserved strip behind it, like the web app);
            // the page scrolls under it. Other tabs keep the normal reserved bar slot.
            val floatBar = showBottomBar && selectedItem?.key == Routes.Home
            val onBarClick: (BottomBarItem) -> Unit = { item ->
                navController.navigate(item.key) {
                    popUpTo<Routes.Home> { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            Scaffold(
                containerColor = Color.Transparent,
                // Bars and screens handle their own insets (see TwinTopBar and TwinBottomBar).
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (showBottomBar && !floatBar) {
                        TwinBottomBar(
                            items = bottomBarItems,
                            selectedKey = selectedItem?.key,
                            onItemClick = onBarClick,
                            // Settings and Live Map paint their page flat near-black, so the bar slot matches it.
                            slotFill = if (selectedItem?.key == Routes.Settings || selectedItem?.key == Routes.LiveMap) Color(0xFF0A0A0C) else Color.Transparent,
                        )
                    }
                },
            ) { innerPadding ->
                // Without the bottom bar, the screen itself must stay above the gesture bar. Chat is the
                // exception: its typing bar already handles the gesture bar and the keyboard, so adding
                // it here too would leave an empty gap under the typing bar.
                val ownsBottomInset = selectedItem?.key == Routes.Chat || selectedItem?.key == Routes.TwinAi
                val insets = if (showBottomBar || ownsBottomInset) Modifier else Modifier.navigationBarsPadding()
                CallBannerHost(
                    onCallScreen = destination?.hasRoute<Routes.Call>() == true,
                    onOpen = { video ->
                        // incoming = true: the call is already running, this page must not start another.
                        navController.navigate(Routes.Call(video = video, incoming = true)) { launchSingleTop = true }
                    },
                ) {
                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier
                            .padding(innerPadding)
                            .then(insets),
                    )
                    if (floatBar) {
                        TwinBottomBar(
                            items = bottomBarItems,
                            selectedKey = selectedItem?.key,
                            onItemClick = onBarClick,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
                }
            }
        }
    }
}
