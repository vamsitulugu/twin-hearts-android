package com.twinhearts.app.feature.movie.ui.components

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi

/**
 * Why this file exists: the lobby's "Subtitles" tile and the player's ⋮ menu both pick a subtitle
 * file, so the choice and the reader live here instead of inside the player.
 */

/** A subtitle file the person chose on this phone. It is never uploaded; each phone loads its own. */
data class SubtitleChoice(val uri: Uri, val name: String, val mime: String)

/** Language tag used so the .srt/.vtt file shows even when the movie has no language of its own. */
internal const val SUBTITLE_LANGUAGE = "en"

/** Media3 reads .srt and .vtt directly (the web page has to convert .srt first). Anything else is refused. */
@OptIn(UnstableApi::class)
internal fun readSubtitle(context: Context, uri: Uri): SubtitleChoice? {
    val name = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull() ?: return null
    val lower = name.lowercase()
    val mime = when {
        lower.endsWith(".srt") -> MimeTypes.APPLICATION_SUBRIP
        lower.endsWith(".vtt") -> MimeTypes.TEXT_VTT
        else -> return null
    }
    return SubtitleChoice(uri, name, mime)
}
