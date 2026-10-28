package com.twinhearts.app.feature.music.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Why this file exists: the only door from Music to the old Express server (R-40, R-60).
 * Every path below was read from routes/music.js, routes/recordings.js and routes/lyrics.js (R-65).
 */
interface MusicApi {

    // ── songs (routes/music.js) ──

    /** All songs the viewer may see. Lyrics bodies are left out (use [song] for those). */
    @GET("api/music/{coupleId}")
    suspend fun songs(@Path("coupleId") coupleId: String, @Query("viewer") viewer: String): List<SongDto>

    /** One song with its lyrics text. */
    @GET("api/music/song/{id}")
    suspend fun song(
        @Path("id") id: String,
        @Query("coupleId") coupleId: String,
        @Query("viewer") viewer: String,
    ): SongDto

    @POST("api/music")
    suspend fun addSong(@Body body: AddSongBody): SongDto

    /** Edit details (uploader only) or save lyrics (either partner). */
    @PATCH("api/music/{id}")
    suspend fun editSong(@Path("id") id: String, @Body body: EditSongBody): OkDto

    @PUT("api/music/{id}/like")
    suspend fun like(@Path("id") id: String, @Body body: LikeBody): OkDto

    @POST("api/music/{id}/play")
    suspend fun recordPlay(@Path("id") id: String, @Body body: ViewerBody): OkDto

    /** DELETE with a JSON body: the server reads `coupleId` and `viewer` from it. */
    @HTTP(method = "DELETE", path = "api/music/{id}", hasBody = true)
    suspend fun deleteSong(@Path("id") id: String, @Body body: ViewerBody): OkDto

    /** Deezer search through our server. */
    @GET("api/music/search")
    suspend fun searchOnline(@Query("q") query: String): OnlineSearchResponse

    // ── playlists ──

    @GET("api/music/{coupleId}/playlists")
    suspend fun playlists(@Path("coupleId") coupleId: String): PlaylistsResponse

    @POST("api/music/playlists")
    suspend fun createPlaylist(@Body body: CreatePlaylistBody): PlaylistDto

    @PATCH("api/music/playlists/{pid}")
    suspend fun renamePlaylist(@Path("pid") id: String, @Body body: RenamePlaylistBody): OkDto

    @HTTP(method = "DELETE", path = "api/music/playlists/{pid}", hasBody = true)
    suspend fun deletePlaylist(@Path("pid") id: String, @Body body: CoupleBody): OkDto

    @POST("api/music/playlists/{pid}/songs")
    suspend fun addToPlaylist(@Path("pid") id: String, @Body body: PlaylistSongsBody): OkDto

    @HTTP(method = "DELETE", path = "api/music/playlists/{pid}/songs/{songId}", hasBody = true)
    suspend fun removeFromPlaylist(
        @Path("pid") id: String,
        @Path("songId") songId: String,
        @Body body: CoupleBody,
    ): OkDto

    // ── lyrics (routes/lyrics.js) ──

    /** A pure cache read: never contacts a lyrics provider. */
    @GET("api/lyrics/status/{songId}")
    suspend fun lyricsStatus(@Path("songId") songId: String): LyricsStatusDto

    /**
     * Asks the server to look lyrics up and save them on the song. The web app calls this only right
     * after a song is saved, never while playing (routes/lyrics.js). The server waits 24 h before
     * searching again for a song that had no match.
     */
    @POST("api/lyrics/auto-fetch")
    suspend fun autoFetchLyrics(@Body body: AutoFetchBody): AutoFetchDto

    // ── recordings (routes/recordings.js) ──

    @GET("api/recordings/{coupleId}")
    suspend fun recordings(@Path("coupleId") coupleId: String): List<RecordingDto>

    @POST("api/recordings")
    suspend fun createRecording(@Body body: CreateRecordingBody): RecordingDto

    @PATCH("api/recordings/{id}")
    suspend fun patchRecording(@Path("id") id: String, @Body body: RecordingPatchBody): OkDto

    @HTTP(method = "DELETE", path = "api/recordings/{id}", hasBody = true)
    suspend fun deleteRecording(@Path("id") id: String, @Body body: RecordingDeleteBody): OkDto
}

/**
 * File uploads (routes/media.js). A separate interface so Hilt can give it an HTTP client with
 * longer timeouts: a song on a slow phone connection takes longer than the normal 90 seconds.
 */
interface MusicUploadApi {

    @Multipart
    @POST("api/media/upload-audio")
    suspend fun uploadAudio(
        @Part file: MultipartBody.Part,
        @Part("coupleId") coupleId: RequestBody,
    ): UploadDto

    @Multipart
    @POST("api/media/upload-cover")
    suspend fun uploadCover(
        @Part file: MultipartBody.Part,
        @Part("coupleId") coupleId: RequestBody,
    ): UploadDto

    /** Voice notes, karaoke takes and videos for "My Recordings". */
    @Multipart
    @POST("api/media/upload-recording")
    suspend fun uploadRecording(
        @Part file: MultipartBody.Part,
        @Part("coupleId") coupleId: RequestBody,
        @Part("trackTitle") trackTitle: RequestBody,
        @Part("mediaType") mediaType: RequestBody,
    ): UploadDto
}
