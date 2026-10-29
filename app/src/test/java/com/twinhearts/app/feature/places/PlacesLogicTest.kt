package com.twinhearts.app.feature.places

import com.twinhearts.app.feature.places.data.mapper.toPlaces
import com.twinhearts.app.feature.places.data.mapper.toStateBody
import com.twinhearts.app.feature.places.domain.formatVisitDate
import com.twinhearts.app.feature.places.domain.model.MemoryNote
import com.twinhearts.app.feature.places.domain.model.PlaceMemory
import com.twinhearts.app.feature.places.domain.parseCoordinate
import com.twinhearts.app.feature.places.domain.placeStats
import com.twinhearts.app.feature.places.domain.visiblePlaces
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Why this file exists: protects the rules the web app also relies on (saved JSON shape, search, sort, stats). Not run when delivered. */
class PlacesLogicTest {
    private val goa = PlaceMemory("p1", "Goa Trip", "Goa", LocalDate.of(2026, 3, 14), photos = listOf("a", "b"), favorite = true, memories = listOf(MemoryNote("n1", null, "hi")))
    private val ooty = PlaceMemory("p2", "Ooty", "Tamil Nadu", LocalDate.of(2026, 5, 1))
    private val none = PlaceMemory("p3", "No date")

    @Test fun statsCountEverything() {
        val s = placeStats(listOf(goa, ooty))
        assertEquals(2, s.places); assertEquals(1, s.favorites); assertEquals(2, s.photos); assertEquals(1, s.memories)
    }

    @Test fun newestFirstAndUndatedLast() {
        assertEquals(listOf("p2", "p1", "p3"), visiblePlaces(listOf(goa, none, ooty), "", false).map { it.id })
    }

    @Test fun searchAndFavoriteFilter() {
        assertEquals(listOf("p1"), visiblePlaces(listOf(goa, ooty), "goa", false).map { it.id })
        assertEquals(listOf("p1"), visiblePlaces(listOf(goa, ooty), "", true).map { it.id })
        assertEquals(listOf("p2"), visiblePlaces(listOf(goa, ooty), "nadu", false).map { it.id })
    }

    @Test fun webJsonRoundTrip() {
        val web = Json.parseToJsonElement(
            """{"other":1,"placesMemories":[{"id":"p9","name":"Beach","visitDate":"2026-01-05","banner":"","photos":["u1"],"lat":15.5,"lng":null,"favorite":true,
               "memories":[{"id":"m1","date":"2026-01-05","text":"fun","favorite":false}],"createdAt":123}]}""",
        )
        val list = web.toPlaces()
        assertEquals(1, list.size)
        assertEquals(LocalDate.of(2026, 1, 5), list[0].visitDate)
        assertNull(list[0].banner); assertNull(list[0].lng)
        assertEquals("fun", list[0].memories[0].text)
        val back = list.toStateBody()["placesMemories"].toString()
        assertEquals(list, Json.parseToJsonElement("""{"placesMemories":$back}""").toPlaces())
    }

    @Test fun nullStateGivesNoPlaces() {
        assertEquals(emptyList<PlaceMemory>(), JsonNull.toPlaces())
    }

    @Test fun datesAndCoordinates() {
        assertEquals("5 Nov 2026", formatVisitDate(LocalDate.of(2026, 11, 5)))
        assertEquals("", formatVisitDate(null))
        assertEquals(15.5, parseCoordinate("15,5", 90.0)!!, 0.0)
        assertNull(parseCoordinate("95", 90.0)); assertNull(parseCoordinate("abc", 90.0))
    }
}
