package com.twinhearts.app.feature.vault

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.vault.ui.VaultScreen

/** Plugs the Secret Vault into the app (R-43). Opened from the Home "Vault" tile. */
fun NavGraphBuilder.vaultGraph(navController: NavHostController) {
    composable<Routes.Vault> {
        VaultScreen(onBack = { navController.popBackStack() })
    }
}
