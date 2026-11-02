package com.twinhearts.app.feature.profile.highlights

import com.twinhearts.app.feature.profile.highlights.data.mapper.toHighlights
import com.twinhearts.app.feature.profile.highlights.data.remote.HighlightDto
import com.twinhearts.app.feature.profile.highlights.data.remote.HighlightItemDto
import com.twinhearts.app.feature.profile.highlights.domain.ClipKind
import com.twinhearts.app.feature.profile.highlights.domain.HighlightCategory
import com.twinhearts.app.feature.profile.highlights.domain.HighlightsMath
import com.twinhearts.app.feature.profile.highlights.domain.HighlightsOwner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HighlightMapperTest {
    private val dtos = listOf(
        HighlightDto("p", "user2", "Dates", "dates", listOf(HighlightItemDto("i4", "image", "http://i"))),
        HighlightDto(
            "m", "user1", "", "travel",
            listOf(HighlightItemDto("i1", "video", "http://v"), HighlightItemDto("i2", "audio", "http://a"), HighlightItemDto("i3", "image", "")),
        ),
        HighlightDto("e", "user1", "Empty", "love", emptyList()),
    )

    @Test fun emptyHighlightIsDroppedAndMineComesFirst() {
        val list = dtos.toHighlights("user1")
        assertEquals(listOf("m", "p"), list.map { it.id })
        assertTrue(list[0].mine)
        assertFalse(list[1].mine)
    }

    @Test fun unusableClipsAreDroppedAndBlankTitleGetsDefault() {
        val mine = dtos.toHighlights("user1").first()
        assertEquals("Highlights", mine.title)
        assertEquals(1, mine.clips.size)
        assertEquals(ClipKind.Video, mine.clips[0].kind)
    }

    @Test fun categoriesAndFilterWork() {
        val list = dtos.toHighlights("user1")
        assertEquals(listOf(HighlightCategory.Travel, HighlightCategory.Dates), HighlightsMath.usedCategories(list))
        assertEquals(listOf("p"), HighlightsMath.visible(list, HighlightCategory.Dates).map { it.id })
    }

    @Test fun eachRowShowsOnlyItsOwner() {
        val list = dtos.toHighlights("user1")
        assertEquals(listOf("m"), HighlightsMath.ownedBy(list, HighlightsOwner.Mine).map { it.id })
        assertEquals(listOf("p"), HighlightsMath.ownedBy(list, HighlightsOwner.Partner).map { it.id })
    }
}
