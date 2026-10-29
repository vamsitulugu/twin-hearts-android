package com.twinhearts.app.feature.memories.domain.model

import java.time.LocalDate
import java.time.YearMonth

/**
 * One photo or video in the shared album (web: `S.photos` item).
 * [path] is where the file sits in the server's storage; it is what lets a delete remove the file
 * too (old items saved before storage uploads have none).
 */
data class Memory(
    val id: String,
    val url: String,
    val name: String,
    val isVideo: Boolean,
    /** Null when the saved date cannot be read. */
    val date: LocalDate?,
    val path: String?,
    val favorite: Boolean,
)

/** A file that was sent to storage and is ready to be added to the album. */
data class UploadedMedia(val url: String, val path: String?, val name: String, val isVideo: Boolean)

/** Photos added per month, for the "Memories Added by Month" bars. */
data class MonthCount(val month: YearMonth, val count: Int)

/** The one-line tip under the camera card (web `renderMemoryAnalytics`). */
sealed interface MemoryInsight {
    data object AddFirst : MemoryInsight
    data class ThisMonth(val thisMonth: Int, val total: Int, val videos: Int) : MemoryInsight
    data class SoFar(val total: Int) : MemoryInsight
}
