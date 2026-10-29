package com.twinhearts.app.feature.profile.domain

import com.twinhearts.app.feature.profile.domain.model.ImportantDate
import com.twinhearts.app.feature.profile.domain.model.ImportantKind
import com.twinhearts.app.feature.profile.domain.model.LoveLanguage
import com.twinhearts.app.feature.profile.domain.model.LoveTip
import com.twinhearts.app.feature.profile.domain.model.SpecialDate
import com.twinhearts.app.feature.profile.domain.model.TogetherSpan
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The small calculations of the Profile page, ported from the web app's profile.js:
 * "together since" span, zodiac sign, next important date and the love-language tip.
 * Pure Kotlin, so they are easy to unit test.
 */
object ProfileMath {

    /** Calendar-style difference: whole years, then months, then days (borrowing like the web code). */
    fun togetherSpan(from: LocalDate, today: LocalDate): TogetherSpan {
        if (from.isAfter(today)) return TogetherSpan(0, 0, 0, notStarted = true)
        var years = today.year - from.year
        var months = today.monthValue - from.monthValue
        var days = today.dayOfMonth - from.dayOfMonth
        if (days < 0) {
            months--
            days += today.minusMonths(1).lengthOfMonth()
        }
        if (months < 0) {
            years--
            months += 12
        }
        return TogetherSpan(years, months, days, notStarted = false)
    }

    /** Western zodiac sign with its symbol, same date table as the web app. */
    fun zodiac(date: LocalDate): String {
        val starts = listOf(
            Triple(1, 20, "Aquarius ♒"), Triple(2, 19, "Pisces ♓"), Triple(3, 21, "Aries ♈"),
            Triple(4, 20, "Taurus ♉"), Triple(5, 21, "Gemini ♊"), Triple(6, 21, "Cancer ♋"),
            Triple(7, 23, "Leo ♌"), Triple(8, 23, "Virgo ♍"), Triple(9, 23, "Libra ♎"),
            Triple(10, 23, "Scorpio ♏"), Triple(11, 22, "Sagittarius ♐"), Triple(12, 22, "Capricorn ♑"),
        )
        var sign = "Capricorn ♑"
        for ((month, day, name) in starts) {
            if (date.monthValue > month || (date.monthValue == month && date.dayOfMonth >= day)) sign = name
        }
        return sign
    }

    /** The next time a yearly date (birthday, anniversary) comes round, today included. */
    fun nextYearly(date: LocalDate, today: LocalDate): LocalDate {
        val thisYear = safeDate(today.year, date)
        return if (thisYear.isBefore(today)) safeDate(today.year + 1, date) else thisYear
    }

    /** 29 February in a non-leap year becomes 28 February instead of crashing. */
    private fun safeDate(year: Int, source: LocalDate): LocalDate =
        LocalDate.of(year, source.month, minOf(source.dayOfMonth, source.month.length(java.time.Year.isLeap(year.toLong()))))

    /**
     * The soonest coming date out of: saved dates (today or later), the anniversary and both birthdays.
     * Returns null when there is nothing coming.
     */
    fun nextImportant(
        dates: List<SpecialDate>,
        anniversary: LocalDate?,
        myBirthday: LocalDate?,
        partnerBirthday: LocalDate?,
        today: LocalDate,
    ): ImportantDate? {
        val all = buildList {
            dates.filter { !it.date.isBefore(today) }.forEach {
                add(ImportantDate(ImportantKind.Event, it.title, it.date, days(today, it.date), it.category))
            }
            anniversary?.let { nextYearly(it, today).let { d -> add(ImportantDate(ImportantKind.Anniversary, "", d, days(today, d), "Anniversary")) } }
            myBirthday?.let { nextYearly(it, today).let { d -> add(ImportantDate(ImportantKind.MyBirthday, "", d, days(today, d), "Birthday")) } }
            partnerBirthday?.let { nextYearly(it, today).let { d -> add(ImportantDate(ImportantKind.PartnerBirthday, "", d, days(today, d), "Birthday")) } }
        }
        return all.minByOrNull { it.date }
    }

    private fun days(from: LocalDate, to: LocalDate): Int = ChronoUnit.DAYS.between(from, to).toInt()

    /** The tip for the two chosen languages, or the general tip when there is no special one. */
    fun loveTip(mine: LoveLanguage, theirs: LoveLanguage): LoveTip = when (mine to theirs) {
        LoveLanguage.WordsOfAffirmation to LoveLanguage.QualityTime -> LoveTip.WordsAndQuality
        LoveLanguage.WordsOfAffirmation to LoveLanguage.ActsOfService -> LoveTip.WordsAndActs
        LoveLanguage.QualityTime to LoveLanguage.PhysicalTouch -> LoveTip.QualityAndTouch
        LoveLanguage.QualityTime to LoveLanguage.WordsOfAffirmation -> LoveTip.QualityAndWords
        LoveLanguage.ActsOfService to LoveLanguage.ReceivingGifts -> LoveTip.ActsAndGifts
        LoveLanguage.PhysicalTouch to LoveLanguage.ReceivingGifts -> LoveTip.TouchAndGifts
        else -> LoveTip.Default
    }

    /** Partner counts as "online" when they shared a location in the last 5 minutes (same as the web). */
    fun isOnline(seenAtMs: Long?, nowMs: Long): Boolean = seenAtMs != null && nowMs - seenAtMs < ONLINE_WINDOW_MS

    private const val ONLINE_WINDOW_MS = 5 * 60_000L
}
