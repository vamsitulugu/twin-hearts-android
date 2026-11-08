package com.twinhearts.app.feature.movie.cinema.data

import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.map
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.movie.cinema.domain.CinemaInvite
import com.twinhearts.app.feature.movie.cinema.domain.CinemaWho
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Why this file exists: the one place the cinema talks to the server and maps errors (R-40, R-46). */
interface CinemaRepository {
    /** null = not signed in, or not connected with a partner yet (the web app says "Connect with your partner first"). */
    suspend fun who(): CinemaWho?

    /** Opens a cinema session. With [inviteNow] the partner is invited straight away (3D MOVIE button); without it the host invites from the lobby. */
    suspend fun open(who: CinemaWho, movie: String, inviteNow: Boolean): AppResult<String>
    suspend fun pending(who: CinemaWho): AppResult<CinemaInvite?>
    suspend fun accept(who: CinemaWho, id: String): AppResult<String>
    suspend fun decline(who: CinemaWho, id: String): AppResult<Unit>
}

class CinemaRepositoryImpl @Inject constructor(
    private val api: CinemaApi,
    private val sessionStore: SessionStore,
) : CinemaRepository {

    override suspend fun who(): CinemaWho? = sessionStore.session.first()
        ?.takeIf { it.paired && !it.coupleId.startsWith("solo_") }
        ?.let { s ->
            // Same rule as the web app: girl = avatar 1, boy = avatar 2; unknown -> user1 = 1, user2 = 2.
            val slot = when (s.gender) {
                "male" -> 2
                "female" -> 1
                else -> if (s.role == "user1") 1 else 2
            }
            CinemaWho(s.coupleId, s.role, s.partnerName, slot)
        }

    override suspend fun open(who: CinemaWho, movie: String, inviteNow: Boolean): AppResult<String> = safeApiCall {
        val made = api.create(CinemaCreateBody(who.coupleId, who.role, who.slot, movie.trim().take(60)))
        // Like the web: if the invite itself fails the host can still invite again from the lobby.
        if (inviteNow) runCatching { api.invite(made.session.id, CinemaRoleBody(who.coupleId, who.role)) }
        made.url
    }

    override suspend fun pending(who: CinemaWho): AppResult<CinemaInvite?> =
        safeApiCall { api.pending(who.coupleId, who.role).invite }
            .map { dto -> dto?.let { CinemaInvite(it.id, it.hostName?.takeIf(String::isNotBlank) ?: who.partnerName, it.movie) } }

    override suspend fun accept(who: CinemaWho, id: String): AppResult<String> =
        safeApiCall { api.accept(id, CinemaRoleBody(who.coupleId, who.role)).url }

    override suspend fun decline(who: CinemaWho, id: String): AppResult<Unit> =
        safeApiCall { api.decline(id, CinemaRoleBody(who.coupleId, who.role)); Unit }
}
