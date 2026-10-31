# Search (Home search bar)

**Before:** tapping the Home search bar only showed the toast "This screen is built in a later step."
**Now:** it opens a full Search page that finds pages, sub-pages, features, chat messages and items inside pages.

## What it searches
| Block | Source | Needs internet | Opens |
|---|---|---|---|
| Pages | `SearchCatalog` (29 pages, names reuse Home labels) | No | the page |
| Features & settings | `SearchCatalog` (85 sub-pages/features: Settings pages, chat tools, games, music tabs, map tabs, ...) | No | the owning page |
| Chat messages | `GET api/chat/{coupleId}/search?q=&filter=all` (same call the Chat page uses) | Yes | Chat |
| Inside your pages | `GET api/data/state/{coupleId}` lists: events, bucket, milestones, reminders, notes, journal, habits, transactions, fights, surprises, photos | Yes | the page the item belongs to |

## How it behaves
- Pages and features update on **every key press** (on the phone). Messages and page content wait 350 ms after typing stops, so the server is not hit on every letter.
- Every typed word must match. Order: title start > inside title > keywords > detail. One-letter typos still match (only when nothing matches exactly).
- Matches are highlighted in the title. If only the body of an item matched, the matching snippet is shown.
- Recent searches (last 8) are kept on the phone only (DataStore key `search_recents`). Clear one or all.
- States (R-41): start (recents + ideas), loading bar, results, empty ("No results for ..."), error line (pages and features still work offline).
- The saved document is read once and kept for 60 seconds; it is pre-loaded when Search opens.

## Privacy rules (important)
- Items marked "only me" / "only partner" follow the same rule as Bucket List and My Space (`canSeeItem`).
- Journal and Surprises show **only items I added** (a partner's surprise must not be spoiled).
- Not searched on purpose: Vault, Period Tracker, Wellness.
- Links and embedded pictures are never used as search text.
- Deleted chat messages and "deleted for me" messages are hidden.
- Typed words are never logged.

## API calls (R-65)
| Action | Method + path | Answer |
|---|---|---|
| Find messages | GET `api/chat/{coupleId}/search?q=<text>&filter=all` | list of message rows (or `{data}` / `{messages}`) |
| Read saved lists | GET `api/data/state/{coupleId}` | the saved document (or `{state}` / `null`) |
Errors map to `AppError` via `safeApiCall`. If only one of the two calls fails, the other still shows. Not verified here: the server code of the chat search route (I read the app's own Chat page code, which uses the same call).

## Not ported / limits
- A result opens the **page**, not the exact inner tab or message. Settings, Music and Chat keep their inner state private, and the router has no deep links. Adding deep links is a Change Request for each of those pages.
- Item lists that the pages keep in other places (for example Collection items, Places) are not searched; only the lists named above were verified in the app code.
- The Top bar's own search icon on other pages is unchanged.

## Files
`feature/search/` : `SearchGraph.kt`, `domain/` (models, matcher, catalog, sources, privacy), `data/` (api, repository, readers, history), `di/`, `ui/` (screen, view model). Strings: `res/values/strings_search.xml`. Tests: `src/test/.../feature/search/`.
