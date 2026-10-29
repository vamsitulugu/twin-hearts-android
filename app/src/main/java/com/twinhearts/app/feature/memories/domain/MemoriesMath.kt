package com.twinhearts.app.feature.memories.domain

import com.twinhearts.app.feature.memories.domain.model.Memory
import com.twinhearts.app.feature.memories.domain.model.MemoryInsight
import com.twinhearts.app.feature.memories.domain.model.MonthCount
import java.time.LocalDate
import java.time.YearMonth

private const val MONTHS_SHOWN = 6

/** Memories added in each of the last six months (this month last), like the web bars. */
fun monthlyCounts(items: List<Memory>, today: LocalDate): List<MonthCount> {
    val now = YearMonth.from(today)
    return (MONTHS_SHOWN - 1 downTo 0).map { back ->
        val month = now.minusMonths(back.toLong())
        MonthCount(month, items.count { it.date?.let(YearMonth::from) == month })
    }
}

fun memoryInsight(items: List<Memory>, today: LocalDate): MemoryInsight {
    if (items.isEmpty()) return MemoryInsight.AddFirst
    val now = YearMonth.from(today)
    val thisMonth = items.count { it.date?.let(YearMonth::from) == now }
    return if (thisMonth > 0) {
        MemoryInsight.ThisMonth(thisMonth, items.size, items.count { it.isVideo })
    } else {
        MemoryInsight.SoFar(items.size)
    }
}

/** Types the server accepts for photos and videos (routes/media.js `ALLOWED_MEDIA_MIME`). */
private val allowedMime = Regex(
    "^(image/(jpeg|png|gif|webp|heic|heif)|video/(mp4|quicktime|webm|3gpp|x-matroska))$",
    RegexOption.IGNORE_CASE,
)

fun isUploadable(mime: String): Boolean = allowedMime.matches(mime)

fun isVideoMime(mime: String): Boolean = mime.startsWith("video/", ignoreCase = true)

/** The id the web gives a new item: time plus five random letters/digits. */
fun newMemoryId(nowMillis: Long, random: () -> Int): String {
    val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
    return nowMillis.toString() + "_" + (1..5).map { chars[random().mod(chars.length)] }.joinToString("")
}
