package com.twinhearts.app.core.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController

/** Release builds have no debug screens, so this does nothing. (The debug version is in src/debug.) */
@Suppress("UNUSED_PARAMETER")
internal fun NavGraphBuilder.debugGraph(navController: NavHostController) = Unit
