package com.twinhearts.app.feature.settings.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Why this file exists: the small pure rules of Settings, kept apart so they are easy to test. */

enum class PinProblem { NotFourDigits, RepeatedDigits, Sequence }

private val FourDigits = Regex("^\\d{4}$")

/** The same three rules as the server (`validateVaultPin`), so a bad PIN is caught before any request. */
fun pinProblem(pin: String): PinProblem? = when {
    !FourDigits.matches(pin) -> PinProblem.NotFourDigits
    pin.all { it == pin[0] } -> PinProblem.RepeatedDigits
    "0123456789".contains(pin) || "9876543210".contains(pin) -> PinProblem.Sequence
    else -> null
}

/** Settings search: a row matches when the typed text is inside its title or its keywords. */
fun matchesSearch(query: String, title: String, keywords: String): Boolean {
    val needle = query.trim().lowercase()
    return needle.isNotEmpty() && "$title $keywords".lowercase().contains(needle)
}

/** "Sent 5 mins ago" in pieces; the screen turns it into words (plural strings). */
sealed interface SentAgo {
    data object Unknown : SentAgo
    data object JustNow : SentAgo
    data class Minutes(val count: Int) : SentAgo
    data class Hours(val count: Int) : SentAgo
    data class Days(val count: Int) : SentAgo
}

fun sentAgo(sentAt: Instant?, now: Instant): SentAgo {
    if (sentAt == null) return SentAgo.Unknown
    val minutes = maxOf(0L, Duration.between(sentAt, now).toMinutes())
    if (minutes < 1) return SentAgo.JustNow
    if (minutes < 60) return SentAgo.Minutes(minutes.toInt())
    val hours = minutes / 60
    if (hours < 24) return SentAgo.Hours(hours.toInt())
    return SentAgo.Days((hours / 24).toInt())
}

/** Whole days since the anniversary; never negative (a future date shows 0). */
fun daysTogether(anniversary: LocalDate, today: LocalDate): Int =
    maxOf(0L, ChronoUnit.DAYS.between(anniversary, today)).toInt()

/** B, KB, MB, GB with the same decimals as the web's `fmtBytes`. */
fun formatBytes(bytes: Long, locale: Locale = Locale.getDefault()): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024 -> String.format(locale, "%.1f KB", bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> String.format(locale, "%.2f MB", bytes / (1024.0 * 1024))
    else -> String.format(locale, "%.2f GB", bytes / (1024.0 * 1024 * 1024))
}
