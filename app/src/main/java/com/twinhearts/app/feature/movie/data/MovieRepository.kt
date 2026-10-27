package com.twinhearts.app.feature.movie.data

import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.movie.data.mapper.toDomain
import com.twinhearts.app.feature.movie.data.remote.AcceptBody
import com.twinhearts.app.feature.movie.data.remote.AddFavoriteBody
import com.twinhearts.app.feature.movie.data.remote.ChatBody
import com.twinhearts.app.feature.movie.data.remote.EndBody
import com.twinhearts.app.feature.movie.data.remote.MovieApi
import com.twinhearts.app.feature.movie.data.remote.PickMovieBody
import com.twinhearts.app.feature.movie.data.remote.ReactionBody
import com.twinhearts.app.feature.movie.data.remote.ReadyBody
import com.twinhearts.app.feature.movie.data.remote.RoleBody
import com.twinhearts.app.feature.movie.data.remote.RoomDto
import com.twinhearts.app.feature.movie.data.remote.StateBody
import com.twinhearts.app.feature.movie.data.remote.UpdateFavoriteBody
import com.twinhearts.app.feature.movie.domain.model.ChatMessage
import com.twinhearts.app.feature.movie.domain.model.CloudLibrary
import com.twinhearts.app.feature.movie.domain.model.HistoryItem
import com.twinhearts.app.feature.movie.domain.model.MovieIdentity
import com.twinhearts.app.feature.movie.domain.model.MovieItem
import com.twinhearts.app.feature.movie.domain.model.MovieReaction
import com.twinhearts.app.feature.movie.domain.model.WatchRoom
import com.twinhearts.app.feature.movie.domain.model.WatchStatus
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** Why this file exists: the one place Watch Together talks to the server and maps errors (R-40, R-46). */
interface MovieRepository {
    suspend fun identity(): MovieIdentity?

    /** Server clock minus phone clock, in ms. Needed so both phones start at the same moment. */
    suspend fun clockOffsetMs(): Long

    suspend fun room(who: MovieIdentity): AppResult<WatchRoom?>
    suspend fun pickMovie(who: MovieIdentity, title: String, durationSec: Int): AppResult<WatchRoom>
    suspend fun setReady(who: MovieIdentity, ready: Boolean): AppResult<WatchRoom>
    suspend fun requestStart(who: MovieIdentity): AppResult<WatchRoom>
    suspend fun cancelStart(who: MovieIdentity): AppResult<WatchRoom>
    suspend fun acceptStart(who: MovieIdentity): AppResult<WatchRoom>
    suspend fun sendState(who: MovieIdentity, playing: Boolean, positionSec: Double, seq: Long): AppResult<WatchRoom>
    suspend fun end(who: MovieIdentity, title: String?, durationSec: Int?, positionSec: Double, sessionKey: String?): AppResult<WatchRoom>
    suspend fun reset(who: MovieIdentity): AppResult<Unit>

    suspend fun sendChat(who: MovieIdentity, sessionKey: String, title: String?, text: String, positionSec: Double): AppResult<Unit>
    suspend fun chat(who: MovieIdentity, sessionKey: String, after: String?): AppResult<List<ChatMessage>>

    suspend fun history(who: MovieIdentity): AppResult<List<HistoryItem>>
    suspend fun deleteHistory(who: MovieIdentity, id: String): AppResult<Unit>
    suspend fun clearHistory(who: MovieIdentity): AppResult<Unit>

    suspend fun favorites(who: MovieIdentity): AppResult<List<MovieItem>>
    suspend fun addFavorite(who: MovieIdentity, movie: MovieItem): AppResult<MovieItem>
    suspend fun updateFavorite(id: String, status: WatchStatus?, rating: Double?): AppResult<Unit>
    suspend fun deleteFavorite(id: String): AppResult<Unit>
    suspend fun search(query: String): AppResult<List<MovieItem>>

    suspend fun library(who: MovieIdentity): AppResult<CloudLibrary>
    suspend fun selectShared(who: MovieIdentity, movieId: String): AppResult<WatchRoom>
    suspend fun playbackUrl(who: MovieIdentity, movieId: String): AppResult<String>
    suspend fun deleteShared(who: MovieIdentity, movieId: String): AppResult<Unit>

    suspend fun sendReaction(who: MovieIdentity, emoji: String): AppResult<Unit>
    suspend fun reactions(who: MovieIdentity, afterMs: Long): AppResult<List<MovieReaction>>
}

class MovieRepositoryImpl @Inject constructor(
    private val api: MovieApi,
    private val sessionStore: SessionStore,
    private val json: Json,
) : MovieRepository {

    override suspend fun identity(): MovieIdentity? = sessionStore.session.first()?.let {
        MovieIdentity(it.userId, it.coupleId, it.role, it.myName, it.partnerName)
    }

    override suspend fun clockOffsetMs(): Long {
        // Take a few samples and keep the one with the shortest round trip: it is the most accurate.
        var bestRtt = Long.MAX_VALUE
        var bestOffset = 0L
        repeat(3) {
            val t0 = System.currentTimeMillis()
            val now = runCatching { api.time().now }.getOrNull() ?: return@repeat
            val rtt = System.currentTimeMillis() - t0
            if (rtt < bestRtt) {
                bestRtt = rtt
                bestOffset = now + rtt / 2 - System.currentTimeMillis()
            }
        }
        return bestOffset
    }

    override suspend fun room(who: MovieIdentity): AppResult<WatchRoom?> = safeApiCall {
        val text = api.room(who.coupleId).string().trim()
        if (text.isEmpty() || text == "null") null else json.decodeFromString<RoomDto>(text).toDomain(who.role)
    }

    override suspend fun pickMovie(who: MovieIdentity, title: String, durationSec: Int) =
        safeApiCall { api.pickMovie(who.coupleId, PickMovieBody(who.role, title, durationSec)).toDomain(who.role) }

    override suspend fun setReady(who: MovieIdentity, ready: Boolean) =
        safeApiCall { api.ready(who.coupleId, ReadyBody(who.role, ready)).toDomain(who.role) }

    override suspend fun requestStart(who: MovieIdentity) =
        safeApiCall { api.requestStart(who.coupleId, RoleBody(who.role)).toDomain(who.role) }

    override suspend fun cancelStart(who: MovieIdentity) =
        safeApiCall { api.cancelStart(who.coupleId, RoleBody(who.role)).toDomain(who.role) }

    override suspend fun acceptStart(who: MovieIdentity) =
        safeApiCall { api.acceptStart(who.coupleId, AcceptBody(who.role, COUNTDOWN_MS)).toDomain(who.role) }

    override suspend fun sendState(who: MovieIdentity, playing: Boolean, positionSec: Double, seq: Long) =
        safeApiCall { api.state(who.coupleId, StateBody(who.role, playing, positionSec, seq)).toDomain(who.role) }

    override suspend fun end(who: MovieIdentity, title: String?, durationSec: Int?, positionSec: Double, sessionKey: String?) =
        safeApiCall {
            val pct = if (durationSec != null && durationSec > 0) ((positionSec / durationSec) * 100).toInt().coerceIn(0, 100) else 0
            api.end(who.coupleId, EndBody(who.role, title, durationSec, pct, sessionKey, positionSec)).toDomain(who.role)
        }

    override suspend fun reset(who: MovieIdentity) = safeApiCall { api.reset(who.coupleId); Unit }

    override suspend fun sendChat(who: MovieIdentity, sessionKey: String, title: String?, text: String, positionSec: Double) =
        safeApiCall { api.sendChat(who.coupleId, ChatBody(who.role, sessionKey, title, text, positionSec)); Unit }

    override suspend fun chat(who: MovieIdentity, sessionKey: String, after: String?) =
        safeApiCall { api.chat(who.coupleId, sessionKey, after).map { it.toDomain(who.role) } }

    override suspend fun history(who: MovieIdentity) =
        safeApiCall { api.history(who.coupleId).map { it.toDomain() } }

    override suspend fun deleteHistory(who: MovieIdentity, id: String) = safeApiCall { api.deleteHistory(who.coupleId, id); Unit }

    override suspend fun clearHistory(who: MovieIdentity) = safeApiCall { api.clearHistory(who.coupleId); Unit }

    override suspend fun favorites(who: MovieIdentity) =
        safeApiCall { api.favorites(who.coupleId).favorites.map { it.toDomain() } }

    override suspend fun addFavorite(who: MovieIdentity, movie: MovieItem) = safeApiCall {
        // Search results have no genre; the details route fills it so the Genres tab works.
        val genre = movie.genre?.takeIf { it.isNotBlank() }
            ?: movie.tmdbId?.let { runCatching { api.details(it).genre }.getOrNull() }
        val body = AddFavoriteBody(
            who.coupleId, movie.tmdbId, movie.title, movie.year, genre, movie.posterUrl, movie.backdropUrl,
            movie.overview, movie.rating, movie.status.api, movie.addedBy ?: who.role,
        )
        api.addFavorite(body).favorite?.toDomain() ?: movie
    }

    override suspend fun updateFavorite(id: String, status: WatchStatus?, rating: Double?) =
        safeApiCall { api.updateFavorite(id, UpdateFavoriteBody(status?.api, rating)); Unit }

    override suspend fun deleteFavorite(id: String) = safeApiCall { api.deleteFavorite(id); Unit }

    override suspend fun search(query: String) = safeApiCall { api.search(query).results.map { it.toDomain() } }

    override suspend fun library(who: MovieIdentity) = safeApiCall { api.library(who.coupleId, who.userId).toDomain() }

    override suspend fun selectShared(who: MovieIdentity, movieId: String) =
        safeApiCall { api.selectShared(who.coupleId, movieId, who.userId).toDomain(who.role) }

    override suspend fun playbackUrl(who: MovieIdentity, movieId: String) =
        safeApiCall { api.playbackUrl(who.coupleId, movieId, who.userId).url }

    override suspend fun deleteShared(who: MovieIdentity, movieId: String) =
        safeApiCall { api.deleteShared(who.coupleId, movieId, who.userId); Unit }

    override suspend fun sendReaction(who: MovieIdentity, emoji: String) =
        safeApiCall { api.sendReaction(who.coupleId, ReactionBody(who.role, emoji)); Unit }

    override suspend fun reactions(who: MovieIdentity, afterMs: Long) =
        safeApiCall { api.reactions(who.coupleId, afterMs).map { it.toDomain(who.role) } }

    private companion object {
        const val COUNTDOWN_MS = 4000
    }
}
