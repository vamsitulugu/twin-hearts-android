package com.twinhearts.app.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The push `type` keys are a contract with the server: they must stay unique and round-trip. */
class NotificationTypeTest {

    @Test
    fun keys_are_unique() {
        val keys = NotificationType.entries.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun every_key_round_trips() {
        NotificationType.entries.forEach { assertEquals(it, NotificationType.fromKey(it.key)) }
    }

    @Test
    fun unknown_key_is_null() {
        assertNull(NotificationType.fromKey("nope"))
        assertNull(NotificationType.fromKey(null))
    }

    @Test
    fun calls_and_sos_ignore_quiet_hours() {
        assertTrue(NotificationType.CALL_INCOMING.breaksQuietHours)
        assertTrue(NotificationType.LOCATION_SOS.breaksQuietHours)
    }

    @Test
    fun server_tags_map_to_types() {
        assertEquals(NotificationType.CHAT_MESSAGE, NotificationType.fromServerTag("chat-msg"))
        assertEquals(NotificationType.CALL_INCOMING, NotificationType.fromServerTag("incoming-call"))
        assertEquals(NotificationType.PARTNER_HUG, NotificationType.fromServerTag("hug"))
        assertEquals(NotificationType.PARTNER_MISS, NotificationType.fromServerTag("missyou"))
        assertEquals(NotificationType.MEMORY_ADDED, NotificationType.fromServerTag("globe-media"))
        assertEquals(NotificationType.SYSTEM, NotificationType.fromServerTag("something-new"))
        assertEquals(NotificationType.SYSTEM, NotificationType.fromServerTag(null))
    }
}
