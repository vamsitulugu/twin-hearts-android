package com.twinhearts.app.feature.ads.data

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.pro.ProGate
import com.twinhearts.app.core.pro.ProStatus
import com.twinhearts.app.feature.ads.domain.AdPolicy
import com.twinhearts.app.feature.ads.domain.AdRules
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Why this file exists: the one owner of everything ad-related in the app.
 *
 * Order of safety checks before ANY ad can appear:
 *  1. Ads are switched on for this build (AdsConfig).
 *  2. The server says this couple is NOT Pro. If the server cannot be reached we show nothing
 *     (a paying couple must never see an ad because their phone was offline).
 *  3. The couple is past the new-user grace (AdPolicy).
 *  4. Google's consent form is done (only shows in regions that require it, e.g. EU/UK).
 *
 * Full-screen ads are loaded in advance and shown only when AdPolicy says it is a fair moment.
 * If no ad is ready at that moment we show nothing: the person never waits for an ad.
 */
@Singleton
class AdsManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val prefs: AdsPrefs,
    private val proGate: ProGate,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _bannersAllowed = MutableStateFlow(false)

    /** True only when every safety check above passed. The Home banner watches this. */
    val bannersAllowed: StateFlow<Boolean> = _bannersAllowed.asStateFlow()

    private var launched = false
    private var launchedAtElapsed = 0L
    private var checking = false
    private var lastCheckElapsed = 0L
    private var recheckAfter = RECHECK_MILLIS
    private var sdkStarted = false
    private var eligible = false

    private var interstitial: InterstitialAd? = null
    private var interstitialLoadedAt = 0L
    private var loadingInterstitial = false
    private var loadFailures = 0
    private var showing = false

    /** Called from MainActivity.onStart. The first call of a process is counted as one "session". */
    fun onAppStart(activity: Activity) {
        Log.d(TAG, "onAppStart: adsEnabled=${AdsConfig.enabled}")
        if (!AdsConfig.enabled) return
        val firstOfProcess = !launched
        launched = true
        if (firstOfProcess) {
            launchedAtElapsed = SystemClock.elapsedRealtime()
            watchProStatus()
        }
        scope.launch {
            if (firstOfProcess) prefs.recordSession(System.currentTimeMillis())
            refreshEligibility(activity)
        }
    }

    /**
     * The person came back Home after using a feature for [timeInFeatureMillis].
     * Shows a full-screen ad only if every rule in AdPolicy agrees and one is already loaded.
     * [stillOnHome] is asked again just before showing, so we never pop up over a screen they moved on to.
     */
    fun onFeatureClosed(activity: Activity, timeInFeatureMillis: Long, stillOnHome: () -> Boolean) {
        if (!AdsConfig.enabled) return
        if (timeInFeatureMillis < AdRules.MIN_TIME_IN_FEATURE_MILLIS) return
        scope.launch {
            refreshEligibility(activity) // cheap: skipped unless the last check is old
            if (!eligible) return@launch
            val nowMillis = System.currentTimeMillis()
            val history = prefs.addBreak(nowMillis)
            val reason = AdPolicy.interstitialSkipReason(
                history = history,
                nowMillis = nowMillis,
                epochDay = LocalDate.now().toEpochDay(),
                sessionAgeMillis = SystemClock.elapsedRealtime() - launchedAtElapsed,
                timeInFeatureMillis = timeInFeatureMillis,
            )
            if (reason != null) {
                Log.d(TAG, "no full-screen ad: $reason")
                return@launch
            }

            val ready = interstitial?.takeIf { SystemClock.elapsedRealtime() - interstitialLoadedAt < AD_MAX_AGE_MILLIS }
            if (ready == null) {
                interstitial = null
                loadInterstitial()
                return@launch // nothing ready: skip silently, never make the person wait
            }
            delay(SHOW_DELAY_MILLIS) // let Home finish drawing first
            if (!eligible || showing || !stillOnHome()) return@launch
            if (activity.isFinishing || activity.isDestroyed) return@launch
            show(activity, ready)
        }
    }

    /** Home is on screen: if the last check failed (offline, cold server, not signed in yet), try again. */
    fun onHomeVisible(activity: Activity) {
        if (!AdsConfig.enabled || !launched) return
        scope.launch { refreshEligibility(activity) }
    }

    // ── safety checks ────────────────────────────────────────────────────────

    /** If this couple becomes Pro (or the server says so), ads switch off at once. */
    private fun watchProStatus() {
        scope.launch {
            proGate.status.collect { status: ProStatus -> if (status.isActive()) disable() }
        }
    }

    private suspend fun refreshEligibility(activity: Activity) {
        val now = SystemClock.elapsedRealtime()
        if (checking) return
        // Ask the server at most every 10 minutes (the first check of a launch always runs).
        if (lastCheckElapsed != 0L && now - lastCheckElapsed < recheckAfter) return
        checking = true
        try {
            lastCheckElapsed = now
            Log.d(TAG, "checking Pro status with the server...")
            val status = when (val result = proGate.refresh()) {
                is AppResult.Success -> result.data
                is AppResult.Failure -> null
            }
            Log.d(TAG, "server answered: $status")
            // Unknown (offline / signed out) or Pro: no ads.
            if (status == null || status.isActive()) {
                Log.d(TAG, "ads off: " + if (status == null) "could not reach server / not signed in (will retry soon)" else "couple is Pro")
                // Could not confirm: retry soon instead of waiting 10 minutes. Pro: the normal long wait is fine.
                recheckAfter = if (status == null) RETRY_AFTER_FAILURE_MILLIS else RECHECK_MILLIS
                disable()
                return
            }
            recheckAfter = RECHECK_MILLIS
            val history = prefs.read(System.currentTimeMillis())
            val graceReason = AdPolicy.bannerSkipReason(history, System.currentTimeMillis())
            if (graceReason != null) {
                Log.d(TAG, "ads off: new-user protection ($graceReason)")
                disable()
                return
            }
            if (!sdkStarted) startSdk(activity)
            if (sdkStarted) enable()
        } finally {
            checking = false
        }
    }

    private suspend fun startSdk(activity: Activity) {
        Log.d(TAG, "starting consent + ads SDK...")
        val consented = withTimeoutOrNull(CONSENT_TIMEOUT_MILLIS) {
            suspendCancellableCoroutine<Boolean> { cont -> gatherConsent(activity) { ok -> if (cont.isActive) cont.resume(ok) } }
        } ?: false
        if (!consented) {
            Log.d(TAG, "ads off: consent not granted / timed out")
            return
        }
        // Keep ads suitable for a couples app: nothing above "teen" content.
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_T)
                .build(),
        )
        suspendCancellableCoroutine<Unit> { cont ->
            MobileAds.initialize(appContext) { if (cont.isActive) cont.resume(Unit) }
        }
        sdkStarted = true
        Log.d(TAG, "ads SDK started")
    }

    /** Google's UMP consent form. Shows only where the law needs it; elsewhere it finishes silently. */
    private fun gatherConsent(activity: Activity, done: (Boolean) -> Unit) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()
        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { done(info.canRequestAds()) }
            },
            { done(info.canRequestAds()) },
        )
    }

    private fun enable() {
        eligible = true
        _bannersAllowed.value = true
        loadInterstitial()
    }

    private fun disable() {
        eligible = false
        _bannersAllowed.value = false
        interstitial = null
    }

    // ── full-screen ads ──────────────────────────────────────────────────────

    private fun loadInterstitial() {
        if (!eligible || interstitial != null || loadingInterstitial || showing) return
        loadingInterstitial = true
        InterstitialAd.load(
            appContext,
            AdsConfig.interstitialId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    loadingInterstitial = false
                    loadFailures = 0
                    Log.d(TAG, "interstitial loaded")
                    interstitial = ad
                    interstitialLoadedAt = SystemClock.elapsedRealtime()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingInterstitial = false
                    interstitial = null
                    Log.d(TAG, "interstitial failed: ${error.code} ${error.message}")
                    loadFailures += 1
                    // Try again a few times, slowly, then give up until the next break.
                    if (loadFailures <= MAX_LOAD_RETRIES) {
                        scope.launch {
                            delay(RETRY_DELAY_MILLIS * loadFailures)
                            loadInterstitial()
                        }
                    }
                }
            },
        )
    }

    private fun show(activity: Activity, ad: InterstitialAd) {
        showing = true
        interstitial = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                scope.launch { prefs.recordShown(System.currentTimeMillis(), LocalDate.now().toEpochDay()) }
            }

            override fun onAdDismissedFullScreenContent() {
                showing = false
                loadInterstitial() // get the next one ready in the background
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                showing = false
                loadInterstitial()
            }
        }
        ad.show(activity)
    }

    private companion object {
        /** Google drops an unused full-screen ad after about an hour; we stop trusting it a bit earlier. */
        const val AD_MAX_AGE_MILLIS = 50L * 60 * 1000
        const val RECHECK_MILLIS = 10L * 60 * 1000
        const val RETRY_AFTER_FAILURE_MILLIS = 20_000L
        const val TAG = "TwinAds"
        const val SHOW_DELAY_MILLIS = 700L
        const val CONSENT_TIMEOUT_MILLIS = 90_000L
        const val RETRY_DELAY_MILLIS = 30_000L
        const val MAX_LOAD_RETRIES = 3
    }
}
