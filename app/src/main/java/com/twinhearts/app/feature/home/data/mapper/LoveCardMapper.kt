package com.twinhearts.app.feature.home.data.mapper

import com.twinhearts.app.feature.home.domain.model.LoveCardState
import com.twinhearts.app.feature.home.domain.model.LoveInteraction
import com.twinhearts.app.feature.home.domain.model.LoveKind
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/** Reads a number the browser saved (always whole milliseconds, sometimes stored as 1.7E12). */
internal fun JsonElement?.asLong(): Long? {
    val primitive = this as? JsonPrimitive ?: return null
    return primitive.longOrNull ?: primitive.doubleOrNull?.toLong()
}

internal fun JsonElement?.asText(): String? = (this as? JsonPrimitive)?.contentOrNull

/**
 * Finds the newest signal in the shared state, the way the web card does.
 * - `touch` and `missYou`: `{from, ts}`
 * - `hug`: counts once it is `accepted` (time = acceptedTs), or when I sent it myself (still pending)
 * - a `pending` hug from my partner is NOT an interaction yet: it becomes [LoveCardState.pendingIncomingHugId]
 */
fun JsonElement?.toLoveCardState(myRole: String): LoveCardState {
    val state = this as? JsonObject ?: return LoveCardState()
    val candidates = mutableListOf<LoveInteraction>()

    fun signal(key: String, kind: LoveKind) {
        val obj = state[key] as? JsonObject ?: return
        val ts = obj["ts"].asLong() ?: return
        candidates += LoveInteraction(kind, fromMe = obj["from"].asText() == myRole, timestampMillis = ts)
    }
    signal("touch", LoveKind.Touch)
    signal("missYou", LoveKind.MissYou)

    var pendingHugId: Long? = null
    (state["hug"] as? JsonObject)?.let { hug ->
        val from = hug["from"].asText()
        when (hug["status"].asText()) {
            "accepted" -> {
                val ts = hug["acceptedTs"].asLong() ?: hug["ts"].asLong()
                if (ts != null) candidates += LoveInteraction(LoveKind.Hug, fromMe = from == myRole, timestampMillis = ts)
            }
            "pending" -> {
                if (from == myRole) {
                    hug["ts"].asLong()?.let { candidates += LoveInteraction(LoveKind.Hug, fromMe = true, timestampMillis = it) }
                } else {
                    pendingHugId = hug["id"].asLong()
                }
            }
        }
    }
    return LoveCardState(last = candidates.maxByOrNull { it.timestampMillis }, pendingIncomingHugId = pendingHugId)
}
