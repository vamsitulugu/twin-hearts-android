package com.twinhearts.app.feature.profile.highlights.data.mapper

import com.twinhearts.app.feature.profile.data.mapper.parseIsoMs
import com.twinhearts.app.feature.profile.highlights.data.remote.HighlightDto
import com.twinhearts.app.feature.profile.highlights.data.remote.HighlightItemDto
import com.twinhearts.app.feature.profile.highlights.domain.ClipKind
import com.twinhearts.app.feature.profile.highlights.domain.Highlight
import com.twinhearts.app.feature.profile.highlights.domain.HighlightCategory
import com.twinhearts.app.feature.profile.highlights.domain.HighlightClip

/** A clip we cannot show (unknown type or no address) is dropped instead of breaking the row. */
fun HighlightItemDto.toClip(): HighlightClip? {
    val kind = when (mediaType) {
        "video" -> ClipKind.Video
        "image" -> ClipKind.Image
        else -> return null
    }
    if (mediaUrl.isBlank()) return null
    return HighlightClip(id, kind, mediaUrl, parseIsoMs(createdAt))
}

/** A highlight without a showable clip has nothing to put in its circle, so it is skipped (null). */
fun HighlightDto.toHighlight(myRole: String): Highlight? {
    val clips = items.mapNotNull { it.toClip() }
    if (clips.isEmpty()) return null
    return Highlight(id, title.ifBlank { DEFAULT_TITLE }, HighlightCategory.fromWire(category), userId == myRole, clips)
}

/** My highlights first, then my partner's; inside each the server's order (newest change first) is kept. */
fun List<HighlightDto>.toHighlights(myRole: String): List<Highlight> =
    mapNotNull { it.toHighlight(myRole) }.sortedBy { !it.mine }

private const val DEFAULT_TITLE = "Highlights"
