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
| **Coming up** row (birthdays, anniversaries, months/day milestones, calendar events, milestones; soonest first, max 8; empty card "Nothing planned yet") | **Yes, v2** | Read from the same shared state. Tapping an item opens Calendar / Love Counter, which show the "built in a later step" note until they exist. |
| **Mood card** (5 one-tap moods, today line, 7-day graph for both of you, date columns, legend, 🔥 streak) | **Yes, v2** | Saves MY mood only. Tap the same mood again to clear. Toast "Mood saved 😊". |
| **Mood Calendar** (full screen: You / partner tabs, 8 moods, month grid, stats) | **Yes, v2** | Opens from the Mood card (a full-screen dialog, so no new route). |
| Card photo menu (3 dots: change card picture / use default) | **No** | Needs image picking, cropping and the upload endpoint. Not ported yet; planned as its own step. |

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

## v2 — Coming up + Mood (API details, R-65)
Same two calls as before, nothing new on the server:
- `GET api/data/state/:coupleId` → we now also read `myBday`, `ptBday`, `anniversary`, `events[{date,title,cat,time}]`, `milestones[{date,title,type}]`, `profile.u1|u2.moods{ "YYYY-MM-DD": "Happy" }`.
- `POST api/data/state` for a mood: `profile.<u1|u2>` = my whole entry with `moods` changed (the server replaces the entry as a whole and only protects the other person's entry), `wellness` = whole object with that day's `mood` set to "😊 Happy" (or removed), and `selectedMood` when the day is today. Same fields the web writes.
Errors: a failed save shows the red box and the real value is read back.

## Differences from the web (on purpose)
- Future days in the Mood Calendar are dimmed and cannot be tapped (the web only dims them). Next-month arrow is disabled past the current month, as on the web.
- Mood names come from strings.xml; the value saved on the server is always the English key ("Happy"), so web and app agree.
- Birthdays use `myBday` / `ptBday` exactly as the web (they are shared fields in the couple state).

## Not verified
Not compiled here. Colours of the 8 moods are the web hex values inside HomeWidgets.kt (see SHARED_CHANGES).

## Tests (v2)
`HomeStateMapperTest` (7), `HomeViewModelTest` (6).
