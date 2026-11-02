package com.twinhearts.app.feature.ads

import com.twinhearts.app.feature.ads.domain.AdHistory
import com.twinhearts.app.feature.ads.domain.AdPolicy
import com.twinhearts.app.feature.ads.domain.AdRules
import com.twinhearts.app.feature.ads.domain.AdSkipReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Locks in the "one ad per interval, never on the wrong screen, never too many" promises. */
class AdPolicyTest {

    private val day = 24L * 60 * 60 * 1000
    private val now = 1_800_000_000_000L
    private val today = now / day
    private val interval = AdRules.INTERSTITIAL_INTERVAL_MILLIS

    /** A couple who has used the app for a week: past the grace, ready for ads. */
    private fun veteran(lastAd: Long = 0L, countToday: Int = 0) = AdHistory(
        firstSeenAtMillis = now - 7 * day,
        sessionCount = 10,
        lastInterstitialAtMillis = lastAd,
        interstitialsToday = countToday,
        countDay = today,
    )

    private fun interstitial(h: AdHistory, usage: Long = interval, suitable: Boolean = true) =
        AdPolicy.interstitialSkipReason(h, now, today, usage, suitable)

    @Test fun `a timed ad becomes due every 8 minutes`() {
        assertEquals(8L * 60_000, AdRules.INTERSTITIAL_INTERVAL_MILLIS)
    }

    private fun entry(h: AdHistory) = AdPolicy.entrySkipReason(h, now, today)

    @Test fun `sing together or a movie gets one ad at the start`() {
        assertNull(entry(veteran()))
        assertNull(entry(veteran(lastAd = now - AdRules.ENTRY_MIN_GAP_MILLIS)))
    }

    @Test fun `no start ad right after another ad`() {
        assertEquals(AdSkipReason.TooSoonSinceLastAd, entry(veteran(lastAd = now - 60_000L)))
    }

    @Test fun `a start ad respects the new-install grace and the daily limit`() {
        val fresh = veteran().copy(firstSeenAtMillis = now - AdRules.NEW_USER_GRACE_MILLIS / 2)
        assertEquals(AdSkipReason.NewUserGrace, entry(fresh))
        assertEquals(AdSkipReason.DailyLimit, entry(veteran(lastAd = now - day / 2, countToday = AdRules.MAX_PER_DAY)))
    }

    @Test fun `a brand-new install sees nothing for the first minutes`() {
        val h = veteran().copy(firstSeenAtMillis = now - AdRules.NEW_USER_GRACE_MILLIS / 2)
        assertEquals(AdSkipReason.NewUserGrace, AdPolicy.bannerSkipReason(h, now))
        assertEquals(AdSkipReason.NewUserGrace, interstitial(h))
    }

    @Test fun `needs at least the minimum number of app launches`() {
        val h = veteran().copy(sessionCount = AdRules.MIN_SESSIONS - 1)
        assertEquals(AdSkipReason.TooFewSessions, AdPolicy.bannerSkipReason(h, now))
    }

    @Test fun `a settled couple may get a banner and a full-screen ad`() {
        assertNull(AdPolicy.bannerSkipReason(veteran(), now))
        assertNull(interstitial(veteran()))
    }

    @Test fun `no full-screen ad before the interval of use has passed`() {
        assertEquals(AdSkipReason.NotDueYet, interstitial(veteran(), usage = interval - 1))
        assertNull(interstitial(veteran(), usage = interval))
    }

    @Test fun `never two ads inside one interval, even across app launches`() {
        val h = veteran(lastAd = now - interval / 2)
        assertEquals(AdSkipReason.TooSoonSinceLastAd, interstitial(h))
        assertNull(interstitial(veteran(lastAd = now - interval)))
    }

    @Test fun `a due ad waits when the screen must not be covered`() {
        assertEquals(AdSkipReason.ScreenNotSuitable, interstitial(veteran(), suitable = false))
    }

    @Test fun `stops at the daily limit and starts again tomorrow`() {
        val full = veteran(lastAd = now - 60 * 60_000L, countToday = AdRules.MAX_PER_DAY)
        assertEquals(AdSkipReason.DailyLimit, interstitial(full))
        // The counter belongs to yesterday: today starts at zero.
        assertNull(interstitial(full.copy(countDay = today - 1)))
    }
}
