package com.twinhearts.app.feature.help

import com.twinhearts.app.feature.help.domain.PENDING_ID_PREFIX
import com.twinhearts.app.feature.help.domain.filterFaqs
import com.twinhearts.app.feature.help.domain.formatClock
import com.twinhearts.app.feature.help.domain.initialOf
import com.twinhearts.app.feature.help.domain.isPending
import com.twinhearts.app.feature.help.domain.mergeSendResult
import com.twinhearts.app.feature.help.domain.model.FaqItem
import com.twinhearts.app.feature.help.domain.model.HelpCategory
import com.twinhearts.app.feature.help.domain.model.SendResult
import com.twinhearts.app.feature.help.domain.model.SenderKind
import com.twinhearts.app.feature.help.domain.model.SupportMessage
import com.twinhearts.app.feature.help.domain.model.SupportMode
import com.twinhearts.app.feature.help.domain.model.SupportTicket
import com.twinhearts.app.feature.help.domain.parseTimestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

class HelpLogicTest {
    private val pair = FaqItem("a", "Pairing & Account", "How do I pair?", "Use Settings.", listOf("phone"))
    private val vault = FaqItem("b", "Privacy & Security", "Vault PIN?", "Four digits.", listOf("security"))
    private val faqs = listOf(pair, vault)

    @Test
    fun emptySearchAndAllTopicsKeepEverything() {
        assertEquals(faqs, filterFaqs(faqs, "  ", HelpCategory.All))
    }

    @Test
    fun searchMatchesQuestionAnswerAndTagsIgnoringCase() {
        assertEquals(listOf(pair), filterFaqs(faqs, "PAIR", HelpCategory.All))
        assertEquals(listOf(vault), filterFaqs(faqs, "digits", HelpCategory.All))
        assertEquals(listOf(pair), filterFaqs(faqs, "phone", HelpCategory.All))
    }

    @Test
    fun topicUsesTheServerCategoryName() {
        assertEquals(listOf(vault), filterFaqs(faqs, "", HelpCategory.Privacy))
        assertTrue(filterFaqs(faqs, "", HelpCategory.Music).isEmpty())
    }

    @Test
    fun parsesZuluAndOffsetTimes() {
        val expected = Instant.parse("2026-10-29T10:15:30Z")
        assertEquals(expected, parseTimestamp("2026-10-29T10:15:30Z"))
        assertEquals(expected, parseTimestamp("2026-10-29T10:15:30.000+00:00"))
        assertEquals(expected, parseTimestamp("2026-10-29T15:45:30+05:30"))
    }

    @Test
    fun badTimesGiveNull() {
        assertNull(parseTimestamp(null))
        assertNull(parseTimestamp(""))
        assertNull(parseTimestamp("yesterday"))
    }

    @Test
    fun clockShowsHourAndMinute() {
        val text = formatClock(Instant.parse("2026-10-29T10:15:00Z"), ZoneOffset.UTC, Locale.US)
        assertTrue(text, text.contains("10:15"))
    }

    @Test
    fun initialIsUppercaseOrU() {
        assertEquals("V", initialOf(" vamsi"))
        assertEquals("U", initialOf("  "))
    }

    @Test
    fun serverModeAndSenderFallBackLikeTheWeb() {
        assertEquals(SupportMode.Human, SupportMode.fromWire("human"))
        assertEquals(SupportMode.Ai, SupportMode.fromWire(null))
        assertEquals(SenderKind.Admin, SenderKind.fromWire("admin"))
        assertEquals(SenderKind.User, SenderKind.fromWire("whatever"))
    }

    private fun msg(id: String, sender: SenderKind = SenderKind.User) =
        SupportMessage(id, sender, "", id, null)

    @Test
    fun mergeUsesServerTicketWhenPresent() {
        val current = listOf(msg("1"), msg("${PENDING_ID_PREFIX}9"))
        val ticket = SupportTicket("t", SupportMode.Ai, listOf(msg("1"), msg("2"), msg("3", SenderKind.Ai)))
        assertEquals(ticket.messages, mergeSendResult(current, SendResult(ticket, null, null)))
    }

    @Test
    fun mergeSwapsPendingForConfirmedWithoutTicket() {
        val pending = msg("${PENDING_ID_PREFIX}9")
        assertTrue(pending.isPending)
        val merged = mergeSendResult(
            listOf(msg("1"), pending),
            SendResult(null, msg("2"), msg("3", SenderKind.Ai)),
        )
        assertEquals(listOf("1", "2", "3"), merged.map { it.id })
    }
}
