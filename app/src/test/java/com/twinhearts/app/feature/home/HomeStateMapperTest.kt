package com.twinhearts.app.feature.home

import com.twinhearts.app.feature.home.data.mapper.toMoodBoard
import com.twinhearts.app.feature.home.data.mapper.toUpcomingEvents
import com.twinhearts.app.feature.home.domain.model.EventTarget
import com.twinhearts.app.feature.home.domain.model.EventTitle
import com.twinhearts.app.feature.home.domain.model.MoodBoard
import com.twinhearts.app.feature.home.domain.model.MoodKind
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HomeStateMapperTest {
    private val today = LocalDate.of(2026, 10, 26)
    private fun json(text: String) = Json.parseToJsonElement(text)

    @Test
    fun noState_givesNoEvents() {
        assertTrue(Json.parseToJsonElement("null").toUpcomingEvents("", today).isEmpty())
    }

    @Test
    fun birthdayThisYear_countsDays() {
        val events = json("""{"myBday":"1999-10-30"}""").toUpcomingEvents("", today)
        assertEquals(1, events.size)
        assertEquals(EventTitle.YourBirthday, events[0].title)
        assertEquals(4, events[0].daysLeft)
    }

    @Test
    fun birthdayAlreadyPassed_rollsToNextYear() {
        val events = json("""{"ptBday":"2000-10-01"}""").toUpcomingEvents("", today)
        assertEquals(LocalDate.of(2027, 10, 1), events[0].date)
    }

    @Test
    fun anniversary_gives_yearly_monthly_and_dayMark() {
        // together since 2025-01-30: yearly 2027-01-30 is later than monthly (2026-10-30) and day mark.
        val events = json("""{"anniversary":"2025-01-30"}""").toUpcomingEvents("", today)
        val titles = events.map { it.title }
        assertTrue(titles.contains(EventTitle.MonthsTogether(21)))
        assertTrue(titles.any { it is EventTitle.DayMark })
        assertTrue(titles.contains(EventTitle.Anniversary(2)))
        assertEquals(events.sortedBy { it.daysLeft }, events) // soonest first
    }

    @Test
    fun calendarEvents_pastOnesAreSkipped_andUseCategoryIcon() {
        val events = json(
            """{"events":[
                {"date":"2026-10-20","title":"Old"},
                {"date":"2026-10-28","title":"Dinner","cat":"Date","time":"8:00 PM"},
                {"date":"2026-10-27","title":""}
            ]}""",
        ).toUpcomingEvents("", today)
        assertEquals(1, events.size)
        assertEquals("🌹", events[0].icon)
        assertEquals("8:00 PM", events[0].detail)
        assertEquals(EventTarget.Calendar, events[0].target)
    }

    @Test
    fun moods_areSplitByRole_andOldEmojiFormIsUnderstood() {
        val state = json(
            """{"profile":{"u1":{"moods":{"2026-10-26":"Happy"}},"u2":{"moods":{"2026-10-26":"😌 Peaceful","2026-10-25":"Nope"}}}}""",
        )
        val asUser1 = state.toMoodBoard("user1")
        assertEquals(MoodKind.Happy, asUser1.mine["2026-10-26"])
        assertEquals(MoodKind.Peaceful, asUser1.partner["2026-10-26"])
        assertNull(asUser1.partner["2026-10-25"])
        val asUser2 = state.toMoodBoard("user2")
        assertEquals(MoodKind.Peaceful, asUser2.mine["2026-10-26"])
    }

    @Test
    fun streak_countsBackFromTodayOrYesterday() {
        val board = MoodBoard(mine = mapOf("2026-10-25" to MoodKind.Happy, "2026-10-24" to MoodKind.Sad))
        assertEquals(2, board.streak(today))
        assertEquals(3, board.copy(mine = board.mine + ("2026-10-26" to MoodKind.Loved)).streak(today))
        assertEquals(0, MoodBoard().streak(today))
    }
}
