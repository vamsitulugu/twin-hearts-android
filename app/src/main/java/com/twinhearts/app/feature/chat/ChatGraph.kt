package com.twinhearts.app.feature.chat

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.chat.ui.ChatScreen
import com.twinhearts.app.feature.storefx.StoreFxThemed

/*
 * Why this file exists: plugs Chat into the app navigation (R-43).
 */

/**
 * Plugs Chat into the app (R-43). Chat uses the existing `Routes.Chat` bottom-bar route.
 * The call buttons open the Calls screen (feature/calls).
 */
fun NavGraphBuilder.chatGraph(navController: NavHostController) {
    composable<Routes.Chat> {
        // Twin Store: an equipped theme recolours the text and glass tokens for the whole chat page.
        StoreFxThemed {
            ChatScreen(
                onBack = { navController.navigate(Routes.Home) { launchSingleTop = true } },
                onCall = { navController.navigate(Routes.Call(video = false)) },
                onVideoCall = { navController.navigate(Routes.Call(video = true)) },
                onOpenRewards = { navController.navigate(Routes.Rewards) },
            )
        }
    }
}
