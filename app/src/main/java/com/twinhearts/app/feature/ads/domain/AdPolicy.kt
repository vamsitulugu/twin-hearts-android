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
    /** Natural breaks seen since the last full-screen ad. */
    val breaksSinceLastAd: Int,
)

/** The knobs. Change a number here and every rule below follows. */
object AdRules {
    // TEST VALUES - restore the real ones before release (see below the file)
    const val NEW_USER_GRACE_MILLIS = 0L
    const val MIN_SESSIONS = 1
    const val MIN_SESSION_AGE_MILLIS = 0L
    const val MIN_TIME_IN_FEATURE_MILLIS = 3_000L
    const val MIN_GAP_MILLIS = 0L
    const val MAX_PER_DAY = 20
    const val BREAKS_BETWEEN_ADS = 1
}

/** Why an ad was NOT shown. Handy for tests and for the debug log. */
enum class AdSkipReason {
    NewUserGrace,
    TooFewSessions,
    SessionTooYoung,
    TooShortInFeature,
    TooSoonSinceLastAd,
    DailyLimit,
    NotEnoughBreaks,
}

object AdPolicy {

    /** The small banner on Home: only the new-user protection applies. Null = allowed. */
    fun bannerSkipReason(history: AdHistory, nowMillis: Long): AdSkipReason? = graceReason(history, nowMillis)

    /**
     * Full-screen ad after leaving a feature and landing back on Home. Null = show it.
     * [timeInFeatureMillis] is how long the person stayed in that feature.
     */
    fun interstitialSkipReason(
        history: AdHistory,
        nowMillis: Long,
        epochDay: Long,
        sessionAgeMillis: Long,
        timeInFeatureMillis: Long,
    ): AdSkipReason? {
        graceReason(history, nowMillis)?.let { return it }
        if (sessionAgeMillis < AdRules.MIN_SESSION_AGE_MILLIS) return AdSkipReason.SessionTooYoung
        if (timeInFeatureMillis < AdRules.MIN_TIME_IN_FEATURE_MILLIS) return AdSkipReason.TooShortInFeature
        val last = history.lastInterstitialAtMillis
        if (last > 0 && nowMillis - last < AdRules.MIN_GAP_MILLIS) return AdSkipReason.TooSoonSinceLastAd
        val today = if (history.countDay == epochDay) history.interstitialsToday else 0
        if (today >= AdRules.MAX_PER_DAY) return AdSkipReason.DailyLimit
        if (history.breaksSinceLastAd < AdRules.BREAKS_BETWEEN_ADS) return AdSkipReason.NotEnoughBreaks
        return null
    }

    private fun graceReason(history: AdHistory, nowMillis: Long): AdSkipReason? = when {
        nowMillis - history.firstSeenAtMillis < AdRules.NEW_USER_GRACE_MILLIS -> AdSkipReason.NewUserGrace
        history.sessionCount < AdRules.MIN_SESSIONS -> AdSkipReason.TooFewSessions
        else -> null
    }
}