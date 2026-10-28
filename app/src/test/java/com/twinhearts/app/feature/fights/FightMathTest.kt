package com.twinhearts.app.feature.fights

import com.twinhearts.app.feature.fights.domain.FightMath
import com.twinhearts.app.feature.fights.domain.model.Fight
import com.twinhearts.app.feature.fights.domain.model.FightInsight
import com.twinhearts.app.feature.fights.domain.model.FightRange
import com.twinhearts.app.feature.fights.domain.model.FightStarter
import com.twinhearts.app.feature.fights.domain.model.FightStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class FightMathTest {
    private val today = LocalDate.of(2026, 10, 28)

    private fun fight(id: Long, daysAgo: Long, status: FightStatus, intensity: Int = 5, resolvedAfter: Long? = null) = Fight(
        id = id, title = "t$id", intensity = intensity, status = status,
        date = today.minusDays(daysAgo).toString(),
        resolvedDate = resolvedAfter?.let { today.minusDays(daysAgo).plusDays(it).toString() },
    )

    @Test
    fun windowKeepsOnlyFightsInsideTheRange() {
        val list = listOf(fight(1, 5, FightStatus.Open), fight(2, 45, FightStatus.Open), fight(3, 100, FightStatus.Open))
        assertEquals(listOf(1L), FightMath.window(list, today, 30, 0).map { it.id })
        assertEquals(listOf(2L), FightMath.window(list, today, 60, 30).map { it.id })
        assertEquals(listOf(1L, 2L), FightMath.inRange(list, today, FightRange.Days90).map { it.id })
        assertEquals(3, FightMath.inRange(list, today, FightRange.All).size)
    }

    @Test
    fun statsAndRate() {
        val list = listOf(
            fight(1, 1, FightStatus.Resolved, intensity = 4, resolvedAfter = 2),
            fight(2, 1, FightStatus.Resolved, intensity = 6),
            fight(3, 1, FightStatus.Open, intensity = 8),
        )
        val st = FightMath.stats(list)
        assertEquals(3, st.total); assertEquals(2, st.resolved); assertEquals(1, st.open)
        assertEquals(6.0, st.intensity ?: 0.0, 0.0001)
        assertEquals(1.0, st.days ?: 0.0, 0.0001) // (2 days + same day) / 2
        assertEquals(67, FightMath.rate(st))
    }

    @Test
    fun emptyListGivesNoNumbers() {
        val st = FightMath.stats(emptyList())
        assertNull(st.intensity); assertNull(st.days)
        assertEquals(0, FightMath.rate(st))
        assertEquals(FightInsight.None, FightMath.insight(st))
    }

    @Test
    fun deltaRules() {
        assertNull(FightMath.delta(null, 5.0))
        assertNull(FightMath.delta(5.0, null))
        assertNull(FightMath.delta(5.0, 0.0))
        assertEquals(-20, FightMath.delta(4.0, 5.0))
        assertEquals(50, FightMath.delta(3.0, 2.0))
    }

    @Test
    fun insightRules() {
        fun st(resolved: Int, open: Int, intensity: Double) =
            com.twinhearts.app.feature.fights.domain.model.FightStats(resolved + open, resolved, open, intensity, null)
        assertEquals(FightInsight.Intense, FightMath.insight(st(1, 1, 7.5)))
        assertEquals(FightInsight.AllResolved, FightMath.insight(st(3, 0, 4.0)))
        assertEquals(FightInsight.Great, FightMath.insight(st(3, 2, 4.0)))
        assertEquals(FightInsight.StillOpen, FightMath.insight(st(1, 4, 4.0)))
    }

    @Test
    fun numberPrintingMatchesTheWeb() {
        assertEquals("5", FightMath.num(5.0))
        assertEquals("5.5", FightMath.num(5.46))
        assertEquals("1", FightMath.num(1.04))
    }

    @Test
    fun recentFirstSortsByDateThenId() {
        val list = listOf(fight(1, 3, FightStatus.Open), fight(2, 1, FightStatus.Open), fight(3, 1, FightStatus.Open))
        assertEquals(listOf(3L, 2L, 1L), FightMath.recentFirst(list).map { it.id })
    }

    @Test
    fun startedByIsStoredAsRealRoles() {
        assertEquals("user1", FightMath.startedByFor(FightStarter.Me, "user1"))
        assertEquals("user2", FightMath.startedByFor(FightStarter.Partner, "user1"))
        assertEquals("both", FightMath.startedByFor(FightStarter.Both, "user1"))
        assertEquals(FightMath.Starter.Me, FightMath.starterOf("user1", "user1", "Likitha"))
        assertEquals(FightMath.Starter.Partner("Likitha"), FightMath.starterOf("user2", "user1", "Likitha"))
    }

    @Test
    fun aiPromptMatchesTheWebSentence() {
        val f = Fight(1, "Plan changes", reason = "Late again")
        assertEquals(
            "We had a fight about: Plan changes. What happened: Late again Give us specific, empathetic advice to understand each other and resolve this quickly.",
            FightMath.aiPrompt(f),
        )
    }
}
