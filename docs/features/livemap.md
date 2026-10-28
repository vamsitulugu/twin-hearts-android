# Live Map — feature audit (Phase 5)

Old web files read: `public/livemap.js`, `livemap-screens.js`, the `#page-map` markup in `index.html`, `routes/location.js`, `routes/route.js`, `routes/tracking.js`, `routes/data.js`.
Opened from: bottom bar **Map** → `Routes.LiveMap`. One route; the 8 sub-screens are kept in the page's own back stack, so Back returns to the screen before.

## Screens
| Screen | What it does |
|---|---|
| Map | Together card (both people, status, distance, share switch), banners (emergency, offline, permission), MapLibre map, ETA card, quick tiles (Partner, Meeting point, Love note, Video call), long-press = drop pin + road route, destination card (Save, Favorite, Share) |
| Track | Me/Partner, Today/Week/Month, date chips, route on map + stops, playback 1x/2x/4x, timeline, GPX export, route-history privacy (on/off, 7/30/forever) |
| Partner | Status, Updated, Away, ETA (no battery or network, as requested), where they are, directions, route history |
| Places | My places, partner's places, favorites (rename, reorder, remove); add place at current location |
| More Options | Map type (Map, Dark, Satellite, Hybrid, Auto), 3D, auto-follow, route line, place names, weather, travel mode, links, clear my history |
| Location Alerts | Arrive / leave / offline toggles; battery / network / GPS toggles; recent activity |
| Location Privacy | Pause 15 min / 1 h / until resumed, invisible mode, approximate (~1 km), emergency share |
| Search | Photon search, 8 category chips, tap a result = destination on the map |

States: loading, content, empty, error on every list (R-41).

## API calls
`GET /api/location/:c`, `POST /api/location/ping|status|stop`, `GET /api/location/:c/trail/:role`, `POST /api/tracking/batch|safety-alert`, `GET /api/tracking/:c/:role/timeline/:date|geofence-events|daily-report/:date`, `GET /api/route/:c/:role/dates|:date|settings`, `POST /api/route/settings`, `DELETE /api/route/:c/:role/history`, `GET|POST /api/data/state` (keys `placesList`, `favorites`, `loveNotes`, `lmAlerts`).
Free public services (no keys): OSRM routing, Photon search, Nominatim address, Open-Meteo weather. Tiles: OpenFreeMap (Map), CARTO (Dark), Esri (Satellite/Hybrid).

## Native behaviour
- A foreground service (`LiveShareService`) pings every ~8 s while sharing is on, also with the app closed. Android shows a notification with a **Stop sharing** button.
- Points missed while offline are sent in one batch when the internet is back. Pause and invisible mode stop pings.
- Polling and GPS listening only run while the page is on screen (R-66).

## Not ported (nothing dropped silently)
| Web feature | Why |
|---|---|
| Video Call tile | Calls (2.3) are not built; the tile shows "built in a later step", like Chat |
| Turn-by-turn voice navigation, "Start" nav panel | Large separate feature; route line + distance + time are shown |
| Street View | Needs an external viewer |
| POI suggestions for the meeting point, Overpass categories with opening hours | Meeting point is the midpoint with its address; category chips use Photon search |
| Search inside the Add Place dialog, manual lat/lng | Search a place, then tap **Save**; Add Place uses your current location |
| Legacy `S.places` object migration | Only `placesList` is read |
| Avatars from profile photos | Initial-letter avatars (profile image not in this feature's data) |
| Altitude/mock-location *warnings* in the UI | Both are still sent to the server |

## Open points to verify on the phone
- `geofence_events` columns (`label`, `event_type`, `occurred_at`) are read from the old route code; the table is not in the repo's migrations.
- Android may stop the service if the app is swiped away on some phones; opening the Map screen restarts sharing.
- Free map servers can be slow or rate-limited; Satellite/Dark use the same providers as the web app.
