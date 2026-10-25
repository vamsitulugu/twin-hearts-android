package com.twinhearts.app.feature.auth

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.auth.ui.ForgotPasswordScreen
import com.twinhearts.app.feature.auth.ui.SignInScreen
import com.twinhearts.app.feature.auth.ui.SignUpScreen

/** Plugs Sign In, Sign Up and Forgot Password into the app (R-43). */
fun NavGraphBuilder.authGraph(navController: NavHostController) {
    composable<Routes.SignIn> {
        SignInScreen(
            onSignedIn = { navController.goHomeClearingEntryScreens() },
            // The Sign In / Sign Up tabs swap places, so Back still returns to Get Started.
            onOpenSignUp = {
                navController.navigate(Routes.SignUp) {
                    popUpTo<Routes.SignIn> { inclusive = true }
                    launchSingleTop = true
                }
            },
            onForgotPassword = { navController.navigate(Routes.ForgotPassword) },
            onBack = { navController.popBackStack() },
        )
    }
    composable<Routes.SignUp> {
        SignUpScreen(
            // A new account has no partner yet, so it goes to Pairing first (Pairing has "Skip for now").
            onSignedIn = { navController.goToPairingClearingEntryScreens() },
            onOpenSignIn = {
                navController.navigate(Routes.SignIn) {
                    popUpTo<Routes.SignUp> { inclusive = true }
                    launchSingleTop = true
                }
            },
            onBack = { navController.popBackStack() },
        )
    }
    composable<Routes.ForgotPassword> {
        ForgotPasswordScreen(onBackToSignIn = { navController.popBackStack() })
    }
}

/** After sign in/up the entry screens are removed, so Back from Home leaves the app. */
private fun NavHostController.goHomeClearingEntryScreens() {
    navigate(Routes.Home) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

/** After sign up the entry screens are removed and the user is invited to connect a partner. */
private fun NavHostController.goToPairingClearingEntryScreens() {
    navigate(Routes.Pairing) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
