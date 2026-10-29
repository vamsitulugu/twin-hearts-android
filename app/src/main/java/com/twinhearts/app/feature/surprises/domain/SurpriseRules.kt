package com.twinhearts.app.feature.surprises.domain

import com.twinhearts.app.feature.surprises.domain.model.Capsule
import com.twinhearts.app.feature.surprises.domain.model.MediaKind
import com.twinhearts.app.feature.surprises.domain.model.PickedMedia
import com.twinhearts.app.feature.surprises.domain.model.Surprise
import java.time.LocalDate

/**
 * Why this file exists: the rules of the page in one place, with no Android code, so they are
 * easy to read and to test. They copy the web: a surprise is locked while today is before its
 * unlock date, and the partner never sees the content of a locked surprise.
 */

const val MAX_VIDEO_MB = 50
const val MAX_AUDIO_MB = 15 // the server refuses voice notes above 15 MB (routes/media.js)
private const val BYTES_PER_MB = 1024L * 1024L

/** Same test as the web's `todayStr() < unlock`: on the unlock day itself it is open. */
fun isLocked(unlock: LocalDate, today: LocalDate): Boolean = today.isBefore(unlock)

/** What a surprise card shows, depending on who is looking and the date. */
enum class SurpriseView {
    /** My partner hid it and it is still locked: only "a surprise is waiting". */
    PartnerLocked,

    /** I hid it and it is still locked: I see the title and the unlock date. */
    MineLocked,

    /** Open: title, message and file are shown. */
    Unlocked,
}

fun viewOf(surprise: Surprise, today: LocalDate): SurpriseView = when {
    !isLocked(surprise.unlock, today) -> SurpriseView.Unlocked
    surprise.mine -> SurpriseView.MineLocked
    else -> SurpriseView.PartnerLocked
}

/** Items are saved with `Date.now()` as id, so the biggest id is the newest. Unreadable ids sink to the end. */
fun newestFirstSurprises(items: List<Surprise>): List<Surprise> =
    items.sortedByDescending { it.id.toLongOrNull() ?: Long.MIN_VALUE }

fun newestFirstCapsules(items: List<Capsule>): List<Capsule> =
    items.sortedByDescending { it.id.toLongOrNull() ?: Long.MIN_VALUE }

private val VIDEO_EXTENSIONS = setOf("mp4", "mov", "m4v", "3gp", "mkv")
private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "oga", "opus", "amr")
private const val MIC_EMOJI = "🎙"
private const val FILM_EMOJI = "🎬"

/**
 * Works out what an attached link is. The web saves a `data:` link (mime inside), this app saves a
 * normal link plus a `mediaKind` hint. For old links without a hint the file ending decides, and the
 * type emoji settles the rare `.webm` (video or audio). Everything else is treated as a picture.
 */
fun mediaKindOf(url: String, hint: String?, typeEmoji: String): MediaKind? {
    if (url.isBlank()) return null
    MediaKind.entries.firstOrNull { it.key == hint }?.let { return it }
    if (url.startsWith("data:", ignoreCase = true)) {
        val mime = url.substring(5).substringBefore(',').substringBefore(';').lowercase()
        return when {
            mime.startsWith("video/") -> MediaKind.Video
            mime.startsWith("audio/") -> MediaKind.Audio
            else -> MediaKind.Image
        }
    }
    val extension = url.substringBefore('#').substringBefore('?').substringAfterLast('.', "").lowercase()
    return when {
        extension == "webm" -> if (typeEmoji.startsWith(MIC_EMOJI)) MediaKind.Audio else MediaKind.Video
        extension in VIDEO_EXTENSIONS -> MediaKind.Video
        extension in AUDIO_EXTENSIONS -> MediaKind.Audio
        extension.length > 5 || extension.isEmpty() -> when {
            typeEmoji.startsWith(FILM_EMOJI) -> MediaKind.Video
            typeEmoji.startsWith(MIC_EMOJI) -> MediaKind.Audio
            else -> MediaKind.Image
        }
        else -> MediaKind.Image
    }
}

/** The file types the server accepts for videos (routes/media.js, ALLOWED_MEDIA_MIME). */
private val VIDEO_MIMES = setOf("video/mp4", "video/quicktime", "video/webm", "video/3gpp", "video/x-matroska")

/** Null when the server would refuse this file type. */
fun classifyMime(mime: String): MediaKind? {
    val clean = mime.substringBefore(';').trim().lowercase()
    return when {
        clean.startsWith("image/") -> MediaKind.Image
        clean in VIDEO_MIMES -> MediaKind.Video
        clean.startsWith("audio/") -> MediaKind.Audio
        else -> null
    }
}

/** Why a picked file was turned away. The screen shows a message for each. */
enum class MediaProblem { Unsupported, TooBig }

/** Checks a picked file before it is attached, so the person hears about a problem right away, not after a long upload. */
fun checkPickedMedia(media: PickedMedia): MediaProblem? {
    val limitMb = when (classifyMime(media.mime)) {
        null -> return MediaProblem.Unsupported
        MediaKind.Image -> return null // photos are shrunk before they are sent
        MediaKind.Video -> MAX_VIDEO_MB
        MediaKind.Audio -> MAX_AUDIO_MB
    }
    return if (media.sizeBytes > limitMb * BYTES_PER_MB) MediaProblem.TooBig else null
}
