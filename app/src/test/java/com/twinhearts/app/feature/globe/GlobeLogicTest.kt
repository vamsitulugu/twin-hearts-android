package com.twinhearts.app.feature.globe

import com.twinhearts.app.feature.globe.domain.arcLegs
import com.twinhearts.app.feature.globe.domain.availableYears
import com.twinhearts.app.feature.globe.domain.countryGlows
import com.twinhearts.app.feature.globe.domain.currencySymbol
import com.twinhearts.app.feature.globe.domain.draftOf
import com.twinhearts.app.feature.globe.domain.filterMemories
import com.twinhearts.app.feature.globe.domain.flagEmoji
import com.twinhearts.app.feature.globe.domain.formatAmount
import com.twinhearts.app.feature.globe.domain.formatDate
import com.twinhearts.app.feature.globe.domain.formatDateRange
import com.twinhearts.app.feature.globe.domain.formatIndian
import com.twinhearts.app.feature.globe.domain.greatCircle
import com.twinhearts.app.feature.globe.domain.groupByCity
import com.twinhearts.app.feature.globe.domain.haversineKm
import com.twinhearts.app.feature.globe.domain.model.AchievementId
import com.twinhearts.app.feature.globe.domain.model.GeoPoint
import com.twinhearts.app.feature.globe.domain.model.GlobeFilter
import com.twinhearts.app.feature.globe.domain.model.MediaKind
import com.twinhearts.app.feature.globe.domain.model.moodEmojiOf
import com.twinhearts.app.feature.globe.domain.parseCoordinates
import com.twinhearts.app.feature.globe.domain.parseCost
import com.twinhearts.app.feature.globe.domain.spentLabel
import com.twinhearts.app.feature.globe.domain.summaryOf
import com.twinhearts.app.feature.globe.domain.travelStats
import com.twinhearts.app.feature.globe.domain.weatherChoiceFor
import com.twinhearts.app.feature.globe.domain.weatherNameFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class GlobeLogicTest {
    private val paris1 = memory("1", "Paris", "France", 48.85, 2.35, "2025-03-01", cost = 5000.0)
    private val paris2 = memory("2", "Paris", "France", 48.85, 2.35, "2025-03-05", "😊 Happy", 2500.0)
    private val rome = memory("3", "Rome", "Italy", 41.9, 12.5, "2024-06-01", "🤩 Excited")
    private val goa = memory("4", "Goa", "India", 15.3, 74.0, null)
    private val all = listOf(paris1, paris2, rome, goa)

    @Test fun indianGrouping() {
        assertEquals("999", formatIndian(999))
        assertEquals("1,000", formatIndian(1000))
        assertEquals("1,00,000", formatIndian(100000))
        assertEquals("12,34,567", formatIndian(1234567))
        assertEquals("1,234.5", formatAmount(1234.5))
    }

    @Test fun moneyAndCurrency() {
        assertEquals("₹", currencySymbol(""))
        assertEquals("$", currencySymbol("usd"))
        assertEquals("₹0", spentLabel(0.0))
        assertEquals("₹12,500", spentLabel(12500.0))
    }

    @Test fun dates() {
        assertEquals("12 Mar 2025", formatDate("2025-03-12"))
        assertEquals("", formatDate(null))
        assertEquals("12 Mar 2025 → 15 Mar 2025", formatDateRange("2025-03-12", "2025-03-15"))
        assertEquals("12 Mar 2025", formatDateRange("2025-03-12", "2025-03-12"))
    }

    @Test fun flags() {
        assertEquals("🇮🇳", flagEmoji("IN"))
        assertEquals("🌍", flagEmoji(null))
        assertEquals("🌍", flagEmoji("XYZ"))
    }

    @Test fun moodEmoji() {
        assertEquals("🥰", moodEmojiOf("🥰 Romantic"))
        assertEquals("❤️", moodEmojiOf(""))
    }

    @Test fun filters() {
        assertEquals(4, filterMemories(all, GlobeFilter.All).size)
        assertEquals(listOf(rome), filterMemories(all, GlobeFilter.Year("2024")))
        assertEquals(listOf(paris2), filterMemories(all, GlobeFilter.Mood("😊")))
        assertEquals(listOf("2025", "2024"), availableYears(all))
    }

    @Test fun grouping_keepsFirstSeenOrder() {
        val groups = groupByCity(all)
        assertEquals(listOf("Paris,France", "Rome,Italy", "Goa,India"), groups.map { it.key })
        assertEquals(2, groups.first().count)
    }

    @Test fun summary() {
        val s = summaryOf(all)
        assertEquals(3, s.cities)
        assertEquals(3, s.countries)
        assertEquals(4, s.memories)
        assertEquals(7500.0, s.totalCost, 0.0)
    }

    @Test fun legs_followTimeAndSkipSameCity() {
        val legs = arcLegs(all)
        assertEquals(1, legs.size)
        assertEquals(0, legs[0].index)
        assertTrue(legs[0].driveable)
        val far = arcLegs(listOf(paris1, memory("9", "Tokyo", "Japan", 35.7, 139.7, "2025-04-01")))
        assertFalse(far.single().driveable)
    }

    @Test fun glows_onePerCountry() {
        assertEquals(3, countryGlows(all).size)
    }

    @Test fun stats_heatTopCitiesAchievements() {
        val st = travelStats(all, YearMonth.of(2025, 4))
        assertEquals(12, st.heat.size)
        assertEquals("2025-04", st.heat.last().month)
        assertEquals(2, st.heat[10].count)
        assertEquals(2, st.heat[10].level)
        assertEquals("Paris", st.topCities.first().city)
        assertTrue(st.achievements.first { it.id == AchievementId.CityExplorer }.unlocked)
        assertFalse(st.achievements.first { it.id == AchievementId.Romantics }.unlocked)
    }

    @Test fun stats_photosAndVoices() {
        val withMedia = memory("5", "A", "B", 1.0, 1.0, media = List(20) { media("p$it", MediaKind.Photo) } + List(5) { media("v$it", MediaKind.Voice) })
        val st = travelStats(listOf(withMedia), YearMonth.of(2025, 1))
        assertTrue(st.achievements.first { it.id == AchievementId.Photographer }.unlocked)
        assertTrue(st.achievements.first { it.id == AchievementId.Storytellers }.unlocked)
    }

    @Test fun weather() {
        assertEquals("Clear", weatherNameFor(63))
        assertEquals("Rainy", weatherChoiceFor(61))
        assertNull(weatherChoiceFor(2))
    }

    @Test fun coordinates() {
        assertEquals(GeoPoint(12.97, 77.59), parseCoordinates("12.97, 77.59"))
        assertNull(parseCoordinates("paris"))
        assertNull(parseCoordinates("99, 10"))
    }

    @Test fun cost() {
        assertEquals(5.5, parseCost("5.5")!!, 0.0)
        assertNull(parseCost("0"))
        assertNull(parseCost(""))
    }

    @Test fun draft_fromMemory() {
        val d = draftOf(paris1)
        assertEquals("Paris, France", d.locationText)
        assertEquals("5000", d.cost)
        assertTrue(d.hasLocation)
    }

    @Test fun geography() {
        val km = haversineKm(GeoPoint(0.0, 0.0), GeoPoint(0.0, 1.0))
        assertTrue(km > 111 && km < 112)
        val gc = greatCircle(GeoPoint(0.0, 0.0), GeoPoint(0.0, 90.0), 4)
        assertEquals(5, gc.size)
        assertEquals(45.0, gc[2].lng, 1e-6)
    }
}
