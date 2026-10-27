package com.twinhearts.app.feature.twinai

import com.twinhearts.app.feature.twinai.domain.MdBlock
import com.twinhearts.app.feature.twinai.domain.TwinAiMarkdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TwinAiMarkdownTest {
    @Test
    fun parsesHeadingsListsAndParagraphs() {
        val blocks = TwinAiMarkdown.parse("## Ideas\n- Picnic\n- Movie\n\n1. First\n\nHello **you**")
        assertTrue(blocks[0] is MdBlock.Heading)
        assertTrue(blocks[1] is MdBlock.Bullet)
        assertTrue(blocks[2] is MdBlock.Bullet)
        assertEquals(1, (blocks[3] as MdBlock.Numbered).number)
        assertTrue(blocks[4] is MdBlock.Paragraph)
    }

    @Test
    fun inlineStyles() {
        val spans = TwinAiMarkdown.spans("a **b** *c* `d`")
        assertEquals(listOf("a ", "b", " ", "c", " ", "d"), spans.map { it.text })
        assertTrue(spans[1].bold)
        assertTrue(spans[3].italic)
        assertTrue(spans[5].code)
    }

    @Test
    fun unfinishedCodeBlockStillShowsWhatArrived() {
        val block = TwinAiMarkdown.parse("```kotlin\nval x = 1").single() as MdBlock.Code
        assertEquals("kotlin", block.language)
        assertEquals("val x = 1", block.code)
    }

    @Test
    fun lonelyMarkersStayPlain() {
        assertEquals("2 * 3", TwinAiMarkdown.spans("2 * 3").joinToString("") { it.text })
    }

    @Test
    fun markdownLinkKeepsLabelAndAddress() {
        val spans = TwinAiMarkdown.spans("see [the guide](https://example.com/a) now")
        assertEquals(listOf("see ", "the guide", " now"), spans.map { it.text })
        assertEquals("https://example.com/a", spans[1].link)
    }

    @Test
    fun onlyWebLinksAreLinks() {
        val spans = TwinAiMarkdown.spans("[x](javascript:alert(1))")
        assertTrue(spans.all { it.link == null })
        assertEquals("[x](javascript:alert(1))", spans.joinToString("") { it.text })
    }
}
