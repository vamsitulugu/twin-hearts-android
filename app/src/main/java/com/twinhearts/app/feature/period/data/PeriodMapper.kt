package com.twinhearts.app.feature.period.data

import com.twinhearts.app.feature.period.domain.DEFAULT_CYCLE_DAYS
import com.twinhearts.app.feature.period.domain.DEFAULT_FLOW_DAYS
import com.twinhearts.app.feature.period.domain.model.Period
import com.twinhearts.app.feature.period.domain.model.PeriodAccess
import com.twinhearts.app.feature.period.domain.model.PeriodData
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.time.LocalDate

internal fun JsonElement?.text(): String = (this as? JsonPrimitive)?.contentOrNull.orEmpty()

/** "female" / "male" or null: anything else counts as "not chosen". */
internal fun validGender(value: String?): String? = value?.takeIf { it == "female" || it == "male" }

/** Web `rk()`: which `profile` entry belongs to this account. */
internal fun profileKey(role: String): String = if (role == "user2") "u2" else "u1"

/**
 * Raw `periods` items to periods, newest first. Items without a readable start date are skipped,
 * so one bad item never hides the rest. Missing flow/cycle fall back to the web defaults (5 / 28).
 */
internal fun JsonArray.toPeriods(): List<Period> = mapNotNull { item ->
    val obj = item as? JsonObject ?: return@mapNotNull null
    val start = runCatching { LocalDate.parse(obj["start"].text()) }.getOrNull() ?: return@mapNotNull null
    Period(
        id = obj["id"].text().ifBlank { start.toString() },
        start = start,
        flow = obj["flow"].text().toIntOrNull()?.takeIf { it > 0 } ?: DEFAULT_FLOW_DAYS,
        cycle = obj["cycle"].text().toIntOrNull()?.takeIf { it > 0 } ?: DEFAULT_CYCLE_DAYS,
        note = obj["note"].text(),
    )
}.sortedByDescending { it.start }

/** Date text to symptom names, from the raw `symptoms` object. */
internal fun JsonObject?.toSymptomMap(): Map<String, List<String>> =
    this?.mapValues { (_, v) -> (v as? JsonArray)?.map { it.text() }?.filter { it.isNotBlank() }.orEmpty() }.orEmpty()

/** Gender saved in the shared profile wins; the sign-up choice on this phone is the fallback. */
internal fun accessFor(profile: JsonObject?, role: String, sessionGender: String?): PeriodAccess {
    val mine = (profile?.get(profileKey(role)) as? JsonObject)?.get("gender").text()
    return when (validGender(mine) ?: validGender(sessionGender)) {
        "female" -> PeriodAccess.Edit
        "male" -> PeriodAccess.ReadOnly
        else -> PeriodAccess.Unknown
    }
}

internal fun JsonObject.toPeriodData(role: String, sessionGender: String?, partnerName: String): PeriodData =
    PeriodData(
        periods = (this["periods"] as? JsonArray ?: JsonArray(emptyList())).toPeriods(),
        symptoms = (this["symptoms"] as? JsonObject).toSymptomMap(),
        access = accessFor(this["profile"] as? JsonObject, role, sessionGender),
        partnerName = partnerName,
    )
