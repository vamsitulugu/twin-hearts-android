package com.twinhearts.app.feature.games.data.mapper

import com.twinhearts.app.feature.games.domain.model.GameId
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
    private fun suffix(side: Side) = if (side == Side.One) "u1" else "u2"
}

/** The server answers JSON `null` when the couple has no state yet. */
internal fun JsonElement.asStateObject(): JsonObject = this as? JsonObject ?: JsonObject(emptyMap())

internal fun JsonObject.gameDoc(game: GameId): JsonElement? =
    this[GamesKeys.doc(game)]?.takeUnless { it is JsonNull }

/** A missing or unreadable profile simply starts fresh. */
internal fun JsonObject.profileOf(side: Side, json: Json): PlayerProfile? {
    val element = this[GamesKeys.profile(side)]?.takeUnless { it is JsonNull } ?: return null
    return runCatching { json.decodeFromJsonElement(PlayerProfile.serializer(), element) }.getOrNull()
}

internal fun JsonObject.presenceOf(side: Side, json: Json): Presence {
    val element = this[GamesKeys.presence(side)]?.takeUnless { it is JsonNull } ?: return Presence()
    return runCatching { json.decodeFromJsonElement(Presence.serializer(), element) }.getOrDefault(Presence())
}
