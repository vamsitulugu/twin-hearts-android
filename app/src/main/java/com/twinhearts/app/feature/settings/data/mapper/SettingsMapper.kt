package com.twinhearts.app.feature.settings.data.mapper

import com.twinhearts.app.core.data.Session
import com.twinhearts.app.feature.settings.data.remote.IncomingRequestDto
import com.twinhearts.app.feature.settings.data.remote.StatusDto
import com.twinhearts.app.feature.settings.domain.model.IncomingRequest
import com.twinhearts.app.feature.settings.domain.model.PartnerStatus
import com.twinhearts.app.feature.settings.domain.model.ProfileEdit
import com.twinhearts.app.feature.settings.domain.model.ProfileGender
import com.twinhearts.app.feature.settings.domain.model.ProfileInfo
import com.twinhearts.app.feature.settings.domain.model.StorageKind
import com.twinhearts.app.feature.settings.domain.model.StorageSummary
import com.twinhearts.app.feature.settings.domain.model.StorageUsage
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

/** Why this file exists: turns the server's JSON into Settings models, and edits back into JSON. */

/** The shared state keeps each person in `profile.u1` / `profile.u2`. */
internal fun profileKey(role: String) = if (role == "user2") "u2" else "u1"
internal fun otherProfileKey(role: String) = if (role == "user2") "u1" else "u2"

internal fun JsonElement?.asText(): String? = (this as? JsonPrimitive)?.contentOrNull

private fun String?.asIsoDate(): LocalDate? {
    val text = this?.trim().orEmpty()
    if (text.length < 10) return null
    return runCatching { LocalDate.parse(text.substring(0, 10)) }.getOrNull()
}

private fun JsonObject?.entry(key: String): JsonObject? = this?.get(key) as? JsonObject

/**
 * Like the web: my own entry (`profile.u1`) wins, then the older top-level keys
 * (`myBday`, `ptBday`, `anniversary`) that Home also reads, then what Sign In saved on the phone.
 */
internal fun JsonObject.toProfileInfo(session: Session, myAvatar: String?, partnerAvatar: String?): ProfileInfo {
    val profile = entry("profile")
    val mine = profile.entry(profileKey(session.role))
    val theirs = profile.entry(otherProfileKey(session.role))
    return ProfileInfo(
        myName = mine?.get("name").asText()?.takeIf { it.isNotBlank() } ?: session.myName,
        partnerName = theirs?.get("name").asText()?.takeIf { it.isNotBlank() } ?: session.partnerName,
        gender = ProfileGender.fromWire(mine?.get("gender").asText()) ?: ProfileGender.fromWire(session.gender),
        anniversary = (this["anniversary"].asText() ?: session.anniversary).asIsoDate(),
        myBirthday = (mine?.get("bday").asText()?.takeIf { it.isNotBlank() } ?: this["myBday"].asText()).asIsoDate(),
        partnerBirthday = (theirs?.get("bday").asText()?.takeIf { it.isNotBlank() } ?: this["ptBday"].asText()).asIsoDate(),
        myAvatarUrl = myAvatar?.takeIf { it.isNotBlank() } ?: mine?.get("avatar").asText()?.takeIf { it.isNotBlank() },
        partnerAvatarUrl = partnerAvatar?.takeIf { it.isNotBlank() } ?: theirs?.get("avatar").asText()?.takeIf { it.isNotBlank() },
    )
}

/**
 * What the web's `saveSettings` writes: the top-level dates and MY `profile` entry. The entry keeps
 * its other fields (moods ...). The server replaces whole top-level keys, so the entry is sent complete.
 */
internal fun profilePatch(state: JsonObject, role: String, edit: ProfileEdit): JsonObject {
    val key = profileKey(role)
    val existing = state.entry("profile").entry(key) ?: JsonObject(emptyMap())
    val fields = buildMap<String, JsonElement> {
        put("name", JsonPrimitive(edit.myName))
        put("bday", JsonPrimitive(edit.myBirthday?.toString().orEmpty()))
        edit.avatarUrl?.let { put("avatar", JsonPrimitive(it)) }
        edit.gender?.let { put("gender", JsonPrimitive(it.wire)) }
    }
    return JsonObject(
        mapOf(
            "anniversary" to JsonPrimitive(edit.anniversary?.toString().orEmpty()),
            "myBday" to JsonPrimitive(edit.myBirthday?.toString().orEmpty()),
            "ptBday" to JsonPrimitive(edit.partnerBirthday?.toString().orEmpty()),
            "profile" to JsonObject(mapOf(key to JsonObject(existing + fields))),
        ),
    )
}

internal fun parseInstant(text: String?): Instant? {
    if (text.isNullOrBlank()) return null
    return runCatching { OffsetDateTime.parse(text).toInstant() }.getOrNull()
        ?: runCatching { LocalDateTime.parse(text).toInstant(ZoneOffset.UTC) }.getOrNull()
}

internal fun StatusDto.toDomain(): PartnerStatus = when (status) {
    "connected" -> PartnerStatus.Connected(partnerName?.takeIf { it.isNotBlank() } ?: "Partner")
    "waiting" -> PartnerStatus.Waiting(
        requestId = requestId.orEmpty(),
        receiverName = receiverName?.takeIf { it.isNotBlank() } ?: "Someone",
        receiverPhone = receiverPhone.orEmpty(),
        sentAt = parseInstant(createdAt),
    )
    "invitation_received" -> PartnerStatus.InvitationReceived(senderName?.takeIf { it.isNotBlank() } ?: "Someone")
    else -> PartnerStatus.NotConnected
}

internal fun IncomingRequestDto.toDomain() = IncomingRequest(
    id = id,
    senderName = senderName?.takeIf { it.isNotBlank() } ?: "Someone",
    senderPhone = senderPhone.orEmpty(),
    senderAvatarUrl = senderAvatar?.takeIf { it.isNotBlank() },
    sentAt = parseInstant(createdAt),
)

// ───────────── storage page (same formula as the web's calcStorageStats) ─────────────

private const val CLOUD_CAPACITY_BYTES = 500L * 1024 * 1024
private const val BASE64_FACTOR = 0.75
private const val VIDEO_FACTOR = 1.5

private fun JsonObject.items(key: String): List<JsonObject> =
    (this[key] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }

private fun JsonObject.urlLength(): Long = this["url"].asText()?.length?.toLong() ?: 0L

internal fun JsonObject.toStorageSummary(): StorageSummary {
    var photos = 0.0
    items("photos").forEach { photos += it.urlLength() * BASE64_FACTOR }
    items("vault").forEach {
        val factor = if (it["type"].asText() == "video") BASE64_FACTOR * VIDEO_FACTOR else BASE64_FACTOR
        photos += it.urlLength() * factor
    }
    val notes = (this["notes"] as? JsonArray).orEmpty().sumOf { it.toString().length.toLong() }
    val otherKeys = listOf("journal", "habits", "bucket", "events", "transactions", "periods", "fights", "milestones")
    val other = JsonObject(otherKeys.mapNotNull { k -> this[k]?.let { k to it } }.toMap()).toString().length.toLong()
    val usages = listOf(
        StorageUsage(StorageKind.Photos, photos.toLong()),
        StorageUsage(StorageKind.Voice, 0L), // the web never counts these two either
        StorageUsage(StorageKind.Chat, 0L),
        StorageUsage(StorageKind.Notes, notes),
        StorageUsage(StorageKind.AppData, other),
    )
    return StorageSummary(usages, usages.sumOf { it.bytes }, CLOUD_CAPACITY_BYTES)
}

/** Photos the web's "Export Photos" would download: `photos` entries of type `image`. */
internal fun JsonObject.imageUrls(): List<String> =
    items("photos").filter { it["type"].asText() == "image" }.mapNotNull { it["url"].asText()?.takeIf { u -> u.isNotBlank() } }

// ───────────── backup restore ─────────────

private val KEEP_ON_THIS_DEVICE = setOf(
    "userId", "coupleId", "myName", "partnerName", "role", "setupDone",
    "paired", "theme", "myAvatar", "partnerAvatar", "_pairToastShown", "vaultPin",
)

/** The web's import keeps who-you-are keys as they are. We simply never send them. */
internal fun backupPatch(imported: JsonObject): JsonObject =
    JsonObject(imported.filterKeys { it !in KEEP_ON_THIS_DEVICE })
