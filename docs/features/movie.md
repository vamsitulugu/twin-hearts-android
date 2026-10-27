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

## Not ported (with reason)
- **Cloud upload** (multipart to R2): large, needs resumable background work. Own session. Cloud movies uploaded from the web can already be watched.
- **Subtitles, reactions, sleep timer, mini voice/video call bar**: reactions/calls use the web "parent window" channel, not the API; calls are Phase 2.3.
- **Manual "Add" form and ⭐ Favorites filter/owner chips**: web stores favourite stars in browser storage, not in the API. Search online covers adding.
- **Resume position after closing the app**: the room keeps the position; the same file must be picked again (web behaves the same: "Select the same movie again to resume").

## Things to know
- `rating` in `movie_favorites` holds the TMDB score when a movie is added; a star tap overwrites it with 1–5 (same column the web uses).
- Not compiled in the Claude sandbox. Build in Android Studio.
