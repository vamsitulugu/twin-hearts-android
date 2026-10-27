package com.twinhearts.app.feature.movie.domain

import java.util.Locale

/** Why this file exists: the time and size text shown in the lobby, player and cloud list. */

/** 3725 s -> "1:02:05", 125 s -> "2:05". */
fun formatClock(totalSec: Double): String {
    val s = totalSec.toLong().coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, sec) else String.format(Locale.US, "%d:%02d", m, sec)
}

/** 6720 s -> "1h 52m". */
fun formatRuntime(totalSec: Double): String {
    val minutes = (totalSec / 60).toLong().coerceAtLeast(0)
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

/** 1_500_000_000 -> "1.4 GB". */
fun formatBytes(bytes: Long): String {
    val mb = bytes / BYTES_PER_MB
    return if (mb >= 1024) String.format(Locale.US, "%.1f GB", mb / 1024.0) else String.format(Locale.US, "%.0f MB", mb)
}

private const val BYTES_PER_MB = 1_048_576.0
