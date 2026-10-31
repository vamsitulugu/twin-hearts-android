package com.twinhearts.app.feature.ads.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.twinhearts.app.feature.ads.data.AdsManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/** Lets any composable reach the shared [AdsManager] without changing ViewModel constructors (same trick as ProGate). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AdsEntryPoint {
    fun adsManager(): AdsManager
}

@Composable
fun rememberAdsManager(): AdsManager {
    val app = LocalContext.current.applicationContext
    return remember(app) { EntryPointAccessors.fromApplication(app, AdsEntryPoint::class.java).adsManager() }
}
