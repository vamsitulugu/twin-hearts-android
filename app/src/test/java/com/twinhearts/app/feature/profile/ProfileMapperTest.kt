package com.twinhearts.app.feature.profile

import com.twinhearts.app.feature.profile.data.mapper.parseDate
import com.twinhearts.app.feature.profile.data.mapper.parseIsoMs
import com.twinhearts.app.feature.profile.data.mapper.toEventJson
import com.twinhearts.app.feature.profile.data.mapper.toJson
import com.twinhearts.app.feature.profile.data.mapper.toMilestones
import com.twinhearts.app.feature.profile.data.mapper.toProfileData
import com.twinhearts.app.feature.profile.domain.model.ConnectionState
import com.twinhearts.app.feature.profile.domain.model.DateAlarm
import com.twinhearts.app.feature.profile.domain.model.DateCategory
import com.twinhearts.app.feature.profile.domain.model.Gender
import com.twinhearts.app.feature.profile.domain.model.LoveLanguage
import com.twinhearts.app.feature.profile.domain.model.MilestoneType
import com.twinhearts.app.feature.profile.domain.model.NewMilestone
import com.twinhearts.app.feature.profile.domain.model.NewSpecialDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProfileMapperTest {

    private fun state(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    @Test
    fun parseDate_acceptsIsoAndRejectsJunk() {
        assertEquals(LocalDate.of(2026, 10, 27), parseDate("2026-10-27"))
        assertEquals(LocalDate.of(2026, 10, 27), parseDate("2026-10-27T10:00:00Z"))
        assertNull(parseDate(""))
        assertNull(parseDate("nonsense"))
    }

    @Test
    fun parseIso_handlesZAndOffset() {
        assertEquals(parseIsoMs("2026-10-27T10:00:00Z"), parseIsoMs("2026-10-27T10:00:00+00:00"))
        assertNull(parseIsoMs(null))
    }

    @Test
    fun toProfileData_readsOwnAndPartnerEntries() {
        val data = state(
            """{"anniversary":"2024-08-20","myBday":"2000-11-02","myBio":"hi","partnerBio":"her","myLoveLang":"Quality Time",
               "ptLoveLang":"Physical Touch","photos":[1,2,3],"bucket":[1],
               "profile":{"u1":{"name":"Vamsi","bday":"2000-11-02","gender":"male"},"u2":{"name":"Likitha","bday":"2001-03-05"}},
               "milestones":[{"id":5,"title":"First trip","date":"2025-01-02","type":"✈️","note":"n"},{"id":6,"title":"","date":"2025-01-02"}],
               "events":[{"title":"Dinner","date":"2026-11-20","cat":"Date"},{"title":"x","date":"bad"}]}""",
        ).toProfileData("user1", "Vamsi", "Partner", "", null, ConnectionState.Connected, "me.jpg", "her.jpg", 5L)
        assertEquals("Likitha", data.partnerName)
        assertEquals(LocalDate.of(2001, 3, 5), data.partnerBirthday)
        assertEquals(Gender.Male, data.myGender)
        assertEquals(LoveLanguage.QualityTime, data.myLoveLanguage)
        assertEquals(3, data.memoriesCount)
        assertEquals(1, data.dreamsCount)
        assertEquals(1, data.milestones.size) // the one without a title is skipped
        assertEquals(1, data.dates.size) // the one with a bad date is skipped
        assertTrue(data.paired)
    }

    @Test
    fun toProfileData_asSecondUser_readsTheOtherEntryAsPartner() {
        val data = state("""{"profile":{"u1":{"name":"A"},"u2":{"name":"B"}}}""")
            .toProfileData("user2", "B", "Partner", "", null, ConnectionState.Connected, null, null, null)
        assertEquals("A", data.partnerName)
    }

    @Test
    fun toProfileData_emptyState_usesSessionValuesAndDefaults() {
        val data = JsonObject(emptyMap()).toProfileData("user1", "Me", "Partner", "2024-01-01", "female", ConnectionState.NotConnected, null, null, null)
        assertEquals("Me", data.myName)
        assertEquals(LocalDate.of(2024, 1, 1), data.anniversary)
        assertEquals(Gender.Female, data.myGender)
        assertTrue(data.milestones.isEmpty())
    }

    @Test
    fun eventAndMilestoneJson_useWebFieldNames() {
        val event = NewSpecialDate("Dinner", LocalDate.of(2026, 11, 20), "19:30", "n", DateCategory.DateNight, DateAlarm.Both).toEventJson(9L)
        assertEquals("Date", event["cat"]!!.jsonPrimitive.content)
        assertEquals("both", event["alarm"]!!.jsonPrimitive.content)
        val milestone = JsonObject(mapOf("milestones" to kotlinx.serialization.json.JsonArray(listOf(NewMilestone("T", LocalDate.of(2026, 1, 1), "", MilestoneType.Proposal).toJson(3L)))))
        assertEquals("💍", milestone.toMilestones().single().emoji)
    }
}
