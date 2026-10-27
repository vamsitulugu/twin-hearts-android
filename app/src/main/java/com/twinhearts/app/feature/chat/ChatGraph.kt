package com.twinhearts.app.feature.chat

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.R
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.chat.ui.ChatScreen

/*
 * Why this file exists: plugs Chat into the app navigation (R-43).
 */

/**
 * Plugs Chat into the app (R-43). Chat uses the existing `Routes.Chat` bottom-bar route.
 * The call buttons belong to Calls (2.3), so for now they show the "built in a later step" note.
 */
fun NavGraphBuilder.chatGraph(navController: NavHostController) {
    composable<Routes.Chat> {
        val context = LocalContext.current
        ChatScreen(
            onBack = { navController.navigate(Routes.Home) { launchSingleTop = true } },
            onCall = { Toast.makeText(context, R.string.chat_pending_calls, Toast.LENGTH_SHORT).show() },
        )
    }
}
