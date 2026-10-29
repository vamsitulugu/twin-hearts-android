package com.twinhearts.app.feature.bucket

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.bucket.ui.BucketScreen

/** Plugs the Bucket List page into the app (R-43). Opened from Home ("Plans › Bucket List"). */
fun NavGraphBuilder.bucketGraph(navController: NavHostController) {
    composable<Routes.BucketList> {
        BucketScreen(onBack = { navController.popBackStack() })
    }
}
