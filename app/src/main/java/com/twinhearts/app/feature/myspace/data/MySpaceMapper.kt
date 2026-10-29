package com.twinhearts.app.feature.myspace.data

import com.twinhearts.app.feature.myspace.domain.MySpaceLogic
import com.twinhearts.app.feature.myspace.domain.model.DefaultMood
import com.twinhearts.app.feature.myspace.domain.model.Habit
import com.twinhearts.app.feature.myspace.domain.model.JournalEntry
import com.twinhearts.app.feature.myspace.domain.model.MySpaceData
import com.twinhearts.app.feature.myspace.domain.model.Note
import com.twinhearts.app.feature.myspace.domain.model.Reminder
import com.twinhearts.app.feature.myspace.domain.model.Visibility
import com.twinhearts.app.feature.myspace.domain.model.WellnessDay
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Turns the web app's saved JSON into the models the screen uses. It never throws: an item that
 * cannot be read is skipped, so one odd entry never hides the rest (R-46). Field names are the
 * web's own (`index.html`: saveJournal, addHabit, addNote, addReminder, saveWellness).
 */
object MySpaceMapper {

    fun data(state: JsonObject, role: String, myName: String, partnerName: String): MySpaceData = MySpaceData(
        journal = journal(state.array("journal")),
        habits = habits(state.array("habits")),
        notes = notes(state.array("notes")),
        reminders = reminders(state.array("reminders")),
        wellness = wellness(state["wellness"] as? JsonObject),
        role = role,
        myName = myName.ifBlank { "You" },
        partnerName = partnerName.ifBlank { "Partner" },
    )

    fun JsonObject.array(key: String): JsonArray = this[key] as? JsonArray ?: JsonArray(emptyList())

    fun journal(arr: JsonArray): List<JournalEntry> = arr.mapNotNull { item ->
        val o = item as? JsonObject ?: return@mapNotNull null
        val body = o["body"].text().takeIf { it.isNotBlank() } ?: return@mapNotNull null
        JournalEntry(
            id = o["id"].text().ifBlank { return@mapNotNull null },
            date = o["date"].text(),
            mood = o["mood"].text(),
            body = body,
            visibility = Visibility.fromKey(o["visibility"].text()),
            by = o["by"].text(),
        )
    }

    fun habits(arr: JsonArray): List<Habit> = arr.mapNotNull { item ->
        val o = item as? JsonObject ?: return@mapNotNull null
        val name = o["name"].text().takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val dates = (o["doneDates"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty()
        Habit(
            id = o["id"].text().ifBlank { return@mapNotNull null },
            name = name,
            emoji = o["emoji"].text().ifBlank { "✅" },
            goal = o["goal"].text().toIntOrNull() ?: 1,
            doneDates = dates,
            // Recomputed from the dates, as the web does when it draws the list.
            streak = MySpaceLogic.streak(dates),
        )
    }

    fun notes(arr: JsonArray): List<Note> = arr.mapNotNull { item ->
        val o = item as? JsonObject ?: return@mapNotNull null
        val text = o["text"].text().takeIf { it.isNotBlank() } ?: return@mapNotNull null
        Note(
            id = o["id"].text().ifBlank { return@mapNotNull null },
            text = text,
            date = o["date"].text(),
            color = o["color"].text(),
            visibility = Visibility.fromKey(o["visibility"].text()),
            by = o["by"].text(),
            me = o["me"].text().toBooleanStrictOrNull() ?: false,
            reminderDate = o["reminderDate"].text(),
            reminderTime = o["reminderTime"].text(),
        )
    }

    fun reminders(arr: JsonArray): List<Reminder> = arr.mapNotNull { item ->
        val o = item as? JsonObject ?: return@mapNotNull null
        val title = o["title"].text().takeIf { it.isNotBlank() } ?: return@mapNotNull null
        Reminder(
            id = o["id"].text().ifBlank { return@mapNotNull null },
            title = title,
            date = o["date"].text(),
            time = o["time"].text(),
            forWhom = Visibility.fromKey(o["forWhom"].text()),
            note = o["note"].text(),
            by = o["by"].text(),
        )
    }

    fun wellness(obj: JsonObject?): Map<String, WellnessDay> {
        if (obj == null) return emptyMap()
        val out = LinkedHashMap<String, WellnessDay>()
        for ((date, value) in obj) {
            val o = value as? JsonObject ?: continue
            out[date] = WellnessDay(
                water = o["water"].text().toDoubleOrNull()?.toInt() ?: 0,
                sleep = o["sleep"].text().toDoubleOrNull() ?: 0.0,
                exercise = o["exercise"].text().toDoubleOrNull()?.toInt() ?: 0,
                mood = o["mood"].text(),
            )
        }
        return out
    }

    fun moodOrDefault(mood: String): String = mood.ifBlank { DefaultMood }

    fun JsonElement?.text(): String = (this as? JsonPrimitive)?.contentOrNull.orEmpty()
}
