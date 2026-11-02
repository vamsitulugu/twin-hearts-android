package com.twinhearts.app.feature.profile.highlights

import com.twinhearts.app.feature.profile.highlights.domain.ClipProblem
import com.twinhearts.app.feature.profile.highlights.domain.HighlightCategory
import com.twinhearts.app.feature.profile.highlights.domain.HighlightsMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HighlightsMathTest {
    @Test fun videoAndPhotoAreAccepted() {
        assertNull(HighlightsMath.problemOf("video/mp4", 10))
        assertNull(HighlightsMath.problemOf("image/jpeg", HighlightsMath.MAX_BYTES))
    }

    @Test fun otherFilesAndHugeFilesAreRefused() {
        assertEquals(ClipProblem.NotMedia, HighlightsMath.problemOf("application/pdf", 10))
        assertEquals(ClipProblem.TooBig, HighlightsMath.problemOf("video/mp4", HighlightsMath.MAX_BYTES + 1))
    }

    @Test fun blankNameFallsBackAndLongNameIsCut() {
        assertEquals("Travel", HighlightsMath.finalTitle("  ", "Travel"))
        assertEquals(HighlightsMath.TITLE_MAX, HighlightsMath.finalTitle("a".repeat(50), "x").length)
    }

    @Test fun unknownCategoryBecomesOther() {
        assertEquals(HighlightCategory.Other, HighlightCategory.fromWire("zzz"))
        assertEquals(HighlightCategory.Love, HighlightCategory.fromWire("love"))
    }
}
