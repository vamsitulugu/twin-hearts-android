# Twin Cinema (3D movie theatre) — feature audit

Old files read: `us-app/routes/cinema.js`, `us-app/public/cinema-invite.js`, `us-app/CINEMA_SETUP.md`,
`twin-cinemas/src/invite.js` (the link the cinema understands).
Opened from: Home → **Cinema** chip (host), side menu → Together → **Cinema**, or my partner's invite (dialog / push).

## The idea in plain words
The cinema is part of the Twin Hearts web app (`us-app/cinema`, built into `us-app/public/cinema`). Its built files are
**bundled inside the APK** (`app/src/main/assets/cinema/`) and shown full screen in a WebView. There is no cinema website to
load, no link that can go down, and the cinema is the same code the web app serves at `/cinema/`.
To update it: change the web cinema, then run `tools/sync-cinema.ps1` and rebuild the app (a cinema change is now a new APK).

How it fits together:
- `core/cinema/BundledCinema.kt` – the private address (`https://appassets.androidplatform.net/cinema/index.html`), turns the server's link into it (only the `#` room settings are kept), path/mime/range rules. Unit tested.
- `core/cinema/BundledCinemaServer.kt` – answers every request for that address from the APK's assets (with Range support). Used by both WebViews below.
- Standalone Cinema screen (`feature/cinema`) and the Watch Together theatre (`feature/movie/cinema`) both use it.
- The server still creates/accepts the room (`/api/cinema/...`); it only supplies the room settings. The cinema itself still talks to Supabase Realtime and the Render API over the internet.

## README exception (needs your OK, see SHARED_CHANGES_cinema.md → CHANGE REQUEST)
README R-01 and §14 say "no WebView screens". You asked for exactly this (cinema inside the app, not in a browser),
so this feature is the **one allowed exception**. `core/cinema` is a second Change Request (shared by two features, R-83). Nothing else in the app uses a WebView.

## Screens and states
| Screen | What it does | States |
|---|---|---|
| Cinema (WebView) | Full screen, sound without a tap, screen stays awake, bars hidden | loading, content, error with Retry |
| Leave cinema? dialog | Shown on the phone's Back button. **Leave** closes, **Stay** keeps watching | – |
| Invite dialog (any screen) | "<name> invited you", **Accept ♥** / **Not now** (partner side) | – |
| Home chip | "Cinema" next to Movie | – |
| Side menu row | Together → Cinema | – |

There is no "empty" state: a link exists or the call failed.

## API calls (read from `routes/cinema.js`, R-65)
| Action | Call | Answer | Errors |
|---|---|---|---|
| Host opens the cinema | `POST /api/cinema/create` `{coupleId, role, slot}` | `{ session, url }` | 400 not paired / missing data |
| Poll for an invite (every 6 s, app open) | `GET /api/cinema/pending/:coupleId?role=` | `{ invite: {id, hostName, movie} \| null }` | – |
| Accept | `POST /api/cinema/:id/accept` `{coupleId, role}` | `{ ok, url }` | 410 expired, 409 no longer open, 403 host |
| Not now | `POST /api/cinema/:id/decline` `{coupleId, role}` | `{ ok }` | 403 |
| Invite / Start / playback | done **inside** the cinema website (Supabase Realtime) | – | – |

Avatar slot (`slot`): girl = 1, boy = 2, unknown → user1 = 1, user2 = 2 (same rule as the web). The partner automatically gets the other avatar.

## How the WebView behaves
- JavaScript, DOM storage and no-tap autoplay are on (the cinema needs them). WebGL is the default.
- **Only the bundled theatre loads.** Any other page is blocked (navigation to anything but `appassets.androidplatform.net` is refused).
- **Leave:** the app adds `&b=https://twinhearts.invalid/` to the link. The cinema's Leave button goes to that address,
  the WebView sees it and closes the screen. `.invalid` can never be a real website. No change in the cinema repo is needed.
- **Choose movie file** works through `onShowFileChooser` (both phones pick the same file, as on the web).
- Full screen (swipe from the edge shows the bars for a moment) and the screen stays on. Rotation works (MainActivity already handles it).
- Needs the internet only for the room (Supabase/Render), not for the theatre files.
- The WebView is paused/resumed with the app and destroyed when the screen closes.
- Pull-to-refresh and ads are switched off on this screen (like Movie).

## Notifications
The server already sends `type: cinema_invite`. The app now knows it (Settings › Notifications shows "Cinema invites").
Tapping it opens Home; the invite watcher then asks the server and shows the Accept / Not now card straight away.

## Not ported (with reasons)
- `&lib=` movie library in the link: the cinema falls back to the server library (`GET /api/cinema/:id/library`) when it is missing. Can be added later.
- Voice/Video buttons in the lobby: they are buttons only in the cinema itself for now.
- Cinema search result in global Search: not asked for.

## Things only you can do
- Supabase: run `us-app/migrations/cinema_sessions.sql` (once).
- Render: deploy `us-app` with the CORS line for `https://appassets.androidplatform.net` (or set `ALLOWED_ORIGINS` on Render). Without it the bundled cinema cannot call the API.
- Cloudflare R2 bucket CORS: add `https://appassets.androidplatform.net` to `AllowedOrigins` (auto-share movie upload/stream).
- After every cinema change: `tools/sync-cinema.ps1`, then rebuild. The old Vercel `twin-cinemas` project is no longer used by the app.
