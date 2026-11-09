package com.twinhearts.app.feature.cinema

import com.twinhearts.app.feature.cinema.domain.CinemaLinks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CinemaLinksTest {
    private val cinema = "https://appassets.androidplatform.net/cinema/index.html#s=abc&r=1&h=1"

    @Test
    fun avatarSlot_followsGender_thenRole() {
        assertEquals(1, CinemaLinks.avatarSlot("female", "user2"))
        assertEquals(2, CinemaLinks.avatarSlot("male", "user1"))
        assertEquals(1, CinemaLinks.avatarSlot(null, "user1"))
        assertEquals(2, CinemaLinks.avatarSlot(null, "user2"))
    }

    @Test
    fun withBackLink_addsEncodedBackLinkToTheHashPart() {
        val url = CinemaLinks.withBackLink(cinema)
        assertEquals("$cinema&b=https%3A%2F%2Ftwinhearts.invalid%2F", url)
    }

    @Test
    fun withBackLink_startsAHashWhenThereIsNone() {
        assertTrue(CinemaLinks.withBackLink("https://appassets.androidplatform.net/cinema/index.html").contains("/#b="))
    }

    @Test
    fun isBackLink_onlyForTheBackAddress() {
        assertTrue(CinemaLinks.isBackLink(CinemaLinks.BACK_LINK))
        assertTrue(CinemaLinks.isBackLink("https://TwinHearts.invalid/anything"))
        assertFalse(CinemaLinks.isBackLink("https://appassets.androidplatform.net/cinema/index.html"))
        assertFalse(CinemaLinks.isBackLink(null))
        assertFalse(CinemaLinks.isBackLink("not a link"))
    }
}
