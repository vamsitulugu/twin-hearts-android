package com.twinhearts.app.feature.chat.domain

import java.net.URI

/**
 * Why this file exists: the small text rules of the web chat in one pure, testable place:
 * WhatsApp-style formatting (*bold*, _italic_, ~strike~, ```mono```, ||spoiler||), links, file sizes.
 */

/** One run of text with the same look. [link] is set for a tappable web address. */
data class RichPiece(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val strike: Boolean = false,
    val mono: Boolean = false,
    val spoiler: Boolean = false,
    val link: String? = null,
)

private val LINK = Regex("""https?://[^\s]+""")
private val MONO = Regex("""```([^`\n]+?)```""")
private val SPOILER = Regex("""\|\|([^|\n]+?)\|\|""")
private val BOLD = Regex("""(^|[\s(])\*([^*\n]+?)\*(?=[\s).,!?]|$)""")
private val ITALIC = Regex("""(^|[\s(])_([^_\n]+?)_(?=[\s).,!?]|$)""")
private val STRIKE = Regex("""(^|[\s(])~([^~\n]+?)~(?=[\s).,!?]|$)""")

/** First web address in [text], or null. */
fun firstLink(text: String?): String? = text?.let { LINK.find(it)?.value }

/** "example.com" for "https://www.example.com/a", or null when the address is not valid. */
fun linkHost(url: String): String? = try {
    URI(url).host?.removePrefix("www.")
} catch (e: java.net.URISyntaxException) {
    null
}

/**
 * Splits [input] into styled pieces. Markers must sit next to whitespace or punctuation, so
 * "5*3=15" or a file name with underscores is left alone (same rule as the web).
 */
fun parseRichText(input: String): List<RichPiece> {
    if (input.isEmpty()) return emptyList()
    var pieces = listOf(RichPiece(input))
    pieces = pieces.flatMap { splitOn(it, MONO, group = 1) { p -> p.copy(mono = true) } }
    pieces = pieces.flatMap { splitOn(it, SPOILER, group = 1) { p -> p.copy(spoiler = true) } }
    pieces = pieces.flatMap { splitFlanked(it, BOLD) { p -> p.copy(bold = true) } }
    pieces = pieces.flatMap { splitFlanked(it, ITALIC) { p -> p.copy(italic = true) } }
    pieces = pieces.flatMap { splitFlanked(it, STRIKE) { p -> p.copy(strike = true) } }
    pieces = pieces.flatMap { splitLinks(it) }
    return pieces.filter { it.text.isNotEmpty() }
}

private fun splitOn(piece: RichPiece, regex: Regex, group: Int, style: (RichPiece) -> RichPiece): List<RichPiece> {
    if (piece.mono) return listOf(piece)
    val out = mutableListOf<RichPiece>()
    var last = 0
    regex.findAll(piece.text).forEach { m ->
        if (m.range.first > last) out += piece.copy(text = piece.text.substring(last, m.range.first))
        out += style(piece.copy(text = m.groupValues[group]))
        last = m.range.last + 1
    }
    if (last < piece.text.length) out += piece.copy(text = piece.text.substring(last))
    return out
}

/** Like [splitOn], but group 1 is the character before the marker and must stay in the text. */
private fun splitFlanked(piece: RichPiece, regex: Regex, style: (RichPiece) -> RichPiece): List<RichPiece> {
    if (piece.mono) return listOf(piece)
    val out = mutableListOf<RichPiece>()
    var last = 0
    regex.findAll(piece.text).forEach { m ->
        val lead = m.groupValues[1]
        val markerStart = m.range.first + lead.length
        if (markerStart > last || lead.isNotEmpty()) out += piece.copy(text = piece.text.substring(last, markerStart))
        out += style(piece.copy(text = m.groupValues[2]))
        last = m.range.last + 1
    }
    if (last < piece.text.length) out += piece.copy(text = piece.text.substring(last))
    return out
}

private fun splitLinks(piece: RichPiece): List<RichPiece> {
    if (piece.mono || piece.spoiler) return listOf(piece)
    val out = mutableListOf<RichPiece>()
    var last = 0
    LINK.findAll(piece.text).forEach { m ->
        if (m.range.first > last) out += piece.copy(text = piece.text.substring(last, m.range.first))
        out += piece.copy(text = m.value, link = m.value)
        last = m.range.last + 1
    }
    if (last < piece.text.length) out += piece.copy(text = piece.text.substring(last))
    return out
}

/** "832 KB", "1.4 MB", or "" when the size is unknown. */
fun formatFileSize(bytes: Long): String = when {
    bytes <= 0L -> ""
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
    else -> "${"%.1f".format(java.util.Locale.ENGLISH, bytes / (1024.0 * 1024.0))} MB"
}

/** Icon for a document, picked by its extension (same set as the web). */
fun fileEmoji(name: String?): String = when (name?.substringAfterLast('.', "")?.lowercase()) {
    "pdf" -> "📕"
    "doc", "docx" -> "📘"
    "xls", "xlsx" -> "📗"
    "ppt", "pptx" -> "📙"
    "zip" -> "🗜️"
    "txt", "rtf" -> "📄"
    "csv" -> "📊"
    "json" -> "🧾"
    else -> "📎"
}

/** The upper-case extension shown under a file name ("PDF"), or "" when there is none. */
fun fileExtension(name: String?): String = name?.substringAfterLast('.', "")?.uppercase().orEmpty()

/** Disappearing-messages label: 0 = "Off", then "30m", "5h", "7d" like the web. */
sealed interface DisappearAmount {
    data object Off : DisappearAmount
    data class Minutes(val value: Int) : DisappearAmount
    data class Hours(val value: Int) : DisappearAmount
    data class Days(val value: Int) : DisappearAmount
}

fun disappearAmount(seconds: Int): DisappearAmount = when {
    seconds <= 0 -> DisappearAmount.Off
    seconds < 3600 -> DisappearAmount.Minutes((seconds + 30) / 60)
    seconds < 86_400 -> DisappearAmount.Hours((seconds + 1800) / 3600)
    else -> DisappearAmount.Days((seconds + 43_200) / 86_400)
}

/** Is the do-not-disturb window ([start], [end] as "HH:mm") active at [minuteOfDay]? Handles overnight windows. */
fun isDndActive(start: String?, end: String?, minuteOfDay: Int): Boolean {
    val s = parseClock(start) ?: return false
    val e = parseClock(end) ?: return false
    return if (s <= e) minuteOfDay in s until e else minuteOfDay >= s || minuteOfDay < e
}

/** "22:30" to minutes since midnight, or null when it is not a valid time. */
fun parseClock(text: String?): Int? {
    val parts = text?.trim()?.split(':') ?: return null
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    return if (h in 0..23 && m in 0..59) h * 60 + m else null
}

/** Where a message sits inside a run of messages from the same person on the same day. */
enum class GroupPosition { Single, First, Middle, Last }
