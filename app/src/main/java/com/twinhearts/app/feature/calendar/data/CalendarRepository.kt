package com.twinhearts.app.feature.calendar.data

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.data.StateCache
import com.twinhearts.app.core.reminders.RemindersSync
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.calendar.data.remote.CalendarApi
import com.twinhearts.app.feature.calendar.data.remote.CalendarSaveBody
import com.twinhearts.app.feature.calendar.domain.model.CalendarEvent
import com.twinhearts.app.feature.calendar.domain.model.EventAlarm
import com.twinhearts.app.feature.calendar.domain.model.EventCategory
import com.twinhearts.app.feature.calendar.domain.model.NewEvent
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import java.time.LocalDate
import javax.inject.Inject

interface CalendarRepository {
    /** The last copy saved on this phone, or null. Used to paint at once and to work offline. */
    suspend fun cachedEvents(): List<CalendarEvent>?

    suspend fun events(): AppResult<List<CalendarEvent>>

    /** Adds an event (and its reminder when an alarm is set) and returns the whole new list. */
    suspend fun addEvent(event: NewEvent): AppResult<List<CalendarEvent>>

    suspend fun deleteEvent(id: String): AppResult<List<CalendarEvent>>

    /** Deletes several events at once (long-press select) with their reminders, in one save. */
    suspend fun deleteEvents(ids: Set<String>): AppResult<List<CalendarEvent>>
}

class CalendarRepositoryImpl @Inject constructor(
    private val api: CalendarApi,
    private val sessionStore: SessionStore,
    private val cache: StateCache,
    private val reminders: RemindersSync,
) : CalendarRepository {

    private val notSignedIn = AppResult.Failure(AppError.Unauthorized())

    override suspend fun cachedEvents(): List<CalendarEvent>? {
        val session = sessionStore.session.first() ?: return null
        val json = cache.read(cacheKey(session)) ?: return null
        return (runCatching { Json.parseToJsonElement(json) }.getOrNull() as? JsonArray)?.toEvents()
    }

    override suspend fun events(): AppResult<List<CalendarEvent>> {
        val session = sessionStore.session.first() ?: return notSignedIn
        return when (val state = readState(session)) {
            is AppResult.Success -> {
                val raw = state.data.rawEvents()
                cache.write(cacheKey(session), raw.toString())
                reminders.sync(state.data["reminders"], session.role)
                AppResult.Success(raw.toEvents())
            }
            is AppResult.Failure -> state
        }
    }

    override suspend fun addEvent(event: NewEvent): AppResult<List<CalendarEvent>> {
        val session = sessionStore.session.first() ?: return notSignedIn
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return read
        }
        val now = System.currentTimeMillis()
        val nextEvents = JsonArray(state.rawEvents().toList() + eventJson(now, event))

        // Same as the web: an alarm needs a time, and it also files a reminder.
        val addReminder = event.alarm != EventAlarm.None && event.time.isNotBlank()
        val nextReminders = if (addReminder) {
            val old = (state["reminders"] as? JsonArray)?.toList().orEmpty()
            JsonArray(old + reminderJson(now + 1, event, session.role))
        } else {
            null
        }
        val patch = buildJsonObject {
            put("events", nextEvents)
            if (nextReminders != null) put("reminders", nextReminders)
        }
        return when (val saved = save(session, patch)) {
            is AppResult.Success -> {
                cache.write(cacheKey(session), nextEvents.toString())
                reminders.sync(nextReminders ?: state["reminders"], session.role)
                AppResult.Success(nextEvents.toEvents())
            }
            is AppResult.Failure -> saved
        }
    }

    override suspend fun deleteEvent(id: String): AppResult<List<CalendarEvent>> = deleteEvents(setOf(id))

    override suspend fun deleteEvents(ids: Set<String>): AppResult<List<CalendarEvent>> {
        val session = sessionStore.session.first() ?: return notSignedIn
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return read
        }
        // Work on the raw items so fields this app does not know about are kept.
        val all = state.rawEvents()
        val removed = all.filter { it is JsonObject && rawId(it) in ids }.filterIsInstance<JsonObject>()
        val kept = JsonArray(all.filterNot { it is JsonObject && rawId(it) in ids })

        // Also drop the reminder that was filed with each removed event, so it does not still ring.
        val oldReminders = state["reminders"] as? JsonArray
        val nextReminders = oldReminders?.let { list ->
            JsonArray(list.filterNot { r -> removed.any { e -> isReminderOf(r, e) } })
        }
        val patch = buildJsonObject {
            put("events", kept)
            if (nextReminders != null && nextReminders.size != oldReminders.size) put("reminders", nextReminders)
        }
        return when (val saved = save(session, patch)) {
            is AppResult.Success -> {
                cache.write(cacheKey(session), kept.toString())
                reminders.sync(nextReminders ?: oldReminders, session.role)
                AppResult.Success(kept.toEvents())
            }
            is AppResult.Failure -> saved
        }
    }

    private fun cacheKey(session: Session) = "calendar:${session.coupleId}"

    /** Same id rule as [toEvents], so a tap on a row always finds its raw item. */
    private fun rawId(obj: JsonObject): String =
        obj["id"].text().ifBlank { "${obj["title"].text()}|${obj["date"].text()}" }

    /** True for the reminder [addEvent] (or the web) filed for this event. */
    private fun isReminderOf(reminder: JsonElement, event: JsonObject): Boolean {
        val r = reminder as? JsonObject ?: return false
        return r["type"].text() == "event" &&
            r["title"].text() == "📅 ${event["title"].text()}" &&
            r["date"].text() == event["date"].text() &&
            r["time"].text() == event["time"].text()
    }

    private suspend fun readState(session: Session): AppResult<JsonObject> =
        when (val result = safeApiCall { api.getState(session.coupleId) }) {
            is AppResult.Success -> AppResult.Success(result.data.state as? JsonObject ?: JsonObject(emptyMap()))
            is AppResult.Failure -> result
        }

    private suspend fun save(session: Session, patch: JsonObject): AppResult<Unit> {
        val body = CalendarSaveBody(session.coupleId, patch, session.role, session.myName)
        return when (val result = safeApiCall { api.saveState(body) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> result
        }
    }

    private fun JsonObject.rawEvents(): JsonArray = this["events"] as? JsonArray ?: JsonArray(emptyList())

    /** Skips anything without a title and a readable date, so one bad item never hides the rest. */
    private fun JsonArray.toEvents(): List<CalendarEvent> = mapNotNull { item ->
        val obj = item as? JsonObject ?: return@mapNotNull null
        val title = obj["title"].text().takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val date = runCatching { LocalDate.parse(obj["date"].text()) }.getOrNull() ?: return@mapNotNull null
        CalendarEvent(
            id = rawId(obj),
            title = title,
            date = date,
            time = obj["time"].text(),
            note = obj["note"].text(),
            category = EventCategory.fromKey(obj["cat"].text()),
            alarm = EventAlarm.fromKey(obj["alarm"].text()),
        )
    }

    /** Field names match the web `addEvent()`: id, title, date, time, note, cat, alarm. */
    private fun eventJson(id: Long, event: NewEvent): JsonObject = buildJsonObject {
        put("id", id)
        put("title", event.title)
        put("date", event.date.toString())
        put("time", event.time)
        put("note", event.note)
        put("cat", event.category.key)
        put("alarm", event.alarm.key)
    }

    /** Matches the reminder the web files for an event with an alarm. */
    private fun reminderJson(id: Long, event: NewEvent, role: String): JsonObject = buildJsonObject {
        put("id", id)
        put("title", "📅 ${event.title}")
        put("date", event.date.toString())
        put("time", event.time)
        put("forWhom", event.alarm.key)
        put("note", event.note)
        put("fired", false)
        put("type", "event")
        // Which phone made it: "For me only" must ring only there. The web ignores this field.
        put("by", role)
    }

    private fun JsonElement?.text(): String = (this as? JsonPrimitive)?.contentOrNull.orEmpty()
}
