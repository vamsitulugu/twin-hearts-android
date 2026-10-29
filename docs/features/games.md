# Feature Audit — Games (hub + Truth or Dare · Ludo · Tic Tac Toe · Memory Match)

**Source screen (web app, read-only reference):** `us-app/public/games.html`

**What the web app does:** a hub (two rank cards, 8 game cards, favourites, "partner is playing") that opens each game on top of it. Games sync live through Supabase tables `games_profiles` / `games_shared` and a realtime channel, called straight from the page.

**Why Android does it differently (README R-60):** the app may not call Supabase. There is no games route on the server, so Games saves into the couple's shared state document (`routes/data.js`) under keys that start with `gm_`. No backend change.

**APIs used:**
- `GET /api/data/state/{coupleId}` — read everything (polled every 2 s inside a game, every 6 s on the hub, only while the screen is visible)
- `POST /api/data/state` — merge top-level keys
- `GET /api/profile/{userId}` and `GET /api/profile/{userId}/partner` — profile photos (once)

**Keys written (all in `app_state.state`):**
| Key | Written by | Content |
|---|---|---|
| `gm_profile_u1` / `gm_profile_u2` | that person only | XP, coins, wins, favourites, last-opened times, streak |
| `gm_presence_u1` / `gm_presence_u2` | that person only | which game they are in + heartbeat time (online = under 25 s old) |
| `gm_tod`, `gm_ludo`, `gm_ttt`, `gm_memory` | both | the whole game, wrapped as `{rev, by, data}` |

**How two phones stay in sync:** every move bumps `rev` and saves the game document. While open, each phone reads it and takes the other phone's copy when it has a higher `rev` (or the same `rev` from the other person). A save that fails is retried on the next poll.

**Screens / states:** Hub (Loading, Content, Error) · each game (Loading, Content, Error). No Empty state: a new couple simply starts fresh games.

**Rewards (same numbers as web):** Tic Tac Toe win +8 XP +5🪙 · Memory pair +6 XP +3🪙 · Ludo capture +5🪙, win +15 XP +10🪙 · Truth or Dare answered +10 XP +4🪙 (both of you), skip −5 XP + random penalty. Level = floor(√(xp/28)) + 1.

**Edge cases handled:** no session → Unauthorized state · offline → retry + one "connection problem" toast · old/odd saved game → starts a new one · both phones opening a new game at once → they converge on one copy · rotation / process death → state reloaded from the server · polling stops in background.

**Fixes over the web app:** Ludo's move log is stored as events, so it reads "You moved…" correctly on each phone (web showed the mover's "You" to both). Memory's win credit and result pop-up reach both phones (web only the one that flipped the last card).

**Not in this delivery (cards show a "Soon" badge and a short note):** Mind Reader, Draw & Guess, Don't Touch The Bomb, Picture Puzzle. Their web code is 300–460 lines each and they need their own screens (canvas, photo picker, shared images). Planned as the second Games delivery.

**Not ported, with reason:**
- In-game video call overlay (`GameVideo`) — Calls are their own feature (2.3).
- Web card photos (`/images/games/*.webp`) — not in the Android repo; cards use a theme-colour gradient and the emoji instead.
- Web progress (Supabase tables) is not shared with Android progress; they are separate stores until a games route exists on the server.

**Differences to check by eye against `docs/reference/`:** card art is gradient + emoji; the Ludo board is drawn with a Canvas (15×15 grid, same path and safe cells as web).

**Handoff row for README §12:** `Games (hub + 4 games) — 🟨 in progress — Phase 6 — second delivery: Mind Reader, Draw, Bomb, Puzzle.`
