package com.twinhearts.app.feature.meetplanner

import com.twinhearts.app.feature.meetplanner.domain.NotesCodec
import com.twinhearts.app.feature.meetplanner.domain.NotesMeta
import com.twinhearts.app.feature.meetplanner.domain.formatKm
import com.twinhearts.app.feature.meetplanner.domain.model.GeoPoint
import com.twinhearts.app.feature.meetplanner.domain.haversineKm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Why this file exists: protects the pure rules (notes header that the web app also writes, distance maths). Not run when delivered. */
class MeetPlannerDomainTest {
    @Test fun notesRoundTrip() {
        val raw = NotesCodec.build("Bring umbrella", NotesMeta(to = "2026-11-07", type = "weekend"))
        val parsed = NotesCodec.parse(raw)
        assertEquals("Bring umbrella", parsed.text)
        assertEquals("2026-11-07", parsed.meta.to)
        assertEquals("weekend", parsed.meta.type)
    }

    @Test fun plainNotesHaveNoMeta() {
        val parsed = NotesCodec.parse("Just text")
        assertEquals("Just text", parsed.text)
        assertTrue(parsed.meta.to == null)
    }

    @Test fun distanceIsSane() {
        val km = haversineKm(GeoPoint(12.9716, 77.5946), GeoPoint(13.0827, 80.2707))
        assertTrue(km in 280.0..300.0)
        assertEquals("1.0 km", formatKm(1.0))
    }
}
