package com.twinhearts.app.feature.movie.data.remote

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Why this file exists: the only door from Watch Together to the movie routes (R-40, R-60). */
interface MovieApi {
    @GET("api/movie/time") suspend fun time(): TimeDto

    /** The server answers the literal `null` when the room does not exist, so we read raw text. */
    @GET("api/movie/{c}") suspend fun room(@Path("c") c: String): ResponseBody

    @POST("api/movie/{c}/movie") suspend fun pickMovie(@Path("c") c: String, @Body b: PickMovieBody): RoomDto
    @POST("api/movie/{c}/ready") suspend fun ready(@Path("c") c: String, @Body b: ReadyBody): RoomDto
    @POST("api/movie/{c}/request-start") suspend fun requestStart(@Path("c") c: String, @Body b: RoleBody): RoomDto
    @POST("api/movie/{c}/cancel-start") suspend fun cancelStart(@Path("c") c: String, @Body b: RoleBody): RoomDto
    @POST("api/movie/{c}/accept-start") suspend fun acceptStart(@Path("c") c: String, @Body b: AcceptBody): RoomDto
    @PATCH("api/movie/{c}/state") suspend fun state(@Path("c") c: String, @Body b: StateBody): RoomDto
    @POST("api/movie/{c}/end") suspend fun end(@Path("c") c: String, @Body b: EndBody): RoomDto
    @POST("api/movie/{c}/reset") suspend fun reset(@Path("c") c: String): OkDto

    @POST("api/movie/{c}/chat") suspend fun sendChat(@Path("c") c: String, @Body b: ChatBody): ChatMessageDto
    @GET("api/movie/{c}/chat")
    suspend fun chat(
        @Path("c") c: String, @Query("sessionKey") sessionKey: String,
        @Query("after") after: String? = null, @Query("limit") limit: Int = 200,
    ): List<ChatMessageDto>

    @GET("api/movie/{c}/history") suspend fun history(@Path("c") c: String): List<HistoryDto>
    @DELETE("api/movie/{c}/history/{id}") suspend fun deleteHistory(@Path("c") c: String, @Path("id") id: String): OkDto
    @DELETE("api/movie/{c}/history") suspend fun clearHistory(@Path("c") c: String): OkDto

    // ── Favorites / library list (TMDB-backed) ──
    @GET("api/movie-fav") suspend fun favorites(@Query("coupleId") coupleId: String): FavoritesDto
    @POST("api/movie-fav") suspend fun addFavorite(@Body b: AddFavoriteBody): FavoriteOneDto
    @PATCH("api/movie-fav/{id}") suspend fun updateFavorite(@Path("id") id: String, @Body b: UpdateFavoriteBody): FavoriteOneDto
    @DELETE("api/movie-fav/{id}") suspend fun deleteFavorite(@Path("id") id: String): OkDto
    @GET("api/movie-search/search") suspend fun search(@Query("q") q: String): SearchDto
    @GET("api/movie-search/{tmdbId}/details") suspend fun details(@Path("tmdbId") id: String): DetailsDto

    // ── Shared cloud storage (these routes need the x-user-id header) ──
    @GET("api/movie-library/{c}") suspend fun library(@Path("c") c: String, @Header("x-user-id") uid: String): LibraryDto
    @GET("api/movie-library/{c}/{m}/url")
    suspend fun playbackUrl(@Path("c") c: String, @Path("m") m: String, @Header("x-user-id") uid: String): UrlDto
    @POST("api/movie-library/{c}/{m}/select")
    suspend fun selectShared(@Path("c") c: String, @Path("m") m: String, @Header("x-user-id") uid: String): RoomDto
    @DELETE("api/movie-library/{c}/{m}")
    suspend fun deleteShared(@Path("c") c: String, @Path("m") m: String, @Header("x-user-id") uid: String): OkDto

    // ── Cloud upload: start, sign part links, finish or cancel (the bytes go straight to storage) ──
    @POST("api/movie-library/{c}/uploads")
    suspend fun startUpload(@Path("c") c: String, @Header("x-user-id") uid: String, @Body b: StartUploadBody): StartUploadDto
    @POST("api/movie-library/{c}/uploads/{m}/parts")
    suspend fun signParts(@Path("c") c: String, @Path("m") m: String, @Header("x-user-id") uid: String, @Body b: PartsBody): PartsDto
    @POST("api/movie-library/{c}/uploads/{m}/complete")
    suspend fun completeUpload(@Path("c") c: String, @Path("m") m: String, @Header("x-user-id") uid: String, @Body b: CompleteBody): SharedMovieDto
    @POST("api/movie-library/{c}/uploads/{m}/abort")
    suspend fun abortUpload(@Path("c") c: String, @Path("m") m: String, @Header("x-user-id") uid: String): OkDto

    // ── Reactions: short-lived emoji (server change needed, see backend-changes/movie) ──
    @POST("api/movie/{c}/reaction") suspend fun sendReaction(@Path("c") c: String, @Body b: ReactionBody): OkDto
    @GET("api/movie/{c}/reactions") suspend fun reactions(@Path("c") c: String, @Query("after") after: Long): List<ReactionDto>
}
