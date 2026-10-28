package com.twinhearts.app.feature.collection

import com.twinhearts.app.feature.collection.domain.CollectionLogic
import com.twinhearts.app.feature.collection.domain.model.PhotoFilter
import com.twinhearts.app.feature.collection.domain.model.PhotoItem
import com.twinhearts.app.feature.collection.domain.model.VoiceFilter
import com.twinhearts.app.feature.collection.domain.model.VoiceItem
import com.twinhearts.app.feature.collection.domain.model.VoiceSort
import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionLogicTest {

    private fun v(id: String, by: String, sec: Int, at: String) =
        VoiceItem(id, "https://x/$id.m4a", by, sec, at, emptyList())

    private val list = listOf(
        v("1", "user1", 10, "2026-10-01T08:00:00Z"),
        v("2", "user2", 95, "2026-10-02T09:00:00Z"),
        v("3", "user1", 3, "2026-10-03T10:00:00Z"),
    )

    private fun run(
        filter: VoiceFilter = VoiceFilter.All,
        sort: VoiceSort = VoiceSort.Newest,
        query: String = "",
        favs: Set<String> = emptySet(),
    ) = CollectionLogic.voices(list, filter, sort, query, favs, "user1", "Vamsi", "Likitha").map { it.id }

    @Test fun `newest first by default`() = assertEquals(listOf("3", "2", "1"), run())
    @Test fun `oldest and shortest sorts`() {
        assertEquals(listOf("1", "2", "3"), run(sort = VoiceSort.Oldest))
        assertEquals(listOf("3", "1", "2"), run(sort = VoiceSort.Shortest))
        assertEquals(listOf("2", "1", "3"), run(sort = VoiceSort.Longest))
    }
    @Test fun `my voice and partner filters use the signed in role`() {
        assertEquals(listOf("3", "1"), run(filter = VoiceFilter.Mine))
        assertEquals(listOf("2"), run(filter = VoiceFilter.Partner))
    }
    @Test fun `favorites filter and sort`() {
        assertEquals(listOf("2"), run(filter = VoiceFilter.Favorites, favs = setOf("2")))
        assertEquals(listOf("2", "3", "1"), run(sort = VoiceSort.Favorites, favs = setOf("2")))
    }
    @Test fun `search matches sender date and duration`() {
        assertEquals(listOf("2"), run(query = "likitha"))
        assertEquals(listOf("1"), run(query = "2026-10-01"))
        assertEquals(listOf("2"), run(query = "1:35"))
    }
    @Test fun `longest filter picks by length, then the chosen sort applies`() {
        // Same order of operations as the web app: filter first, sort after.
        assertEquals(listOf("3", "2", "1"), run(filter = VoiceFilter.Longest))
        assertEquals(listOf("2", "1", "3"), run(filter = VoiceFilter.Longest, sort = VoiceSort.Longest))
    }

    @Test fun `duration labels`() {
        assertEquals("0:05", CollectionLogic.durationLabel(5))
        assertEquals("1:35", CollectionLogic.durationLabel(95))
        assertEquals("0:00", CollectionLogic.durationLabel(-4))
    }
    @Test fun `total duration`() {
        assertEquals("0s", CollectionLogic.totalDuration(0))
        assertEquals("45s", CollectionLogic.totalDuration(45))
        assertEquals("2m", CollectionLogic.totalDuration(120))
        assertEquals("2m 5s", CollectionLogic.totalDuration(125))
        assertEquals("1h 03m", CollectionLogic.totalDuration(3780))
    }

    private fun p(id: String, date: String, video: Boolean = false) = PhotoItem(id, "u", video, date, "Chat photo", true)
    private val photos = listOf(p("a", "2026-10-05"), p("b", "2026-09-20", true), p("c", "2026-10-01"), p("d", ""))

    @Test fun `photo filters`() {
        assertEquals(listOf("b"), CollectionLogic.photos(photos, PhotoFilter.Videos, "", emptySet()).map { it.id })
        assertEquals(listOf("a", "c", "d"), CollectionLogic.photos(photos, PhotoFilter.Photos, "", emptySet()).map { it.id })
        assertEquals(listOf("c"), CollectionLogic.photos(photos, PhotoFilter.Favorites, "", setOf("c")).map { it.id })
        assertEquals(listOf("a"), CollectionLogic.photos(photos, PhotoFilter.All, "10-05", emptySet()).map { it.id })
    }
    @Test fun `photos group by month newest first`() {
        val g = CollectionLogic.photosByMonth(photos)
        assertEquals(listOf("2026-10", "2026-09", ""), g.map { it.first })
        assertEquals(listOf("a", "c"), g[0].second.map { it.id })
    }
}
