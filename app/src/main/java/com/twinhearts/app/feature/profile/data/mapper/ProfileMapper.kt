package com.twinhearts.app.feature.profile.data.mapper

import com.twinhearts.app.feature.profile.domain.model.ConnectionState
import com.twinhearts.app.feature.profile.domain.model.Gender
import com.twinhearts.app.feature.profile.domain.model.LoveLanguage
import com.twinhearts.app.feature.profile.domain.model.Milestone
import com.twinhearts.app.feature.profile.domain.model.MilestoneType
import com.twinhearts.app.feature.profile.domain.model.NewMilestone
import com.twinhearts.app.feature.profile.domain.model.NewSpecialDate
import com.twinhearts.app.feature.profile.domain.model.ProfileData
import com.twinhearts.app.feature.profile.domain.model.SpecialDate
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.OffsetDateTime

/** Turns the shared couple state into [ProfileData] and back into patches (R-03). Pure functions, covered by unit tests. */

/** user1 → "u1", user2 → "u2": the key of a person inside `profile`. */
fun profileKey(role: String): String = if (role == "user2") "u2" else "u1"

fun otherProfileKey(role: String): String = if (role == "user2") "u1" else "u2"

private fun JsonElement?.text(): String = (this as? JsonPrimitive)?.contentOrNull.orEmpty()
private fun JsonElement?.obj(): JsonObject? = this as? JsonObject
private fun JsonElement?.arr(): JsonArray? = this as? JsonArray

/** A date like "2026-10-27", or null when empty or not readable. */
fun parseDate(text: String?): LocalDate? =
    if (text.isNullOrBlank()) null else runCatching { LocalDate.parse(text.trim().take(DATE_LENGTH)) }.getOrNull()

private const val DATE_LENGTH = 10

/** The server sends ISO times with "Z" or "+00:00". Null when missing or odd. */
fun parseIsoMs(text: String?): Long? =
    if (text.isNullOrBlank()) null else runCatching { OffsetDateTime.parse(text).toInstant().toEpochMilli() }.getOrNull()

/** The raw `profile.<key>` entry of a person, or an empty object. */
fun JsonObject.profileEntry(key: String): JsonObject = this["profile"].obj()?.get(key).obj() ?: JsonObject(emptyMap())

/**
 * Builds what the Profile screens show. [sessionName] / [sessionPartnerName] / [sessionAnniversary] are the
 * values saved on this phone at sign-in; the shared state wins when it has a newer one (same as the web).
 */
fun JsonObject.toProfileData(
    role: String,
    sessionName: String,
    sessionPartnerName: String,
    sessionAnniversary: String,
    sessionGender: String?,
    connection: ConnectionState,
    myAvatarUrl: String?,
    partnerAvatarUrl: String?,
    partnerSeenAtMs: Long?,
): ProfileData {
    val mine = profileEntry(profileKey(role))
    val theirs = profileEntry(otherProfileKey(role))
    // When connected, the partner's own profile entry is the truth for their name and birthday.
    val partnerName = theirs["name"].text().ifBlank { this["partnerName"].text().ifBlank { sessionPartnerName } }
    val partnerBirthday = if (theirs.containsKey("bday")) parseDate(theirs["bday"].text()) else parseDate(this["ptBday"].text())
    return ProfileData(
        myName = sessionName.ifBlank { this["myName"].text() },
        partnerName = partnerName.ifBlank { "Partner" },
        role = role,
        connection = connection,
        anniversary = parseDate(this["anniversary"].text().ifBlank { sessionAnniversary }),
        myBirthday = parseDate(this["myBday"].text().ifBlank { mine["bday"].text() }),
        partnerBirthday = partnerBirthday,
        myBio = this["myBio"].text(),
        partnerBio = this["partnerBio"].text(),
        myLoveLanguage = LoveLanguage.fromWire(this["myLoveLang"].text()),
        partnerLoveLanguage = LoveLanguage.fromWire(this["ptLoveLang"].text()),
        myGender = Gender.fromWire(mine["gender"].text()) ?: Gender.fromWire(sessionGender),
        myAvatarUrl = myAvatarUrl?.takeIf { it.isNotBlank() },
        partnerAvatarUrl = partnerAvatarUrl?.takeIf { it.isNotBlank() },
        memoriesCount = this["photos"].arr()?.size ?: 0,
        dreamsCount = this["bucket"].arr()?.size ?: 0,
        milestones = toMilestones(),
        dates = toSpecialDates(),
        partnerSeenAtMs = partnerSeenAtMs,
    )
}

/** Skips any milestone without a title or a readable date, so one bad item never hides the rest. */
fun JsonObject.toMilestones(): List<Milestone> = this["milestones"].arr().orEmpty().mapNotNull { item ->
    val o = item.obj() ?: return@mapNotNull null
    val title = o["title"].text().ifBlank { return@mapNotNull null }
    val date = parseDate(o["date"].text()) ?: return@mapNotNull null
    Milestone(
        id = (o["id"] as? JsonPrimitive)?.longOrNull ?: 0L,
        emoji = o["type"].text().ifBlank { MilestoneType.Custom.emoji },
        title = title,
        date = date,
        note = o["note"].text(),
    )
}

fun JsonObject.toSpecialDates(): List<SpecialDate> = this["events"].arr().orEmpty().mapNotNull { item ->
    val o = item.obj() ?: return@mapNotNull null
    val title = o["title"].text().ifBlank { return@mapNotNull null }
    val date = parseDate(o["date"].text()) ?: return@mapNotNull null
    SpecialDate(title, date, o["cat"].text())
}

/** Field names match the web `addEvent()`: id, title, date, time, note, cat, alarm. */
fun NewSpecialDate.toEventJson(id: Long): JsonObject = buildJsonObject {
    put("id", id)
    put("title", title)
    put("date", date.toString())
    put("time", time)
    put("note", note)
    put("cat", category.key)
    put("alarm", alarm.key)
}

/** The reminder the web files for an event that has an alarm and a time. */
fun NewSpecialDate.toReminderJson(id: Long): JsonObject = buildJsonObject {
    put("id", id)
    put("title", "📅 $title")
    put("date", date.toString())
    put("time", time)
    put("forWhom", alarm.key)
    put("note", note)
    put("fired", false)
    put("type", "event")
}

/** Field names match the web `addMilestone()`: id, title, date, note, type. */
fun NewMilestone.toJson(id: Long): JsonObject = buildJsonObject {
    put("id", id)
    put("title", title)
    put("date", date.toString())
    put("note", note)
    put("type", type.emoji)
}
