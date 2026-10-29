package com.twinhearts.app.feature.bucket

import com.twinhearts.app.feature.bucket.data.toDreams
import com.twinhearts.app.feature.bucket.domain.canSeeDream
import com.twinhearts.app.feature.bucket.domain.model.BucketCategory
import com.twinhearts.app.feature.bucket.domain.model.BucketVisibility
import com.twinhearts.app.feature.bucket.domain.progressLabel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BucketLogicTest {

    @Test fun `both is visible to everyone`() {
        assertTrue(canSeeDream("both", "user1", me = false, role = "user2"))
        assertTrue(canSeeDream(null, null, me = false, role = "user2"))
    }

    @Test fun `self is visible only to its author, or when nobody is recorded`() {
        assertTrue(canSeeDream("self", "user1", me = false, role = "user1"))
        assertFalse(canSeeDream("self", "user1", me = false, role = "user2"))
        assertTrue(canSeeDream("self", "", me = false, role = "user2"))
        assertTrue(canSeeDream("self", "user1", me = true, role = "user2"))
    }

    @Test fun `partner-only is hidden from its author`() {
        assertFalse(canSeeDream("partner", "user1", me = false, role = "user1"))
        assertTrue(canSeeDream("partner", "user1", me = false, role = "user2"))
    }

    @Test fun `mapper hides private dreams of the partner and keeps the rest`() {
        val items = Json.parseToJsonElement(
            """[{"id":1,"title":"Paris","done":true,"date":"2027-05-01","cat":"Travel","visibility":"both","by":"user1"},
               {"id":2,"title":"Secret gift","visibility":"self","by":"user1"},
               {"id":3,"title":"Dream Goals item","target":"2028-01-02","by":"user1"},
               {"id":4,"title":"  "}]""",
        ).jsonArray
        val mine = items.toDreams("user2")
        assertEquals(listOf("Paris", "Dream Goals item"), mine.map { it.title })
        assertEquals(BucketCategory.Travel, mine[0].category)
        assertEquals(LocalDate.parse("2028-01-02"), mine[1].date)   // `target` from Dream Goals
        assertEquals(BucketVisibility.Both, mine[1].visibility)
        assertEquals(3, items.toDreams("user1").size)               // the author sees their own secret
    }

    @Test fun `progress label counts done over total`() {
        val items = Json.parseToJsonElement("""[{"id":1,"title":"A","done":true},{"id":2,"title":"B"}]""").jsonArray
        assertEquals("1/2", progressLabel(items.toDreams("user1")))
        assertEquals("0/0", progressLabel(emptyList()))
    }
}
