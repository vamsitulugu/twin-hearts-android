package com.twinhearts.app.feature.music

import com.twinhearts.app.feature.music.domain.LibraryQueries
import com.twinhearts.app.feature.music.domain.model.RecordingFilter
import com.twinhearts.app.feature.music.domain.model.RecordingOwner
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingSearchTest {
    private val rows = listOf(
        recording("a").copy(title = "Café voice note"),
        recording("b", by = "user2").copy(title = "Birthday wish"),
    )

    private fun ids(query: String) =
        LibraryQueries.filterRecordings(rows, RecordingFilter.Audio, RecordingOwner.Everyone, "user1", query).map { it.id }

    @Test
    fun blankQuery_keepsEverything() {
        assertEquals(setOf("a", "b"), ids("").toSet())
        assertEquals(setOf("a", "b"), ids("   ").toSet())
    }

    @Test
    fun query_ignoresCaseAndAccents() {
        assertEquals(listOf("a"), ids("cafe"))
        assertEquals(listOf("b"), ids("BIRTHDAY"))
    }

    @Test
    fun query_andOwnerFilterWorkTogether() {
        val mineOnly = LibraryQueries.filterRecordings(rows, RecordingFilter.Audio, RecordingOwner.Mine, "user1", "birthday")
        assertEquals(emptyList<String>(), mineOnly.map { it.id })
    }
}
