package com.twinhearts.app.feature.globe.domain

import com.twinhearts.app.feature.globe.domain.model.Achievement
import com.twinhearts.app.feature.globe.domain.model.AchievementId
import com.twinhearts.app.feature.globe.domain.model.ArcLeg
import com.twinhearts.app.feature.globe.domain.model.CityCount
import com.twinhearts.app.feature.globe.domain.model.CityGroup
import com.twinhearts.app.feature.globe.domain.model.CountryGlow
import com.twinhearts.app.feature.globe.domain.model.GeoPoint
import com.twinhearts.app.feature.globe.domain.model.GlobeFilter
import com.twinhearts.app.feature.globe.domain.model.GlobeSummary
import com.twinhearts.app.feature.globe.domain.model.HeatCell
import com.twinhearts.app.feature.globe.domain.model.MediaKind
import com.twinhearts.app.feature.globe.domain.model.Memory
import com.twinhearts.app.feature.globe.domain.model.MemoryDraft
import com.twinhearts.app.feature.globe.domain.model.MoodCount
import com.twinhearts.app.feature.globe.domain.model.TravelStats
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * Why this file exists: every rule of the Memory Globe that is not drawing or networking lives here:
 * filters, city groups, trip legs, country glows, stats, achievements, money and date text.
 * The rules are the ones in the web app's globe.html, rewritten as small testable functions.
 */

/** The seven moods of the form, exactly as the web app saves them. */
val MOOD_CHOICES = listOf("😊 Happy", "🥰 Romantic", "😌 Peaceful", "😤 Tense", "🤩 Excited", "😔 Sad", "🥹 Emotional")

val WEATHER_CHOICES = listOf("Sunny", "Cloudy", "Rainy", "Snowy", "Windy", "Stormy", "Foggy", "Clear")

val CURRENCY_CHOICES = listOf("INR", "USD", "EUR", "GBP", "JPY", "AED", "SGD")

/** The three mood chips of the filter bar. */
val FILTER_MOODS = listOf("😊" to "Happy", "🥰" to "Romantic", "🤩" to "Excited")

private const val EARTH_RADIUS_KM = 6371.0
private const val MAX_YEAR_CHIPS = 4
private const val TOP_CITIES = 8
private const val HEAT_MONTHS = 12
private const val MAX_HEAT_LEVEL = 4

// ── filters, groups, summaries ───────────────────────────────────────────────────────────────

/** Keeps the memories the selected chip allows. */
fun filterMemories(all: List<Memory>, filter: GlobeFilter): List<Memory> = when (filter) {
    GlobeFilter.All -> all
    is GlobeFilter.Year -> all.filter { it.dateFrom?.startsWith(filter.year) == true }
    is GlobeFilter.Mood -> all.filter { it.mood.isNotBlank() && it.mood.startsWith(filter.emoji) }
}

/** Years that have memories, newest first. The web app had 2025 and 2024 typed in; these follow the data. */
fun availableYears(all: List<Memory>): List<String> =
    all.mapNotNull { it.dateFrom?.take(4)?.takeIf { y -> y.length == 4 && y.all(Char::isDigit) } }
        .distinct()
        .sortedDescending()
        .take(MAX_YEAR_CHIPS)

/** One group per city + country, in the order they first appear. */
fun groupByCity(memories: List<Memory>): List<CityGroup> {
    val groups = LinkedHashMap<String, MutableList<Memory>>()
    memories.forEach { groups.getOrPut(it.cityKey) { mutableListOf() }.add(it) }
    return groups.map { (key, list) ->
        val first = list.first()
        CityGroup(key, first.city, first.country, first.lat, first.lng, first.mood, list)
    }
}

fun summaryOf(memories: List<Memory>): GlobeSummary = GlobeSummary(
    cities = memories.map { it.city }.distinct().size,
    countries = memories.map { it.country }.distinct().size,
    memories = memories.size,
    totalCost = memories.sumOf { it.tripCost ?: 0.0 },
)

/** Lines between memories that follow each other in time. Staying in one city draws nothing. */
fun arcLegs(memories: List<Memory>): List<ArcLeg> {
    val sorted = memories.filter { !it.dateFrom.isNullOrBlank() }.sortedBy { it.dateFrom }
    val legs = mutableListOf<ArcLeg>()
    for (i in 0 until sorted.size - 1) {
        val a = sorted[i]
        val b = sorted[i + 1]
        if (a.city == b.city && a.country == b.country) continue
        val from = GeoPoint(a.lat, a.lng)
        val to = GeoPoint(b.lat, b.lng)
        legs += ArcLeg(i, from, to, haversineKm(from, to))
    }
    return legs
}

/** One glow per country, placed at the average of its memories. */
fun countryGlows(memories: List<Memory>): List<CountryGlow> {
    class Acc(var lat: Double = 0.0, var lng: Double = 0.0, var count: Int = 0, val cities: MutableSet<String> = mutableSetOf())
    val byCountry = LinkedHashMap<String, Acc>()
    memories.filter { it.country.isNotBlank() }.forEach { m ->
        val acc = byCountry.getOrPut(m.country) { Acc() }
        acc.lat += m.lat
        acc.lng += m.lng
        acc.count += 1
        acc.cities += m.city
    }
    return byCountry.map { (country, a) ->
        CountryGlow(country, GeoPoint(a.lat / a.count, a.lng / a.count), a.cities.size, a.count)
    }
}

// ── stats page ───────────────────────────────────────────────────────────────────────────────

/** Travel Stats. [now] is a parameter so tests do not depend on today's date. */
fun travelStats(all: List<Memory>, now: YearMonth): TravelStats {
    val cities = all.map { it.city }.distinct()
    val countries = all.map { it.country }.distinct()
    val totalCost = all.sumOf { it.tripCost ?: 0.0 }
    val photos = all.sumOf { m -> m.mediaOf(MediaKind.Photo).size }
    val voices = all.sumOf { m -> m.mediaOf(MediaKind.Voice).size }

    val cityFreq = LinkedHashMap<String, Int>()
    all.forEach { cityFreq[it.city] = (cityFreq[it.city] ?: 0) + 1 }
    val top = cityFreq.entries.sortedByDescending { it.value }.take(TOP_CITIES).map { CityCount(it.key, it.value) }

    val moodFreq = LinkedHashMap<String, Int>()
    all.filter { it.mood.isNotBlank() }.forEach { m ->
        val emoji = m.mood.substringBefore(' ')
        moodFreq[emoji] = (moodFreq[emoji] ?: 0) + 1
    }

    val perMonth = HashMap<String, Int>()
    all.forEach { m -> m.dateFrom?.takeIf { it.length >= 7 }?.let { d -> perMonth.merge(d.take(7), 1, Int::plus) } }
    val heat = (HEAT_MONTHS - 1 downTo 0).map { back ->
        val month = now.minusMonths(back.toLong()).toString()
        val count = perMonth[month] ?: 0
        HeatCell(month, count, min(count, MAX_HEAT_LEVEL))
    }

    val achievements = listOf(
        Achievement(AchievementId.FirstMemory, all.size >= 1),
        Achievement(AchievementId.CityExplorer, cities.size >= 3),
        Achievement(AchievementId.WorldTraveler, countries.size >= 3),
        Achievement(AchievementId.BigSpender, totalCost >= 10_000),
        Achievement(AchievementId.Photographer, photos >= 20),
        Achievement(AchievementId.ContinentHopper, countries.size >= 5),
        Achievement(AchievementId.Romantics, all.size >= 5),
        Achievement(AchievementId.GlobeMasters, cities.size >= 10),
        Achievement(AchievementId.Storytellers, voices >= 5),
    )
    return TravelStats(
        memories = all.size,
        cities = cities.size,
        countries = countries.size,
        totalCost = totalCost,
        photos = photos,
        topCities = top,
        moods = moodFreq.map { MoodCount(it.key, it.value) },
        heat = heat,
        achievements = achievements,
    )
}

// ── text formatting ──────────────────────────────────────────────────────────────────────────

/** 1234567 -> "12,34,567": Indian digit grouping, like the web app's `toLocaleString('en-IN')`. */
fun formatIndian(value: Long): String {
    val negative = value < 0
    val digits = kotlin.math.abs(value).toString()
    val out = if (digits.length <= 3) {
        digits
    } else {
        val head = digits.dropLast(3)
        val tail = digits.takeLast(3)
        head.reversed().chunked(2).joinToString(",").reversed() + "," + tail
    }
    return if (negative) "-$out" else out
}

/** A cost as written on a card: whole numbers without decimals, others with up to two. */
fun formatAmount(value: Double): String {
    val whole = value.roundToLong()
    if (kotlin.math.abs(value - whole) < 0.005) return formatIndian(whole)
    val intPart = value.toLong()
    val fraction = "%.2f".format(Locale.ROOT, kotlin.math.abs(value - intPart)).drop(1).trimEnd('0')
    return formatIndian(intPart) + fraction
}

fun currencySymbol(code: String): String = when (code.trim().uppercase(Locale.ROOT)) {
    "", "INR" -> "₹"
    "USD" -> "$"
    "EUR" -> "€"
    "GBP" -> "£"
    "JPY" -> "¥"
    "SGD" -> "S$"
    else -> code.trim().uppercase(Locale.ROOT) + " "
}

/** "₹12,500" for the memory's own currency. */
fun formatMoney(amount: Double, currency: String): String = currencySymbol(currency) + formatAmount(amount)

/** The "spent" chip: rupees, rounded. */
fun spentLabel(totalCost: Double): String = "₹" + formatIndian(if (totalCost > 0) totalCost.roundToLong() else 0L)

/** The stats page tile: "₹12k", or "₹0". */
fun spentShortLabel(totalCost: Double): String = if (totalCost > 0) "₹" + (totalCost / 1000).roundToLong() + "k" else "₹0"

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

/** "2025-03-12" -> "12 Mar 2025". Anything that is not a date comes back unchanged. */
fun formatDate(value: String?): String {
    if (value.isNullOrBlank()) return ""
    return runCatching { LocalDate.parse(value.take(10)).format(DATE_FORMAT) }.getOrDefault(value)
}

/** "12 Mar 2025 → 15 Mar 2025", or just the first date when there is no other end. */
fun formatDateRange(from: String?, to: String?): String {
    val start = formatDate(from)
    if (to.isNullOrBlank() || to == from) return start
    return "$start → ${formatDate(to)}"
}

/** First [limit] characters of [text], with "..." when it was cut (the web cards do the same). */
fun preview(text: String, limit: Int): String = if (text.length > limit) text.take(limit) + "..." else text

/** A flag from a two-letter country code; the globe emoji when the code is unknown. */
fun flagEmoji(iso2: String?): String {
    val code = iso2?.trim()?.uppercase(Locale.ROOT).orEmpty()
    if (code.length != 2 || !code.all { it in 'A'..'Z' }) return "🌍"
    val base = 0x1F1E6
    return String(Character.toChars(base + (code[0] - 'A'))) + String(Character.toChars(base + (code[1] - 'A')))
}

// ── weather and places ───────────────────────────────────────────────────────────────────────

private val WEATHER_NAMES = mapOf(
    0 to "Clear", 1 to "Mostly Clear", 2 to "Partly Cloudy", 3 to "Cloudy", 45 to "Foggy",
    51 to "Drizzle", 61 to "Rainy", 71 to "Snowy", 80 to "Showers", 95 to "Stormy",
)

/** Open-Meteo weather code -> the web app's weather word. */
fun weatherNameFor(code: Int): String = WEATHER_NAMES[code] ?: WEATHER_NAMES[(code / 10) * 10] ?: "Clear"

/** The form's weather choice for [code], or null when the word is not one of the choices. */
fun weatherChoiceFor(code: Int): String? = weatherNameFor(code).takeIf { it in WEATHER_CHOICES }

private val COORDINATES = Regex("""^\s*(-?\d+\.?\d*)\s*,\s*(-?\d+\.?\d*)\s*$""")

/** "12.97, 77.59" typed into the search box -> a point, or null when it is not coordinates. */
fun parseCoordinates(text: String): GeoPoint? {
    val m = COORDINATES.matchEntire(text) ?: return null
    val lat = m.groupValues[1].toDoubleOrNull() ?: return null
    val lng = m.groupValues[2].toDoubleOrNull() ?: return null
    if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
    return GeoPoint(lat, lng)
}

/** The form starts from this when a memory is edited. */
fun draftOf(m: Memory): MemoryDraft = MemoryDraft(
    city = m.city,
    country = m.country,
    state = m.state,
    lat = m.lat,
    lng = m.lng,
    locationText = if (m.city.isNotBlank() && m.country.isNotBlank()) "${m.city}, ${m.country}" else "",
    tripName = m.tripName,
    dateFrom = m.dateFrom.orEmpty(),
    dateTo = m.dateTo.orEmpty(),
    mood = m.mood,
    weather = m.weather,
    temperature = m.temperature,
    cost = m.tripCost?.let { if (it == it.roundToLong().toDouble()) it.roundToLong().toString() else it.toString() }.orEmpty(),
    currency = m.currency.ifBlank { "INR" },
    notes = m.notes,
    favoriteMoment = m.favoriteMoment,
    restaurants = m.restaurants,
    hotels = m.hotels,
    gifts = m.gifts,
)

/** "Add Memory Here": a fresh form for a city that already has a pin. */
fun draftForCity(group: CityGroup): MemoryDraft = MemoryDraft(
    city = group.city,
    country = group.country,
    lat = group.lat,
    lng = group.lng,
    locationText = "${group.city}, ${group.country}",
)

/** A cost typed in the form. Empty or zero means "no cost" (the web app did the same). */
fun parseCost(text: String): Double? = text.trim().toDoubleOrNull()?.takeIf { it != 0.0 }

// ── geography ────────────────────────────────────────────────────────────────────────────────

fun haversineKm(a: GeoPoint, b: GeoPoint): Double {
    val dLat = Math.toRadians(b.lat - a.lat)
    val dLng = Math.toRadians(b.lng - a.lng)
    val h = sin(dLat / 2).let { it * it } +
        cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLng / 2).let { it * it }
    return EARTH_RADIUS_KM * 2 * atan2(sqrt(h), sqrt(1 - h))
}

/** Points along the shortest path over the globe between [a] and [b], both ends included. */
fun greatCircle(a: GeoPoint, b: GeoPoint, steps: Int): List<GeoPoint> {
    val n = max(steps, 1)
    val (ax, ay, az) = unitVector(a)
    val (bx, by, bz) = unitVector(b)
    val dot = (ax * bx + ay * by + az * bz).coerceIn(-1.0, 1.0)
    val omega = kotlin.math.acos(dot)
    if (omega < 1e-9) return listOf(a, b)
    val sinO = sin(omega)
    return (0..n).map { i ->
        val t = i.toDouble() / n
        val k1 = sin((1 - t) * omega) / sinO
        val k2 = sin(t * omega) / sinO
        val x = k1 * ax + k2 * bx
        val y = k1 * ay + k2 * by
        val z = k1 * az + k2 * bz
        GeoPoint(Math.toDegrees(asin(z.coerceIn(-1.0, 1.0))), Math.toDegrees(atan2(y, x)))
    }
}

private fun unitVector(p: GeoPoint): Triple<Double, Double, Double> {
    val lat = p.lat * PI / 180.0
    val lng = p.lng * PI / 180.0
    return Triple(cos(lat) * cos(lng), cos(lat) * sin(lng), sin(lat))
}
