package com.twinhearts.app.feature.movie.domain

import com.twinhearts.app.feature.movie.domain.model.MovieItem
import com.twinhearts.app.feature.movie.domain.model.MovieOwner
import com.twinhearts.app.feature.movie.domain.model.WatchStatus

/** Why this file exists: the Library filter chips and the Genres grid, kept as plain testable logic. */
enum class LibraryFilter { All, Recent, Favorites, Watchlist, Watching, Watched }

/** The owner chips of the web Library: Everyone, Mine, Partner's, Together. */
enum class OwnerFilter { Everyone, Mine, Partner, Together }

/** The genre names of the web "Genres" tab, in the same order. */
val movieGenres: List<String> = listOf(
    "Romance", "Action", "Comedy", "Drama", "Thriller", "Horror", "Sci-Fi", "Fantasy", "Animation",
    "Documentary", "Bollywood", "Other",
)

fun List<MovieItem>.applyFilter(
    filter: LibraryFilter,
    genre: String?,
    query: String,
    owner: OwnerFilter = OwnerFilter.Everyone,
    role: String? = null,
    favorites: Set<String> = emptySet(),
): List<MovieItem> {
    var list = when (filter) {
        LibraryFilter.All -> this
        LibraryFilter.Recent -> take(RECENT_COUNT)
        LibraryFilter.Favorites -> filter { it.id in favorites }
        LibraryFilter.Watchlist -> filter { it.status == WatchStatus.Watchlist }
        LibraryFilter.Watching -> filter { it.status == WatchStatus.Watching }
        LibraryFilter.Watched -> filter { it.status == WatchStatus.Watched }
    }
    if (owner != OwnerFilter.Everyone) {
        val wanted = when (owner) {
            OwnerFilter.Mine -> MovieOwner.Mine
            OwnerFilter.Partner -> MovieOwner.Partner
            else -> MovieOwner.Together
        }
        list = list.filter { it.owner(role) == wanted }
    }
    if (genre != null) list = list.filter { it.matchesGenre(genre) }
    val q = query.trim()
    if (q.isNotEmpty()) list = list.filter { it.title.contains(q, ignoreCase = true) }
    return list
}

/** "both" means watched together; the phone's own role means mine; the other role is the partner's. Old rows count as mine. */
fun MovieItem.owner(role: String?): MovieOwner = when {
    addedBy == "both" -> MovieOwner.Together
    addedBy == null || role == null || addedBy == role -> MovieOwner.Mine
    else -> MovieOwner.Partner
}

/** A movie can have several genres ("Drama, Romance"). Anything not in the list counts as "Other". */
fun MovieItem.matchesGenre(name: String): Boolean {
    val own = rawGenres().split(',').map { it.trim() }.filter { it.isNotEmpty() }
    return if (name == "Other") own.none { it in movieGenres } else own.any { it.equals(name, true) }
}

private fun MovieItem.rawGenres(): String = (genre ?: "").replace("Science Fiction", "Sci-Fi")

fun List<MovieItem>.countByStatus(status: WatchStatus): Int = count { it.status == status }

private const val RECENT_COUNT = 10

/** Emoji and two colours of a genre card, copied from the web page (movie-collection.js GENRES). */
data class GenreStyle(val emoji: String, val startArgb: Long, val endArgb: Long)

fun genreStyle(name: String?): GenreStyle = when (name) {
    "Romance" -> GenreStyle("💕", 0xFFC21A8B, 0xFF7C0A57)
    "Action" -> GenreStyle("💥", 0xFFFF8A3D, 0xFF6B1D0B)
    "Comedy" -> GenreStyle("😂", 0xFFFFD14A, 0xFF7A4A06)
    "Drama" -> GenreStyle("🎭", 0xFFC21A8B, 0xFF7C0A57)
    "Thriller" -> GenreStyle("😰", 0xFF826A78, 0xFF331B28)
    "Horror" -> GenreStyle("👻", 0xFF9AA0A6, 0xFF2A0A0A)
    "Sci-Fi" -> GenreStyle("🚀", 0xFFC21A8B, 0xFF5C0A37)
    "Fantasy" -> GenreStyle("🧙", 0xFF5EE0B0, 0xFF0E4A3A)
    "Animation" -> GenreStyle("🎨", 0xFFC21A8B, 0xFF511336)
    "Documentary" -> GenreStyle("📽️", 0xFFC9B27C, 0xFF4A3A16)
    "Bollywood" -> GenreStyle("🎪", 0xFFC21A8B, 0xFF7C0A57)
    else -> GenreStyle("🎬", 0xFF7C0A57, 0xFF3A0425)
}
