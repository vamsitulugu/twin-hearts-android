package com.twinhearts.app.feature.cinema3d.data

import android.content.Context
import com.twinhearts.app.feature.cinema3d.domain.TheatreLayout
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The three files the 3D theatre needs. The two model buffers are DIRECT buffers because Filament reads them natively.
 * Hand each one to Filament as `buffer.duplicate()` so the original position is never moved.
 */
class CinemaFiles(
    val hall: ByteBuffer,
    val lobby: ByteBuffer,
    val layout: TheatreLayout,
)

/** Reads `assets/cinema3d/*` off the main thread. */
@Singleton
class CinemaAssets @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(): CinemaFiles = withContext(Dispatchers.IO) {
        CinemaFiles(
            hall = readDirect(HALL_FILE),
            lobby = readDirect(LOBBY_FILE),
            layout = json.decodeFromString(TheatreLayout.serializer(), readText(LAYOUT_FILE)),
        )
    }

    private fun readDirect(name: String): ByteBuffer {
        val bytes = context.assets.open("$DIR/$name").use { it.readBytes() }
        val buffer = ByteBuffer.allocateDirect(bytes.size)
        buffer.put(bytes)
        buffer.flip()
        return buffer
    }

    private fun readText(name: String): String =
        context.assets.open("$DIR/$name").use { it.readBytes().toString(Charsets.UTF_8) }

    private companion object {
        const val DIR = "cinema3d"
        const val HALL_FILE = "theatre_hall.glb"
        const val LOBBY_FILE = "theatre_lobby.glb"
        const val LAYOUT_FILE = "theatre_layout.json"
    }
}
