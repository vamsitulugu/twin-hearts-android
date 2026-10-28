package com.twinhearts.app.feature.twinai

import com.twinhearts.app.feature.twinai.data.mapper.latestMood
import com.twinhearts.app.feature.twinai.data.mapper.latestMood
import com.twinhearts.app.feature.twinai.data.mapper.parseServerInstant
import com.twinhearts.app.feature.twinai.data.mapper.toDomain
import com.twinhearts.app.feature.twinai.data.remote.ResultDto
import com.twinhearts.app.feature.twinai.data.remote.RoundDto
import com.twinhearts.app.feature.twinai.data.remote.SectionDto
import com.twinhearts.app.feature.twinai.data.remote.SessionDto
import com.twinhearts.app.feature.twinai.domain.model.ProPlan
import com.twinhearts.app.feature.twinai.domain.model.ProUpgradeForm
import com.twinhearts.app.feature.twinai.domain.model.RoundStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TwinAiMapperTest {
    @Test
    fun roundMapsStatusAndResult() {
        val dto = RoundDto(
            id = "r1", roundNumber = 2, status = "done", youSubmitted = true, partnerSubmitted = true,
            result = ResultDto("conflict", listOf(SectionDto("Why", "Because")), safetyFlag = false),
        )
        val round = dto.toDomain()
        assertEquals(RoundStatus.Done, round.status)
        assertEquals("CONFLICT", round.result?.intent)
        assertEquals("Why", round.result?.sections?.single()?.title)
    }

    @Test
    fun unknownStatusIsPending() {
        assertEquals(RoundStatus.Pending, RoundStatus.from("whatever"))
        assertTrue(RoundStatus.Analyzing.inProgress)
        assertTrue(!RoundStatus.Done.inProgress)
    }

    @Test
    fun blankTitleBecomesNull() {
        assertNull(SessionDto("s", title = "  ").toDomain().title)
    }

    @Test
    fun parsesSupabaseTimes() {
        assertNotNull(parseServerInstant("2026-10-26T09:00:00.123+00:00"))
        assertNotNull(parseServerInstant("2026-10-26T09:00:00Z"))
        assertNull(parseServerInstant("nope"))
        assertNull(parseServerInstant(null))
    }

    @Test
    fun latestMoodReadsNewestJournalEntry() {
        val state = Json.parseToJsonElement("""{"journal":[{"mood":"😊","body":"x"},{"mood":"😢"}]}""")
        assertEquals("😊", latestMood(state))
        assertEquals("", latestMood(Json.parseToJsonElement("""{"journal":[]}""")))
        assertEquals("", latestMood(Json.parseToJsonElement("null")))
    }

    @Test
    fun latestMoodReadsFirstJournalEntry() {
        val state = Json.parseToJsonElement("""{"journal":[{"mood":"😊","body":"x"},{"mood":"😢"}]}""")
        assertEquals("😊", latestMood(state))
    }

    @Test
    fun latestMoodIsEmptyWhenThereIsNoJournal() {
        assertEquals("", latestMood(Json.parseToJsonElement("""{"other":1}""")))
        assertEquals("", latestMood(Json.parseToJsonElement("null")))
        assertEquals("", latestMood(null))
    }

    @Test
    fun upgradeFormNeedsAllRequiredFields() {
        val form = ProUpgradeForm(ProPlan.Monthly, "A", "1", "B", "2", "TXN")
        assertTrue(form.isComplete)
        assertTrue(!form.copy(transactionId = " ").isComplete)
        assertEquals(499, ProPlan.Lifetime.amount)
    }
}
