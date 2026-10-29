package com.twinhearts.app.feature.surprises

import com.twinhearts.app.feature.surprises.domain.MAX_AUDIO_MB
import com.twinhearts.app.feature.surprises.domain.MAX_VIDEO_MB
import com.twinhearts.app.feature.surprises.domain.MediaProblem
import com.twinhearts.app.feature.surprises.domain.SurpriseView
import com.twinhearts.app.feature.surprises.domain.checkPickedMedia
import com.twinhearts.app.feature.surprises.domain.classifyMime
import com.twinhearts.app.feature.surprises.domain.isLocked
import com.twinhearts.app.feature.surprises.domain.mediaKindOf
import com.twinhearts.app.feature.surprises.domain.model.MediaKind
import com.twinhearts.app.feature.surprises.domain.model.PickedMedia
import com.twinhearts.app.feature.surprises.domain.model.Surprise
import com.twinhearts.app.feature.surprises.domain.newestFirstSurprises
import com.twinhearts.app.feature.surprises.domain.viewOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SurpriseRulesTest {
    private val today = LocalDate.of(2026, 10, 28)

    private fun surprise(id: String = "1", unlock: LocalDate = today, mine: Boolean = false) =
        Surprise(id, "🎁", "Title", "", "", null, unlock, mine)

    @Test fun `a future date is locked and the day itself is open`() {
        assertTrue(isLocked(today.plusDays(1), today))
        assertFalse(isLocked(today, today))
        assertFalse(isLocked(today.minusDays(1), today))
    }

    @Test fun `partner never sees a locked surprise, the author does`() {
        assertEquals(SurpriseView.PartnerLocked, viewOf(surprise(unlock = today.plusDays(3), mine = false), today))
        assertEquals(SurpriseView.MineLocked, viewOf(surprise(unlock = today.plusDays(3), mine = true), today))
    }

    @Test fun `an unlocked surprise is open to both`() {
        assertEquals(SurpriseView.Unlocked, viewOf(surprise(unlock = today, mine = false), today))
        assertEquals(SurpriseView.Unlocked, viewOf(surprise(unlock = today.minusDays(9), mine = true), today))
    }

    @Test fun `newest id comes first and unreadable ids go last`() {
        val sorted = newestFirstSurprises(listOf(surprise("5"), surprise("9"), surprise("abc")))
        assertEquals(listOf("9", "5", "abc"), sorted.map { it.id })
    }

    @Test fun `web data links are recognised by their mime type`() {
        assertEquals(MediaKind.Video, mediaKindOf("data:video/mp4;base64,AAA", null, "🎁"))
        assertEquals(MediaKind.Audio, mediaKindOf("data:audio/webm;base64,AAA", null, "🎁"))
        assertEquals(MediaKind.Image, mediaKindOf("data:image/png;base64,AAA", null, "🎁"))
    }

    @Test fun `normal links are recognised by the hint, then the file ending, then the emoji`() {
        assertEquals(MediaKind.Video, mediaKindOf("https://x/a.jpg", "video", "🎁"))
        assertEquals(MediaKind.Video, mediaKindOf("https://x/y/a.MP4?token=1", null, "🎁"))
        assertEquals(MediaKind.Audio, mediaKindOf("https://x/a.m4a", null, "🎁"))
        assertEquals(MediaKind.Audio, mediaKindOf("https://x/a.webm", null, "🎙️"))
        assertEquals(MediaKind.Video, mediaKindOf("https://x/a.webm", null, "🎬"))
        assertEquals(MediaKind.Video, mediaKindOf("https://x/abc", null, "🎬"))
        assertEquals(MediaKind.Image, mediaKindOf("https://x/a.jpg", null, "📷"))
    }

    @Test fun `no link means no media`() {
        assertNull(mediaKindOf("", "video", "🎁"))
        assertNull(mediaKindOf("   ", null, "🎬"))
    }

    @Test fun `only the file types the server accepts are allowed`() {
        assertEquals(MediaKind.Image, classifyMime("image/heic"))
        assertEquals(MediaKind.Video, classifyMime("video/mp4"))
        assertEquals(MediaKind.Audio, classifyMime("audio/mpeg; charset=x"))
        assertNull(classifyMime("video/x-msvideo"))
        assertNull(classifyMime("application/pdf"))
    }

    @Test fun `big or unsupported files are turned away before upload`() {
        val mb = 1024L * 1024L
        assertEquals(MediaProblem.Unsupported, checkPickedMedia(PickedMedia("u", "a.pdf", "application/pdf", 1)))
        assertEquals(MediaProblem.TooBig, checkPickedMedia(PickedMedia("u", "a.mp4", "video/mp4", (MAX_VIDEO_MB + 1) * mb)))
        assertEquals(MediaProblem.TooBig, checkPickedMedia(PickedMedia("u", "a.mp3", "audio/mpeg", (MAX_AUDIO_MB + 1) * mb)))
        assertNull(checkPickedMedia(PickedMedia("u", "a.mp4", "video/mp4", 10 * mb)))
        assertNull(checkPickedMedia(PickedMedia("u", "a.jpg", "image/jpeg", 900 * mb))) // photos are shrunk first
    }
}
