package com.twinhearts.app.feature.cinema.data

import com.twinhearts.app.core.common.AppError
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Why this file exists: the one place the Cinema feature talks to the server and maps errors (R-40, R-46). */
interface CinemaRepository {
    /** Who I am for the cinema, or null when nobody is signed in or the couple is not paired yet. */
    suspend fun identity(): CinemaIdentity?

    /**
     * Opens the couple's cinema room and returns the link to load in the WebView. Host or guest is decided by the server
     * (whoever is first is the host; the other joins automatically). [invite] also invites my partner in the same request.
     */
    suspend fun open(who: CinemaIdentity, invite: Boolean = false): AppResult<String>

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

    override suspend fun open(who: CinemaIdentity, invite: Boolean): AppResult<String> = retrying {
        safeApiCall { api.create(CreateBody(who.coupleId, who.role, CinemaLinks.avatarSlot(who.gender, who.role), invite)).url }
    }

    override suspend fun accept(who: CinemaIdentity, sessionId: String): AppResult<String> = retrying {
        safeApiCall { api.accept(sessionId, WhoBody(who.coupleId, who.role)).url }
    }

    /**
     * The server (Render) sleeps and a first call can time out or answer 502-504 while it wakes up: that used to show as
     * "the invite did not go". Both calls are safe to repeat (create re-uses the open room, accept is idempotent),
     * so a network problem or a waking server is tried again; every other answer (expired, not found...) is final.
     */
    private suspend fun <T> retrying(call: suspend () -> AppResult<T>): AppResult<T> {
        var result = call()
        var attempt = 1
        while (attempt < MAX_TRIES && result.isTransient()) {
            delay(RETRY_DELAY_MS * attempt)
            result = call()
            attempt++
        }
        return result
    }

    private fun AppResult<*>.isTransient(): Boolean {
        val error = (this as? AppResult.Failure)?.error ?: return false
        return error is AppError.Network || (error is AppError.Server && error.code in 500..504)
    }

    override suspend fun pendingInvite(who: CinemaIdentity): AppResult<CinemaInvite?> =
        safeApiCall { api.pending(who.coupleId, who.role).invite }.map { it?.toDomain() }

    override suspend fun decline(who: CinemaIdentity, sessionId: String): AppResult<Unit> =
        safeApiCall { api.decline(sessionId, WhoBody(who.coupleId, who.role)) }.map { }

    private fun InviteDto.toDomain() = CinemaInvite(id = id, hostName = hostName.orEmpty(), movie = movie)

    private companion object {
        const val SOLO_PREFIX = "solo_"
        const val MAX_TRIES = 3
        const val RETRY_DELAY_MS = 1_500L
    }
}
