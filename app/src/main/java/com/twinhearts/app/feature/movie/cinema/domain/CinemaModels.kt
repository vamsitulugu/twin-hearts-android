package com.twinhearts.app.feature.movie.cinema.domain

/** Why this file exists: the plain shapes the cinema screens use (the UI never sees DTOs). */

/** Who is opening the cinema: the couple, which side I am, and which avatar I get (1 = girl, 2 = boy; the guest always gets the other). */
data class CinemaWho(val coupleId: String, val role: String, val partnerName: String, val slot: Int)

/** The movie file this phone picked on Watch Together (null for a shared cloud movie: the theatre streams that itself). */
data class LocalMovieRef(val uri: String, val title: String)

/** [url] is the theatre address the server made for me. [local] is played inside the theatre, so nobody chooses the movie a second time. */
data class CinemaLaunch(val url: String, val local: LocalMovieRef?)

data class CinemaInvite(val id: String, val hostName: String, val movie: String?)
