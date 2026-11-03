package com.twinhearts.app.feature.rewards.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.feature.ads.di.rememberAdsManager

/**
 * Phase 3: what the Rewards screens need to offer "watch an ad". [allowed] = ads are on for this couple (not Pro,
 * consent done), so the button may be shown; [ready] = an ad is loaded and can play now; [show] plays it and calls
 * exactly one of its callbacks.
 */
internal class AdPlayer(
    val allowed: Boolean,
    val ready: Boolean,
    val show: (onEarned: () -> Unit, onFail: () -> Unit) -> Unit,
)

@Composable
internal fun rememberAdPlayer(): AdPlayer {
    val ads = rememberAdsManager()
    val context = LocalContext.current
    val allowed by ads.rewardedAllowed.collectAsStateWithLifecycle()
    val ready by ads.rewardedReady.collectAsStateWithLifecycle()
    return remember(ads, context, allowed, ready) {
        AdPlayer(allowed, ready) { onEarned, onFail ->
            val activity = context.findActivity()
            if (activity == null) {
                onFail()
            } else {
                ads.showRewarded(
                    activity,
                    onEarned = onEarned,
                    onFail = {
                        Toast.makeText(context, R.string.rewards_ad_failed, Toast.LENGTH_SHORT).show()
                        onFail()
                    },
                )
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
