package com.twinhearts.app.feature.myspace

import com.twinhearts.app.feature.myspace.domain.MySpaceLogic
import com.twinhearts.app.feature.myspace.domain.model.Habit
import com.twinhearts.app.feature.myspace.domain.model.JournalEntry
import com.twinhearts.app.feature.myspace.domain.model.Reminder
import com.twinhearts.app.feature.myspace.domain.model.Visibility
import com.twinhearts.app.feature.myspace.domain.model.WellnessDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MySpaceLogicTest {

    private val today = LocalDate.of(2026, 10, 29)
    private fun d(back: Int) = today.minusDays(back.toLong()).toString()

    private fun habit(id: String, vararg back: Int) =
        Habit(id, "h$id", "✅", 1, back.map { d(it) }, MySpaceLogic.streak(back.map { d(it) }, today))

    private fun entry(id: String, by: String, vis: Visibility) = JournalEntry(id, d(0), "", "text $id", vis, by)

    // ---- visibility (web canSeeItem) ----
    @Test fun `both is seen by everyone`() = assertTrue(MySpaceLogic.canSee(Visibility.Both, "user2", false, "user1"))
    @Test fun `self is mine only`() {
        assertTrue(MySpaceLogic.canSee(Visibility.Self, "user1", false, "user1"))
        assertFalse(MySpaceLogic.canSee(Visibility.Self, "user2", false, "user1"))
    }
    @Test fun `self with no author counts as mine`() = assertTrue(MySpaceLogic.canSee(Visibility.Self, "", false, "user1"))
    @Test fun `partner-only is for the other person`() {
        assertTrue(MySpaceLogic.canSee(Visibility.Partner, "user2", false, "user1"))
        assertFalse(MySpaceLogic.canSee(Visibility.Partner, "user1", false, "user1"))
    }

    @Test fun `partner journal hides their private entries`() {
        val all = listOf(
            entry("1", "user1", Visibility.Self),
            entry("2", "user2", Visibility.Self),
            entry("3", "user2", Visibility.Both),
            entry("4", "user2", Visibility.Partner),
        )
        assertEquals(listOf("1"), MySpaceLogic.myJournal(all, "user1").map { it.id })
        assertEquals(listOf("3", "4"), MySpaceLogic.partnerJournal(all, "user1").map { it.id })
    }

    // ---- streaks (web calcStreak) ----
    @Test fun `no days is no streak`() = assertEquals(0, MySpaceLogic.streak(emptyList(), today))
    @Test fun `streak counts back from today`() = assertEquals(3, MySpaceLogic.streak(listOf(d(0), d(1), d(2)), today))
    @Test fun `streak survives today not done yet`() = assertEquals(2, MySpaceLogic.streak(listOf(d(1), d(2)), today))
    @Test fun `a gap ends the streak`() = assertEquals(1, MySpaceLogic.streak(listOf(d(0), d(2), d(3)), today))
    @Test fun `old days only is zero`() = assertEquals(0, MySpaceLogic.streak(listOf(d(5)), today))

    // ---- habit summary ----
    @Test fun `empty habits summary is zero`() = assertEquals(MySpaceLogic.HabitSummary(0, 0, 0, 0), MySpaceLogic.habitSummary(emptyList(), today))

    @Test fun `summary counts today average and week consistency`() {
        val habits = listOf(habit("1", 0, 1, 2, 3, 4, 5, 6), habit("2", 1))
        val s = MySpaceLogic.habitSummary(habits, today)
        assertEquals(2, s.total)
        assertEquals(1, s.doneToday)
        assertEquals(4, s.avgStreak) // (7 + 1) / 2
        assertEquals(57, s.consistency) // 8 of 14 days
    }

    @Test fun `heat level thresholds match the web`() {
        assertEquals(0, MySpaceLogic.heatLevel(0, 3))
        assertEquals(1, MySpaceLogic.heatLevel(1, 4))
        assertEquals(2, MySpaceLogic.heatLevel(1, 2))
        assertEquals(2, MySpaceLogic.heatLevel(2, 3)) // 0.667 is still under 0.67, as on the web
        assertEquals(3, MySpaceLogic.heatLevel(3, 4))
        assertEquals(0, MySpaceLogic.heatLevel(1, 0))
    }

    @Test fun `heatmap is 84 days oldest first`() {
        val map = MySpaceLogic.heatmap(listOf(habit("1", 0)), today)
        assertEquals(84, map.size)
        assertEquals(3, map.last())
        assertEquals(0, map.first())
    }

    // ---- wellness ----
    @Test fun `goals met counts water sleep and exercise`() {
        assertEquals(3, MySpaceLogic.goalsMet(WellnessDay(8, 7.0, 30)))
        assertEquals(1, MySpaceLogic.goalsMet(WellnessDay(8, 6.5, 10)))
        assertEquals(0, MySpaceLogic.goalsMet(WellnessDay()))
    }

    @Test fun `week list has seven days ending today`() {
        val week = MySpaceLogic.lastSevenDays(mapOf(d(0) to WellnessDay(2, 6.0, 0)), today)
        assertEquals(7, week.size)
        assertEquals(today, week.last().first)
        assertEquals(2, week.last().second?.water)
        assertEquals(null, week.first().second)
    }

    @Test fun `averages cover every logged day`() {
        val avg = MySpaceLogic.wellnessAverages(mapOf("a" to WellnessDay(4, 6.0, 0), "b" to WellnessDay(8, 8.0, 0)))
        assertEquals(6.0, avg.water, 0.001)
        assertEquals(7.0, avg.sleep, 0.001)
        assertEquals(0.0, MySpaceLogic.wellnessAverages(emptyMap()).water, 0.001)
    }

    // ---- reminders ----
    @Test fun `reminders sort soonest first`() {
        fun r(id: String, date: String, time: String) = Reminder(id, "t", date, time, Visibility.Both, "")
        val sorted = MySpaceLogic.sortedReminders(listOf(r("c", "2026-11-02", "08:00"), r("a", "2026-11-01", "09:00"), r("b", "2026-11-01", "10:00")))
        assertEquals(listOf("a", "b", "c"), sorted.map { it.id })
    }
}
