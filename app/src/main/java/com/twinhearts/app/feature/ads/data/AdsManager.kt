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
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.lang.ref.WeakReference
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
 * Full-screen ads are kind to the person:
 *  - TIMED: one ad becomes due per `AdRules.INTERSTITIAL_INTERVAL_MILLIS` of the app being open on screen.
 *    It never covers chat, calls, games, music / sing-together or movies. It waits and plays when the
 *    person is back on a calm page (see AdBreakWatcher).
 *  - LEAVING A PAGE: after `AdRules.IDLE_PAGE_MILLIS` on one page, one ad plays when the person goes back or
 *    closes it (never while they are still there).
 *  - SESSION START: Sing Together and Movies get one ad as they start.
 *  - MOVIE BREAK: every `AdRules.MOVIE_BREAK_INTERVAL_MILLIS` of a movie both phones show one ad together
 *    (the server picks the moment, see WatchViewModel).
 *  - EARN CARD: every `AdRules.EARN_PROMPT_INTERVAL_MILLIS` a small card offers "watch an ad, earn coins".
 *    It is the person's choice to tap it, so there is no daily limit (the server pays each watched ad).
 * One ad is always loaded in advance.
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

    private val _rewardedAllowed = MutableStateFlow(false)

    /** True when watch-an-ad buttons may be shown (ads on, couple is not Pro, consent done). */
    val rewardedAllowed: StateFlow<Boolean> = _rewardedAllowed.asStateFlow()

    private val _rewardedReady = MutableStateFlow(false)

    /** True when a rewarded ad is loaded and can play right now. */
    val rewardedReady: StateFlow<Boolean> = _rewardedReady.asStateFlow()

    private val _earnPromptVisible = MutableStateFlow(false)

    /** True while the small "watch an ad, earn coins" card should be on screen. */
    val earnPromptVisible: StateFlow<Boolean> = _earnPromptVisible.asStateFlow()

    private var rewarded: RewardedAd? = null
    private var rewardedLoadedAt = 0L
    private var loadingRewarded = false
    private var rewardedFailures = 0
    private var nextRewardedLoadAllowedAt = 0L

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
    private var nextLoadAllowedAt = 0L
    private var showing = false

    // Timer: "usage" is time the app was open on screen since the last ad. Only touched on the main thread.
    private var activityRef: WeakReference<Activity>? = null
    private var tickerJob: Job? = null
    private var usageBeforeThisStretch = 0L
    private var foregroundSince = 0L // 0 = app is in the background
    private var screenSuitable = false // set by AdBreakWatcher on every screen change
    private var suitableSince = 0L // when the current calm screen appeared (0 = not on a calm screen)
    private var lastLoggedReason: Any? = null

    // Total time the app has been on screen (never reset), the base for page-stay and earn-card timing.
    private var fgTotalBefore = 0L
    private var screenEnteredFg = -1L // when the current page appeared, on the clock above (-1 = no page yet)
    private var screenCountsForIdle = false
    private var idleDwell = 0L // how long the page we just left was open, while an ad for that is pending
    private var idlePendingSince = 0L // elapsedRealtime when the pending leaving-ad was earned (0 = none)
    private var earnClockStartFg = 0L // the card's 4-minute clock starts here

    /** Called from MainActivity.onStart. The first call of a process is counted as one "session". */
    fun onAppStart(activity: Activity) {
        Log.d(TAG, "onAppStart: adsEnabled=${AdsConfig.enabled}")
        if (!AdsConfig.enabled) return
        val firstOfProcess = !launched
        launched = true
        activityRef = WeakReference(activity)
        if (foregroundSince == 0L) foregroundSince = SystemClock.elapsedRealtime()
        if (firstOfProcess) {
            launchedAtElapsed = SystemClock.elapsedRealtime()
            watchProStatus()
        }
        scope.launch {
            if (firstOfProcess) prefs.recordSession(System.currentTimeMillis())
            refreshEligibility(activity)
        }
        startTicker()
    }

    /** Called from MainActivity.onStop: the clock only runs while the app is on screen. */
    fun onAppStop() {
        if (foregroundSince != 0L) {
            val stretch = SystemClock.elapsedRealtime() - foregroundSince
            usageBeforeThisStretch += stretch
            fgTotalBefore += stretch
            foregroundSince = 0L
        }
        _earnPromptVisible.value = false
        tickerJob?.cancel()
        tickerJob = null
    }

    /** AdBreakWatcher tells us on every screen change whether an ad may cover the new screen. */
    fun onScreenChanged(suitable: Boolean, countsForIdle: Boolean = true) {
        // Every screen change restarts the "settle" clock, so a due ad never lands mid-transition.
        suitableSince = if (suitable) SystemClock.elapsedRealtime() else 0L
        screenSuitable = suitable
        if (!suitable) _earnPromptVisible.value = false // the card never sits on chat, calls, the map or a movie

        // How long was the page we are leaving open? 5 minutes or more earns one ad on the next calm page.
        val nowFg = fgTotalMillis()
        if (eligible && screenEnteredFg >= 0 && screenCountsForIdle && nowFg - screenEnteredFg >= AdRules.IDLE_PAGE_MILLIS) {
            idleDwell = nowFg - screenEnteredFg
            idlePendingSince = SystemClock.elapsedRealtime()
        }
        screenEnteredFg = nowFg
        screenCountsForIdle = countsForIdle
    }

    /**
     * Sing Together or a Movie is starting: plays ONE ad now (if one is ready and the rules agree),
     * so nothing has to interrupt the session later. If no ad is ready we skip it, never make anyone wait.
     */
    fun onSessionStart(activity: Activity) {
        if (!AdsConfig.enabled) return
        scope.launch {
            refreshEligibility(activity)
            if (!eligible || showing) return@launch
            val nowMillis = System.currentTimeMillis()
            val reason = AdPolicy.entrySkipReason(prefs.read(nowMillis), nowMillis, LocalDate.now().toEpochDay())
            if (reason != null) {
                Log.d(TAG, "no start ad: $reason")
                return@launch
            }
            val ready = readyInterstitial()
            if (ready == null) {
                loadInterstitial()
                Log.d(TAG, "no start ad: none loaded yet")
                return@launch
            }
            delay(START_AD_DELAY_MILLIS) // let the screen draw first
            if (!eligible || showing || activity.isFinishing || activity.isDestroyed) return@launch
            Log.d(TAG, "start ad: showing")
            show(activity, ready)
        }
    }

    /** Home is on screen: if the last check failed (offline, cold server, not signed in yet), try again. */
    fun onHomeVisible(activity: Activity) {
        if (!AdsConfig.enabled || !launched) return
        scope.launch { refreshEligibility(activity) }
    }

    // ── the timer ────────────────────────────────────────────────────────────

    private fun fgTotalMillis(): Long =
        fgTotalBefore + if (foregroundSince != 0L) SystemClock.elapsedRealtime() - foregroundSince else 0L

    private fun usageMillis(): Long =
        usageBeforeThisStretch + if (foregroundSince != 0L) SystemClock.elapsedRealtime() - foregroundSince else 0L

    private fun resetUsage() {
        usageBeforeThisStretch = 0L
        if (foregroundSince != 0L) foregroundSince = SystemClock.elapsedRealtime()
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            while (isActive) {
                runCatching { tick() }.onFailure { Log.d(TAG, "tick failed: ${it.message}") }
                delay(TICK_MILLIS)
            }
        }
    }

    /** Runs every few seconds while the app is open: keeps an ad loaded and shows it when the timer is due. */
    private suspend fun tick() {
        val activity = activityRef?.get() ?: return
        if (activity.isFinishing || activity.isDestroyed) return
        refreshEligibility(activity) // cheap: skipped unless the last check is old
        if (!eligible || showing) return
        loadInterstitial() // no-op when one is loaded/loading or we are backing off after a failure
        loadRewarded()

        val nowMillis = System.currentTimeMillis()
        val calmScreen = screenSuitable && suitableSince != 0L &&
            SystemClock.elapsedRealtime() - suitableSince >= AdRules.SETTLE_ON_SCREEN_MILLIS

        // 1) A page that was open for 5+ minutes was just left: one ad now (waits for a calm page, expires after 2 min).
        if (idlePendingSince != 0L) {
            if (SystemClock.elapsedRealtime() - idlePendingSince > IDLE_PENDING_EXPIRES_MILLIS) {
                idlePendingSince = 0L
            } else if (AdPolicy.idleExitSkipReason(prefs.read(nowMillis), nowMillis, LocalDate.now().toEpochDay(), idleDwell, calmScreen) == null) {
                val readyIdle = readyInterstitial()
                if (readyIdle != null) {
                    idlePendingSince = 0L
                    _earnPromptVisible.value = false
                    show(activity, readyIdle)
                    return
                }
            }
        }

        val reason = AdPolicy.interstitialSkipReason(
            history = prefs.read(nowMillis),
            nowMillis = nowMillis,
            epochDay = LocalDate.now().toEpochDay(),
            usageSinceLastAdMillis = usageMillis(),
            // A screen counts as calm only after it has been on screen for a moment, so an ad never pops
            // up in the middle of a page transition.
            screenIsSuitable = screenSuitable && suitableSince != 0L &&
                SystemClock.elapsedRealtime() - suitableSince >= AdRules.SETTLE_ON_SCREEN_MILLIS,
        )
        if (reason != lastLoggedReason) {
            lastLoggedReason = reason
            Log.d(TAG, "full-screen ad: " + (reason ?: "due, showing as soon as one is ready"))
        }
        if (reason == null) {
            val ready = readyInterstitial()
            if (ready != null) { // never make the person wait for an ad; the next tick asks Google for a fresh one
                _earnPromptVisible.value = false
                show(activity, ready)
                return
            }
        }

        // 2) The small earn-coins card, only when an ad is loaded so a tap always works.
        if (!_earnPromptVisible.value && _rewardedAllowed.value && _rewardedReady.value) {
            val due = AdPolicy.earnPromptSkipReason(prefs.read(nowMillis), nowMillis, fgTotalMillis() - earnClockStartFg, calmScreen)
            if (due == null) {
                Log.d(TAG, "earn card: showing")
                _earnPromptVisible.value = true
            }
        }
    }

    /** The card was closed, timed out or used: its next appearance is one full interval from now. */
    fun onEarnPromptClosed() {
        _earnPromptVisible.value = false
        earnClockStartFg = fgTotalMillis()
    }

    /** True when this phone may show a movie ad break (ads on, couple not Pro, consent done). */
    fun movieBreakAllowed(): Boolean = eligible

    /**
     * The ad break in the middle of a movie. Plays one full-screen ad now if one is ready, then calls [onClosed]
     * (also when no ad could be shown, so the movie never waits on an ad). Google's ads close with their own
     * close button / end card; an app cannot close them for the person.
     */
    fun showMovieBreak(activity: Activity, onClosed: () -> Unit) {
        if (!eligible || showing || activity.isFinishing || activity.isDestroyed) { onClosed(); return }
        scope.launch {
            val nowMillis = System.currentTimeMillis()
            if (AdPolicy.movieBreakSkipReason(prefs.read(nowMillis), nowMillis) != null) { onClosed(); return@launch }
            val ready = readyInterstitial()
            if (ready == null) {
                loadInterstitial()
                Log.d(TAG, "movie break: no ad loaded, skipping")
                onClosed()
                return@launch
            }
            show(activity, ready, onClosed)
        }
    }

    /** The loaded ad if it is still fresh, otherwise null (and the stale one is dropped). */
    private fun readyInterstitial(): InterstitialAd? {
        val ad = interstitial ?: return null
        if (SystemClock.elapsedRealtime() - interstitialLoadedAt < AD_MAX_AGE_MILLIS) return ad
        interstitial = null
        return null
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
                recheckAfter = RETRY_AFTER_FAILURE_MILLIS // look again soon, the grace is only minutes long
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
        _rewardedAllowed.value = AdsConfig.rewardedEnabled
        loadInterstitial()
        loadRewarded()
    }

    private fun disable() {
        eligible = false
        _bannersAllowed.value = false
        _rewardedAllowed.value = false
        interstitial = null
        rewarded = null
        _rewardedReady.value = false
    }

    // ── rewarded ads (the person chooses to watch; coins / a free spin are paid by the server) ─────────

    private fun loadRewarded() {
        if (!AdsConfig.rewardedEnabled || !eligible || loadingRewarded || showing) return
        val current = rewarded
        if (current != null) {
            if (SystemClock.elapsedRealtime() - rewardedLoadedAt < AD_MAX_AGE_MILLIS) return
            rewarded = null
            _rewardedReady.value = false
        }
        if (SystemClock.elapsedRealtime() < nextRewardedLoadAllowedAt) return
        loadingRewarded = true
        RewardedAd.load(
            appContext,
            AdsConfig.rewardedId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    loadingRewarded = false
                    rewardedFailures = 0
                    Log.d(TAG, "rewarded loaded")
                    rewarded = ad
                    rewardedLoadedAt = SystemClock.elapsedRealtime()
                    _rewardedReady.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingRewarded = false
                    rewarded = null
                    _rewardedReady.value = false
                    Log.d(TAG, "rewarded failed: ${error.code} ${error.message}")
                    rewardedFailures += 1
                    nextRewardedLoadAllowedAt = SystemClock.elapsedRealtime() +
                        minOf(LOAD_BACKOFF_MILLIS * rewardedFailures, LOAD_BACKOFF_MAX_MILLIS)
                }
            },
        )
    }

    /**
     * Plays the loaded rewarded ad. [onEarned] runs when Google says the person earned the reward (they watched it
     * to the end); [onFail] runs when no ad could be shown or it was closed early. Exactly one of the two runs.
     */
    fun showRewarded(activity: Activity, onEarned: () -> Unit, onFail: () -> Unit) {
        val ad = rewarded
        if (!eligible || showing || ad == null || activity.isFinishing || activity.isDestroyed) {
            loadRewarded()
            onFail()
            return
        }
        showing = true
        rewarded = null
        _rewardedReady.value = false
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                showing = false
                resetUsage() // no timed ad right after the person just watched one
                earnClockStartFg = fgTotalMillis()
                loadRewarded()
                if (!earned) onFail()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                showing = false
                loadRewarded()
                onFail()
            }
        }
        ad.show(activity) {
            earned = true
            onEarned()
        }
    }

    // ── full-screen ads ──────────────────────────────────────────────────────

    private fun loadInterstitial() {
        if (!eligible || interstitial != null || loadingInterstitial || showing) return
        if (SystemClock.elapsedRealtime() < nextLoadAllowedAt) return
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
                    // "No fill" is common for new ad units: ask again later, slowly, but never give up for good.
                    val wait = minOf(LOAD_BACKOFF_MILLIS * loadFailures, LOAD_BACKOFF_MAX_MILLIS)
                    nextLoadAllowedAt = SystemClock.elapsedRealtime() + wait
                }
            },
        )
    }

    private fun show(activity: Activity, ad: InterstitialAd, onClosed: (() -> Unit)? = null) {
        showing = true
        interstitial = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                scope.launch { prefs.recordShown(System.currentTimeMillis(), LocalDate.now().toEpochDay()) }
            }

            override fun onAdDismissedFullScreenContent() {
                showing = false
                resetUsage() // the next ad is one full interval from now
                earnClockStartFg = fgTotalMillis() // and the earn card does not follow straight after
                loadInterstitial() // get the next one ready in the background
                onClosed?.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                showing = false
                loadInterstitial()
                onClosed?.invoke()
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
        const val CONSENT_TIMEOUT_MILLIS = 90_000L
        const val TICK_MILLIS = 5_000L
        const val START_AD_DELAY_MILLIS = 500L
        const val IDLE_PENDING_EXPIRES_MILLIS = 2L * 60 * 1000
        const val LOAD_BACKOFF_MILLIS = 30_000L
        const val LOAD_BACKOFF_MAX_MILLIS = 5L * 60 * 1000
    }
}
