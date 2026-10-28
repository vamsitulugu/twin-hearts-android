# Fight Log — audit and explanation

Reference: `docs/reference/images.pdf` page 29 (dashboard, empty state). Web source: `public/index.html` (`#page-fights`, the `fl*` functions), data in the shared state key `fights` (`routes/data.js`).

## Screens (all inside one page, like the web)
| Part | Web | Android |
|---|---|---|
| Top bar: back, menu, logo, "Fight Log", search, Sync | yes | `TwinTopBar` (menu/search show "built in a later step", same as Chat) |
| Conflict Resolution Rate: title + (i), "How well you both resolve conflicts", range picker (30 days / 3 months / 6 months / All time), ring, Resolved/Open/Total, insight strip | yes | `ResolutionRateCard` |
| Avg Intensity / Avg Resolution Time with change since last month | yes | `MetricTiles` |
| Log a Fight button | yes | `LogFightButton` |
| Recent Fights (3 shown, View All / Show Less), rows with category tile, date, Open/Resolved, x/10 | yes | `FightRow` |
| Empty state "No conflicts recorded." + Reconnect Activities button | yes | `NoFightsYet` |
| Reconnect Activities row | yes | `ReconnectRow` |
| Log Fight wizard: Details (title, what happened, category, intensity 1-10, who started, status) > Feelings (9 chips, need) > Resolution (approach chips, what next time) , Save Draft, Next | yes | `LogWizard` |
| Fight details sheet: facts, text blocks, feelings, approach, resolved date, Mark resolved, AI Help, Delete (with confirm) | yes | `FightDetailSheet` |
| Reconnect Activities: 5 filters, 8 activities, tap to open the "how" | yes | `ActivitiesView` |

## Data and API
Fights live inside the couple's shared state, key `fights` (an array, newest first), the same as the web.
- Read: `GET /api/data/state/:coupleId` (answer is the state itself).
- Write: `POST /api/data/state` with `{coupleId, state:{fights:[...]}, senderRole, myName}`. The server merges top-level keys, so the **whole** `fights` array is sent.
- To avoid wiping a partner's change, every write first re-reads the latest array, edits it, then saves. Unknown fields on existing fights are kept.
- A new fight has the same fields as the web (`id`=milliseconds, `title`, `reason`, `category`, `intensity`, `startedBy` as `user1/user2/both/unsure`, `feelings`, `need`, `approach`, `notes`, `status`, `date`, `resolvedDate`, `visibility:"both"`, `by`).
- Saving a fight makes the server push "New Fight Logged" to your partner (same as the web).
- Draft: kept only on this phone (`uwl_fightDraft`), like the web's local draft. Restored once when you open Log a Fight.

## Behaviour copied from the web
- Rate = resolved / total of the chosen range. Insight rules: none / intense (avg >= 7) / all resolved / doing great (>= 60%) / still open.
- Trend: this month (30 days) vs the month before (60 to 30 days). Lower is better for both numbers (drop = green, rise = red).
- Back inside the page: wizard step back > dashboard; activities > dashboard; then leaves the page.
- Next step needs a title ("Give this fight a short title first").
- AI Help sends the web's sentence to Twin AI and opens the Twin page, which asks it once (`TwinPromptHandoff`).

## Different from the web (and why)
- Dates use the phone's own date (the web uses the UTC date, which is a day behind for some hours in India).
- "1 day": the web prints "1 days" because of a small bug; Android prints "1 day".
- Resolving a fight does not award XP yet: the Level feature is not built in this app. (The web gives +6 XP.)
- Menu and search buttons show the "built in a later step" note, like Chat.
- No live refresh: the list reloads when you open the page and when you press Sync.
- The bottom bar stays visible on this page (as in your reference) with no tab highlighted.
