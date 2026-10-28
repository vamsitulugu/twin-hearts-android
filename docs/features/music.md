# Music (library, player, lyrics, karaoke, recordings) — feature audit (Phase 5.1 + 5.2)

Old web files read: `public/music.html`, `public/music/*.js` (core, library, nowplaying, recordings, karaoke), `routes/music.js`, `routes/recordings.js`, `routes/lyrics.js`, `routes/media.js`, `MUSIC.md`, `migrations/music_v2.sql`.
Reference screens: `docs/reference/images.pdf` (Music tab, My Recordings tab).
Opened from: Home → **Music** chip → `Routes.Music`.

## Screens and states
| Screen | What it does | States |
|---|---|---|
| Header | Back, logo, "Music", Search, Sync | – |
| Music tab | Hero ("Music brings us closer"), tiles (My Playlist · Partner · Featured · Karaoke · Liked), Recently Played, Quick Actions, Our Playlists | loading, content, **empty** (hero + "Your library is empty" + Add songs), error + Retry |
| Song lists | My Playlist, Partner's Playlist, Featured, Liked, Recently Played. Play / Shuffle, sort chips | empty text per list |
| Playlist | Play, Shuffle, Add songs, Rename, Delete, remove a song from the song menu | empty, content |
| Search | Songs + playlists (accent and case blind); empty box shows recent songs | idle, results, none |
| Search Online | Deezer search through the server, 30 s preview, "Add" to Featured | idle, loading, results, none, error |
| Song menu | Play, Play next, Add to queue, Like, Add to playlist, Sing along, Lyrics, Details, Share, Edit details (uploader), Delete (uploader) | – |
| Now Playing | Cover, seek bar, shuffle, previous, play/pause, next, repeat (off / all / one), like, lyrics, queue, sleep timer | buffering |
| Mini player | Always at the bottom while a song is loaded; tap to open Now Playing | – |
| Lock screen / notification | Media3 session: play, pause, next, previous, seek. Music keeps playing with the screen off | – |
| Lyrics | Timed (.lrc) lyrics highlight and scroll with the song; plain lyrics scroll. Paste lyrics or import a `.lrc` file | loading, content, empty, error |
| Karaoke list / Sing | Songs that have lyrics. Sing plays the song, scrolls lyrics and records the microphone, then saves a "karaoke" recording | no lyrics, singing |
| Hero slider | Swipeable "Suggested for you" covers (up to 12 songs with artwork, hearted first). Moves on every 4.5 s, waits 7 s after a touch. Tap plays | no artwork → the static banner |
| Made for Us | Smart mixes from real data: Our Mix, Loved by Both, Partner's Favourites, On Repeat (3+ songs played twice), New Together (last 2 weeks), Sing Along (3+ with lyrics). Opens as a song list | hidden when no mix qualifies |
| Music Settings | ⋮ → Music Settings. Playback (crossfade, find lyrics automatically, normalise volume, sound effects), Karaoke (vocal reduction), Library (add songs, sync), Storage (downloaded music, clear, delete all my recordings), Milestones (11, computed live) | – |
| Sound effects sheet | Presets (Flat, Studio, Bathroom, Stadium, Bass boost, Bright), sliders for Reverb, Echo, Bass, Treble, switches for Vocal reduction and Normalise, Reset | custom when a slider is moved |
| Offline download | Song menu → Download for offline / Remove download. Plays from the saved copy with no internet | downloading, ready, failed |
| Recordings search | Search box on the Recordings tab (title, accents and case ignored) | – |
| My Recordings tab | Audio / Video / Favorites, Record Audio, Record Video, Everyone / Mine / partner's, play, favorite, rename, delete | loading, content, empty, error |

## API calls (read from the old routes, R-65)
| Action | Call |
|---|---|
| Songs | `GET /api/music/:coupleId?viewer=` (no lyrics text) |
| One song + lyrics | `GET /api/music/song/:id?coupleId&viewer` |
| Add song | `POST /api/music` `{coupleId, uploadedBy, title, artist, audioUrl, coverUrl, durationSec, album, genre, year, visibility, source?}` |
| Edit details / save lyrics | `PATCH /api/music/:id` `{coupleId, viewer, title?, artist?, album?, genre?, visibility?, lyrics?, lyricsSource?}` |
| Like / count a play / delete | `PUT /api/music/:id/like`, `POST /api/music/:id/play`, `DELETE /api/music/:id` (JSON body `{coupleId, viewer}`) |
| Online search | `GET /api/music/search?q=` |
| Playlists | `GET /api/music/:coupleId/playlists` (`{available, playlists}`), `POST /api/music/playlists`, `PATCH/DELETE /api/music/playlists/:pid`, `POST /api/music/playlists/:pid/songs`, `DELETE /api/music/playlists/:pid/songs/:songId` |
| Find lyrics after adding a song | `POST /api/lyrics/auto-fetch` `{songId, coupleId, title, artist, album, durationSec}` (called only right after a song is saved, like the web import worker, never during playback; the server waits 24 h before searching again for a miss) |
| Cached lyrics | `GET /api/lyrics/status/:songId` (cache read only, never contacts a provider) |
| Upload song / artwork | `POST /api/media/upload-audio`, `POST /api/media/upload-cover` (multipart `file`, `coupleId`) |
| Recordings | `GET /api/recordings/:coupleId`, `POST /api/recordings`, `PATCH /api/recordings/:id`, `DELETE /api/recordings/:id`; file via `POST /api/media/upload-recording` (`file`, `coupleId`, `trackTitle`, `mediaType`) |

Errors: server sentences are shown as they are (R-46). If playlists are not set up on the server (`migrations/music_v2.sql` not applied) the page shows the same hint as the web and every song still works.

## Rules followed
- One player for the whole app (`MusicPlayerController`) shared with `MusicPlaybackService`; the Screen never touches ExoPlayer (R-42).
- Large uploads use their own HTTP client (5 min timeout); the song is streamed from the phone, never loaded into memory. Limit 60 MB, same as the server.
- Song tags (title, artist, album, genre, year, artwork) are read on the phone with `MediaMetadataRetriever`, like the web's metadata service.
- Permissions are asked only when needed, with a reason first: microphone (record / sing), notifications (first time music plays, Android 13+). Video recording opens the phone's camera app, so no camera permission is added.
- Recordings and online previews are played through the same player but are never counted as listens.

## Rules followed (5.2 additions)
- Sound effects run **on the phone** in a small audio processor (`SoundChain`, plain Kotlin, unit tested) placed in ExoPlayer's audio path. It copies the web graph: vocal reduction (L−R above 150 Hz, bass kept), bass shelf 200 Hz, treble shelf 4.5 kHz, compressor (−24 dB, 4:1, soft knee, ×1.25 make-up) for Normalise, reverb and echo (0.28 s, 34 % feedback). When everything is off the audio passes through untouched. It only touches 16-bit stereo.
- Crossfade is a **fade**: the end of a song fades out and the next fades in by the chosen 0–12 s. It is skipped for songs shorter than twice that time and on repeat-one.
- "Our Mix" keeps the same order all day (it does not reshuffle every time a listen is counted).
- Offline copies are saved in the app's private storage (`files/music_offline`), streamed to disk (never loaded into memory), removed on uninstall.
- Music settings are saved on the phone (DataStore), like the web keeps them in the browser.

## Not ported yet (nothing silently dropped)
| Web feature | Why / when |
|---|---|
| Sing Together (karaoke during a video call) | Needs the in-call screen (Phase 2.3 Calls). Solo karaoke is built. |
| True overlapping crossfade (two songs at once) | Needs a second player. The app fades out / fades in instead. |
| Search *inside* lyrics text | The server's song list does not include lyric text, so it cannot be searched without downloading every lyric. |
| Lyrics library (admin dashboard) | Admin-only screen on the web. |
| A "Search again" lyrics button | The server only looks lyrics up right after a song is saved; Paste or Import covers the rest. |
| Hamburger menu in the header | The app has no side drawer; the bottom bar and Home cover it. |
| Automatic offline download | Downloads are manual (song menu), so mobile data is never used without a tap. |

## Open items
- Not compiled in the sandbox (Android SDK / Google Maven are not reachable). First build may show small import or type fixes.
- Media3 `session` is new (`1.4.1`, same version as the Movie feature).
- The sound-effects processor uses Media3 `@UnstableApi` classes (`DefaultRenderersFactory.buildAudioSink`, `DefaultAudioSink.Builder`, `BaseAudioProcessor`). They are written for 1.4.1; if the first build says `buildAudioSink` "overrides nothing", the parameter list differs in your exact Media3 and only `MusicPlayerController.effectsRenderers()` needs adjusting.
- Reverb, echo, bass, treble, vocal reduction and normalise are tested as audio maths (`SoundChainTest`), but how they *sound* has to be judged on a real phone.
- The pushed pages (lists, lyrics, sing) reset if Android kills the app in the background; the library itself reloads.
