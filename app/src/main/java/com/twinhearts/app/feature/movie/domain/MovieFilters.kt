package com.twinhearts.app.feature.movie.domain

import com.twinhearts.app.feature.movie.domain.model.MovieItem
import com.twinhearts.app.feature.movie.domain.model.WatchStatus

/** Why this file exists: the Library filter chips and the Genres grid, kept as plain testable logic. */
enum class LibraryFilter { All, Recent, Watchlist, Watching, Watched }

/** The genre names of the web "Genres" tab, in the same order. */
val movieGenres: List<String> = listOf(
    "Romance", "Action", "Comedy", "Drama", "Thriller", "Horror", "Sci-Fi", "Fantasy", "Animation",
    "Documentary", "Bollywood", "Other",
)

fun List<MovieItem>.applyFilter(filter: LibraryFilter, genre: String?, query: String): List<MovieItem> {
    var list = when (filter) {
        LibraryFilter.All -> this
        LibraryFilter.Recent -> take(RECENT_COUNT)
        LibraryFilter.Watchlist -> filter { it.status == WatchStatus.Watchlist }
        LibraryFilter.Watching -> filter { it.status == WatchStatus.Watching }
        LibraryFilter.Watched -> filter { it.status == WatchStatus.Watched }
    }
    if (genre != null) list = list.filter { it.matchesGenre(genre) }
    val q = query.trim()
    if (q.isNotEmpty()) list = list.filter { it.title.contains(q, ignoreCase = true) }
    return list
}

/** A movie can have several genres ("Drama, Romance"). Anything not in the list counts as "Other". */
fun MovieItem.matchesGenre(name: String): Boolean {
    val own = rawGenres().split(',').map { it.trim() }.filter { it.isNotEmpty() }
    return if (name == "Other") own.none { it in movieGenres } else own.any { it.equals(name, true) }
}

private fun MovieItem.rawGenres(): String = (genre ?: "").replace("Science Fiction", "Sci-Fi")

fun List<MovieItem>.countByStatus(status: WatchStatus): Int = count { it.status == status }

private const val RECENT_COUNT = 10
