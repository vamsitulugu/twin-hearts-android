package com.twinhearts.app.feature.ads.ui

import android.content.Context
import android.content.ContextWrapper
import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.ads.data.AdsConfig
import com.twinhearts.app.feature.ads.di.rememberAdsManager

/**
 * Why this file exists: the small banner card shown on Home (below "All pages", above the quote).
 *
 * Good-experience rules built in:
 *  - It draws NOTHING until an ad has really loaded, so there is never an empty box or a gap.
 *  - It draws nothing for Pro couples, new couples, offline phones, or when the server could not confirm.
 *  - It is clearly labelled "Sponsored" and sits inside a card that matches the app.
 */
@Composable
fun AdSlot(modifier: Modifier = Modifier) {
    if (!AdsConfig.enabled) return
    val ads = rememberAdsManager()
    val allowed by ads.bannersAllowed.collectAsStateWithLifecycle()
    if (allowed) BannerCard(modifier)
}

@Composable
private fun BannerCard(modifier: Modifier) {
    val context = LocalContext.current
    val edge = TwinTheme.spacing.screenEdge
    val cardPadding = TwinTheme.spacing.s
    // Width the banner may use: the screen minus the page edges and the card padding on both sides.
    val widthDp = (LocalConfiguration.current.screenWidthDp - ((edge.value + cardPadding.value) * 2)).toInt()

    // Which banner finished loading. Comparing with the current banner keeps a rotated screen from showing a blank one.
    var loadedBanner by remember { mutableStateOf<AdView?>(null) }
    val banner = remember(widthDp) {
        AdView(context).apply {
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp))
            adUnitId = AdsConfig.bannerId
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    loadedBanner = this@apply
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (loadedBanner === this@apply) loadedBanner = null
                }
            }
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(banner) { onDispose { banner.destroy() } }

    // Pause the banner while the app is in the background (saves battery and data).
    val owner = context.findActivity() as? LifecycleOwner
    DisposableEffect(banner, owner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> banner.pause()
                Lifecycle.Event.ON_RESUME -> banner.resume()
                else -> Unit
            }
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose { owner?.lifecycle?.removeObserver(observer) }
    }

    if (loadedBanner === banner) {
        GlassCard(
            modifier = modifier
                .padding(horizontal = edge)
                .fillMaxWidth(),
            contentPadding = cardPadding,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
                Text(
                    text = stringResource(R.string.ads_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = TwinTheme.colors.inkFaint,
                )
                AndroidView(factory = { banner }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** The Activity behind a Compose context (needed to follow its lifecycle). */
internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
