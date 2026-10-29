package com.twinhearts.app.feature.surprises

import com.twinhearts.app.feature.surprises.data.mapper.capsuleJson
import com.twinhearts.app.feature.surprises.data.mapper.surpriseJson
import com.twinhearts.app.feature.surprises.data.mapper.toSurprisesData
import com.twinhearts.app.feature.surprises.data.mapper.xp
import com.twinhearts.app.feature.surprises.domain.model.MediaKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SurprisesMapperTest {
    private fun state(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject

    @Test fun `reads items saved by the web app`() {
        val data = state(
            """{"surprises":[
                {"id":1700000000000,"type":"🎬","title":"Movie night","msg":"Hi","mediaUrl":"data:video/mp4;base64,AAA","unlock":"2026-12-01","by":"user1","opened":false},
                {"id":1700000005000,"type":"🎁","title":"Gift","msg":"","mediaUrl":"","unlock":"2026-11-01","by":"user2","opened":false}
            ],"capsules":[{"id":1,"msg":"Dear us","unlock":"2027-01-01","by":"user1","created":"2026-10-28","opened":true}]}""",
        ).toSurprisesData(myRole = "user1")

        assertEquals(listOf("1700000005000", "1700000000000"), data.surprises.map { it.id }) // newest first
        val mine = data.surprises.last()
        assertTrue(mine.mine)
        assertEquals(MediaKind.Video, mine.mediaKind)
        assertEquals(LocalDate.of(2026, 12, 1), mine.unlock)
        assertFalse(data.surprises.first().mine)
        assertNull(data.surprises.first().mediaKind)
        assertTrue(data.capsules.single().opened)
        assertEquals("Dear us", data.capsules.single().message)
    }

    @Test fun `one broken item does not hide the others`() {
        val data = state(
            """{"surprises":[{"id":1,"title":"no date"},"junk",{"id":2,"title":"ok","unlock":"2026-11-01","by":"user1"}]}""",
        ).toSurprisesData("user1")
        assertEquals(listOf("2"), data.surprises.map { it.id })
    }

    @Test fun `a state with nothing in it gives empty lists`() {
        val data = state("{}").toSurprisesData("user1")
        assertTrue(data.surprises.isEmpty())
        assertTrue(data.capsules.isEmpty())
    }

    @Test fun `a new surprise uses the web field names plus the app's two extras`() {
        val json = surpriseJson(
            id = 42, emoji = "🎙️", title = "T", message = "M", unlock = LocalDate.of(2026, 12, 25),
            role = "user2", mediaUrl = "https://x/a.m4a", mediaKind = MediaKind.Audio, mediaPath = "c1/a.m4a",
        )
        assertEquals("42", json["id"]!!.jsonPrimitive.content)
        assertEquals("🎙️", json["type"]!!.jsonPrimitive.content)
        assertEquals("T", json["title"]!!.jsonPrimitive.content)
        assertEquals("M", json["msg"]!!.jsonPrimitive.content)
        assertEquals("2026-12-25", json["unlock"]!!.jsonPrimitive.content)
        assertEquals("user2", json["by"]!!.jsonPrimitive.content)
        assertEquals("false", json["opened"]!!.jsonPrimitive.content)
        assertEquals("audio", json["mediaKind"]!!.jsonPrimitive.content)
        assertEquals("c1/a.m4a", json["mediaPath"]!!.jsonPrimitive.content)
    }

    @Test fun `a surprise without a file has no media fields`() {
        val json = surpriseJson(1, "🎁", "T", "", LocalDate.of(2026, 12, 25), "user1", "", null, null)
        assertEquals("", json["mediaUrl"]!!.jsonPrimitive.content)
        assertNull(json["mediaKind"])
        assertNull(json["mediaPath"])
    }

    @Test fun `a new capsule uses the web field names`() {
        val json = capsuleJson(7, "Hello", LocalDate.of(2027, 2, 14), "user1", LocalDate.of(2026, 10, 28))
        assertEquals("Hello", json["msg"]!!.jsonPrimitive.content)
        assertEquals("2027-02-14", json["unlock"]!!.jsonPrimitive.content)
        assertEquals("2026-10-28", json["created"]!!.jsonPrimitive.content)
        assertEquals("false", json["opened"]!!.jsonPrimitive.content)
    }

    @Test fun `xp is read as a number and defaults to zero`() {
        assertEquals(35L, state("""{"xp":35}""").xp())
        assertEquals(0L, state("{}").xp())
        assertEquals(0L, state("""{"xp":"oops"}""").xp())
    }
}
