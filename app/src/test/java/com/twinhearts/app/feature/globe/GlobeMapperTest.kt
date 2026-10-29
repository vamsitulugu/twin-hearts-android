package com.twinhearts.app.feature.globe

import com.twinhearts.app.feature.globe.data.mapper.mediaWire
import com.twinhearts.app.feature.globe.data.mapper.toDomain
import com.twinhearts.app.feature.globe.data.mapper.toWire
import com.twinhearts.app.feature.globe.data.remote.MemoryDto
import com.twinhearts.app.feature.globe.domain.model.MediaKind
import com.twinhearts.app.feature.globe.domain.model.MemoryDraft
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobeMapperTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true }

    @Test fun serverRow_numbersMayBeTextOrNumbers() {
        val dto = json.decodeFromString<MemoryDto>(
            """{"id":"m1","city":"Paris","country":"France","lat":"48.85","lng":2.35,"temperature":25,"trip_cost":"5000.50",
               "date_from":"2025-03-12T00:00:00+00:00","mood":"🥰 Romantic","restaurants":["Le Jules"],
               "globe_memory_media":[{"id":7,"type":"photo","url":"https://x/p.jpg"},{"id":"8","type":"voice","url":"https://x/v.m4a","duration":"0:12"},{"id":"9","type":"alien"}]}""",
        )
        val m = dto.toDomain()!!
        assertEquals(48.85, m.lat, 0.0)
        assertEquals(2.35, m.lng, 0.0)
        assertEquals("25", m.temperature)
        assertEquals(5000.5, m.tripCost!!, 0.0)
        assertEquals("2025-03-12", m.dateFrom)
        assertEquals(listOf("Le Jules"), m.restaurants)
        // The unknown "alien" media type is skipped, the others are kept.
        assertEquals(listOf(MediaKind.Photo, MediaKind.Voice), m.media.map { it.kind })
        assertEquals("0:12", m.media[1].duration)
    }

    @Test fun rowWithoutPosition_isSkipped() {
        assertNull(json.decodeFromString<MemoryDto>("""{"id":"m1","city":"X"}""").toDomain())
    }

    @Test fun draft_toWire_keepsExplicitNullDates_andWebFieldNames() {
        val draft = MemoryDraft(city = " Paris ", country = "France", lat = 48.85, lng = 2.35, tripName = "Trip", cost = "0", restaurants = listOf("A"))
        val wire = draft.toWire("user1")
        assertEquals(JsonNull, wire["date_from"])
        assertEquals(JsonNull, wire["trip_cost"])
        assertEquals("Paris", wire["city"]!!.jsonPrimitive.content)
        assertEquals("user1", wire["by_role"]!!.jsonPrimitive.content)
        assertTrue(wire.containsKey("favorite_moment"))
    }

    @Test fun mediaWire_shapes() {
        val voice = mediaWire(MediaKind.Voice, "https://x/v.m4a", "0:05", null)
        assertEquals("voice", voice["type"]!!.jsonPrimitive.content)
        val journal = mediaWire(MediaKind.Journal, null, null, "dear diary")
        assertEquals("dear diary", journal["journal_text"]!!.jsonPrimitive.content)
        assertTrue(!journal.containsKey("url"))
    }
}
