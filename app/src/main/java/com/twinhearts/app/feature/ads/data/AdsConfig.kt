package com.twinhearts.app.feature.ads.data

import com.twinhearts.app.BuildConfig

/**
 * Why this file exists: the only place that reads the ad ids. They come from build.gradle.kts:
 * debug builds always use Google's TEST ids (safe to tap), release builds use the real ids from
 * local.properties, and if those are missing ads are simply switched off (never test ads for real people).
 */
internal object AdsConfig {
    val enabled: Boolean get() = BuildConfig.ADS_ENABLED
    val bannerId: String get() = BuildConfig.ADMOB_BANNER_ID
    val interstitialId: String get() = BuildConfig.ADMOB_INTERSTITIAL_ID
}
