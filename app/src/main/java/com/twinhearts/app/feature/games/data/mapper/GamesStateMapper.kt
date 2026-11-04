package com.twinhearts.app.feature.games.data.mapper

import com.twinhearts.app.feature.games.domain.model.GameId
import com.twinhearts.app.feature.games.domain.model.GameRequest
import com.twinhearts.app.feature.games.domain.model.PlayerProfile
import com.twinhearts.app.feature.games.domain.model.Presence
import com.twinhearts.app.feature.games.domain.model.Side
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/**
 * Where Games keeps things inside the shared state. One key per thing, so the two phones
 * rarely write the same key (the server merges top-level keys only).
 */
internal object GamesKeys {
    fun doc(game: GameId) = "gm_${game.key}"
    fun profile(side: Side) = "gm_profile_${suffix(side)}"
    fun presence(side: Side) = "gm_presence_${suffix(side)}"
    /** One pending game invitation, written by the inviter under their own side key. */
    fun request(side: Side) = "gm_request_${suffix(side)}"
    const val END = "gm_end"
    /** The web app keeps games without an Android twin under this key; ending a game clears both. */
    fun webDoc(game: GameId) = "gm_web_${game.key}"
    private fun suffix(side: Side) = if (side == Side.One) "u1" else "u2"
}

/** The server answers JSON `null` when the couple has no state yet. */
private const val REQUEST_TTL_MS = 120_000L

internal fun JsonElement.asStateObject(): JsonObject = this as? JsonObject ?: JsonObject(emptyMap())

internal fun JsonObject.gameDoc(game: GameId): JsonElement? =
    this[GamesKeys.doc(game)]?.takeUnless { it is JsonNull }

internal fun JsonObject.gameEndOf(json: Json): com.twinhearts.app.feature.games.domain.model.GameEnd? {
    val element = this[GamesKeys.END]?.takeUnless { it is JsonNull } ?: return null
    return runCatching { json.decodeFromJsonElement(com.twinhearts.app.feature.games.domain.model.GameEnd.serializer(), element) }.getOrNull()
}

/** A missing or unreadable profile simply starts fresh. */
internal fun JsonObject.profileOf(side: Side, json: Json): PlayerProfile? {
    val element = this[GamesKeys.profile(side)]?.takeUnless { it is JsonNull } ?: return null
    return runCatching { json.decodeFromJsonElement(PlayerProfile.serializer(), element) }.getOrNull()
}

internal fun JsonObject.presenceOf(side: Side, json: Json): Presence {
    val element = this[GamesKeys.presence(side)]?.takeUnless { it is JsonNull } ?: return Presence()
    return runCatching { json.decodeFromJsonElement(Presence.serializer(), element) }.getOrDefault(Presence())
}

/** The pending invitation I received from my partner (written under their side's request key). */
internal fun JsonObject.pendingRequestOf(mySide: Side, json: Json): GameRequest? {
    val from = mySide.other
    val element = this[GamesKeys.request(from)]?.takeUnless { it is JsonNull } ?: return null
    val request = runCatching { json.decodeFromJsonElement(GameRequest.serializer(), element) }.getOrNull() ?: return null
    // An invitation nobody answered for two minutes is old news.
    return request.takeIf { kotlin.math.abs(System.currentTimeMillis() - it.ts) < REQUEST_TTL_MS }
}
