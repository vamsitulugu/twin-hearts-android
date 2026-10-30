package com.twinhearts.app.core.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

/**
 * `GET /api/data/state/:coupleId` (us-app/routes/data.js) answers the saved document itself
 * (`{ "events": [...], "photos": [...] }`), or JSON `null` when nothing is saved yet. Older code
 * expected it inside a `{ "state": {...} }` wrapper and so read an empty document, then saved
 * lists built from that empty one over the real data on the server.
 *
 * This returns the document for either shape, so every page reads what is really saved.
 */
fun unwrapState(root: JsonElement?): JsonElement? {
    val obj = root as? JsonObject ?: return null
    return (obj["state"] as? JsonObject) ?: obj
}

/**
 * Base serializer for a page's `…StateResponse(val state: JsonElement?)` class. Put
 * `@Serializable(with = …)` on the class and the page's repository code stays unchanged.
 */
abstract class StateEnvelopeSerializer<T>(
    serialName: String,
    private val make: (JsonElement?) -> T,
    private val read: (T) -> JsonElement?,
) : KSerializer<T> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(serialName)

    override fun deserialize(decoder: Decoder): T {
        val input = decoder as? JsonDecoder ?: error("StateEnvelopeSerializer needs JSON")
        return make(unwrapState(input.decodeJsonElement()))
    }

    override fun serialize(encoder: Encoder, value: T) {
        val out = encoder as? JsonEncoder ?: error("StateEnvelopeSerializer needs JSON")
        out.encodeJsonElement(buildJsonObject { read(value)?.let { put("state", it) } })
    }
}
