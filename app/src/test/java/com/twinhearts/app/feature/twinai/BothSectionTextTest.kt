package com.twinhearts.app.feature.twinai

import com.twinhearts.app.feature.twinai.data.mapper.asReadableText
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/** Why this file exists: an odd AI answer (a list instead of text) must show, never break the round. */
class BothSectionTextTest {
    private fun read(json: String) = Json.parseToJsonElement(json).asReadableText()

    @Test fun text_staysText() = assertEquals("Hello", read("\"Hello\""))
    @Test fun list_becomesLines() = assertEquals("one\ntwo", read("[\"one\",\"two\"]"))
    @Test fun number_becomesText() = assertEquals("5", read("5"))
    @Test fun null_isEmpty() = assertEquals("", null.asReadableText())
}
