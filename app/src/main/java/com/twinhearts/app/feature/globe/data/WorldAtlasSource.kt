package com.twinhearts.app.feature.globe.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Why this file exists: loads the bundled world map once and keeps it for the next visit to the Globe. */
@Singleton
class WorldAtlasSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val lock = Mutex()
    private var cached: WorldAtlas? = null

    /** The map, or null when the file is missing or damaged (the globe then shows only its pins). */
    suspend fun atlas(): WorldAtlas? = lock.withLock {
        cached ?: withContext(Dispatchers.IO) {
            runCatching { context.assets.open(ASSET).use { WorldAtlas.parse(it) } }.getOrNull()
        }?.also { cached = it }
    }

    private companion object {
        const val ASSET = "globe_countries.bin"
    }
}
