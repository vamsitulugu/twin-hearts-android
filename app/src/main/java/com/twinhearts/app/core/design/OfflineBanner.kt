package com.twinhearts.app.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.twinhearts.app.R

/** Thin strip shown above a page's content when it is showing the saved copy because there is no connection. */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    Text(
        text = stringResource(R.string.polish_offline_banner),
        style = MaterialTheme.typography.labelMedium,
        color = colors.ink,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .background(colors.glass)
            .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.s),
    )
}

/**
 * The one "You're offline" strip for the whole app, laid over the top of every screen. It slides in
 * when the phone loses internet and out again when it returns. Call it inside the root `Box`.
 */
@Composable
fun androidx.compose.foundation.layout.BoxScope.GlobalOfflineBanner(online: Boolean) {
    androidx.compose.animation.AnimatedVisibility(
        visible = !online,
        enter = androidx.compose.animation.slideInVertically { -it } + androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.slideOutVertically { -it } + androidx.compose.animation.fadeOut(),
        modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter),
    ) {
        OfflineBanner(modifier = Modifier.statusBarsPadding())
    }
}
