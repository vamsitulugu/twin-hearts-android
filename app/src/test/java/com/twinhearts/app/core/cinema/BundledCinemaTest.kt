package com.twinhearts.app.core.cinema

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BundledCinemaTest {
    @Test
    fun localUrl_keepsOnlyTheRoomSettings() {
        assertEquals(
            "https://appassets.androidplatform.net/cinema/index.html#s=abc&r=1&h=1",
            BundledCinema.localUrl("https://twinhearts.vercel.app/cinema/#s=abc&r=1&h=1"),
        )
    }

    @Test
    fun localUrl_isNullWithoutRoomSettings() {
        assertNull(BundledCinema.localUrl("https://twinhearts.vercel.app/cinema/"))
        assertNull(BundledCinema.localUrl("https://twinhearts.vercel.app/cinema/#"))
        assertNull(BundledCinema.localUrl(null))
    }

    @Test
    fun isOwnUrl_onlyTheAppsOwnHttpsHost() {
        assertTrue(BundledCinema.isOwnUrl("https://appassets.androidplatform.net/cinema/index.html"))
        assertFalse(BundledCinema.isOwnUrl("http://appassets.androidplatform.net/cinema/index.html"))
        assertFalse(BundledCinema.isOwnUrl("https://twinhearts.vercel.app/cinema/"))
        assertFalse(BundledCinema.isOwnUrl("https://appassets.androidplatform.net.evil.com/"))
        assertFalse(BundledCinema.isOwnUrl("not a link"))
        assertFalse(BundledCinema.isOwnUrl(null))
    }

    @Test
    fun assetPath_mapsCinemaFiles_andRefusesEverythingElse() {
        assertEquals("cinema/index.html", BundledCinema.assetPath("/cinema/"))
        assertEquals("cinema/index.html", BundledCinema.assetPath("/cinema"))
        assertEquals("cinema/models/couple_f.glb", BundledCinema.assetPath("/cinema/models/couple_f.glb"))
        assertNull(BundledCinema.assetPath("/leave"))
        assertNull(BundledCinema.assetPath("/twin-movie"))
        assertNull(BundledCinema.assetPath("/cinema/../secret.txt"))
        assertNull(BundledCinema.assetPath("/cinema/a//b"))
        assertNull(BundledCinema.assetPath(null))
    }

    @Test
    fun mimeFor_coversTheCinemaFileTypes() {
        assertEquals("text/javascript", BundledCinema.mimeFor("cinema/assets/index-x.js"))
        assertEquals("model/gltf-binary", BundledCinema.mimeFor("cinema/models/a.glb"))
        assertEquals("audio/mpeg", BundledCinema.mimeFor("cinema/audio/a.MP3"))
        assertEquals("video/mp4", BundledCinema.mimeFor("cinema/ads/a.mp4"))
        assertEquals("application/octet-stream", BundledCinema.mimeFor("cinema/x.unknown"))
        assertTrue(BundledCinema.isText("text/css"))
        assertFalse(BundledCinema.isText("audio/mpeg"))
    }

    @Test
    fun parseRange_readsTheCommonShapes() {
        assertEquals(0L..99L, BundledCinema.parseRange("bytes=0-99", 1000))
        assertEquals(500L..999L, BundledCinema.parseRange("bytes=500-", 1000))
        assertEquals(900L..999L, BundledCinema.parseRange("bytes=-100", 1000))
        assertEquals(0L..999L, BundledCinema.parseRange("bytes=0-5000", 1000))
        assertNull(BundledCinema.parseRange(null, 1000))
        assertNull(BundledCinema.parseRange("bytes=-", 1000))
        assertNull(BundledCinema.parseRange("items=1-2", 1000))
        val past = BundledCinema.parseRange("bytes=2000-", 1000)!!
        assertTrue(past.first > past.last)
    }
}
