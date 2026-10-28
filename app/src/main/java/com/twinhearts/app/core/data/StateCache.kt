package com.twinhearts.app.core.data

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers the last good copy of a page's data so it can show instantly and work offline.
 * Callers store only their own small slice (never the whole shared state, which can hold
 * large photos) under a key like "calendar:<coupleId>". Failures are ignored: a cache is a bonus.
 */
@Singleton
class StateCache @Inject constructor(
    private val dao: CacheDao,
) {
    suspend fun read(key: String): String? = runCatching { dao.get(key)?.json }.getOrNull()

    suspend fun write(key: String, json: String) {
        runCatching { dao.put(CacheEntryEntity(key, json, System.currentTimeMillis())) }
    }
}
