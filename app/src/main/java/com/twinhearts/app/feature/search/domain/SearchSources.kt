package com.twinhearts.app.feature.search.domain

import androidx.annotation.StringRes
import com.twinhearts.app.R
import com.twinhearts.app.feature.search.domain.model.SearchDestination

/**
 * Why this file exists: which lists inside the couple's saved document Search may read, which
 * fields of each item it looks at, and which page an item opens. Only keys and fields that the
 * app's own pages already read are listed (R-65). Vault, Period Tracker and Wellness are left
 * out on purpose: they are private.
 */
internal data class StateSource(
    /** Top-level key in the saved document. */
    val key: String,
    @StringRes val label: Int,
    val emoji: String,
    val destination: SearchDestination,
    /** Fields tried in order for the row title. */
    val titleFields: List<String>,
    /** Other fields that can match (never shown as the title). */
    val bodyFields: List<String>,
    /** True when only items I added may show (journal, surprises: a partner's must stay hidden). */
    val mineOnly: Boolean = false,
)

internal val stateSources: List<StateSource> = listOf(
    StateSource("events", R.string.search_source_events, "📅", SearchDestination.Calendar, listOf("title"), listOf("note")),
    StateSource("bucket", R.string.search_source_bucket, "✅", SearchDestination.BucketList, listOf("title"), listOf("cat")),
    StateSource("milestones", R.string.search_source_milestones, "🏅", SearchDestination.Profile, listOf("title", "name"), listOf("note")),
    StateSource("reminders", R.string.search_source_reminders, "⏰", SearchDestination.MySpace, listOf("title", "text"), listOf("note", "body")),
    StateSource("notes", R.string.search_source_notes, "🗒️", SearchDestination.MySpace, listOf("title", "name"), listOf("body", "text", "note")),
    StateSource("journal", R.string.search_source_journal, "📔", SearchDestination.MySpace, listOf("title"), listOf("body", "text", "note"), mineOnly = true),
    StateSource("habits", R.string.search_source_habits, "🔁", SearchDestination.MySpace, listOf("name", "title"), listOf("goal")),
    StateSource("transactions", R.string.search_source_transactions, "💸", SearchDestination.Money, listOf("desc", "description"), listOf("category")),
    StateSource("fights", R.string.search_source_fights, "⚖️", SearchDestination.Fights, listOf("title", "reason"), listOf("category", "notes", "feelings", "need", "approach")),
    StateSource("surprises", R.string.search_source_surprises, "🎁", SearchDestination.Surprises, listOf("title"), listOf("msg"), mineOnly = true),
    StateSource("photos", R.string.search_source_photos, "📸", SearchDestination.Memories, listOf("name"), emptyList()),
)
