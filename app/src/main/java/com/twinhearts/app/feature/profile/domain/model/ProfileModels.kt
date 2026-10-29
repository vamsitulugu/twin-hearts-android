package com.twinhearts.app.feature.profile.domain.model

import java.time.LocalDate

/**
 * Plain Kotlin models the Profile screens use (R-03). They are filled by the mapper in data/mapper
 * from the couple's shared state, so no network type ever reaches the UI.
 */

/** "I am" choice on Edit Profile. [wire] is what the old app stores (`profile.u1.gender`). */
enum class Gender(val wire: String) {
    Female("female"),
    Male("male"),
    ;

    companion object {
        fun fromWire(value: String?): Gender? = entries.firstOrNull { it.wire == value }
    }
}

/** The five love languages. [wire] is the English name the old app stores in `myLoveLang` / `ptLoveLang`. */
enum class LoveLanguage(val wire: String, val emoji: String) {
    WordsOfAffirmation("Words of Affirmation", "💬"),
    QualityTime("Quality Time", "⏰"),
    ActsOfService("Acts of Service", "🛠️"),
    PhysicalTouch("Physical Touch", "🤗"),
    ReceivingGifts("Receiving Gifts", "🎁"),
    ;

    companion object {
        fun fromWire(value: String?): LoveLanguage? = entries.firstOrNull { it.wire == value }
    }
}

/** Milestone types of the web form. [emoji] is stored in `type`, so never change it. */
enum class MilestoneType(val emoji: String) {
    FirstMessage("❤️"),
    FirstCall("☎️"),
    FirstMeet("🥹"),
    Proposal("💍"),
    Trip("✈️"),
    Anniversary("🎉"),
    Custom("💫"),
}

/** Event categories of the web "Add Event" form. [key] is stored in `cat`. */
enum class DateCategory(val key: String, val emoji: String) {
    Anniversary("Anniversary", "💕"),
    DateNight("Date", "🌹"),
    Birthday("Birthday", "🎂"),
    Trip("Trip", "✈️"),
    Special("Special", "⭐"),
    Study("Study", "📚"),
    Other("Other", "📌"),
}

/** Who gets the alarm of a new date (`alarm` on the web: "", "self", "both"). */
enum class DateAlarm(val key: String) {
    None(""),
    Self("self"),
    Both("both"),
}

data class Milestone(
    val id: Long,
    val emoji: String,
    val title: String,
    val date: LocalDate,
    val note: String,
)

/** A saved special date (a calendar event). Only what the Profile needs. */
data class SpecialDate(
    val title: String,
    val date: LocalDate,
    val category: String,
)

enum class ConnectionState { Connected, Waiting, NotConnected }

/** Everything the Profile screens show, read from the couple's shared state and the profile rows. */
data class ProfileData(
    val myName: String,
    val partnerName: String,
    val role: String,
    val connection: ConnectionState,
    val anniversary: LocalDate?,
    val myBirthday: LocalDate?,
    val partnerBirthday: LocalDate?,
    val myBio: String,
    val partnerBio: String,
    val myLoveLanguage: LoveLanguage?,
    val partnerLoveLanguage: LoveLanguage?,
    val myGender: Gender?,
    val myAvatarUrl: String?,
    val partnerAvatarUrl: String?,
    val memoriesCount: Int,
    val dreamsCount: Int,
    val milestones: List<Milestone>,
    val dates: List<SpecialDate>,
    /** When the partner last shared a location (epoch millis); null when unknown. */
    val partnerSeenAtMs: Long?,
) {
    val paired: Boolean get() = connection == ConnectionState.Connected
}

/** What Edit Profile hands to the repository. */
data class MyProfileEdit(
    val name: String,
    val birthday: LocalDate?,
    val anniversary: LocalDate?,
    val gender: Gender?,
    val bio: String,
)

/**
 * What Edit Details (partner) hands to the repository. While connected, the partner's name and birthday
 * come from their own profile, so only [bio] is saved; [includeIdentity] is true only when not connected.
 */
data class PartnerEdit(
    val includeIdentity: Boolean,
    val name: String,
    val birthday: LocalDate?,
    val bio: String,
)

/** What the "Add Date" and "Add Milestone" sheets hand to the repository. */
data class NewSpecialDate(
    val title: String,
    val date: LocalDate,
    val time: String,
    val note: String,
    val category: DateCategory,
    val alarm: DateAlarm,
)

data class NewMilestone(
    val title: String,
    val date: LocalDate,
    val note: String,
    val type: MilestoneType,
)

enum class ImportantKind { Event, Anniversary, MyBirthday, PartnerBirthday }

/** The soonest coming date for the "Next Important Date" card. */
data class ImportantDate(
    val kind: ImportantKind,
    /** Only used for [ImportantKind.Event]. */
    val title: String,
    val date: LocalDate,
    val daysLeft: Int,
    val category: String,
)

/** The love-language tip shown when both partners picked one. */
enum class LoveTip {
    WordsAndQuality,
    WordsAndActs,
    QualityAndTouch,
    QualityAndWords,
    ActsAndGifts,
    TouchAndGifts,
    Default,
}

/** Years / months / days between two dates. [notStarted] is true when the date is still ahead. */
data class TogetherSpan(
    val years: Int,
    val months: Int,
    val days: Int,
    val notStarted: Boolean,
)
