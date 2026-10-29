package com.twinhearts.app.feature.profile

import com.twinhearts.app.feature.profile.domain.ProfileMath
import com.twinhearts.app.feature.profile.domain.model.ImportantKind
import com.twinhearts.app.feature.profile.domain.model.LoveLanguage
import com.twinhearts.app.feature.profile.domain.model.LoveTip
import com.twinhearts.app.feature.profile.domain.model.SpecialDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProfileMathTest {
    private val today = LocalDate.of(2026, 10, 27)

    @Test
    fun togetherSpan_countsYearsMonthsDays() {
        val span = ProfileMath.togetherSpan(LocalDate.of(2024, 8, 20), today)
        assertEquals(2, span.years)
        assertEquals(2, span.months)
        assertEquals(7, span.days)
        assertFalse(span.notStarted)
    }

    @Test
    fun togetherSpan_borrowsMonthWhenDayIsSmaller() {
        val span = ProfileMath.togetherSpan(LocalDate.of(2026, 9, 30), today)
        assertEquals(0, span.years)
        assertEquals(0, span.months)
        assertEquals(27, span.days)
    }

    @Test
    fun togetherSpan_futureDate_isNotStarted() {
        assertTrue(ProfileMath.togetherSpan(LocalDate.of(2027, 1, 1), today).notStarted)
    }

    @Test
    fun togetherSpan_today_isZeroDays() {
        val span = ProfileMath.togetherSpan(today, today)
        assertEquals(0, span.days)
        assertFalse(span.notStarted)
    }

    @Test
    fun zodiac_matchesWebTable() {
        assertEquals("Scorpio ♏", ProfileMath.zodiac(LocalDate.of(2000, 10, 25)))
        assertEquals("Capricorn ♑", ProfileMath.zodiac(LocalDate.of(2000, 1, 5)))
        assertEquals("Aquarius ♒", ProfileMath.zodiac(LocalDate.of(2000, 1, 20)))
    }

    @Test
    fun nextYearly_todayCountsAsToday_pastRollsToNextYear() {
        assertEquals(today, ProfileMath.nextYearly(LocalDate.of(1999, 10, 27), today))
        assertEquals(LocalDate.of(2027, 3, 1), ProfileMath.nextYearly(LocalDate.of(2000, 3, 1), today))
    }

    @Test
    fun nextYearly_leapDay_doesNotCrash() {
        assertEquals(LocalDate.of(2027, 2, 28), ProfileMath.nextYearly(LocalDate.of(2000, 2, 29), today))
    }

    @Test
    fun nextImportant_picksSoonestAndIgnoresPastEvents() {
        val next = ProfileMath.nextImportant(
            dates = listOf(SpecialDate("Old", LocalDate.of(2026, 1, 1), "Trip"), SpecialDate("Dinner", LocalDate.of(2026, 11, 20), "Date")),
            anniversary = LocalDate.of(2024, 12, 25),
            myBirthday = LocalDate.of(2000, 11, 2),
            partnerBirthday = null,
            today = today,
        )
        assertEquals(ImportantKind.MyBirthday, next!!.kind)
        assertEquals(6, next.daysLeft)
    }

    @Test
    fun nextImportant_nothing_isNull() {
        assertNull(ProfileMath.nextImportant(emptyList(), null, null, null, today))
    }

    @Test
    fun loveTip_knownPairAndDefault() {
        assertEquals(LoveTip.WordsAndQuality, ProfileMath.loveTip(LoveLanguage.WordsOfAffirmation, LoveLanguage.QualityTime))
        assertEquals(LoveTip.Default, ProfileMath.loveTip(LoveLanguage.ReceivingGifts, LoveLanguage.ReceivingGifts))
    }

    @Test
    fun isOnline_withinFiveMinutes() {
        assertTrue(ProfileMath.isOnline(1_000_000L, 1_000_000L + 4 * 60_000L))
        assertFalse(ProfileMath.isOnline(1_000_000L, 1_000_000L + 6 * 60_000L))
        assertFalse(ProfileMath.isOnline(null, 5L))
    }
}
