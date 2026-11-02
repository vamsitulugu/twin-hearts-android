# Our Profile › Highlights (feature audit)

Instagram-style circles right under the couple card on **Our Profile** (web `us-app` + this app).

## Screens and actions
| Part | What it does | API |
|---|---|---|
| Row | "New" (+) circle, then **my** circles (my partner's are on their profile). Category chips appear when 2+ categories exist | `GET /api/highlights/:coupleId` |
| Add sheet step 1 | Record video (camera) or Upload from device (gallery, up to 10) | – |
| Add sheet step 2 | Preview, then **New highlight** (name ≤24 + category) or **Add to existing** (mine only). Progress bar, retry keeps going in the same highlight | `POST /api/highlights/upload` (multipart, one clip per call) |
| Viewer | Full screen, progress bars, tap left third = back, rest = next, hold = pause, photo 5 s, video to the end | – |
| Owner menu (⋮) | Edit name & category, Add more clips, Delete clip, Delete highlight | `PATCH /:id`, `DELETE /item/:itemId`, `DELETE /:id` |
| Long-press my circle | Opens Edit | `PATCH /:id` |

States: loading (3 skeleton circles), content, empty (hint text), error (inline "Try again").
Errors: shown with the shared `AppError` texts. Only the owner can change a highlight (server checks `userId`).

## Backend
Needs `migrations/highlights.sql` run in Supabase and the `/api/highlights` route deployed (web repo, see `HIGHLIGHTS.md`).

## Not ported / not done
- Custom cover picture (cover = first clip).
- Push notification to partner when a highlight is added.
- Video first-frame uses Android's MediaMetadataRetriever (no new library).

## Partner profile (update)
Tapping my partner's picture on Our Profile (it has a rotating pink/blue/purple ring) opens `ProfileScreenKind.PartnerProfile`: their photo on top (tap = large photo), name, then `HighlightsSection(owner = Partner)` — watch-only, no "+". The ring stands still when system animations are off.
