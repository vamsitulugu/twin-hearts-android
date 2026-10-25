package com.twinhearts.app.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.twinhearts.app.BuildConfig
import com.twinhearts.app.R
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.TwinTopBar

/** Stand-in for a feature that is not built yet. Debug builds also get a link to the design gallery. */
@Composable
internal fun PendingFeatureScreen(
    title: String,
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TwinTopBar(title = title, showLogo = true)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            EmptyState(
                title = title,
                message = stringResource(R.string.core_pending_message),
                actionLabel = if (BuildConfig.DEBUG) stringResource(R.string.core_debug_gallery) else null,
                onAction = if (BuildConfig.DEBUG) onOpenGallery else null,
            )
        }
    }
}
