package com.twinhearts.app.feature.twinai.domain

/**
 * Why this file exists: Twin answers in light markdown (the web app renders headers, bold,
 * lists, code and quotes). This turns the text into blocks the UI can draw, with no Android code
 * so it can be unit tested.
 */
sealed interface MdBlock {
    data class Heading(val level: Int, val spans: List<MdSpan>) : MdBlock
    data class Paragraph(val spans: List<MdSpan>) : MdBlock
    data class Bullet(val spans: List<MdSpan>) : MdBlock
    data class Numbered(val number: Int, val spans: List<MdSpan>) : MdBlock
    data class Quote(val spans: List<MdSpan>) : MdBlock
    data class Code(val language: String, val code: String) : MdBlock
}

data class MdSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val code: Boolean = false,
    /** Web address for `[text](https://...)` links; null for normal text. */
    val link: String? = null,
)

object TwinAiMarkdown {

    fun parse(raw: String): List<MdBlock> {
        val blocks = mutableListOf<MdBlock>()
        val lines = raw.replace("\r\n", "\n").split("\n")
        var i = 0
        val para = StringBuilder()
        fun flushParagraph() {
            if (para.isNotBlank()) blocks += MdBlock.Paragraph(spans(para.toString().trim()))
            para.clear()
        }
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()
            when {
                trimmed.startsWith("```") -> {
                    flushParagraph()
                    val lang = trimmed.removePrefix("```").trim().ifEmpty { "text" }
                    val code = StringBuilder()
                    i++
                    // An unfinished block (the reply is still streaming) shows what has arrived so far.
                    while (i < lines.size && !lines[i].trim().startsWith("```")) {
                        if (code.isNotEmpty()) code.append('\n')
                        code.append(lines[i]); i++
                    }
                    blocks += MdBlock.Code(lang, code.toString())
                }
                trimmed.isEmpty() -> flushParagraph()
                trimmed.startsWith("### ") -> { flushParagraph(); blocks += MdBlock.Heading(3, spans(trimmed.drop(4))) }
                trimmed.startsWith("## ") -> { flushParagraph(); blocks += MdBlock.Heading(2, spans(trimmed.drop(3))) }
                trimmed.startsWith("# ") -> { flushParagraph(); blocks += MdBlock.Heading(1, spans(trimmed.drop(2))) }
                trimmed.startsWith("> ") -> { flushParagraph(); blocks += MdBlock.Quote(spans(trimmed.drop(2))) }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    flushParagraph(); blocks += MdBlock.Bullet(spans(trimmed.drop(2)))
                }
                NUMBERED.matches(trimmed) -> {
                    flushParagraph()
                    NUMBERED.matchEntire(trimmed)?.let { m ->
                        blocks += MdBlock.Numbered(m.groupValues[1].toIntOrNull() ?: 1, spans(m.groupValues[2]))
                    }
                }
                else -> {
                    if (para.isNotEmpty()) para.append('\n')
                    para.append(trimmed)
                }
            }
            i++
        }
        flushParagraph()
        return blocks
    }

    /** Inline **bold**, *italic*, `code` and [links](https://...). Unmatched markers stay as plain text. */
    fun spans(text: String): List<MdSpan> {
        val out = mutableListOf<MdSpan>()
        val plain = StringBuilder()
        fun flush() { if (plain.isNotEmpty()) { out += MdSpan(plain.toString()); plain.clear() } }
        var i = 0
        while (i < text.length) {
            val found = if (text[i] == '[') matchLink(text, i) else null
            when {
                found != null -> {
                    flush(); out += MdSpan(found.first, link = found.second); i = found.third
                }
                text.startsWith("**", i) && text.indexOf("**", i + 2) > i + 2 -> {
                    val end = text.indexOf("**", i + 2)
                    flush(); out += MdSpan(text.substring(i + 2, end), bold = true); i = end + 2
                }
                text[i] == '`' && text.indexOf('`', i + 1) > i + 1 -> {
                    val end = text.indexOf('`', i + 1)
                    flush(); out += MdSpan(text.substring(i + 1, end), code = true); i = end + 1
                }
                text[i] == '*' && !text.startsWith("**", i) && text.indexOf('*', i + 1) > i + 1 -> {
                    val end = text.indexOf('*', i + 1)
                    flush(); out += MdSpan(text.substring(i + 1, end), italic = true); i = end + 1
                }
                else -> { plain.append(text[i]); i++ }
            }
        }
        flush()
        return out
    }

    /** `[label](http...)` at [start]: (label, address, index after the closing bracket), or null. Only http(s) links count. */
    private fun matchLink(text: String, start: Int): Triple<String, String, Int>? {
        val close = text.indexOf("](", start + 1)
        if (close <= start + 1) return null
        val end = text.indexOf(')', close + 2)
        if (end < 0) return null
        val label = text.substring(start + 1, close)
        val url = text.substring(close + 2, end).trim()
        if (label.contains('\n') || !(url.startsWith("https://") || url.startsWith("http://"))) return null
        return Triple(label, url, end + 1)
    }

    private val NUMBERED = Regex("""^(\d+)\. (.*)$""")
}
