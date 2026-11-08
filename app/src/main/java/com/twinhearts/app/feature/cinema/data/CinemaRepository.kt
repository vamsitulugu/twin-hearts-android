package com.twinhearts.app.feature.cinema.data

import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.map
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.cinema.data.remote.CinemaApi
import com.twinhearts.app.feature.cinema.data.remote.CreateBody
import com.twinhearts.app.feature.cinema.data.remote.InviteDto
import com.twinhearts.app.feature.cinema.data.remote.WhoBody
import com.twinhearts.app.feature.cinema.domain.CinemaLinks
import com.twinhearts.app.feature.cinema.domain.model.CinemaIdentity
import com.twinhearts.app.feature.cinema.domain.model.CinemaInvite
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Why this file exists: the one place the Cinema feature talks to the server and maps errors (R-40, R-46). */
interface CinemaRepository {
    /** Who I am for the cinema, or null when nobody is signed in or the couple is not paired yet. */
    suspend fun identity(): CinemaIdentity?

    /** Host: opens a session and returns the link to load in the WebView. */
    suspend fun open(who: CinemaIdentity): AppResult<String>

    /** Partner: accepts the invite and returns the link to load in the WebView. */
    suspend fun accept(who: CinemaIdentity, sessionId: String): AppResult<String>

    suspend fun pendingInvite(who: CinemaIdentity): AppResult<CinemaInvite?>

    suspend fun decline(who: CinemaIdentity, sessionId: String): AppResult<Unit>
}

class CinemaRepositoryImpl @Inject constructor(
    private val api: CinemaApi,
    private val sessionStore: SessionStore,
) : CinemaRepository {

    override suspend fun identity(): CinemaIdentity? {
        val session = sessionStore.session.first() ?: return null
        // "solo_" couples have no partner: the server refuses them too.
        if (!session.paired || session.coupleId.startsWith(SOLO_PREFIX)) return null
        return CinemaIdentity(session.coupleId, session.role, session.gender)
    }

    override suspend fun open(who: CinemaIdentity): AppResult<String> = safeApiCall {
        api.create(CreateBody(who.coupleId, who.role, CinemaLinks.avatarSlot(who.gender, who.role))).url
    }

    override suspend fun accept(who: CinemaIdentity, sessionId: String): AppResult<String> = safeApiCall {
        api.accept(sessionId, WhoBody(who.coupleId, who.role)).url
    }

    override suspend fun pendingInvite(who: CinemaIdentity): AppResult<CinemaInvite?> =
        safeApiCall { api.pending(who.coupleId, who.role).invite }.map { it?.toDomain() }

    override suspend fun decline(who: CinemaIdentity, sessionId: String): AppResult<Unit> =
        safeApiCall { api.decline(sessionId, WhoBody(who.coupleId, who.role)) }.map { }

    private fun InviteDto.toDomain() = CinemaInvite(id = id, hostName = hostName.orEmpty(), movie = movie)

    private companion object {
        const val SOLO_PREFIX = "solo_"
    }
}
