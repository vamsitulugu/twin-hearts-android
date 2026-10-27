package com.twinhearts.app.feature.us.data.mapper

import com.twinhearts.app.core.data.Session
import com.twinhearts.app.feature.us.data.remote.BucketItemDto
import com.twinhearts.app.feature.us.data.remote.MediaItemDto
import com.twinhearts.app.feature.us.domain.model.CollectionItem
import com.twinhearts.app.feature.us.domain.model.DreamGoal
import com.twinhearts.app.feature.us.domain.model.LoveCounterState
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

/** Converts the session + shared state into the Love Counter domain model. */
fun toLoveCounterState(anniversary: String?, session: Session): LoveCounterState {
    val anniversary = anniversary.takeIf { it.isNotBlank() }
    val started = startedAtFrom(anniversary)
    val today = LocalDate.now()
    val days = if (started != null) ChronoUnit.DAYS.between(started, today).toInt().coerceAtLeast(0) else 0
    val period = if (started != null) Period.between(started, today) else Period.ZERO
    val target = nextAnniversary(anniversary, today)
    val daysLeft = if (target != null) ChronoUnit.DAYS.between(today, target).toInt().coerceAtLeast(0) else 0
    return LoveCounterState(
        anniversary = anniversary,
        startedAt = started?.toString(),
        daysTogether = days,
        years = period.years,
        months = period.months,
        remainingDays = daysLeft,
        remainingHours = daysLeft * 24,
        remainingMinutes = daysLeft * 24 * 60,
        remainingSeconds = daysLeft * 24 * 60 * 60,
    )
}

/** Converts bucket items from shared state into DreamGoal domain models. */
fun toDreamGoals(items: List<BucketItemDto>, myRole: String): List<DreamGoal> {
    return items.mapNotNull { item ->
        val title = item.title ?: return@mapNotNull null
        DreamGoal(
            id = item.id ?: java.util.UUID.randomUUID().toString(),
            title = title,
            emoji = item.emoji ?: "\uD83D\uDE0D",
            description = item.desc,
            targetDate = item.target,
            priority = item.priority ?: "medium",
            progress = item.progress ?: 0,
            completed = item.done == true,
            createdBy = item.by,
        )
    }
}

/** Converts media items from shared state into CollectionItem domain models. */
fun toCollectionItems(items: List<MediaItemDto>): List<CollectionItem> {
    return items.map { item ->
        CollectionItem(
            id = item.id ?: java.util.UUID.randomUUID().toString(),
            type = item.type ?: "photo",
            url = item.url,
            sender = item.by ?: "Partner",
            sentAt = item.ts ?: 0L,
            durationSec = item.duration,
        )
    }
}

private fun startedAtFrom(anniversary: String?): LocalDate? {
    return anniversary?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it.substring(0, 10)) }.getOrNull() }
}

private fun nextAnniversary(anniversary: String?, today: LocalDate): LocalDate? {
    return anniversary?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it.substring(0, 10)) }.getOrNull() }?.let { start ->
        val thisYear = start.withYear(today.year)
        if (thisYear.isBefore(today) || thisYear == today) start.withYear(today.year + 1) else thisYear
    }
}
