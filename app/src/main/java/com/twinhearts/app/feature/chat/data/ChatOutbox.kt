package com.twinhearts.app.feature.chat.data

import com.twinhearts.app.core.data.StateCache
import com.twinhearts.app.feature.chat.data.remote.SendMessageBody
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** One message that could not be sent yet because there was no connection. */
@Serializable
data class OutboxItem(
    val coupleId: String,
    val body: SendMessageBody,
    val queuedAtMillis: Long,
)

/**
 * Why this file exists: a message typed with no signal must not be lost, and must not need a second
 * tap. It waits here (saved on the phone, so it survives closing the app) until [ChatOutboxWorker]
 * sends it when the connection is back. The server saves by `clientId`, so if an attempt is repeated
 * the message still appears only once.
 *
 * Stored in the same small key/value table as the other offline copies, so no database change is
 * needed. It is cleared with that table when someone signs out.
 */
@Singleton
class ChatOutbox @Inject constructor(
    private val cache: StateCache,
    private val json: Json,
) {
    private val mutex = Mutex()
    private val serializer = ListSerializer(OutboxItem.serializer())

    suspend fun all(): List<OutboxItem> = mutex.withLock { load() }

    suspend fun add(item: OutboxItem) = mutex.withLock {
        val kept = load().filterNot { it.body.clientId == item.body.clientId }
        save((kept + item).takeLast(MAX_ITEMS))
    }

    suspend fun remove(clientId: String) = mutex.withLock {
        val current = load()
        val next = current.filterNot { it.body.clientId == clientId }
        if (next.size != current.size) save(next)
    }

    suspend fun contains(clientId: String): Boolean = all().any { it.body.clientId == clientId }

    private suspend fun load(): List<OutboxItem> =
        cache.read(KEY)?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() } ?: emptyList()

    private suspend fun save(items: List<OutboxItem>) {
        cache.write(KEY, json.encodeToString(serializer, items))
    }

    private companion object {
        const val KEY = "chat_outbox"
        const val MAX_ITEMS = 100
    }
}
