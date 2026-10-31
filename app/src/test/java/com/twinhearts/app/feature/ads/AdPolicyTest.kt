package com.twinhearts.app.feature.ads

import com.twinhearts.app.feature.ads.domain.AdHistory
import com.twinhearts.app.feature.ads.domain.AdPolicy
import com.twinhearts.app.feature.ads.domain.AdRules
import com.twinhearts.app.feature.ads.domain.AdSkipReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Locks in the "not too many ads" promises so nobody loosens them by accident. */
class AdPolicyTest {

    private val day = 24L * 60 * 60 * 1000
    private val now = 1_800_000_000_000L
    private val today = now / day

    /** A couple who has used the app for a week: past the grace, ready for ads. */
    private fun veteran(
        lastAd: Long = 0L,
        countToday: Int = 0,
        breaks: Int = 2,
    ) = AdHistory(
        firstSeenAtMillis = now - 7 * day,
        sessionCount = 10,
        lastInterstitialAtMillis = lastAd,
        interstitialsToday = countToday,
        countDay = today,
        breaksSinceLastAd = breaks,
    )

    private fun interstitial(h: AdHistory, sessionAge: Long = 5 * 60_000L, inFeature: Long = 2 * 60_000L) =
        AdPolicy.interstitialSkipReason(h, now, today, sessionAge, inFeature)

    @Test fun `new couple sees nothing for the first 48 hours`() {
        val h = veteran().copy(firstSeenAtMillis = now - 10 * 60 * 60 * 1000)
        assertEquals(AdSkipReason.NewUserGrace, AdPolicy.bannerSkipReason(h, now))
        assertEquals(AdSkipReason.NewUserGrace, interstitial(h))
    }

    @Test fun `needs at least three app launches`() {
        val h = veteran().copy(sessionCount = 2)
        assertEquals(AdSkipReason.TooFewSessions, AdPolicy.bannerSkipReason(h, now))
    }

    @Test fun `a settled couple may get a banner and a full-screen ad`() {
        assertNull(AdPolicy.bannerSkipReason(veteran(), now))
        assertNull(interstitial(veteran()))
    }

    @Test fun `no full-screen ad right after opening the app`() {
        assertEquals(AdSkipReason.SessionTooYoung, interstitial(veteran(), sessionAge = 30_000L))
    }

    @Test fun `a quick visit is not a natural break`() {
        assertEquals(AdSkipReason.TooShortInFeature, interstitial(veteran(), inFeature = 10_000L))
    }

    @Test fun `waits between two full-screen ads`() {
        val h = veteran(lastAd = now - 3 * 60_000L)
        assertEquals(AdSkipReason.TooSoonSinceLastAd, interstitial(h))
        assertNull(interstitial(veteran(lastAd = now - AdRules.MIN_GAP_MILLIS - 1)))
    }

    @Test fun `stops at the daily limit and starts again tomorrow`() {
        val full = veteran(lastAd = now - 60 * 60_000L, countToday = AdRules.MAX_PER_DAY)
        assertEquals(AdSkipReason.DailyLimit, interstitial(full))
        // The counter belongs to yesterday: today starts at zero.
        val yesterday = full.copy(countDay = today - 1)
        assertNull(interstitial(yesterday))
    }

    @Test fun `only every second natural break may show an ad`() {
        assertEquals(AdSkipReason.NotEnoughBreaks, interstitial(veteran(breaks = 1)))
        assertNull(interstitial(veteran(breaks = 2)))
    }
}
