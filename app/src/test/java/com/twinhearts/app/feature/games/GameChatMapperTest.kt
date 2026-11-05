package com.twinhearts.app.feature.games

import com.twinhearts.app.feature.games.data.mapper.chatMessagesOf
import com.twinhearts.app.feature.games.data.mapper.toChatBox
import com.twinhearts.app.feature.games.domain.model.GameChatKind
import com.twinhearts.app.feature.games.domain.model.GameChatMessage
import com.twinhearts.app.feature.games.domain.model.Side
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The in-game chat must read and write exactly what the web page (game-comms.js) stores. */
class GameChatMapperTest {

    private fun parse(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    @Test
    fun readsWhatTheWebPageSaved() {
        val state = parse(
            """{"gm_gchat_u2":{"ts":5,"m":[
                {"id":"a1","t":"text","x":"hello","ts":1000},
                {"id":"a2","t":"voice","u":"https://s/v.webm","d":7.4,"ts":2000}
            ]}}""",
        )
        val list = state.chatMessagesOf(Side.Two, mine = false)
        assertEquals(listOf("a1", "a2"), list.map { it.id })
        assertEquals(GameChatKind.Text, list[0].kind)
        assertEquals("hello", list[0].text)
        assertEquals(GameChatKind.Voice, list[1].kind)
        assertEquals("https://s/v.webm", list[1].url)
        assertEquals(7, list[1].seconds)
        assertTrue(list.none { it.mine })
    }

    @Test
    fun readsTheKeyOfTheRequestedSideOnly() {
        val state = parse("""{"gm_gchat_u1":{"m":[{"id":"x","t":"text","x":"one","ts":1}]}}""")
        assertTrue(state.chatMessagesOf(Side.Two, mine = false).isEmpty())
        assertEquals(1, state.chatMessagesOf(Side.One, mine = true).size)
    }

    @Test
    fun skipsOddMessagesButKeepsTheRest() {
        val state = parse(
            """{"gm_gchat_u1":{"m":[
                {"id":"ok","t":"text","x":"fine","ts":1},
                {"t":"text","x":"no id","ts":2},
                {"id":"bad-kind","t":"gif","ts":3},
                {"id":"no-time","t":"text","x":"x"},
                "junk"
            ]}}""",
        )
        assertEquals(listOf("ok"), state.chatMessagesOf(Side.One, mine = true).map { it.id })
    }

    @Test
    fun missingOrNullKeyGivesEmptyList() {
        assertTrue(parse("{}").chatMessagesOf(Side.One, mine = true).isEmpty())
        assertTrue(parse("""{"gm_gchat_u1":null}""").chatMessagesOf(Side.One, mine = true).isEmpty())
    }

    @Test
    fun writesTheWebShapeAndReadsItBack() {
        val mine = listOf(
            GameChatMessage("t1", GameChatKind.Text, text = "hi", sentAt = 1_000, mine = true),
            GameChatMessage("v1", GameChatKind.Voice, url = "https://s/v.m4a", seconds = 4, sentAt = 2_000, mine = true),
        )
        val box = mine.toChatBox(now = 9_999)
        val first = box["m"]!!.jsonArray[0].jsonObject
        assertEquals("text", first["t"]!!.jsonPrimitive.content)
        assertEquals("hi", first["x"]!!.jsonPrimitive.content)
        assertEquals("voice", box["m"]!!.jsonArray[1].jsonObject["t"]!!.jsonPrimitive.content)
        assertEquals("9999", box["ts"]!!.jsonPrimitive.content)

        val state = buildJsonObject { put("gm_gchat_u1", box) }
        val back = state.chatMessagesOf(Side.One, mine = true)
        assertEquals(listOf("t1", "v1"), back.map { it.id })
        assertEquals(4, back[1].seconds)
        assertEquals("https://s/v.m4a", back[1].url)
    }
}
