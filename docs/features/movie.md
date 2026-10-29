# Watch Together (movies) — feature audit (Phase 5)

Old web files read: `public/movie.html`, `movie.js`, `movie-collection.js`, `routes/routes-movie.js`, `movie-search.js`, `movie-favorites.js`, `movie-library.js`.
Opened from: Home → **Movie** chip → `Routes.Movie`.

## Screens and states
| Screen | What it does | States |
|---|---|---|
| Header | Back, "Watch Together", History button | – |
| Home tab | Banner (first movie) + rows: Watched Together, 🍿 Watching, Watchlist | loading, content, empty, error |
| Library tab | Search box, Total/Watched/Watchlist tiles, filter chips, poster grid, tap = details | all four |
| Library → Search online | TMDB search, tap a result → **Add to Watchlist** | idle, loading, results, none, failed |
| Genres tab | 12 genres (same list as web), tap = open Library filtered | – |
| Watch tab | **Direct Device Video** (pick a file) or **Cloud Storage** (pick a shared movie, usage bar) | loading, content, empty, error |
| Lobby | My movie + Change, match banner, Auto Sync, You/Partner chips, I'M READY, START TOGETHER, Waiting, Cancel Movie Together | – |
| Invite dialog | MOVIE NIGHT, ACCEPT / NOT NOW (partner side) | – |
| Countdown | 3 · 2 · 1, same start moment on both phones | – |
| Player | Media3 video, play/pause, ±10 s, seek bar, Sync, Message, Log, End, floating partner message | loading link, playing |
| History sheet | Watch History, delete one, Clear all | all four |

## API calls (read from the old routes)
| Action | Call |
|---|---|
| Server clock | `GET /api/movie/time` → `{now}` |
| Read room | `GET /api/movie/:coupleId` → room row or literal `null` |
| Pick local movie | `POST /api/movie/:c/movie` `{role,title,durationSec}` |
| Ready | `POST /api/movie/:c/ready` `{role,ready}` |
| Request / cancel / accept start | `POST .../request-start`, `.../cancel-start` `{role}`, `.../accept-start` `{role,countdownMs:4000}` |
| Play / pause / seek | `PATCH .../state` `{role,playing,positionSec,actionSeq,playbackRate}` (older `actionSeq` is ignored by the server) |
| End | `POST .../end` `{role,movieTitle,durationSec,completedPct,sessionKey,positionSec}` |
| Cancel movie | `POST .../reset` |
| Chat | `POST/GET .../chat` (`sessionKey` = room's `scheduled_start_at` text) |
| History | `GET .../history`, `DELETE .../history/:id`, `DELETE .../history` |
| List | `GET/POST /api/movie-fav`, `PATCH/DELETE /api/movie-fav/:id` |
| Online search | `GET /api/movie-search/search?q=`, `GET /api/movie-search/:tmdbId/details` (genre) |
| Cloud | `GET /api/movie-library/:c`, `GET .../:movieId/url`, `POST .../:movieId/select` — header `x-user-id` (the server checks it) |

Errors: server text such as "A movie session is already in progress." is shown as is (R-46). Cloud routes answer 503 "Shared cloud storage is not set up yet." when storage is off.

## How both phones stay together (R-66)
No websocket exists. The app **polls the room every 1.5 s** and chat every 2 s, only while the screen is visible. Both phones measure the server clock offset (best of 3 samples), so the countdown ends at the same moment. In the player: jump if 2 s or more apart, speed ±3 % if 0.5–2 s apart (same rules as web). The server keeps status `countdown` while watching, so the app decides "watching" from the shared clock, not from status.

## Added in v2 (the remaining web features)
| Feature | How it works in the app |
|---|---|
| Cloud upload | Watch → Cloud Storage → **Upload Movie**. `POST /uploads` → sign part links (`/parts`, 3 at a time) → `PUT` each 16 MB piece straight to storage → `POST /complete`. Each piece retries 5 times with a fresh link. **Cancel upload** calls `/abort`. |
| Cloud delete | 🗑 on each row, same confirm text as web. `DELETE /api/movie-library/:c/:movieId`. Unfinished uploads show "Incomplete upload" and can be deleted. |
| Subtitles | Player ⋮ → Subtitles: pick a `.srt` or `.vtt` from the phone (each phone loads its own, nothing uploaded). Media3 reads both directly. |
| Screen Fit | Fit / Fill / Zoom. |
| Picture in Picture | Android PiP window (manifest change). |
| Sleep Timer | Off → 15 → 30 → 45 → 60 min; pauses for both. |
| Keep Screen On | Switch, saved on the phone, default on; applies while playing. |
| Movie Info | Title, length, source, size (cloud), subtitles. |
| Reactions | ❤️ 😂 🥺 😱 😘 float up. **Needs `backend-changes/movie`** for the partner to see them. |
| Add Movie form | Library → Add Movie: name, year, genre, rating, status, owner. Saved to the shared list (`POST /api/movie-fav`, owner stored in `added_by`). |
| ⭐ Favorites + owner chips | Favorites kept on the phone (web keeps them in the browser). Owner chips: Everyone / Mine / Partner's / Together. "Your Favorites" row on Home. |
| Server messages | Texts like "A movie session is already in progress." are now shown as sent. |

## Not ported (with reason)
- **Poster upload, personal review, favourite scene in Add Movie, Edit Movie**: the server list has no field for them (`PATCH` only changes status and rating).
- **Upload while the app is closed**: runs while the app is alive; a background service is a later change. Screen stays on during upload.
- **Reactions web ↔ app**: the web page uses Supabase Realtime; app-to-app only.

## Things to know
- `rating` in `movie_favorites` holds the TMDB score when a movie is added; a star tap overwrites it with 1–5 (same column the web uses).
- Not compiled in the Claude sandbox. Build in Android Studio.

## Added in v3
- **UI matched to the web page:** tabs with icons, Home hero card (eyebrow, chips, Watch Together / View Details, ⋮), "See all" on rows, colourful genre cards (poster or the web gradient, emoji, "N Movies"), Library search + Add + Search Online buttons, five stat tiles (Total, Watched, Watchlist, Favorites, ⭐Avg), Direct Device / Cloud Storage buttons with icons, "Pick a movie from your phone" card, "YOUR MOVIES" label.
- **In-movie calls:** Voice and Video buttons in the player. The partner's video (or a voice pill with timer, mute, hang up) shows as a draggable window over the movie, using the app-wide call engine. The graph file (`MovieGraph.kt`) plugs it in through `MovieCallHooks`, so the movie code does not import Calls.

## Added in v4 (the last "not ported" items)
- **Add / Edit Movie, full form:** poster (picked from the gallery, shrunk to a JPEG and sent with `POST /api/media/upload`), name, year, genre, rating, status, owner, **personal review** (the movie's `overview`) and **favourite scene** (`scene`). Open a saved movie → **Edit**; the detail sheet now shows the scene.
- **Upload with the app closed:** a foreground service (`UploadService`) keeps the upload running when the app is closed or the screen is off, shows progress in a notification with a Cancel button, and posts "ready to watch together" when done. Android 13+ asks for the notification permission first; the upload runs even if you say no.
- **Reactions web ↔ app:** the server now relays app reactions to the web page's realtime channel, and the web page also posts its reactions to the server, so both directions work. Files in `backend-changes/movie/`.
- Server changes are needed for Edit, scene and reactions (see `backend-changes/movie/README.md`). Without them the app still runs; those extra fields are just not saved.
