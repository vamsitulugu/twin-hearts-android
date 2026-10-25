package com.twinhearts.app.feature.pairing.domain

import java.time.OffsetDateTime

/** Minutes between [createdAt] (an ISO date from the server) and [nowMillis]. Null if it cannot be read. */
fun elapsedMinutes(createdAt: String?, nowMillis: Long): Long? {
    if (createdAt.isNullOrBlank()) return null
    val created = try {
        OffsetDateTime.parse(createdAt).toInstant().toEpochMilli()
    } catch (e: java.time.format.DateTimeParseException) {
        return null
    }
    return (nowMillis - created).coerceAtLeast(0L) / 60_000L
}
