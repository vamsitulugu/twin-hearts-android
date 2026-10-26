package com.twinhearts.app.feature.home

import com.twinhearts.app.feature.home.data.mapper.toLoveCardState
import com.twinhearts.app.feature.home.domain.model.LoveKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Checks the card reads the shared state the same way the web card does. */
class LoveCardMapperTest {
    private fun parse(json: String) = Json.parseToJsonElement(json)

    @Test fun nullState_isEmpty() {
        val card = JsonNull.toLoveCardState("user1")
        assertNull(card.last)
        assertNull(card.pendingIncomingHugId)
    }

    @Test fun newestSignalWins() {
        val state = parse("""{"touch":{"from":"user2","ts":1000},"missYou":{"from":"user1","ts":2000,"status":"sent"}}""")
        val last = state.toLoveCardState("user1").last!!
        assertEquals(LoveKind.MissYou, last.kind)
        assertEquals(true, last.fromMe)
    }

    @Test fun touchFromPartner_isReceived() {
        val last = parse("""{"touch":{"from":"user2","ts":5000}}""").toLoveCardState("user1").last!!
        assertEquals(LoveKind.Touch, last.kind)
        assertEquals(false, last.fromMe)
        assertEquals(5000L, last.timestampMillis)
    }

    @Test fun incomingPendingHug_isOfferedNotShownAsSignal() {
        val card = parse("""{"hug":{"id":77,"from":"user2","ts":77,"status":"pending"}}""").toLoveCardState("user1")
        assertNull(card.last)
        assertEquals(77L, card.pendingIncomingHugId)
    }

    @Test fun myPendingHug_showsAsSent() {
        val card = parse("""{"hug":{"id":9,"from":"user1","ts":9,"status":"pending"}}""").toLoveCardState("user1")
        assertEquals(LoveKind.Hug, card.last!!.kind)
        assertEquals(true, card.last!!.fromMe)
        assertNull(card.pendingIncomingHugId)
    }

    @Test fun acceptedHug_usesAcceptedTime_andNumbersMayBeDecimal() {
        val card = parse("""{"hug":{"id":1,"from":"user2","ts":1,"status":"accepted","acceptedTs":1.7E12}}""").toLoveCardState("user1")
        assertEquals(LoveKind.Hug, card.last!!.kind)
        assertEquals(1_700_000_000_000L, card.last!!.timestampMillis)
    }
}
