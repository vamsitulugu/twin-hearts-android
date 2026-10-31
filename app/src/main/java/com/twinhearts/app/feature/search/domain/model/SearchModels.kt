package com.twinhearts.app.feature.search.domain.model

/*
 * Why this file exists: the plain Kotlin shapes the Search screen uses. They hold finished text
 * only, so the UI never needs to know where a result came from (R-40).
 */

/** Where a result goes when tapped. Mapped to a real route in SearchGraph (R-83). */
enum class SearchDestination {
    Home, Chat, TwinAi, LiveMap, Settings, Profile, Pro, Movie, MeetPlanner, Fights,
    LoveCounter, DreamGoals, Collection, Money, Calendar, Music, Study, Level, Places,
    Memories, BucketList, PeriodTracker, Help, MySpace, Vault, Surprises, Globe, Games, DailyRoutine,
}

/** The four blocks of the result list, in the order they are shown. */
enum class SearchSection { Pages, Features, Messages, Inside }

/** One row of the result list. [where] is the page it lives in, e.g. "Settings". */
data class SearchResult(
    val key: String,
    val section: SearchSection,
    val title: String,
    val detail: String,
    val where: String,
    val emoji: String,
    val destination: SearchDestination,
    val score: Int,
)

/** A thing that can be searched: the text the matcher looks at, plus how to show it. */
data class SearchDoc(
    val key: String,
    val section: SearchSection,
    val title: String,
    val detail: String,
    val where: String,
    val emoji: String,
    val destination: SearchDestination,
    /** Extra words that should also find this row (synonyms, sub-page names). */
    val keywords: String,
)

/** State of the "messages + page content" lookup, which needs the network. */
sealed interface ContentStatus {
    data object Idle : ContentStatus
    data object Loading : ContentStatus
    data object Done : ContentStatus
    data object Failed : ContentStatus
}
