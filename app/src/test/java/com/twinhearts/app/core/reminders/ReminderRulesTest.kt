package com.twinhearts.app.core.reminders

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class ReminderRulesTest {

    private val zone: ZoneId = ZoneOffset.UTC
    private val now = LocalDateTime.of(2026, 10, 28, 12, 0).atZone(zone).toInstant().toEpochMilli()

    private fun item(
        id: String = "1",
        date: String = "2026-10-28",
        time: String = "18:00",
        forWhom: String = "self",
        fired: Boolean = false,
        by: String = "user1",
    ) = ReminderItem(id, "Title", "", date, time, forWhom, fired, by)

    private fun ids(vararg items: ReminderItem, role: String = "user1") =
        ReminderRules.upcoming(items.toList(), role, now, zone).map { it.id }

    @Test fun `a future reminder for me is scheduled at its exact time`() {
        val result = ReminderRules.upcoming(listOf(item()), "user1", now, zone)
        assertEquals(LocalDateTime.of(2026, 10, 28, 18, 0).atZone(zone).toInstant().toEpochMilli(), result.single().triggerAtMillis)
    }

    @Test fun `fired and past reminders are skipped`() {
        assertEquals(emptyList<String>(), ids(item("a", fired = true), item("b", time = "11:00"), item("c", date = "2026-10-27")))
    }

    @Test fun `unreadable date or time is skipped`() {
        assertEquals(emptyList<String>(), ids(item("a", time = ""), item("b", date = "soon"), item("c", time = "6pm")))
    }

    @Test fun `for both rings on both phones`() {
        assertEquals(listOf("1"), ids(item(forWhom = "both", by = "user1"), role = "user2"))
    }

    @Test fun `for me only rings only on the phone that made it`() {
        assertEquals(emptyList<String>(), ids(item(forWhom = "self", by = "user1"), role = "user2"))
        assertEquals(listOf("1"), ids(item(forWhom = "self", by = "user1"), role = "user1"))
    }

    @Test fun `a reminder with no creator and for me still rings`() {
        assertEquals(listOf("1"), ids(item(forWhom = "self", by = ""), role = "user2"))
        assertEquals(listOf("1"), ids(item(forWhom = "", by = ""), role = "user1"))
    }

    @Test fun `partner reminders never ring here`() {
        assertEquals(emptyList<String>(), ids(item(forWhom = "partner")))
    }

    @Test fun `result is sorted by time and capped`() {
        val many = (1..80).map { item(id = "r$it", date = "2026-11-%02d".format((it % 28) + 1)) }
        val result = ReminderRules.upcoming(many, "user1", now, zone)
        assertEquals(ReminderRules.MAX_ALARMS, result.size)
        assertEquals(result.sortedBy { it.triggerAtMillis }, result)
    }

    @Test fun `blank id is skipped`() {
        assertEquals(emptyList<String>(), ids(item(id = "")))
    }
}
