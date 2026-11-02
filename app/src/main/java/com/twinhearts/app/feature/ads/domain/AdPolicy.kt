package com.twinhearts.app.feature.ads.domain

/*
 * Why this file exists: ONE place that decides "is it fair to show an ad right now?".
 * It is plain Kotlin (no Android), so the rules are unit tested and easy to tune.
 * Everything a person would call "too many ads" is a number in [AdRules].
 */

/** What this phone remembers about ads between app launches. */
data class AdHistory(
    val firstSeenAtMillis: Long,
    val sessionCount: Int,
    /** 0 = no full-screen ad has ever been shown. */
    val lastInterstitialAtMillis: Long,
    val interstitialsToday: Int,
    /** The calendar day (epoch day) that [interstitialsToday] belongs to. */
    val countDay: Long,
)

/** The knobs. Change a number here and every rule below follows. */
object AdRules {
    /**
     * HOW OFTEN a full-screen ad becomes DUE: once per this much time the app is open on screen.
     * A due ad never covers chat, calls, games, music/sing-together or movies: it waits and plays
     * as soon as the person is back on a calm page (Home, planners, personal pages...).
     */
    const val INTERSTITIAL_INTERVAL_MILLIS = 8L * 60 * 1000

    /**
     * Sing Together and Movies get ONE ad at the very start, then are never interrupted.
     * That start-ad is skipped if another ad played less than this long ago.
     */
    const val ENTRY_MIN_GAP_MILLIS = 3L * 60 * 1000

    /** A calm screen must be on screen this long before a due ad may cover it. */
    const val SETTLE_ON_SCREEN_MILLIS = 1_500L

    /** A brand-new install sees no ads for this long (kept short on purpose: the app is free with ads). */
    const val NEW_USER_GRACE_MILLIS = 5L * 60 * 1000
    const val MIN_SESSIONS = 1

    /** Safety net only. At one ad per 8 minutes this is about 3 hours of non-stop use. */
    const val MAX_PER_DAY = 24
}

/** Why an ad was NOT shown. Handy for tests and for the debug log. */
enum class AdSkipReason {
    NewUserGrace,
    TooFewSessions,
    NotDueYet,
    TooSoonSinceLastAd,
    DailyLimit,
    ScreenNotSuitable,
}

object AdPolicy {

    /** The small banner on Home: only the new-user protection applies. Null = allowed. */
    fun bannerSkipReason(history: AdHistory, nowMillis: Long): AdSkipReason? = graceReason(history, nowMillis)

    /**
     * Timed full-screen ad. Null = show it now.
     * [usageSinceLastAdMillis] is how long the app has been open on screen since the last ad
     * (or since it was opened). [screenIsSuitable] is false on screens that must never be covered
     * (calls, payment, sign-in...): the ad simply waits until the person is somewhere else.
     */
    fun interstitialSkipReason(
        history: AdHistory,
        nowMillis: Long,
        epochDay: Long,
        usageSinceLastAdMillis: Long,
        screenIsSuitable: Boolean,
    ): AdSkipReason? {
        graceReason(history, nowMillis)?.let { return it }
        if (usageSinceLastAdMillis < AdRules.INTERSTITIAL_INTERVAL_MILLIS) return AdSkipReason.NotDueYet
        val last = history.lastInterstitialAtMillis
        if (last > 0 && nowMillis - last < AdRules.INTERSTITIAL_INTERVAL_MILLIS) return AdSkipReason.TooSoonSinceLastAd
        val today = if (history.countDay == epochDay) history.interstitialsToday else 0
        if (today >= AdRules.MAX_PER_DAY) return AdSkipReason.DailyLimit
        if (!screenIsSuitable) return AdSkipReason.ScreenNotSuitable
        return null
    }

    /**
     * The one ad at the start of Sing Together or a Movie. Null = show it.
     * Skipped when another ad played a moment ago, so nobody gets two ads back to back.
     */
    fun entrySkipReason(history: AdHistory, nowMillis: Long, epochDay: Long): AdSkipReason? {
        graceReason(history, nowMillis)?.let { return it }
        val last = history.lastInterstitialAtMillis
        if (last > 0 && nowMillis - last < AdRules.ENTRY_MIN_GAP_MILLIS) return AdSkipReason.TooSoonSinceLastAd
        val today = if (history.countDay == epochDay) history.interstitialsToday else 0
        if (today >= AdRules.MAX_PER_DAY) return AdSkipReason.DailyLimit
        return null
    }

    private fun graceReason(history: AdHistory, nowMillis: Long): AdSkipReason? = when {
        nowMillis - history.firstSeenAtMillis < AdRules.NEW_USER_GRACE_MILLIS -> AdSkipReason.NewUserGrace
        history.sessionCount < AdRules.MIN_SESSIONS -> AdSkipReason.TooFewSessions
        else -> null
    }
}
