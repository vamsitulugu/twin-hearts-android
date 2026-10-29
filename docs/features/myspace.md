# Feature: My Space

Native Compose page; the web app (`us-app`, `#page-myspace`) and `docs/reference/images.pdf` page 43 were the reference. Entry: Home "My Space" tile → `Routes.MySpace`. Code: `feature/myspace/`. The bottom bar stays visible (as in the reference).

## What it has
Banner, one rounded tab bar with five tabs, and the same data as the web:
- **Journal** — mood chips (7), write box, "Only Me / Both See / Partner Only" dropdown, Save Entry. My entries, plus a "Partner's Entries" card with what my partner shared (private ones are never shown). Empty state has "Write First Entry" (focuses the box).
- **Habits** — today's ring (7-day consistency), tip, 12-week heatmap, list with tick circle + streak, Add Habit dialog.
- **Notes** — note + visibility + optional reminder date/time; soft tinted cards.
- **Wellness** — today's water / sleep / exercise vs goals (8 / 7 h / 30 min), tip, 7-day sleep bars, mood history, log + save, averages.
- **Reminders** — title, date, time, for whom, note; list soonest first. Rings on the phone through `core/reminders` (see reminders.md); asks for the notification permission once.
- Long-press any item (or its trash button) to delete; long-press selects several; Back leaves selection. Petals on add/tick; silent refresh every ~30 s.

## Server calls
`GET api/data/state/{coupleId}` and `POST api/data/state` (top-level key merge). Keys: `journal`, `habits`, `notes`, `reminders`, `wellness`. Only the key that changed is sent; every write re-reads the latest state first and edits the raw JSON, so fields the web adds that this app does not know are kept. Item fields are the web's (`id`=ms, `visibility`, `by`, habit `doneDates/streak/doneToday`, note `color/me/reminderDate`, reminder `forWhom/fired`).

## Decisions
- Dates use the phone's local day. The web uses the UTC day (`toISOString`), so between midnight and about 05:30 in India the two apps can disagree about "today" for one tick.
- Habits are one shared list (as on the web). Journal and notes use `by` (user1/user2) for "mine" vs "partner's" and the web's `canSeeItem` rule.
- Visibility dropdown instead of chips, to match the reference.

## Not done / known gaps
- Not compiled here (no Android SDK). Logic (`MySpaceLogic`) is unit tested and was run with kotlinc.
- A note's own alarm (`reminderDate`) is saved for the web but does not ring the phone yet; only the Reminders tab does.
- No offline copy (journal text is private; not cached on the phone).
- Web XP/level points are not awarded from Android.
- Concurrent edits: later save wins per key.
