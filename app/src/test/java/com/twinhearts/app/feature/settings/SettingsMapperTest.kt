package com.twinhearts.app.feature.settings

import com.twinhearts.app.feature.auth.testSession
import com.twinhearts.app.feature.settings.data.mapper.backupPatch
import com.twinhearts.app.feature.settings.data.mapper.imageUrls
import com.twinhearts.app.feature.settings.data.mapper.parseInstant
import com.twinhearts.app.feature.settings.data.mapper.profilePatch
import com.twinhearts.app.feature.settings.data.mapper.toDomain
import com.twinhearts.app.feature.settings.data.mapper.toProfileInfo
import com.twinhearts.app.feature.settings.data.mapper.toStorageSummary
import com.twinhearts.app.feature.settings.data.remote.StatusDto
import com.twinhearts.app.feature.settings.domain.model.PartnerStatus
import com.twinhearts.app.feature.settings.domain.model.ProfileEdit
import com.twinhearts.app.feature.settings.domain.model.ProfileGender
import com.twinhearts.app.feature.settings.domain.model.StorageKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class SettingsMapperTest {
    private fun json(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    @Test
    fun profileInfo_prefers_my_profile_entry_then_top_level_keys_then_session() {
        val state = json(
            """{"anniversary":"2025-10-12","myBday":"1990-01-01","ptBday":"1991-02-02",
               "profile":{"u1":{"name":"Vamsi V","bday":"1999-03-04","gender":"male","avatar":"a.jpg"},
                          "u2":{"name":"Likitha"}}}""",
        )
        val info = state.toProfileInfo(testSession(), myAvatar = null, partnerAvatar = "p.jpg")
        assertEquals("Vamsi V", info.myName)
        assertEquals("Likitha", info.partnerName)
        assertEquals(ProfileGender.Male, info.gender)
        assertEquals(LocalDate.of(2025, 10, 12), info.anniversary)
        assertEquals(LocalDate.of(1999, 3, 4), info.myBirthday) // profile entry beats top-level myBday
        assertEquals(LocalDate.of(1991, 2, 2), info.partnerBirthday) // falls back to ptBday
        assertEquals("a.jpg", info.myAvatarUrl)
        assertEquals("p.jpg", info.partnerAvatarUrl)
    }

    @Test
    fun profileInfo_on_an_empty_state_uses_the_saved_session() {
        val info = JsonObject(emptyMap()).toProfileInfo(testSession(), null, null)
        assertEquals("Vamsi", info.myName)
        assertEquals("Partner", info.partnerName)
        assertNull(info.anniversary)
        assertNull(info.myAvatarUrl)
    }

    @Test
    fun profileInfo_for_user2_reads_the_u2_entry_as_mine() {
        val state = json("""{"profile":{"u1":{"name":"A"},"u2":{"name":"B"}}}""")
        val info = state.toProfileInfo(testSession().copy(role = "user2"), null, null)
        assertEquals("B", info.myName)
        assertEquals("A", info.partnerName)
    }

    @Test
    fun profilePatch_keeps_other_profile_fields_and_writes_the_web_keys() {
        val state = json("""{"profile":{"u1":{"name":"Old","moods":[1,2]},"u2":{"name":"Z"}}}""")
        val edit = ProfileEdit(
            myName = "New", partnerName = "Likitha", gender = ProfileGender.Female,
            anniversary = LocalDate.of(2025, 10, 12), myBirthday = null,
            partnerBirthday = LocalDate.of(1998, 5, 6), avatarUrl = "me.jpg",
        )
        val patch = profilePatch(state, "user1", edit)
        assertEquals("2025-10-12", patch["anniversary"]!!.jsonPrimitive.content)
        assertEquals("", patch["myBday"]!!.jsonPrimitive.content)
        assertEquals("1998-05-06", patch["ptBday"]!!.jsonPrimitive.content)
        val mine = patch["profile"]!!.jsonObject["u1"]!!.jsonObject
        assertEquals("New", mine["name"]!!.jsonPrimitive.content)
        assertEquals("female", mine["gender"]!!.jsonPrimitive.content)
        assertEquals("me.jpg", mine["avatar"]!!.jsonPrimitive.content)
        assertNotNull(mine["moods"]) // untouched fields survive
        assertNull(patch["profile"]!!.jsonObject["u2"]) // never writes the partner's entry
    }

    @Test
    fun blank_names_become_you_and_partner() {
        val clean = ProfileEdit("  ", "", null, null, null, null, null).normalized()
        assertEquals("You", clean.myName)
        assertEquals("Partner", clean.partnerName)
    }

    @Test
    fun partnerStatus_mapping() {
        assertEquals(PartnerStatus.Connected("Likitha"), StatusDto("connected", partnerName = "Likitha").toDomain())
        assertEquals(PartnerStatus.NotConnected, StatusDto("not_connected").toDomain())
        assertEquals(PartnerStatus.NotConnected, StatusDto("something_new").toDomain())
        val waiting = StatusDto("waiting", requestId = "r1", receiverName = "Anu", receiverPhone = "91", createdAt = "2026-10-29T10:00:00.123456+00:00").toDomain()
        assertEquals(PartnerStatus.Waiting("r1", "Anu", "91", Instant.parse("2026-10-29T10:00:00.123456Z")), waiting)
    }

    @Test
    fun parseInstant_handles_offset_naive_and_bad_text() {
        assertEquals(Instant.parse("2026-10-29T10:00:00Z"), parseInstant("2026-10-29T10:00:00Z"))
        assertEquals(Instant.parse("2026-10-29T10:00:00Z"), parseInstant("2026-10-29T10:00:00"))
        assertNull(parseInstant("yesterday"))
        assertNull(parseInstant(null))
    }

    @Test
    fun storageSummary_uses_the_web_formula() {
        val state = json(
            """{"photos":[{"url":"${"x".repeat(400)}"}],
               "vault":[{"type":"video","url":"${"y".repeat(400)}"},{"type":"image","url":"${"z".repeat(400)}"}],
               "notes":[{"t":"hi"}],"journal":[1],"habits":[]}""",
        )
        val summary = state.toStorageSummary()
        val bytes = summary.categories.associate { it.kind to it.bytes }
        // photos 400*0.75 + vault video 400*0.75*1.5 + vault image 400*0.75 = 300 + 450 + 300
        assertEquals(1050L, bytes.getValue(StorageKind.Photos))
        assertEquals(0L, bytes.getValue(StorageKind.Voice))
        assertEquals(0L, bytes.getValue(StorageKind.Chat))
        assertEquals("""{"t":"hi"}""".length.toLong(), bytes.getValue(StorageKind.Notes))
        assertTrue(bytes.getValue(StorageKind.AppData) > 0)
        assertEquals(summary.categories.sumOf { it.bytes }, summary.totalBytes)
        assertEquals(500L * 1024 * 1024, summary.capacityBytes)
    }

    @Test
    fun imageUrls_only_lists_images_with_an_address() {
        val state = json(
            """{"photos":[{"type":"image","url":"https://a/1.jpg"},{"type":"video","url":"https://a/2.mp4"},
               {"type":"image","url":""},{"type":"image","url":"data:image/png;base64,AAAA"}]}""",
        )
        assertEquals(listOf("https://a/1.jpg", "data:image/png;base64,AAAA"), state.imageUrls())
    }

    @Test
    fun backupPatch_never_sends_who_you_are() {
        val patch = backupPatch(
            json("""{"userId":"u9","coupleId":"c9","role":"user2","myName":"X","vaultPin":"1357","paired":true,"notes":[1],"photos":[]}"""),
        )
        assertEquals(setOf("notes", "photos"), patch.keys)
    }
}
