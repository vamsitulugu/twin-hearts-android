package com.twinhearts.app.feature.games.data.mapper

import com.twinhearts.app.feature.games.domain.model.GameChatKind
import com.twinhearts.app.feature.games.domain.model.GameChatMessage
import com.twinhearts.app.feature.games.domain.model.Side
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

/**
 * Why this file exists: reads and writes the in-game chat in the exact shape the web app uses
 * (`gm_gchat_u1` / `gm_gchat_u2` = `{ m: [{id, t, x, u, d, ts}], ts }`), so web and Android phones
 * can chat with each other. Reading is forgiving: one odd message is skipped, never the whole chat.
 */
internal object GameChatKeys {
    fun key(side: Side) = "gm_gchat_${if (side == Side.One) "u1" else "u2"}"
}

internal fun JsonObject.chatMessagesOf(side: Side, mine: Boolean): List<GameChatMessage> {
    val box = this[GameChatKeys.key(side)]?.takeUnless { it is JsonNull } as? JsonObject ?: return emptyList()
    val list = box["m"] as? JsonArray ?: return emptyList()
    return list.mapNotNull { it.toChatMessage(mine) }
}

private fun JsonElement.toChatMessage(mine: Boolean): GameChatMessage? {
    val o = this as? JsonObject ?: return null
    val id = o.text("id")?.takeIf { it.isNotBlank() } ?: return null
    val kind = when (o.text("t")) {
        "text" -> GameChatKind.Text
        "voice" -> GameChatKind.Voice
        else -> return null
    }
    val sentAt = o.text("ts")?.toDoubleOrNull()?.toLong() ?: return null
    return GameChatMessage(
        id = id,
        kind = kind,
        text = o.text("x").orEmpty(),
        url = o.text("u").orEmpty(),
        seconds = o.text("d")?.toDoubleOrNull()?.toInt() ?: 0,
        sentAt = sentAt,
        mine = mine,
    )
}

/** The value saved under my key. */
internal fun List<GameChatMessage>.toChatBox(now: Long): JsonObject = buildJsonObject {
    put(
        "m",
        buildJsonArray {
            forEach { m ->
                add(
                    buildJsonObject {
                        put("id", m.id)
                        put("t", if (m.kind == GameChatKind.Voice) "voice" else "text")
                        put("x", m.text)
                        put("u", m.url)
                        put("d", m.seconds)
                        put("ts", m.sentAt)
                    },
                )
            }
        },
    )
    put("ts", now)
}

private fun JsonObject.text(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull
