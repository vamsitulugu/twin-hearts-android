# Feature Audit — Couple Level

**Source (web app, read-only reference):** `us-app/public/index.html` (`#page-level`, `renderLevelPage`, `levelFromXP`, `xpForLevel`, `BADGES`, `XP_ACTIONS`, `addXP`) and `us-app/public/level.css`.

**API used (read from `routes/data.js`, not guessed):**
- `GET /api/data/state/{coupleId}` → `{ state }`. Level reads only `state.xp` (a number). No new backend route. The app never writes XP: on the web, `addXP()` is called by Chat, habits, fights, songs, wellness, love capsules, dreams and surprises. When those features are ported they should add to the same `xp` key.

**Rules (copied exactly):**
- Level = `floor(sqrt(xp / 25)) + 1`; level N starts at `(N-1)² × 25` XP.
- Ring and top bar fill = `xp / XP-of-next-level` (not progress inside the level), as on the web.
- Badges: Sweet Start 50, Getting Close 150, Best Friends 300, Soulmates 600, Forever Us 1000.
- Eight ways to earn XP: Chatting +1, Habit done +5, Fight resolved +6, Share a song +5, Wellness logged +2, Love capsule +15, Dream completed +8, Surprise +10.

**Screen (one scrolling page, same card order as the web):**
Hero (ring, XP, bar, "N XP to next level", insight row that scrolls to the next card) → Next unlock → Locked badges (first one highlighted) → Badges earned → How to earn XP (2 columns; 1 on phones under 360 dp).
States: Loading skeleton, Error with retry (Sync button also reloads), Content. A missing or odd `xp` counts as 0, which shows the "start earning XP" insight.

**Opened from:** Home → "Couple Level" shortcut (`Routes.Level`).

**Not ported, with reason:**
- Live update when XP changes while the page is open: the web re-renders when it adds XP itself; the app reloads on open and from Sync (push updates are Phase 2.5).
- No reference screenshot of this page exists in `docs/reference/images.pdf`, so the look follows `level.css` and the app theme colours (card, accent, soft accent) instead of pixel-comparing.
- Badge icons use the closest Material icons (e.g. a heart for "Getting Close", whose web icon is a heart with a handshake).

**Tests:** `LevelLogicTest` (level formula, percentages, badge unlocking, all-unlocked, negative XP, reading `xp` from the saved state). No ViewModel tests yet.

**Manual checks (not compiled in the Claude sandbox):** open Home → Couple Level. With XP 0: level 1, empty earned card, "start earning XP" row (tap scrolls to How to earn XP). With XP 60 on the web: level 2, 60%, Sweet Start earned, Getting Close next with 40%. With 1000+: "Every badge unlocked" cards. Airplane mode → error + retry.
