package com.twinhearts.app.feature.collection.domain

import com.twinhearts.app.feature.collection.domain.model.PhotoFilter
import com.twinhearts.app.feature.collection.domain.model.PhotoItem
import com.twinhearts.app.feature.collection.domain.model.VoiceFilter
import com.twinhearts.app.feature.collection.domain.model.VoiceItem
import com.twinhearts.app.feature.collection.domain.model.VoiceSort

/** Pure rules copied from collection.html so they can be unit tested. No Android classes here. */
object CollectionLogic {

    /** Filter, search and sort voice messages exactly like the web `applySortFilterSearch`. */
    fun voices(
        list: List<VoiceItem>,
        filter: VoiceFilter,
        sort: VoiceSort,
        query: String,
        favorites: Set<String>,
        myRole: String,
        myName: String,
        partnerName: String,
    ): List<VoiceItem> {
        val q = query.trim().lowercase()
        var out = list.filter { v ->
            val sender = (if (v.by == myRole) myName else partnerName).lowercase()
            val matches = q.isEmpty() ||
                dateOf(v).contains(q) ||
                sender.contains(q) ||
                durationLabel(v.durationSec).contains(q)
            matches && when (filter) {
                VoiceFilter.Mine -> v.by == myRole
                VoiceFilter.Partner -> v.by != myRole
                VoiceFilter.Favorites -> v.id in favorites
                else -> true
            }
        }
        if (filter == VoiceFilter.Recent) out = out.sortedByDescending { it.createdAt }.take(20)
        if (filter == VoiceFilter.Longest) out = out.sortedByDescending { it.durationSec }.take(20)
        return when (sort) {
            VoiceSort.Newest -> out.sortedByDescending { it.createdAt }
            VoiceSort.Oldest -> out.sortedBy { it.createdAt }
            VoiceSort.Longest -> out.sortedByDescending { it.durationSec }
            VoiceSort.Shortest -> out.sortedBy { it.durationSec }
            VoiceSort.Favorites -> out.sortedByDescending { it.createdAt }.sortedByDescending { it.id in favorites } // newest first inside each group
        }
    }

    fun photos(list: List<PhotoItem>, filter: PhotoFilter, query: String, favorites: Set<String>): List<PhotoItem> {
        val q = query.trim().lowercase()
        return list.filter { p ->
            (q.isEmpty() || p.name.lowercase().contains(q) || p.date.contains(q)) && when (filter) {
                PhotoFilter.Photos -> !p.isVideo
                PhotoFilter.Videos -> p.isVideo
                PhotoFilter.Favorites -> p.id in favorites
                PhotoFilter.All -> true
            }
        }
    }

    /** "YYYY-MM-DD" part of an ISO time, or "". */
    fun dateOf(v: VoiceItem): String = if (v.createdAt.length >= 10) v.createdAt.substring(0, 10) else ""

    /** "m:ss", as the web `fmtDurationLabel`. */
    fun durationLabel(sec: Int): String {
        val s = sec.coerceAtLeast(0)
        return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
    }

    /** Total length: "0s", "45s", "2m 5s", "1h 03m". Same shape as the web `fmtTotalDur`. */
    fun totalDuration(sec: Int): String {
        val s = sec.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val r = s % 60
        return when {
            h > 0 -> "${h}h ${m.toString().padStart(2, '0')}m"
            m > 0 -> if (r > 0) "${m}m ${r}s" else "${m}m"
            else -> "${r}s"
        }
    }

    /** Groups photos by "YYYY-MM", newest month first. Photos without a date go under "". */
    fun photosByMonth(list: List<PhotoItem>): List<Pair<String, List<PhotoItem>>> =
        list.groupBy { if (it.date.length >= 7) it.date.substring(0, 7) else "" }
            .toList()
            .sortedByDescending { it.first }

    /** Groups voice messages by day, in the order given. */
    fun voicesByDay(list: List<VoiceItem>): List<Pair<String, List<VoiceItem>>> =
        list.groupBy { dateOf(it) }.toList()
}
