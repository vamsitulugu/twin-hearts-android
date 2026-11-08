# Twin Cinema (3D movie theatre) — feature audit

Old files read: `us-app/routes/cinema.js`, `us-app/public/cinema-invite.js`, `us-app/CINEMA_SETUP.md`,
`twin-cinemas/src/invite.js` (the link the cinema understands).
Opened from: Home → **Cinema** chip (host), side menu → Together → **Cinema**, or my partner's invite (dialog / push).

## The idea in plain words
The cinema is a separate website (`twin-cinemas.vercel.app`). The app does **not** copy or rebuild it.
It shows that website full screen inside a WebView, so every cinema change is just a Vercel deploy, never a new APK.

## README exception (needs your OK, see SHARED_CHANGES_cinema.md → CHANGE REQUEST)
README R-01 and §14 say "no WebView screens". You asked for exactly this (cinema inside the app, not in a browser),
so this feature is the **one allowed exception**. It is limited to this feature; nothing else in the app uses a WebView.

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
- **Only the cinema's own website loads.** Any other page is blocked. Plain `http` is never loaded.
- **Leave:** the app adds `&b=https://twinhearts.invalid/` to the link. The cinema's Leave button goes to that address,
  the WebView sees it and closes the screen. `.invalid` can never be a real website. No change in the cinema repo is needed.
- **Choose movie file** works through `onShowFileChooser` (both phones pick the same file, as on the web).
- Full screen (swipe from the edge shows the bars for a moment) and the screen stays on. Rotation works (MainActivity already handles it).
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
- Render: deploy the current `us-app` (it already has `routes/cinema.js`).
- Vercel → twin-cinemas → set `VITE_SUPABASE_URL` and `VITE_SUPABASE_ANON_KEY`, then redeploy. Without them the cinema runs in "local test mode" and two phones can never meet.
