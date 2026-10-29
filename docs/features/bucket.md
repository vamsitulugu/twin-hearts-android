# Feature Audit — Bucket List

**Web source (read-only reference):** `us-app/public/index.html` → `#page-bucket`, `#bucketModal`, `addBucket / toggleBucket / delBucket / renderBucket`, long-press selection code, `canSeeItem()` (privacy), `routes/data.js`.
**Reference screenshot:** `docs/reference/images.pdf`, page 49 (empty state).

## Screen
| Part | Web behaviour | Android |
|---|---|---|
| Top bar | back, logo, title "Bucket List", Sync | Same (back, logo, title, Sync). |
| Card header | 🌟 "Our Bucket List", `done/total` pill, "+ Add Dream" | Same. Pill counts only dreams *you can see*. |
| Empty | "Nothing on the list yet" + hint + "Add Dream" | Same text, same button. |
| Row | tick circle, category emoji + title, privacy badge, short date, delete | Same. Done = filled circle + strike-through. |
| Add form | Dream/Goal, Category (7), Target Date (optional), Who can see this? (Both / Only Me) | Bottom sheet, same fields, same labels, "Add Dream ✨". |
| Selection | long-press row → select several → bin → confirm | Same. Back button leaves selection first. |
| Delete | confirm dialog, "will be permanently deleted" | Same wording. |
| Complete a dream | +8 XP (`addXP(8)`) | Same: `xp` is raised by 8 when a dream is ticked (not when un-ticked). |

States: loading skeleton, content, empty, error with retry (R-41).

## API (R-65, read from `routes/data.js`)
- `GET /api/data/state/{coupleId}` → `{ state: {...} }` (state may be `null`).
- `POST /api/data/state` body `{ coupleId, state: {bucket: [...], xp?}, senderRole, myName }`. The server **merges top-level keys**, so we send only `bucket` (and `xp`).
- Errors: 401 / 5xx / offline → mapped to `AppError` (R-46).
- Item fields (same as web): `id` (number), `title`, `done`, `date` (yyyy-MM-dd or ""), `cat`, `visibility` (`both|self|partner`), `by` (`user1|user2`).
- Table: `app_state` (one JSON row per couple). No new backend route.

## Decisions
- **Raw JSON is kept.** Delete/tick rewrite the raw array, so fields written by other screens (Dream Goals writes `emoji`, `desc`, `priority`, `progress`, `target`) and your partner's *private* dreams are never lost.
- Dream Goals saves its date as `target`; we show it as the date when `date` is empty.
- Privacy rule copied exactly from web `canSeeItem()` (`canSeeDream`, unit tested).

## Not ported (with reason)
- Petals animation on add/complete: decorative; Phase 7 polish.
- Page-wide Search and Menu buttons in the top bar: those belong to the app-wide search/menu feature (not built); no dead buttons shipped.
- Bottom bar: shown only on the five tab screens by core's rule (same as Movie/Meet Planner), unlike the web screenshot.
- Last-write-wins: two people saving at the same second can overwrite each other (same as the web). We always re-read the newest state just before saving to keep that window tiny.
