package com.twinhearts.app.feature.us.domain.model

/** One dream/goal from the couple's shared bucket list. */
data class DreamGoal(
    val id: String,
    val title: String,
    val emoji: String,
    val description: String?,
    val targetDate: String?,
    val priority: String, // "high" | "medium" | "low"
    val progress: Int, // 0..100
    val completed: Boolean,
    val createdBy: String?, // "user1" | "user2"
)

/** One shared memory item in the Collection (photo/voice/video). */
data class CollectionItem(
    val id: String,
    val type: String, // "photo" | "video" | "voice"
    val url: String?,
    val sender: String,
    val sentAt: Long,
    val durationSec: Int?,
)

/** The couple's love-counter state. */
data class LoveCounterState(
    val anniversary: String?, // ISO "YYYY-MM-DD" from Session / shared state
    val startedAt: String?,
    val daysTogether: Int,
    val years: Int,
    val months: Int,
    val remainingDays: Int,
    val remainingHours: Int,
    val remainingMinutes: Int,
    val remainingSeconds: Int,
)

/** UI state for the Us tab container. */
data class UsUiState(
    val loveCounter: LoveCounterState = LoveCounterState(),
    val collection: List<CollectionItem> = emptyList(),
    val dreamGoals: List<DreamGoal> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
