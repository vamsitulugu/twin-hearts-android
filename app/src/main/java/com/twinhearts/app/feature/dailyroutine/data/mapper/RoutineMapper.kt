package com.twinhearts.app.feature.dailyroutine.data.mapper

import com.twinhearts.app.feature.dailyroutine.domain.model.Repeat
import com.twinhearts.app.feature.dailyroutine.domain.model.Reminder
import com.twinhearts.app.feature.dailyroutine.domain.model.Routine
import com.twinhearts.app.feature.dailyroutine.domain.model.RoutineDraft
import com.twinhearts.app.feature.dailyroutine.domain.model.RoutineStatus
import com.twinhearts.app.feature.dailyroutine.domain.model.RoutineType
import com.twinhearts.app.feature.dailyroutine.domain.model.TimeOverride
import com.twinhearts.app.feature.dailyroutine.domain.model.Visibility
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import java.time.LocalDate

/** Turns the server's routine rows (`rowOut` in routes/daily-routine.js) into [Routine], and a form into a request body. */

private fun JsonElement?.text(): String? = (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content

/** A row without an id, a valid title or valid times cannot be shown, so it is skipped. */
fun JsonElement?.toRoutines(): List<Routine> =
    (this as? JsonArray).orEmpty().mapNotNull { (it as? JsonObject)?.toRoutine() }

internal fun JsonObject.toRoutine(): Routine? {
    val id = get("id").text()?.takeIf { it.isNotBlank() } ?: return null
    val title = get("title").text()?.takeIf { it.isNotBlank() } ?: return null
    val start = get("startTime").text()?.takeIf(::isHhmm) ?: return null
    val end = get("endTime").text()?.takeIf(::isHhmm) ?: return null
    return Routine(
        id = id,
        role = get("role").text().orEmpty(),
        title = title,
        description = get("description").text().orEmpty(),
        type = RoutineType.fromKey(get("type").text()),
        location = get("location").text().orEmpty(),
        notes = get("notes").text().orEmpty(),
        startTime = start,
        endTime = end,
        date = get("date").text()?.take(DATE_LENGTH)?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        repeat = Repeat.fromKey(get("repeat").text()),
        repeatDays = (get("repeatDays") as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.intOrNull },
        visibility = Visibility.fromKey(get("visibility").text()),
        reminder = Reminder.fromKey(get("reminder").text()),
        statusByDate = (get("statusByDate") as? JsonObject).orEmpty().mapValues { RoutineStatus.fromKey(it.value.text()) },
        timeByDate = (get("timeByDate") as? JsonObject).orEmpty().mapNotNull { (day, value) ->
            val entry = value as? JsonObject ?: return@mapNotNull null
            val s = entry["start"].text()?.takeIf(::isHhmm) ?: return@mapNotNull null
            val e = entry["end"].text()?.takeIf(::isHhmm) ?: return@mapNotNull null
            day to TimeOverride(s, e)
        }.toMap(),
    )
}

/** The `routine` object of a create / update request. Blank optional texts are sent as null, like the web. */
fun RoutineDraft.toBody(date: LocalDate): JsonObject = buildJsonObject {
    put("title", title.trim())
    put("startTime", startTime)
    put("endTime", endTime)
    put("type", type.key)
    put("description", description.trim().ifBlank { null })
    put("repeat", repeat.key)
    put("reminder", reminder.key)
    put("visibility", visibility.key)
    put("location", location.trim().ifBlank { null })
    put("notes", notes.trim().ifBlank { null })
    put("date", date.toString())
}

private fun isHhmm(text: String): Boolean = HHMM.matches(text)

private val HHMM = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
private const val DATE_LENGTH = 10
