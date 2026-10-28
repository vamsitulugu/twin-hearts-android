package com.twinhearts.app.feature.fights.domain

import com.twinhearts.app.feature.fights.domain.model.Fight
import com.twinhearts.app.feature.fights.domain.model.FightInsight
import com.twinhearts.app.feature.fights.domain.model.FightRange
import com.twinhearts.app.feature.fights.domain.model.FightStarter
import com.twinhearts.app.feature.fights.domain.model.FightStats
import com.twinhearts.app.feature.fights.domain.model.FightStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Why this file exists: every number on the Fight Log dashboard, ported 1:1 from the web page
 * (flWindow, flStats, flDelta, the insight rules) so both apps always agree. Pure Kotlin, easy to test.
 */
object FightMath {

    /** Fights from [fromDaysAgo] days back up to (not including) [toDaysAgo] days back; 0 means "until now". */
    fun window(fights: List<Fight>, today: LocalDate, fromDaysAgo: Int, toDaysAgo: Int): List<Fight> {
        val from = today.minusDays(fromDaysAgo.toLong())
        val to = if (toDaysAgo > 0) today.minusDays(toDaysAgo.toLong()) else LocalDate.MAX
        return fights.filter { f ->
            val d = f.localDate ?: return@filter false
            !d.isBefore(from) && d.isBefore(to)
        }
    }

    fun inRange(fights: List<Fight>, today: LocalDate, range: FightRange): List<Fight> =
        range.days?.let { window(fights, today, it, 0) } ?: fights

    /** Fights logged straight as "resolved" have no resolved date: same day. */
    fun resolveDays(f: Fight): Double {
        val resolved = runCatching { LocalDate.parse(f.resolvedDate) }.getOrNull() ?: return 0.0
        val start = f.localDate ?: return 0.0
        return maxOf(0L, ChronoUnit.DAYS.between(start, resolved)).toDouble()
    }

    fun stats(list: List<Fight>): FightStats {
        val resolved = list.filter { it.status == FightStatus.Resolved }
        return FightStats(
            total = list.size,
            resolved = resolved.size,
            open = list.count { it.status == FightStatus.Open },
            intensity = list.takeIf { it.isNotEmpty() }?.let { l -> l.sumOf { it.intensity }.toDouble() / l.size },
            days = resolved.takeIf { it.isNotEmpty() }?.let { l -> l.sumOf { resolveDays(it) } / l.size },
        )
    }

    fun rate(st: FightStats): Int = if (st.total == 0) 0 else ((st.resolved.toDouble() / st.total) * 100).roundToInt()

    /** Percent change from [before] to [now]; null when there is nothing to compare. */
    fun delta(now: Double?, before: Double?): Int? {
        if (now == null || before == null || before == 0.0) return null
        return (((now - before) / before) * 100).roundToInt()
    }

    fun insight(st: FightStats): FightInsight {
        val rate = rate(st)
        val intensity = st.intensity ?: 0.0
        return when {
            st.total == 0 -> FightInsight.None
            intensity >= 7 -> FightInsight.Intense
            rate == 100 -> FightInsight.AllResolved
            rate >= 60 -> FightInsight.Great
            else -> FightInsight.StillOpen
        }
    }

    /** Newest first; same day: higher id first. */
    fun recentFirst(fights: List<Fight>): List<Fight> =
        fights.sortedWith(compareByDescending<Fight> { it.date }.thenByDescending { it.id })

    /** Rounds to one decimal and drops a trailing ".0" (JavaScript's number printing). */
    fun num(n: Double): String {
        val rounded = (n * 10).roundToInt() / 10.0
        return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString() else rounded.toString()
    }

    /** `me` / `partner` become the real role the web stores in startedBy. */
    fun startedByFor(starter: FightStarter, myRole: String): String = when (starter) {
        FightStarter.Me -> myRole
        FightStarter.Partner -> if (myRole == "user1") "user2" else "user1"
        FightStarter.Both -> "both"
        FightStarter.Unsure -> "unsure"
    }

    /** Which words the detail sheet uses for "Started by": Both, Not sure, Me, or the partner's name. */
    sealed interface Starter {
        data object Both : Starter
        data object Unsure : Starter
        data object Me : Starter
        data class Partner(val name: String) : Starter
        data object Unknown : Starter
    }

    fun starterOf(startedBy: String, myRole: String, partnerName: String): Starter = when (startedBy) {
        "both" -> Starter.Both
        "unsure" -> Starter.Unsure
        "user1", "user2" -> if (startedBy == myRole) Starter.Me else Starter.Partner(partnerName.ifBlank { "Partner" })
        else -> Starter.Unknown
    }

    /** The message sent to Twin AI from "AI Help" (same sentence as the web). */
    fun aiPrompt(f: Fight): String =
        "We had a fight about: ${f.title}." + (if (f.reason.isNotBlank()) " What happened: ${f.reason}" else "") +
            " Give us specific, empathetic advice to understand each other and resolve this quickly."
}
