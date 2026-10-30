package com.twinhearts.app.feature.money

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.money.ui.MoneyScreen
import com.twinhearts.app.core.design.TwinNeutralPage

/** Plugs the Money page into the app (R-43). Opened from Home (Plans list). */
fun NavGraphBuilder.moneyGraph(navController: NavHostController) {
    composable<Routes.Money> { TwinNeutralPage {
        MoneyScreen(onBack = { navController.popBackStack() })
    }
    }
}
