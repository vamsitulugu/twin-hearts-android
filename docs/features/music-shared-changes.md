# Music — changes outside `feature/music/` (R-30, R-31)

All of these are already applied in the zip. Listed so you can review them.

| File | Change |
|---|---|
| `gradle/libs.versions.toml` | + `androidx-media3-session` (uses the existing `media3 = "1.4.1"`; exoplayer and ui were added by Movie) |
| `app/build.gradle.kts` | + `implementation(libs.androidx.media3.session)` |
| `core/navigation/Routes.kt` | + `@Serializable data object Music` |
| `core/navigation/FeatureGraphs.kt` | + `musicGraph(navController)` and its import |
| `feature/home/HomeRoute.kt` | + `HomeShortcut.Music -> navController.navigate(Routes.Music)` |
| `AndroidManifest.xml` | + permission `FOREGROUND_SERVICE_MEDIA_PLAYBACK`; + `<service MusicPlaybackService foregroundServiceType="mediaPlayback">` (RECORD_AUDIO, FOREGROUND_SERVICE and POST_NOTIFICATIONS already existed) |
| `res/xml/chat_file_paths.xml` | + `<cache-path name="music_capture" path="music_capture/" />` (video message from the camera app) |
| `res/values/strings_music.xml` | new, all Music words |

No database (Room) entities. No backend changes. The server needs `migrations/music_v2.sql` applied for playlists (already part of the web app).
