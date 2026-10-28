package com.twinhearts.app.feature.study.data

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.study.data.remote.StudyApi
import com.twinhearts.app.feature.study.data.remote.StudySaveBody
import com.twinhearts.app.feature.study.domain.model.NewSlot
import com.twinhearts.app.feature.study.domain.model.NewSubject
import com.twinhearts.app.feature.study.domain.model.SlotAlarm
import com.twinhearts.app.feature.study.domain.model.StudyData
import com.twinhearts.app.feature.study.domain.model.StudyDay
import com.twinhearts.app.feature.study.domain.model.StudyPerson
import com.twinhearts.app.feature.study.domain.model.StudySlot
import com.twinhearts.app.feature.study.domain.model.Subject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import java.time.LocalDate
import javax.inject.Inject

interface StudyRepository {
    suspend fun load(): AppResult<StudyData>
    suspend fun addSubject(subject: NewSubject): AppResult<StudyData>

    /** Moves the finished-chapters count of one subject by [delta], never below 0 or above the total. */
    suspend fun changeProgress(id: String, delta: Int): AppResult<StudyData>
    suspend fun deleteSubject(id: String): AppResult<StudyData>

    /** Adds a slot (and its reminder when an alarm and a start time are set). */
    suspend fun addSlot(slot: NewSlot): AppResult<StudyData>
    suspend fun toggleSlot(id: String): AppResult<StudyData>
    suspend fun deleteSlot(id: String): AppResult<StudyData>
}

/**
 * The web keeps the two partners' study data under different keys: user1 uses
 * `mySubjects` / `myTimetable`, user2 uses `ptSubjects` / `ptTimetable`. Each person only
 * ever writes their own pair here, so one partner can never overwrite the other's lists.
 */
private class StudyKeys(
    val mineSubjects: String,
    val mineSlots: String,
    val partnerSubjects: String,
    val partnerSlots: String,
)

private fun keysFor(role: String) =
    if (role == "user2") {
        StudyKeys("ptSubjects", "ptTimetable", "mySubjects", "myTimetable")
    } else {
        StudyKeys("mySubjects", "myTimetable", "ptSubjects", "ptTimetable")
    }

class StudyRepositoryImpl @Inject constructor(
    private val api: StudyApi,
    private val sessionStore: SessionStore,
) : StudyRepository {

    private val notSignedIn = AppResult.Failure(AppError.Unauthorized())

    override suspend fun load(): AppResult<StudyData> {
        val session = sessionStore.session.first() ?: return notSignedIn
        return when (val state = readState(session)) {
            is AppResult.Success -> AppResult.Success(state.data.toStudyData(session))
            is AppResult.Failure -> state
        }
    }

    override suspend fun addSubject(subject: NewSubject): AppResult<StudyData> {
        val item = buildJsonObject {
            put("id", System.currentTimeMillis())
            put("name", subject.name)
            put("total", subject.total)
            put("done", subject.done)
            put("emoji", subject.emoji.ifBlank { DEFAULT_EMOJI })
        }
        return edit({ it.mineSubjects }, { it + item })
    }

    override suspend fun changeProgress(id: String, delta: Int): AppResult<StudyData> =
        edit({ it.mineSubjects }, { items ->
            items.map { element ->
                val obj = element as? JsonObject
                if (obj != null && obj["id"].text() == id) {
                    val total = obj["total"].int()
                    val done = (obj["done"].int() + delta).coerceIn(0, total.coerceAtLeast(0))
                    // Copy the item so fields this app does not know about are kept.
                    JsonObject(obj + ("done" to JsonPrimitive(done)))
                } else {
                    element
                }
            }
        })

    override suspend fun deleteSubject(id: String): AppResult<StudyData> =
        edit({ it.mineSubjects }, { items -> items.filterNot { (it as? JsonObject)?.get("id").text() == id } })

    override suspend fun addSlot(slot: NewSlot): AppResult<StudyData> {
        val now = System.currentTimeMillis()
        val item = buildJsonObject {
            put("id", now)
            put("day", slot.day.key)
            put("start", slot.start)
            put("end", slot.end)
            put("subject", slot.subject)
            put("done", false)
        }
        // Same reminder the web files for a slot with an alarm.
        val reminder = if (slot.alarm != SlotAlarm.None && slot.start.isNotBlank()) {
            buildJsonObject {
                put("id", now + 1)
                put("title", "Study: ${slot.subject}")
                put("time", slot.start)
                put("date", LocalDate.now().toString())
                put("forWhom", slot.alarm.key)
                put("note", "${slot.day.key} slot")
                put("fired", false)
            }
        } else {
            null
        }
        return edit({ it.mineSlots }, { it + item }, reminder)
    }

    override suspend fun toggleSlot(id: String): AppResult<StudyData> =
        edit({ it.mineSlots }, { items ->
            items.map { element ->
                val obj = element as? JsonObject
                if (obj != null && obj["id"].text() == id) {
                    val done = (obj["done"] as? JsonPrimitive)?.booleanOrNull ?: false
                    JsonObject(obj + ("done" to JsonPrimitive(!done)))
                } else {
                    element
                }
            }
        })

    override suspend fun deleteSlot(id: String): AppResult<StudyData> =
        edit({ it.mineSlots }, { items -> items.filterNot { (it as? JsonObject)?.get("id").text() == id } })

    /**
     * Reads the saved state, changes one of my two lists, saves only that key (plus `reminders`
     * when a reminder is added) and returns the page data with the change applied.
     */
    private suspend fun edit(
        keyOf: (StudyKeys) -> String,
        change: (List<JsonElement>) -> List<JsonElement>,
        reminder: JsonObject? = null,
    ): AppResult<StudyData> {
        val session = sessionStore.session.first() ?: return notSignedIn
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return read
        }
        val key = keyOf(keysFor(session.role))
        val next = JsonArray(change((state[key] as? JsonArray)?.toList().orEmpty()))
        val patch = buildJsonObject {
            put(key, next)
            if (reminder != null) {
                val reminders = (state["reminders"] as? JsonArray)?.toList().orEmpty()
                put("reminders", JsonArray(reminders + reminder))
            }
        }
        val body = StudySaveBody(session.coupleId, patch, session.role, session.myName)
        return when (val result = safeApiCall { api.saveState(body) }) {
            is AppResult.Success -> AppResult.Success(JsonObject(state + patch).toStudyData(session))
            is AppResult.Failure -> result
        }
    }

    private suspend fun readState(session: Session): AppResult<JsonObject> =
        when (val result = safeApiCall { api.getState(session.coupleId) }) {
            is AppResult.Success -> AppResult.Success(result.data.state as? JsonObject ?: JsonObject(emptyMap()))
            is AppResult.Failure -> result
        }

    private fun JsonObject.toStudyData(session: Session): StudyData {
        val keys = keysFor(session.role)
        return StudyData(
            me = StudyPerson(subjectsAt(keys.mineSubjects), slotsAt(keys.mineSlots)),
            partner = StudyPerson(subjectsAt(keys.partnerSubjects), slotsAt(keys.partnerSlots)),
            paired = session.paired,
            myName = session.myName,
            partnerName = session.partnerName,
        )
    }

    /** Skips items without a name, so one bad item never hides the rest. */
    private fun JsonObject.subjectsAt(key: String): List<Subject> =
        (this[key] as? JsonArray).orEmpty().mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val name = obj["name"].text().takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val total = obj["total"].int().coerceAtLeast(0)
            Subject(
                id = obj["id"].text().ifBlank { name },
                name = name,
                emoji = obj["emoji"].text().ifBlank { DEFAULT_EMOJI },
                total = total,
                done = obj["done"].int().coerceIn(0, total),
            )
        }

    /** The web only shows slots whose day is one of the seven names, so others are skipped. */
    private fun JsonObject.slotsAt(key: String): List<StudySlot> =
        (this[key] as? JsonArray).orEmpty().mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val day = StudyDay.fromKey(obj["day"].text()) ?: return@mapNotNull null
            val subject = obj["subject"].text()
            StudySlot(
                id = obj["id"].text().ifBlank { "$day|${obj["start"].text()}|$subject" },
                day = day,
                start = obj["start"].text(),
                end = obj["end"].text(),
                subject = subject,
                done = (obj["done"] as? JsonPrimitive)?.booleanOrNull ?: false,
            )
        }

    private fun JsonArray?.orEmpty(): List<JsonElement> = this?.toList() ?: emptyList()

    private fun JsonElement?.text(): String = (this as? JsonPrimitive)?.contentOrNull.orEmpty()

    /** A whole number from a JSON number or text ("10", "10.0"); anything else is 0. */
    private fun JsonElement?.int(): Int = text().toDoubleOrNull()?.toInt() ?: 0

    private companion object {
        const val DEFAULT_EMOJI = "📚"
    }
}
