package com.twinhearts.app.feature.home

import com.twinhearts.app.feature.home.data.mapper.toCardPhotos
import com.twinhearts.app.feature.home.domain.model.CardPhoto
import com.twinhearts.app.feature.home.domain.model.CardPhotoSlot
import com.twinhearts.app.feature.home.domain.model.LoveKind
import com.twinhearts.app.feature.home.domain.model.toCardSlot
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardPhotoMapperTest {
    private fun json(text: String) = Json.parseToJsonElement(text)

    @Test
    fun noState_orNoDashImgs_givesNoPhotos() {
        assertTrue(json("null").toCardPhotos().isEmpty())
        assertTrue(json("""{"touch":{}}""").toCardPhotos().isEmpty())
    }

    @Test
    fun readsUrlAndPath_perSlot() {
        val photos = json(
            """{"dashImgs":{"hug":{"url":"https://x/h.jpg","path":"c1/1.jpg"},"miss":{"url":"https://x/m.jpg"}}}""",
        ).toCardPhotos()
        assertEquals(CardPhoto("https://x/h.jpg", "c1/1.jpg"), photos[CardPhotoSlot.Hug])
        assertEquals(CardPhoto("https://x/m.jpg", ""), photos[CardPhotoSlot.MissYou])
        assertEquals(2, photos.size)
    }

    @Test
    fun slotWithoutUrl_isIgnored() {
        val photos = json("""{"dashImgs":{"idle":{"url":"  ","path":"p"},"touch":"oops"}}""").toCardPhotos()
        assertTrue(photos.isEmpty())
    }

    @Test
    fun newestSignal_choosesThePicture() {
        assertEquals(CardPhotoSlot.Idle, null.toCardSlot())
        assertEquals(CardPhotoSlot.Hug, LoveKind.Hug.toCardSlot())
        assertEquals(CardPhotoSlot.Touch, LoveKind.Touch.toCardSlot())
        assertEquals(CardPhotoSlot.MissYou, LoveKind.MissYou.toCardSlot())
    }

    @Test
    fun slotKeys_matchTheWeb() {
        assertEquals(listOf("idle", "hug", "touch", "miss"), CardPhotoSlot.entries.map { it.key })
    }
}
