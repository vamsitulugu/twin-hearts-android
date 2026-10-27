# Feature Audit — Us (Love Counter · Collection · Dream Goals)

**Source screens (web app, read-only reference):**
- `us-app/public/lovecounter.html` — Love Counter: days-together ring, time grid, milestones, anniversary
- `us-app/public/collection.html` — Collection: tabs, hero stats, shared memory list with play/favourite/delete
- `us-app/public/dreamgoals.html` — Dream Goals: hero stats, insight card, category progress, dream cards grid

**APIs used (read from `routes/data.js`, not guessed):**
- `GET /api/data/state/{coupleId}` — whole shared state JSON document (or `null`)
- `POST /api/data/state` — merge top-level keys into the shared state
- No new backend routes needed. Data lives in the same `app_state` document Home uses.

**Tables (from `migrations/*.sql` / `routes/data.js`):**
- `app_state` — single JSON document per `couple_id`; keys used: `anniversary`, `bucket` (dream goals), `photos` (collection media)

**Screens, states, buttons, inputs:**
| Screen | States | Key controls |
|--------|--------|--------------|
| Love Counter | Loading, Content, Empty, Error | Days ring, time grid (years/months/days/hours/minutes/seconds), live dot, milestone card, anniversary card |
| Collection | Loading, Content, Empty, Error | Search field, stat tiles, memory list rows (play / favourite / delete) |
| Dream Goals | Loading, Content, Empty, Error | Stat tiles, insight card, category progress cards, dream cards (emoji, title, target, progress, complete/edit/delete) |

**Edge cases handled:**
- No signed-in session → `Unauthorized` error state
- Empty shared state → Empty state with helpful hint
- Network failure → Error state with retry
- Server sleeping (Render free tier) → long timeouts + loading skeleton + retry

**Not ported from web, with reason:**
- Real-time push updates for new dreams/memories — the old repo uses polling/SSE via `diffAndNotify`; this first release loads on open with manual refresh (FCM push is Phase 2.5)
- Dream goal creation form — the "Add Goal" FAB is wired but no input screen yet; listed as TODO-free placeholder? No — it navigates back to Us screen (refresh). The add form is intentionally out of scope for this session.
- Collection upload from camera/gallery — out of scope (CameraX is Phase 3)

**Route registration (to be added by the user via SHARED_CHANGES.md):**
- `Routes.Us` in `core/navigation/Routes.kt`
- `usGraph(navController)` in `core/navigation/FeatureGraphs.kt` (featureGraphs block)

**SHARED_CHANGES.md entries:** see the delivered `SHARED_CHANGES.md` in this zip.
