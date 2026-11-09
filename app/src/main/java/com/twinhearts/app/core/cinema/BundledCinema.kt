package com.twinhearts.app.core.cinema

import java.net.URI

/**
 * Why this file exists: the Twin Cinema 3D theatre now lives INSIDE the app (app/src/main/assets/cinema/, a copy of the
 * web app's built /cinema/ folder), so there is no separate website to load. All the small rules about that are here in
 * plain Kotlin (no Android classes) so they can be unit tested:
 *  - the private address the theatre is shown at,
 *  - turning the link the server gives us (https://twinhearts.vercel.app/cinema/#s=...) into that address,
 *  - which asset file a request means (and refusing anything that tries to leave the cinema folder),
 *  - reading a "Range: bytes=a-b" header (movies and ads need it for seeking).
 *
 * The address uses a virtual host. Nothing is ever downloaded from it: [BundledCinemaServer] answers every request
 * for it from the APK. It is the same https origin for every file, so ES modules, fetch() and audio work normally.
 */
object BundledCinema {
    /** Virtual host answered by the app itself. Same name the Watch Together theatre already uses for its movie stream. */
    const val HOST = "appassets.androidplatform.net"

    /** Folder inside app/src/main/assets/ that holds the cinema build. */
    const val ASSET_DIR = "cinema"

    /** The theatre page, without the room settings. */
    const val ENTRY = "https://$HOST/$ASSET_DIR/index.html"

    /**
     * Takes the link the server made for this person and keeps only its `#` part (room, avatar, names, host flag...).
     * Returns null when there is no `#` part: such a link has no room, so it is never opened.
     */
    fun localUrl(serverUrl: String?): String? {
        val hash = serverUrl?.substringAfter('#', "")?.takeIf { it.isNotBlank() } ?: return null
        return "$ENTRY#$hash"
    }

    /** True when [url] is on the app's own virtual host. The only pages the cinema screen may show. */
    fun isOwnUrl(url: String?): Boolean {
        val uri = try {
            url?.let { URI(it) }
        } catch (ignored: Exception) {
            null // a link that cannot be read is treated as "not allowed"
        } ?: return false
        return uri.scheme.equals("https", ignoreCase = true) && uri.host.equals(HOST, ignoreCase = true)
    }

    /**
     * Maps a request path to the asset file it means, or null when it is not a cinema file.
     * "/cinema/" and "/cinema" mean the theatre page. Any ".." (or an encoded one) is refused.
     */
    fun assetPath(urlPath: String?): String? {
        val path = urlPath ?: return null
        if (path == "/$ASSET_DIR" || path == "/$ASSET_DIR/") return "$ASSET_DIR/index.html"
        if (!path.startsWith("/$ASSET_DIR/")) return null
        val clean = path.removePrefix("/")
        if (clean.contains("..") || clean.contains('\\') || clean.contains("//")) return null
        return clean
    }

    /** Content type for the file types the cinema ships. Unknown types are plain bytes (the cinema loads them by code). */
    fun mimeFor(path: String): String = when (path.substringAfterLast('.', "").lowercase()) {
        "html" -> "text/html"
        "js", "mjs" -> "text/javascript"
        "css" -> "text/css"
        "json" -> "application/json"
        "webmanifest" -> "application/manifest+json"
        "glb" -> "model/gltf-binary"
        "gltf" -> "model/gltf+json"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "ogg" -> "audio/ogg"
        "m4a" -> "audio/mp4"
        "mp4" -> "video/mp4"
        "webm" -> "video/webm"
        "webp" -> "image/webp"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "svg" -> "image/svg+xml"
        "ico" -> "image/x-icon"
        "txt" -> "text/plain"
        else -> "application/octet-stream"
    }

    /** True for types that are read as text (they get a charset). */
    fun isText(mime: String): Boolean =
        mime.startsWith("text/") || mime == "application/json" || mime == "application/manifest+json" || mime == "image/svg+xml"

    /**
     * Reads a "Range: bytes=a-b" header for a file of [total] bytes. Returns the first and last byte to send,
     * or null when there is no usable range (send the whole file). A start beyond the end returns an empty range (start > end).
     */
    fun parseRange(header: String?, total: Long): LongRange? {
        val m = header?.let { Regex("""bytes=(\d*)-(\d*)""").matchEntire(it.trim()) } ?: return null
        val a = m.groupValues[1]
        val b = m.groupValues[2]
        if (a.isEmpty() && b.isEmpty()) return null
        val start: Long
        var end = total - 1
        if (a.isNotEmpty()) {
            start = a.toLong()
            if (b.isNotEmpty()) end = minOf(b.toLong(), total - 1)
        } else {
            start = maxOf(0L, total - b.toLong()) // "bytes=-500": the last 500 bytes
        }
        return start..end
    }
}
