package com.twinhearts.app.feature.meetplanner.domain

/**
 * Why this file exists: the server has no columns for "end date" and "trip type", so the web app
 * hides them as one header line at the top of the notes: `[[mp:{"to":"2026-11-07","type":"weekend"}]]`.
 * We write and read the same line, so plans made on the web and on the phone open on both.
 */
data class NotesMeta(val to: String? = null, val type: String? = null)

data class ParsedNotes(val meta: NotesMeta, val text: String)

private val HEADER = Regex("^\\[\\[mp:(\\{.*?\\})\\]\\]\\n?")
private val PAIR = Regex("\"(\\w+)\"\\s*:\\s*\"([^\"]*)\"")

object NotesCodec {
    /** Splits stored notes into the hidden meta and the text the couple wrote. */
    fun parse(raw: String?): ParsedNotes {
        val source = raw.orEmpty()
        val match = HEADER.find(source) ?: return ParsedNotes(NotesMeta(), source)
        val pairs = PAIR.findAll(match.groupValues[1]).associate { it.groupValues[1] to it.groupValues[2] }
        return ParsedNotes(
            meta = NotesMeta(to = pairs["to"], type = pairs["type"]),
            text = source.removeRange(match.range),
        )
    }

    /** Builds the value to store. Null when there is neither meta nor text. */
    fun build(text: String, meta: NotesMeta): String? {
        val parts = listOfNotNull(
            meta.to?.takeIf { it.isNotEmpty() }?.let { "\"to\":\"$it\"" },
            meta.type?.takeIf { it.isNotEmpty() }?.let { "\"type\":\"$it\"" },
        )
        val head = if (parts.isEmpty()) "" else "[[mp:{${parts.joinToString(",")}}]]\n"
        return (head + text.trim()).trim().ifEmpty { null }
    }
}
