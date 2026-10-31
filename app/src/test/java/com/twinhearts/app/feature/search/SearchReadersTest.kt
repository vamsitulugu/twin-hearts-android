package com.twinhearts.app.feature.search

import com.twinhearts.app.feature.search.data.itemsOf
import com.twinhearts.app.feature.search.data.toFoundMessages
import com.twinhearts.app.feature.search.domain.stateSources
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchReadersTest {

    private fun source(key: String) = stateSources.first { it.key == key }

    @Test fun messagesSkipDeletedAndEmptyRows() {
        val json = Json.parseToJsonElement(
            """[{"id":1,"text":"hello love","sender_role":"user1","created_at":"2026-10-29T10:00:00Z"},
               {"id":2,"text":"gone","deleted":true},
               {"id":3,"text":"hidden","deleted_for":["user1"]},
               {"id":4,"text":null},
               "junk"]""",
        )
        val found = json.toFoundMessages("user1")
        assertEquals(listOf("1"), found.map { it.id })
    }

    @Test fun messagesAcceptWrappedList() {
        val json = Json.parseToJsonElement("""{"data":[{"id":"9","text":"hi"}]}""")
        assertEquals(1, json.toFoundMessages("user1").size)
    }

    @Test fun partnersSelfOnlyItemsAreHidden() {
        val doc = Json.parseToJsonElement(
            """{"bucket":[{"id":"a","title":"Paris","visibility":"self","by":"user2"},
                          {"id":"b","title":"Rome","visibility":"both","by":"user2"}]}""",
        ).jsonObject
        assertEquals(listOf("Rome"), doc.itemsOf(source("bucket"), "user1").map { it.title })
    }

    @Test fun partnersJournalAndSurprisesStayHidden() {
        val doc = Json.parseToJsonElement(
            """{"journal":[{"id":"1","title":"Mine","by":"user1"},{"id":"2","title":"Theirs","by":"user2"}],
                "surprises":[{"id":"3","title":"Gift for me","by":"user2"}]}""",
        ).jsonObject
        assertEquals(listOf("Mine"), doc.itemsOf(source("journal"), "user1").map { it.title })
        assertTrue(doc.itemsOf(source("surprises"), "user1").isEmpty())
    }

    @Test fun linksAndPicturesAreNeverSearchText() {
        val doc = Json.parseToJsonElement(
            """{"photos":[{"id":"1","name":"Beach day","url":"https://x/y.jpg"},{"id":"2","name":"data:image/png;base64,AAAA"}]}""",
        ).jsonObject
        assertEquals(listOf("Beach day"), doc.itemsOf(source("photos"), "user1").map { it.title })
    }

    @Test fun missingOrOddListsGiveNothing() {
        val doc = Json.parseToJsonElement("""{"events":"nope"}""").jsonObject
        assertTrue(doc.itemsOf(source("events"), "user1").isEmpty())
        assertTrue(doc.itemsOf(source("fights"), "user1").isEmpty())
    }
}
