package com.twinhearts.app.feature.twinai.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Why this file exists: the exact JSON shapes of routes/ai.js and routes/ai-both.js (R-65). */

@Serializable
data class AiChatBody(
    val message: String,
    val myName: String,
    val partnerName: String,
    val anniversary: String,
    val recentMood: String = "",
)

/** routes/ai.js `POST /chat` answer. */
@Serializable
data class AiReplyDto(val reply: String? = null)

/** One `data:` line of the `POST /chat/stream` answer: a piece of text, or an error. */
@Serializable
data class SseChunkDto(val delta: String? = null, val error: String? = null)

@Serializable
data class FlagDto(val enabled: Boolean = false)

@Serializable
data class NewSessionBody(val coupleId: String, val role: String, val title: String? = null)

@Serializable
data class SectionDto(val title: String = "", val content: String = "")

@Serializable
data class ResultDto(
    val intent: String = "GENERAL",
    val sections: List<SectionDto> = emptyList(),
    @SerialName("safety_flag") val safetyFlag: Boolean = false,
)

/** Used for the round in every answer; fields the server leaves out fall back to safe defaults. */
@Serializable
data class RoundDto(
    val id: String,
    @SerialName("round_number") val roundNumber: Int = 1,
    val status: String = "pending",
    @SerialName("you_submitted") val youSubmitted: Boolean = false,
    @SerialName("partner_submitted") val partnerSubmitted: Boolean = false,
    @SerialName("your_content") val yourContent: String? = null,
    val result: ResultDto? = null,
)

@Serializable
data class SessionDto(
    val id: String,
    val title: String? = null,
    @SerialName("last_activity_at") val lastActivityAt: String? = null,
    @SerialName("round_count") val roundCount: Int = 0,
)

@Serializable
data class NewSessionDto(val session: SessionDto, val round: RoundDto)

@Serializable
data class SessionDetailDto(val session: SessionDto, val rounds: List<RoundDto> = emptyList())

@Serializable
data class NextRoundDto(val round: RoundDto)

@Serializable
data class SubmitBody(val coupleId: String, val role: String, val content: String)

@Serializable
data class RoleBody(val coupleId: String, val role: String)

@Serializable
data class CoupleBody(val coupleId: String)

/** Answers that only say "it worked" (`{status}`, `{deleted}`); the content is not needed. */
@Serializable
data class StatusDto(val status: String? = null, val deleted: Boolean? = null)

@Serializable
data class ProStatusDto(val isPro: Boolean = false, val plan: String? = null, val expiresAt: String? = null)

/** The `/api/pro/upgrade-request` answer; the content is not needed, only that it worked. */
@Serializable
data class UpgradeAckDto(val success: Boolean? = null, val ok: Boolean? = null)
