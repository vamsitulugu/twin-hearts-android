# Feature 2.1 — Home (audit + explanation)

Sources read: `public/index.html` (`#page-dashboard`, `sendTouch`, `sendMissYou`, `sendHug`, `acceptHug`, hug popup), `public/dashboard.js` (greeting, quotes, ConnCard), `public/dashboard-widgets.js` (all-pages groups, footer quotes), `routes/data.js` (shared state), `public/images/dashboard/*`.

## What Home has (top to bottom) and what is built
| Part | Built now? | Notes |
|---|---|---|
| Logo + "Twin Hearts", my avatar (opens profile) | Yes | Avatar = first letter of my name. Photo comes with Profile (2.6). |
| "Good evening, Name 👋" + daily quote (italic pink last word) | Yes | Same 7 quotes and the same day-of-year rule as the web. |
| Search bar + grid/menu button | Yes (look) | Tapping shows "This screen is built in a later step." |
| Chips: Camera (highlighted), Music, Movie, Meet Planner, Daily Routine | Yes (look) | Same note on tap. |
| **Stay Connected** card: picture, caption, 🤗 Hug / ❤️ Touch / 🥺 Miss You | **Yes, working** | See below. |
| Incoming hug: "Name is sending you a hug 🤗" + Accept / Later | Yes | |
| "All pages" groups (Us, Explore Together, My Space, Plans) + bottom quote | Yes | Our Profile opens its (stand-in) screen; the rest show the note. |
| "Coming up" events, mood card | **No** | Need Calendar and mood data (features 3.x). |
| Card photo menu (3 dots: change card picture) | **No** | Later, with image picking. |

## How Hug / Touch / Miss You work (same data as the web)
They live in the couple's shared state on the server (`GET /api/data/state/:coupleId`, `POST /api/data/state`).
The server merges top-level keys, so the app sends only the keys it changes:
- Touch → `touch: {from, ts}`
- Miss You → `missYou: {id, from, ts, status:"sent"}` and `missCounts` (+1 for me)
- Hug → `hug: {id, from, ts, status:"pending"}`; partner accepts → `status:"accepted"`, `acceptedTs`, `acceptedBy` and `hugStats` (+1)
The card shows the newest of touch / missYou / accepted hug (or my own pending hug), and picks the matching picture (idle / hug / touch / miss). Looks for news every 8 s while Home is visible. The caption ("🤗 Hug received · 5m ago") refreshes every 30 s.
Buttons are disabled until a partner is connected; a "Connect with your partner ❤️" card opens Pairing instead.

## States
Loading is silent (the card shows the idle picture first). If a refresh fails the screen keeps what it has. Send errors show in a red box (server wording); good news in a green box that disappears after 3.5 s (replaces the web's toast).

## Not ported yet (and why)
- Vibration, sound, notification and "petals" animation when my partner sends something (web: vibrate 10 s on touch). Needs permissions and Firebase — Phase 2.5.
- "Hug shared" popup and XP/level points (`addXP`): belong with Level (3.x).
- Realtime push of the state: the app checks every 8 s instead.

## Risk to know
The state is one big JSON document and the app reads all of it every 8 s. If it grows large (many photos saved inside it) this wastes data. A small server route that returns only `touch`, `hug`, `missYou` would fix that (optional backend change, not needed today).

## New wording (not in the web)
"🤗 You and NAME hugged 💕" (after accepting a hug). Everything else is copied.

## Tests
`LoveCardMapperTest` (6), `HomeViewModelTest` (4).
