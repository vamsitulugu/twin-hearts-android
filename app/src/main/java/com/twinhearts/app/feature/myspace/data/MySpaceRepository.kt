package com.twinhearts.app.feature.myspace.data

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.core.reminders.RemindersSync
import com.twinhearts.app.feature.myspace.data.MySpaceMapper.array
import com.twinhearts.app.feature.myspace.data.MySpaceMapper.text
import com.twinhearts.app.feature.myspace.data.remote.MySpaceApi
import com.twinhearts.app.feature.myspace.data.remote.MySpaceSaveBody
import com.twinhearts.app.feature.myspace.domain.MySpaceLogic
import com.twinhearts.app.feature.myspace.domain.model.MySpaceData
import com.twinhearts.app.feature.myspace.domain.model.NewHabit
import com.twinhearts.app.feature.myspace.domain.model.NewJournal
import com.twinhearts.app.feature.myspace.domain.model.NewNote
import com.twinhearts.app.feature.myspace.domain.model.NewReminder
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import javax.inject.Inject

interface MySpaceRepository {
    suspend fun load(): AppResult<MySpaceData>

    // Every change below returns the whole page again, as saved.
    suspend fun addJournal(entry: NewJournal): AppResult<MySpaceData>
    suspend fun deleteJournal(ids: Set<String>): AppResult<MySpaceData>

    suspend fun addHabit(habit: NewHabit): AppResult<MySpaceData>
    suspend fun toggleHabit(id: String, day: LocalDate): AppResult<MySpaceData>
    suspend fun deleteHabits(ids: Set<String>): AppResult<MySpaceData>

    suspend fun addNote(note: NewNote): AppResult<MySpaceData>
    suspend fun deleteNotes(ids: Set<String>): AppResult<MySpaceData>

    suspend fun addReminder(reminder: NewReminder): AppResult<MySpaceData>
    suspend fun deleteReminders(ids: Set<String>): AppResult<MySpaceData>

    suspend fun saveWellness(day: LocalDate, water: Int, sleep: Double, exercise: Int): AppResult<MySpaceData>
}

class MySpaceRepositoryImpl @Inject constructor(
    private val api: MySpaceApi,
    private val sessionStore: SessionStore,
    private val reminders: RemindersSync,
) : MySpaceRepository {

    private val notSignedIn = AppResult.Failure(AppError.Unauthorized())

    override suspend fun load(): AppResult<MySpaceData> {
        val session = sessionStore.session.first() ?: return notSignedIn
        return when (val read = readState(session)) {
            is AppResult.Success -> {
                // Ring the phone for reminders the other phone or the web added.
                reminders.sync(read.data["reminders"], session.role)
                AppResult.Success(read.data.toData(session))
            }
            is AppResult.Failure -> read
        }
    }

    // ---- journal ----
    override suspend fun addJournal(entry: NewJournal) = change("journal") { session, now, items ->
        val item = buildJsonObject {
            put("id", now)
            put("date", LocalDate.now().toString())
            put("mood", entry.mood)
            put("body", entry.body)
            put("visibility", entry.visibility.key)
            put("by", session.role)
        }
        // Newest first, as the web does (`unshift`).
        JsonArray(listOf<JsonElement>(item) + items)
    }

    override suspend fun deleteJournal(ids: Set<String>) = change("journal") { _, _, items -> items.without(ids) }

    // ---- habits ----
    override suspend fun addHabit(habit: NewHabit) = change("habits") { session, now, items ->
        val item = buildJsonObject {
            put("id", now)
            put("name", habit.name)
            put("emoji", habit.emoji)
            put("goal", habit.goal)
            put("streak", 0)
            put("doneToday", false)
            put("doneDates", buildJsonArray { })
            put("by", session.role)
        }
        JsonArray(items + item)
    }

    override suspend fun toggleHabit(id: String, day: LocalDate) = change("habits") { _, _, items ->
        val today = day.toString()
        JsonArray(
            items.map { el ->
                val o = el as? JsonObject
                if (o == null || o["id"].text() != id) return@map el
                val dates = (o["doneDates"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content }.orEmpty()
                val nowDone = today !in dates
                val next = if (nowDone) dates + today else dates.filter { it != today }
                JsonObject(
                    o + mapOf(
                        "doneDates" to JsonArray(next.map { JsonPrimitive(it) }),
                        "doneToday" to JsonPrimitive(nowDone),
                        "streak" to JsonPrimitive(MySpaceLogic.streak(next, day)),
                    ),
                )
            },
        )
    }

    override suspend fun deleteHabits(ids: Set<String>) = change("habits") { _, _, items -> items.without(ids) }

    // ---- notes ----
    override suspend fun addNote(note: NewNote) = change("notes") { session, now, items ->
        val item = buildJsonObject {
            put("id", now)
            put("text", note.text)
            put("date", LocalDate.now().toString())
            put("color", NOTE_COLORS[items.size % NOTE_COLORS.size])
            put("me", true)
            put("visibility", note.visibility.key)
            put("by", session.role)
            if (note.reminderDate.isNotBlank()) {
                put("reminderDate", note.reminderDate)
                put("reminderTime", note.reminderTime.ifBlank { "09:00" })
                put("alarmed", false)
            }
        }
        JsonArray(listOf<JsonElement>(item) + items)
    }

    override suspend fun deleteNotes(ids: Set<String>) = change("notes") { _, _, items -> items.without(ids) }

    // ---- reminders ----
    override suspend fun addReminder(reminder: NewReminder) = change("reminders") { session, now, items ->
        val item = buildJsonObject {
            put("id", now)
            put("title", reminder.title)
            put("date", reminder.date)
            put("time", reminder.time)
            put("forWhom", reminder.forWhom.key)
            put("note", reminder.note)
            put("fired", false)
            // Who made it: "self" reminders ring only on this person's phone (core/reminders).
            put("by", session.role)
        }
        JsonArray(items + item)
    }

    override suspend fun deleteReminders(ids: Set<String>) = change("reminders") { _, _, items -> items.without(ids) }

    // ---- wellness ----
    override suspend fun saveWellness(day: LocalDate, water: Int, sleep: Double, exercise: Int): AppResult<MySpaceData> {
        val session = sessionStore.session.first() ?: return notSignedIn
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return read
        }
        val all = state["wellness"] as? JsonObject ?: JsonObject(emptyMap())
        val key = day.toString()
        val before = all[key] as? JsonObject
        // Keep the mood the web saved for the day (and anything else it keeps there).
        val merged = JsonObject(
            (before ?: JsonObject(emptyMap())) + mapOf(
                "water" to JsonPrimitive(water),
                "sleep" to JsonPrimitive(sleep),
                "exercise" to JsonPrimitive(exercise),
            ),
        )
        val nextWellness = JsonObject(all + (key to merged))
        return save(session, state, "wellness", nextWellness)
    }

    // ---- plumbing ----

    /** Reads the saved state, changes one list, saves just that key, and returns the page. */
    private suspend fun change(
        key: String,
        edit: (Session, Long, JsonArray) -> JsonArray,
    ): AppResult<MySpaceData> {
        val session = sessionStore.session.first() ?: return notSignedIn
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return read
        }
        val next = edit(session, System.currentTimeMillis(), state.array(key))
        return save(session, state, key, next)
    }

    private suspend fun save(session: Session, state: JsonObject, key: String, value: JsonElement): AppResult<MySpaceData> {
        val patch = JsonObject(mapOf(key to value))
        val body = MySpaceSaveBody(session.coupleId, patch, session.role, session.myName)
        return when (val r = safeApiCall { api.saveState(body) }) {
            is AppResult.Success -> {
                val next = JsonObject(state + patch)
                // Deleting or adding a reminder changes what the phone should ring for.
                if (key == "reminders") reminders.sync(next["reminders"], session.role)
                AppResult.Success(next.toData(session))
            }
            is AppResult.Failure -> r
        }
    }

    private suspend fun readState(session: Session): AppResult<JsonObject> =
        when (val result = safeApiCall { api.getState(session.coupleId) }) {
            is AppResult.Success -> AppResult.Success(result.data.state as? JsonObject ?: JsonObject(emptyMap()))
            is AppResult.Failure -> result
        }

    private fun JsonObject.toData(session: Session): MySpaceData =
        MySpaceMapper.data(this, session.role, session.myName, session.partnerName)

    /** Works on the raw items so fields this app does not know about stay in the saved state. */
    private fun JsonArray.without(ids: Set<String>): JsonArray =
        JsonArray(filterNot { it is JsonObject && it["id"].text() in ids })

    private companion object {
        /** Same colours the web `noteColors` uses, in the same order. */
        val NOTE_COLORS = listOf(
            "rgba(107,9,58,0.12)", "rgba(107,9,58,0.12)", "rgba(100,200,150,0.12)",
            "rgba(255,180,50,0.12)", "rgba(107,9,58,0.12)",
        )
    }
}
