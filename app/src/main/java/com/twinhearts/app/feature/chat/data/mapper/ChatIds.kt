package com.twinhearts.app.feature.chat.data.mapper

import java.util.concurrent.ConcurrentHashMap

/**
 * Why this file exists: the `chat_messages` table uses text (UUID) ids, but the screens use a
 * number per message. [idOf] makes that number from the server's id and remembers the original,
 * and [raw] gives the original back for every call that sends a message id to the server
 * (reply, edit, delete, star, react, pin, view-once). Sending the number instead made the
 * server answer "invalid input syntax for type uuid" (HTTP 500).
 */
object ChatIds {
    private val originals = ConcurrentHashMap<Long, String>()

    /** The number the screens use for the server id [text]. Plain numbers stay as they are. */
    fun idOf(text: String): Long {
        val number = text.toLongOrNull() ?: (text.hashCode().toLong() and 0x7FFFFFFFL).coerceAtLeast(1L)
        originals[number] = text
        return number
    }

    /** The id the server knows this message by. */
    fun raw(id: Long): String = originals[id] ?: id.toString()
}
