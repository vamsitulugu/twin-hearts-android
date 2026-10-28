package com.twinhearts.app.feature.fights

import com.twinhearts.app.feature.fights.data.mapper.asResolved
import com.twinhearts.app.feature.fights.data.mapper.fightsArray
import com.twinhearts.app.feature.fights.data.mapper.newFightJson
import com.twinhearts.app.feature.fights.data.mapper.toFight
import com.twinhearts.app.feature.fights.data.mapper.toFights
import com.twinhearts.app.feature.fights.domain.model.FightForm
import com.twinhearts.app.feature.fights.domain.model.FightStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FightsMapperTest {
    private val sample = """
        {"fights":[{"id":1761000000000,"title":"Plan changes","reason":"Late","category":"Time","intensity":7,
        "startedBy":"user1","feelings":["Angry","Hurt"],"need":"Calm talk","approach":["talk"],"notes":"Call first",
        "status":"open","date":"2026-10-20","visibility":"both","by":"user1","extra":"keep me"}]}
    """.trimIndent()

    @Test
    fun readsAFightWrittenByTheWebPage() {
        val f = Json.parseToJsonElement(sample).fightsArray().toFights().single()
        assertEquals(1761000000000L, f.id)
        assertEquals("Time", f.category)
        assertEquals(7, f.intensity)
        assertEquals(listOf("Angry", "Hurt"), f.feelings)
        assertEquals(FightStatus.Open, f.status)
        assertNull(f.resolvedDate)
    }

    @Test
    fun acceptsStateWithOrWithoutWrapperAndNull() {
        assertEquals(1, Json.parseToJsonElement("""{"state":$sample}""").fightsArray().size)
        assertEquals(0, Json.parseToJsonElement("null").fightsArray().size)
        assertEquals(0, Json.parseToJsonElement("""{"other":1}""").fightsArray().size)
    }

    @Test
    fun skipsEntriesWithoutAnIdAndClampsIntensity() {
        val arr = Json.parseToJsonElement("""{"fights":[{"title":"x"},{"id":"5","title":"y","intensity":99}]}""").fightsArray()
        val list = arr.toFights()
        assertEquals(1, list.size)
        assertEquals(5L, list.single().id)
        assertEquals(10, list.single().intensity)
    }

    @Test
    fun newFightHasTheSameShapeAsTheWeb() {
        val form = FightForm(title = " Plan ", category = "Time", intensity = 6, status = FightStatus.Resolved, feelings = listOf("Sad"))
        val obj = newFightJson(42L, form, "both", "2026-10-28", "user1")
        assertEquals("Plan", obj["title"]?.jsonPrimitive?.content)
        assertEquals("resolved", obj["status"]?.jsonPrimitive?.content)
        assertEquals("2026-10-28", obj["resolvedDate"]?.jsonPrimitive?.content)
        assertEquals("both", obj["visibility"]?.jsonPrimitive?.content)
        assertEquals("user1", obj["by"]?.jsonPrimitive?.content)
        assertEquals(42L, obj.toFight()?.id)
    }

    @Test
    fun openFightHasNoResolvedDate() {
        val obj = newFightJson(1L, FightForm(title = "a"), "both", "2026-10-28", "user1")
        assertTrue("resolvedDate" !in obj)
    }

    @Test
    fun resolvingKeepsUnknownFields() {
        val obj: JsonObject = Json.parseToJsonElement(sample).jsonObject["fights"]!!.let { (it as kotlinx.serialization.json.JsonArray)[0].jsonObject }
        val done = obj.asResolved("2026-10-28")
        assertEquals("resolved", done["status"]?.jsonPrimitive?.content)
        assertEquals("2026-10-28", done["resolvedDate"]?.jsonPrimitive?.content)
        assertEquals("keep me", done["extra"]?.jsonPrimitive?.content)
    }
}
