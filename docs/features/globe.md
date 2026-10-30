# Memory Globe — feature audit (Phase 6)

Old web files read: `public/globe.html` (2,286 lines, all the page logic), `routes/globe.js`, `public/globe.js` (misplaced copy of the route), `routes/media.js`, `routes/location.js`, `routes/pro.js`, `public/js/pro-card.js`.
Opened from: Home → **Globe** chip → `Routes.Globe`.

## The native decision (README says "decide native approach in its own audit")
The web page draws a 3D Earth with **Cesium** (satellite imagery, terrain, 3D buildings, needs an ion token). Cesium does not exist for Android, and MapLibre 11 has no reliable globe mode. So:

| Web | Native |
|---|---|
| Cesium 3D globe | **(v2: real textured 3D Earth, see globe-3d.md)** Earlier: own globe drawn in Compose (`ui/globe/GlobeCanvas.kt`): country shapes from a bundled map (Natural Earth, public domain, 77 KB, no network), graticule, atmosphere, drag with inertia, pinch zoom, idle spin, fly-to. A full world frame costs about 0.5 ms (measured). |
| Cesium street-level imagery | The **Map** view (MapLibre + OpenFreeMap): zoom in for real roads and buildings. The "Street level" badge shows from zoom 15. |
| Leaflet "Map" view | MapLibre flat map, same pins. |
| Timeline | Native `LazyColumn`. |
| Memory panels | Bottom sheets (82% height) on phones, a side panel on wide screens (>= 700 dp). |

## Screens and states
| Screen | What it does | States |
|---|---|---|
| Top bar | Back, title, Search, Statistics, Filter; tabs Globe / Map / Timeline; quick buttons Live Location, Reset view | – |
| Globe | Heart pins (mood emoji, count badge), pins merge into a number bubble when they crowd (tap = zoom in), trip arcs between memories in date order, glowing visited countries, live dots, search marker | loading, content, empty, error + retry |
| Map | Same pins on real map, real road routes for legs <= 1200 km (OSRM), great-circle lines for longer ones, live dots + trails | same |
| Timeline | Newest first, mood-coloured dots, first photo, notes preview | same |
| Filter chips | All, years that exist in the data (newest 4), 😊 Happy, 🥰 Romantic, 🤩 Excited | – |
| Search | City / country / landmark (Nominatim), or `lat, lng` typed in; tap = fly there + toast | idle, typing, results |
| Stat chips + Stats page | cities, countries, memories, spent; page has totals, 12-month activity strip, most visited cities, mood counts, 9 achievements | – |
| City sheet | Flag, city, "country · N memories", **+ Add Memory Here**, one card per memory (View / Edit / Delete) | content, empty |
| Memory sheet | Hero photo, trip details grid, notes, favourite moment, photos, videos, voice notes (play/pause), journal entries, restaurants / hotels / gifts, Add More (Photo, Video, Voice, Journal), Edit, Delete | – |
| Pin a Memory form | Place search (400 ms, 3+ letters), weather auto-fill, trip name, dates (calendar), mood, weather, temperature, cost + currency, notes, favourite moment, 3 chip lists, photos / video / voice (uploaded at once), journal, save | idle, saving |
| Full-screen viewer | Swipe between photos and videos, pinch / double-tap zoom, Media3 video | – |

## API calls (read from the old routes)
| Action | Call |
|---|---|
| List | `GET /api/globe/:coupleId` (rows with `globe_memory_media`) |
| Create / edit | `POST /api/globe` / `PUT /api/globe/:id` `{coupleId, memory{…}}` (+ `senderRole`, `myName` on create so the partner gets the push) |
| Delete | `DELETE /api/globe/:id?coupleId=` |
| Add media / journal | `POST /api/globe/:memoryId/media` `{coupleId, media{type,url,duration | journal_text}}` (one item per request, works with both server versions) |
| Delete media | `DELETE /api/globe/media/:mediaId?coupleId=` |
| Photos, videos | `POST /api/media/upload` (multipart `file`, `coupleId`) → `{url}` |
| Voice notes | `POST /api/media/upload-voice` (multipart `file`, `coupleId`, `senderRole`) → `{url}` |
| Pro check | `GET /api/pro/status?coupleId=` → `{isPro}` |
| Live location | `GET /api/location/:coupleId`, `POST /api/location/ping`, `POST /api/location/stop`, `GET /api/location/:coupleId/trail/:role` |
| Free services | Nominatim search, Open-Meteo weather, OSRM roads (same as web) |

## Rules kept from the web
- "Pin a Memory" needs **Pro** (toast "Memory Globe is a Pro feature"). Viewing memories and **+ Add Memory Here** are not gated (same as web).
- Memories of one city + country share one pin. Arcs follow `date_from` order; same-city steps draw nothing.
- Costs are summed in ₹ for the chips (same as web).
- Wording copied from the web (`strings_globe.xml`).

## Small differences on purpose
| Change | Why |
|---|---|
| Voice notes use `upload-voice` | The web uses `/upload`, which the server rejects for audio (see `backend-changes/globe/README.md`). |
| Year chips follow the data | The web had 2025 and 2024 typed in. |
| Cost shows the memory's own currency symbol | The web always wrote ₹ even for USD. The chips still sum in ₹. |
| Live location uses 10-second polling while the page is open | The web used a Supabase realtime channel; R-66 says poll or push, no sockets. Stops when the page is hidden. |
| Delete buttons on photos are always visible | On the web they appear on mouse hover only, which does not exist on a phone. |
| Journal entry uses a dialog | The web used a browser `prompt()`. |
| Country glow and arcs use the app's colour tokens (pink / orchid / green / accent) | R-50: no hard-coded colours. The web used amber / yellow / orange. |

## Not ported (and why)
| Item | Why |
|---|---|
| Cesium satellite imagery, terrain, photorealistic 3D tiles | Needs Cesium / a paid imagery service. The Map view gives real streets and buildings. |
| ⭐ Bookmarks | `toggleBookmark` exists in `globe.html` but no button ever calls it (dead code), so there is nothing to port. |
| Realtime "partner is typing a memory" style pings | None existed besides location. |
| The Pro **upgrade card** (payment) | Pro upgrade is its own Phase 6 session. The Globe only reads Pro status. See *Change request* below. |

## Change requests for core (not applied)
1. **Go Pro sheet in core.** Twin AI has a private Go Pro sheet. Move it to `core/design` (or `core/pro`) so Globe, Music and Movie can open it. When that exists, Globe's `requestPin()` should open it instead of only showing the toast.
2. **Amber / warning token.** The web used amber for visited-country glows. Add a `warning` token to `TwinColors` if you want it back.

## Known limits
- Not compiled here (no Android SDK in the sandbox). The pure logic (`domain/`, `WorldAtlas`) **was compiled with Kotlin 2.0.21 and tested** (see `GlobeLogicTest`, `GlobeProjectionTest`); the UI is reviewed by hand.
- Small countries without an outline at this map detail (Singapore, Maldives, Malta, ...) have flags and name matching but no shape on the globe. Their pins and glows still show.
- Leaving the page with Live Location on does not send a "stop"; the server marks you offline when pings stop.
