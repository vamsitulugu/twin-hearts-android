package com.twinhearts.app.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import com.twinhearts.app.feature.money.data.remote.MoneyStateResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StateEnvelopeTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true }

    private fun parse(text: String) = json.decodeFromString(MoneyStateResponse.serializer(), text)

    @Test fun `server shape - document at top level is read`() {
        val state = parse("""{"transactions":[{"id":"1"}],"xp":40}""").state as JsonObject
        assertEquals(1, (state["transactions"] as JsonArray).size)
    }

    @Test fun `wrapped shape is still read`() {
        val state = parse("""{"state":{"transactions":[{"id":"1"},{"id":"2"}]}}""").state as JsonObject
        assertEquals(2, (state["transactions"] as JsonArray).size)
    }

    @Test fun `no saved state yet (JSON null) gives null, not a crash`() {
        assertNull(parse("null").state)
    }

    @Test fun `empty object gives empty document`() {
        assertNotNull(parse("{}").state)
    }
}
