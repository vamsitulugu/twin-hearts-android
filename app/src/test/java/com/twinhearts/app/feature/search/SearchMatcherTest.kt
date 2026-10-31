package com.twinhearts.app.feature.search

import com.twinhearts.app.feature.search.domain.SearchMatcher
import com.twinhearts.app.feature.search.domain.canSeeItem
import com.twinhearts.app.feature.search.domain.model.SearchDestination
import com.twinhearts.app.feature.search.domain.model.SearchDoc
import com.twinhearts.app.feature.search.domain.model.SearchSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchMatcherTest {

    /** Like the real catalog: the page a feature lives in is also one of its keywords. */
    private fun doc(key: String, title: String, keywords: String = "", detail: String = "", where: String = "") =
        SearchDoc(key, SearchSection.Features, title, detail, where, "x", SearchDestination.Home, "$keywords $where".trim())

    private val docs = listOf(
        doc("cal", "Calendar", "events dates schedule birthday"),
        doc("notif", "Notifications", "push alerts quiet hours", where = "Settings"),
        doc("music", "Music", "songs playlist lyrics"),
        doc("lyrics", "Lyrics", "words synced", where = "Music"),
        doc("fight", "Log a Fight", "argument quarrel"),
    )

    @Test fun blankQueryGivesNothing() {
        assertTrue(SearchMatcher.rank("   ", docs).isEmpty())
    }

    @Test fun titlePrefixBeatsKeyword() {
        val keys = SearchMatcher.rank("music", docs).map { it.key }
        assertEquals("music", keys.first())
        assertTrue("lyrics" in keys) // its parent page name is a keyword
    }

    @Test fun synonymFindsPage() {
        assertEquals("fight", SearchMatcher.rank("quarrel", docs).first().key)
        assertEquals("notif", SearchMatcher.rank("quiet hours", docs).first().key)
    }

    @Test fun everyWordMustMatch() {
        assertTrue(SearchMatcher.rank("calendar quarrel", docs).isEmpty())
    }

    @Test fun accentsAndCaseAreIgnored() {
        val list = listOf(doc("cafe", "Café dates"))
        assertEquals(1, SearchMatcher.rank("CAFE", list).size)
    }

    @Test fun oneLetterTypoStillFinds() {
        assertEquals("cal", SearchMatcher.rank("calender", docs).first().key)
        assertTrue(SearchMatcher.rank("notifcations", docs).any { it.key == "notif" })
    }

    @Test fun exactMatchHidesTypoNoise() {
        val list = listOf(doc("fight", "Fight Log"), doc("look", "Appearance", "dark light"))
        assertEquals(listOf("fight"), SearchMatcher.rank("fight", list).map { it.key })
        // With no exact row, the near-miss is still offered.
        assertEquals(listOf("look"), SearchMatcher.rank("ligth", list).map { it.key })
    }

    @Test fun shortWordsAreNotTypoMatched() {
        assertTrue(SearchMatcher.rank("mus1", listOf(doc("a", "Map"))).isEmpty())
    }

    @Test fun editDistanceRules() {
        assertTrue(SearchMatcher.editDistanceAtMostOne("abcd", "abcd"))
        assertTrue(SearchMatcher.editDistanceAtMostOne("abcd", "abxd"))
        assertTrue(SearchMatcher.editDistanceAtMostOne("abcd", "abd"))
        assertTrue(SearchMatcher.editDistanceAtMostOne("abd", "abcd"))
        assertFalse(SearchMatcher.editDistanceAtMostOne("abcd", "axyd"))
        assertFalse(SearchMatcher.editDistanceAtMostOne("abcd", "ab"))
    }

    @Test fun snippetCutsAroundMatch() {
        val text = "a".repeat(100) + " dinner " + "b".repeat(100)
        val s = SearchMatcher.snippet(text, listOf("dinner"), radius = 20)
        assertTrue(s.startsWith("…") && s.endsWith("…") && "dinner" in s)
        assertEquals("short", SearchMatcher.snippet("short", listOf("x")))
    }

    @Test fun limitIsApplied() {
        val many = (1..50).map { doc("k$it", "Song $it") }
        assertEquals(10, SearchMatcher.rank("song", many, limit = 10).size)
    }

    @Test fun privacyRule() {
        assertTrue(canSeeItem("self", "user1", false, "user1"))
        assertFalse(canSeeItem("self", "user2", false, "user1"))
        assertTrue(canSeeItem("partner", "user2", false, "user1"))
        assertFalse(canSeeItem("partner", "user1", false, "user1"))
        assertTrue(canSeeItem("", "user2", false, "user1"))
    }
}
