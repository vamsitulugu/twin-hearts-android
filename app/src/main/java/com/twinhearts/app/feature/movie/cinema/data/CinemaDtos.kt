package com.twinhearts.app.feature.movie.cinema.data

import kotlinx.serialization.Serializable

/** Why this file exists: the shapes of routes/cinema.js answers and bodies (read from the server code, R-65). */
@Serializable data class CinemaCreateBody(val coupleId: String, val role: String, val slot: Int, val movie: String)
@Serializable data class CinemaRoleBody(val coupleId: String, val role: String)
@Serializable data class CinemaSessionDto(val id: String)
@Serializable data class CinemaCreateDto(val session: CinemaSessionDto, val url: String)
@Serializable data class CinemaInviteDto(val id: String, val hostName: String? = null, val movie: String? = null)
@Serializable data class CinemaPendingDto(val invite: CinemaInviteDto? = null)
@Serializable data class CinemaAcceptDto(val url: String)
@Serializable data class CinemaAckDto(val ok: Boolean? = null)
