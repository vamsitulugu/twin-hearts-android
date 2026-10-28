package com.twinhearts.app.feature.study

import com.twinhearts.app.feature.study.domain.hoursByDay
import com.twinhearts.app.feature.study.domain.model.StudyDay
import com.twinhearts.app.feature.study.domain.model.StudyInsight
import com.twinhearts.app.feature.study.domain.model.StudySlot
import com.twinhearts.app.feature.study.domain.model.Subject
import com.twinhearts.app.feature.study.domain.slotHours
import com.twinhearts.app.feature.study.domain.slotsOfDay
import com.twinhearts.app.feature.study.domain.slotsWithOtherDay
import com.twinhearts.app.feature.study.domain.studyHero
import com.twinhearts.app.feature.study.domain.studyInsight
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek

class StudyMathTest {

    private fun slot(id: String, day: String, start: String, end: String, done: Boolean = false) =
        StudySlot(id, StudyDay.fromKey(day), day, start, end, "Maths", done)

    @Test fun `slot length is in hours and bad times count zero`() {
        assertEquals(1.5, slotHours(slot("a", "Monday", "18:00", "19:30")), 0.0)
        assertEquals(0.0, slotHours(slot("b", "Monday", "", "19:30")), 0.0)
        assertEquals(0.0, slotHours(slot("c", "Monday", "20:00", "19:00")), 0.0)
    }

    @Test fun `hours are added per weekday Sunday first`() {
        val bars = hoursByDay(listOf(slot("a", "Monday", "10:00", "12:00"), slot("b", "Monday", "14:00", "15:00"), slot("c", "Sunday", "09:00", "10:00")))
        assertEquals(DayOfWeek.SUNDAY, bars.first().first)
        assertEquals(1.0, bars.first().second, 0.0)
        assertEquals(3.0, bars.first { it.first == DayOfWeek.MONDAY }.second, 0.0)
    }

    @Test fun `a slot with an unknown day is kept under its saved label`() {
        val odd = slot("x", "Weekend", "10:00", "11:00")
        assertEquals(null, odd.day)
        assertEquals(mapOf("Weekend" to listOf(odd)), slotsWithOtherDay(listOf(odd, slot("m", "Monday", "10:00", "11:00"))))
        assertEquals(emptyList<StudySlot>(), slotsOfDay(listOf(odd), DayOfWeek.MONDAY))
    }

    @Test fun `unknown day still counts in weekly hours total`() {
        val hero = studyHero(emptyList(), listOf(slot("x", "Weekend", "10:00", "12:00"), slot("m", "Monday", "10:00", "11:00")))
        assertEquals(3.0, hero.weeklyHours, 0.0)
    }

    @Test fun `hero adds chapters across subjects`() {
        val hero = studyHero(listOf(Subject("1", "A", "📚", 10, 5), Subject("2", "B", "📚", 10, 2)), emptyList())
        assertEquals(35, hero.percent)
        assertEquals(7, hero.doneChapters)
        assertEquals(20, hero.totalChapters)
        assertEquals(2, hero.subjectCount)
    }

    @Test fun `insight depends on the share of slots done`() {
        assertEquals(StudyInsight.NoSlots, studyInsight(emptyList()))
        val four = { done: Int -> (0 until 4).map { slot("s$it", "Monday", "10:00", "11:00", it < done) } }
        assertEquals(StudyInsight.Great(100), studyInsight(four(4)))
        assertEquals(StudyInsight.Some(2, 4), studyInsight(four(2)))
        assertEquals(StudyInsight.Behind(1, 4), studyInsight(four(1)))
    }
}
