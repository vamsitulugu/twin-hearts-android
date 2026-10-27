package com.twinhearts.app.feature.twinai.domain.model

import com.twinhearts.app.core.common.AppError
import java.time.Instant

/** Why this file exists: plain Kotlin shapes the Twin AI screens use (no network types, R-40). */

/** Who is talking to Twin, taken from the saved Session. */
data class AiIdentity(
    val coupleId: String,
    val role: String,
    val myName: String,
    val partnerName: String,
    val anniversary: String,
)

enum class AiAuthor { Me, Twin }

enum class AiMessageState { Streaming, Done, Failed }

/** One bubble in the YOU conversation. [question] is what Twin was asked (used by Retry/Regenerate). */
data class AiMessage(
    val id: String,
    val author: AiAuthor,
    val text: String,
    val time: Instant,
    val state: AiMessageState = AiMessageState.Done,
    val question: String? = null,
    val error: AppError? = null,
)

/** The five chips under the hero and in the "+" sheet. [prompt] is the text sent to Twin. */
enum class QuickPrompt(val emoji: String, val prompt: String) {
    PlanDate("💑", "Give us 5 creative and romantic date night ideas for tonight"),
    Argued("💬", "We had a fight. Give us a step-by-step guide to talk it through with love"),
    Sweet("💌", "Write a heartfelt, poetic love letter from me to my partner"),
    DeepTalk("💭", "Give us 5 deep conversation questions to learn more about each other"),
    Surprise("🎁", "Plan a romantic surprise — give me 3 detailed and thoughtful options"),
}

/** What the reply stream tells the ViewModel. */
sealed interface AiStreamEvent {
    data class Delta(val text: String) : AiStreamEvent
    data object Done : AiStreamEvent
    data class Failure(val error: AppError) : AiStreamEvent
}

// ───────────── BOTH mode ─────────────

enum class RoundStatus {
    Pending, Analyzing, Done, Safety, Failed;

    companion object {
        fun from(raw: String?): RoundStatus = when (raw) {
            "analyzing" -> Analyzing
            "done" -> Done
            "safety" -> Safety
            "failed" -> Failed
            else -> Pending
        }
    }

    /** Still moving: the screen keeps asking the server while this is true. */
    val inProgress: Boolean get() = this == Pending || this == Analyzing
}

data class BothSection(val title: String, val content: String)

data class BothResult(val intent: String, val safety: Boolean, val sections: List<BothSection>)

data class BothRound(
    val id: String,
    val number: Int,
    val status: RoundStatus,
    val youSubmitted: Boolean,
    val partnerSubmitted: Boolean,
    val yourContent: String?,
    val result: BothResult?,
)

data class BothSession(
    val id: String,
    val title: String?,
    val roundCount: Int,
    val lastActivity: Instant?,
    /** Server label such as CONFLICT; null until Twin has worked out what the talk is about. */
    val intent: String? = null,
)

data class BothDiscussion(val session: BothSession, val rounds: List<BothRound>) {
    /** The round being answered now (the newest one). */
    val current: BothRound? get() = rounds.maxByOrNull { it.number }
}

/** Names and photos of the two partners, shown above an open BOTH discussion. Photos are null when none is set. */
data class BothPeople(
    val myName: String,
    val partnerName: String,
    val myAvatar: String? = null,
    val partnerAvatar: String? = null,
)
