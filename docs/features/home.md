# Feature 2.1 — Home (audit + explanation)

Sources read: `public/index.html` (`#page-dashboard`, `sendTouch`, `sendMissYou`, `sendHug`, `acceptHug`, hug popup), `public/dashboard.js` (greeting, quotes, ConnCard), `public/dashboard-widgets.js` (all-pages groups, footer quotes), `routes/data.js` (shared state), `public/images/dashboard/*`.

## What Home has (top to bottom) and what is built
| Part | Built now? | Notes |
|---|---|---|
| Logo + "Twin Hearts", my avatar (opens profile) | Yes | Avatar = first letter of my name. Photo comes with Profile (2.6). |
| "Good evening, Name 👋" + daily quote (italic pink last word) | Yes | Same 7 quotes and the same day-of-year rule as the web. |
| Search bar + grid/menu button | Yes (look) | Tapping shows "This screen is built in a later step." |
| Chips: **Memories** (highlighted), Music, Movie, Meet Planner, Daily Routine | Yes (look) | v3: first chip now says "Memories" (the web wording; v1/v2 said "Camera"). Same note on tap. |
| **Stay Connected** card: picture, caption, 🤗 Hug / ❤️ Touch / 🥺 Miss You | **Yes, working** | See below. |
| Incoming hug: "Name is sending you a hug 🤗" + Accept / Later | Yes | |
| "All pages" groups (Us, Explore Together, My Space, Plans) + bottom quote | Yes | Our Profile opens its (stand-in) screen; the rest show the note. |
| **Coming up** row (birthdays, anniversaries, months/day milestones, calendar events, milestones; soonest first, max 8; empty card "Nothing planned yet") | **Yes, v2** | Read from the same shared state. Tapping an item opens Calendar / Love Counter, which show the "built in a later step" note until they exist. |
| **Mood card** (5 one-tap moods, today line, 7-day graph for both of you, date columns, legend, 🔥 streak) | **Yes, v2** | Saves MY mood only. Tap the same mood again to clear. Toast "Mood saved 😊". |
| **Mood Calendar** (full screen: You / partner tabs, 8 moods, month grid, stats) | **Yes, v2** | Opens from the Mood card (a full-screen dialog, so no new route). |
| Card photo menu (3 dots: Set my own image / Use default image) + **Card photos** editor | **Yes, v3** | See "v3" below. |

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

## v3 — Card photo menu, Card photos editor, card look (pin-to-pin)
Sources read: `public/dashboard-widgets.js` (photo editor, 3-dots menu), `public/dashboard.css` (`.dh-card`, `.dh-cap`, `.dh-act`, `.dh-shade`), `public/dashboard-widgets.css` (`.dh-edit`, `.dh-menu-pop`, `.dw-*`), `public/index.html` (`uploadMediaFile`, chips), `routes/media.js`, `routes/data.js`, and `docs/reference/images.pdf` page 2.

### What changed on the card (to match the screenshot)
- Caption ("🤗 Hug sent · 3d ago") is now a small dark pill at the **top-left** (it was bottom-centre).
- 3-dots round button at the **top-right**, menu with **Set my own image** / **Use default image** (the second is greyed out when the picture is not custom).
- Hug / Touch / Miss You buttons: rounded squares (16 dp, 46 dp high), white glass; the one that matches the newest signal turns solid white (web `is-active`).
- Two shades over the picture like the web (dark at the bottom, light at the top), card radius 24 dp, 1 dp white border.
- Chip label "Memories".

### Card photos editor (full screen, opens from "Set my own image")
Idle / Hug / Touch / Miss You tabs, 16:9 frame with "16:9" badge, **Change photo** (system photo picker, no permission), drag to move, pinch or slider to zoom (1x to 4x), rule-of-thirds guide, **Save**, **Use default picture**. Same limits as the web: image files only, max 30 MB, saved as a 1280x720 JPEG (quality 88).
Each tab keeps its own picked photo until you save (like the web).

### API calls (R-65, read from the old routes)
| Action | Call | Body | Answer / errors |
|---|---|---|---|
| Save photo, step 1 | `POST api/media/upload` (multipart) | `file` = JPEG `dashboard-<slot>.jpg`, `coupleId` | `{url, path}`. 400 "coupleId required", 413 too large, 415 wrong type, 500 storage error (`{error}`). |
| Save photo, step 2 | `POST api/data/state` | `state: { dashImgs: { ...old, <slot>: {url, path} } }`, `senderRole`, `myName` | `{ok: true}`. The server merges only top-level keys, so the whole `dashImgs` is sent. |
| Save photo, step 3 | `DELETE api/media/delete` | `{path, coupleId}` of the OLD file | `{ok: true}`. Best effort: if it fails we ignore it (an unused file stays in storage). 403 if path is not under the couple's folder. |
| Use default | `POST api/data/state` then `DELETE api/media/delete` | `dashImgs` without that slot | same as above |
| Read | `GET api/data/state/:coupleId` (already used) | | `dashImgs.{idle,hug,touch,miss}.{url,path}` |
Slot names saved on the server: `idle`, `hug`, `touch`, `miss` (same as the web, so both apps show the same pictures).

### States
Card: the built-in picture shows while a custom one loads or if it fails to load (never a blank card, R-24). Editor: picture / cropping / saving (buttons disabled, "Saving…") / green "Photo saved 💕" / red error (server wording or "Please choose an image", "That photo is too large (max 30 MB)", "Couldn't open that image", "Could not save the photo").

### Not ported (and why)
- Mouse-wheel zoom, Esc key, slide-up animation of the web overlay: no meaning on a phone.
- A picked-but-unsaved photo is lost if Android kills the app in the background (a bitmap cannot be stored in saved state). Saved pictures are not affected.

### Risks to know
- Photos are shrunk to at most 3072 px on the long side before cropping (saves memory on small phones). A very strong zoom on a huge photo is therefore slightly softer than the web.
- Not compiled here. Not tried against the real server.

### Tests (v3)
`CropMathTest` (8), `CardPhotoMapperTest` (5), `CardPhotoEditorViewModelTest` (11), `HomeViewModelTest` (+3).
