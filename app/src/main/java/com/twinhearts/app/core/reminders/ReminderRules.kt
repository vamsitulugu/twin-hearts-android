package com.twinhearts.app.core.reminders

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** One entry of the shared `reminders` list, as the web app and this app both write it. */
data class ReminderItem(
    val id: String,
    val title: String,
    val note: String,
    val date: String,
    val time: String,
    /** "self", "both" or "partner" (web values). Blank counts as "self". */
    val forWhom: String,
    val fired: Boolean,
    /** Role ("user1"/"user2") of the person who made it. Web reminders have none. */
    val by: String,
)

/** A reminder the phone should ring for, at an exact moment. */
data class PhoneAlarm(
    val id: String,
    val title: String,
    val note: String,
    val triggerAtMillis: Long,
)

/** Decides which shared reminders this phone rings for. Pure, so it is unit tested. */
object ReminderRules {

    /** Android limits pending alarms; the nearest ones are enough because we re-sync often. */
    const val MAX_ALARMS = 60

    fun upcoming(
        items: List<ReminderItem>,
        myRole: String,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<PhoneAlarm> = items.mapNotNull { item ->
        if (item.fired || item.id.isBlank()) return@mapNotNull null
        if (!isForMe(item, myRole)) return@mapNotNull null
        val date = runCatching { LocalDate.parse(item.date) }.getOrNull() ?: return@mapNotNull null
        val time = runCatching { LocalTime.parse(item.time) }.getOrNull() ?: return@mapNotNull null
        val at = date.atTime(time).atZone(zone).toInstant().toEpochMilli()
        if (at <= nowMillis) return@mapNotNull null
        PhoneAlarm(item.id, item.title, item.note, at)
    }.sortedBy { it.triggerAtMillis }.take(MAX_ALARMS)

    /** "partner" is for the other phone. "self" rings only for its creator (blank creator: rings). */
    private fun isForMe(item: ReminderItem, myRole: String): Boolean = when (item.forWhom.ifBlank { "self" }) {
        "both" -> true
        "self" -> item.by.isBlank() || item.by == myRole
        else -> false
    }
}
