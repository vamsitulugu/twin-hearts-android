# Feature: Meet Planner

Native Compose screen set; the web app (`us-app`) was used only as a UI/behaviour reference.
Entry: Home "Meet Planner" chip → `Routes.MeetPlanner`. Code: `feature/meetplanner/`.

## Screens (one route, internal back stack)
Home (upcoming card, stats, popular cities) · Plan (name, city, dates, trip type, budget, travel mode, notes) · Find Places (category chips, search, nearby) · Your Stops (reorder/remove) · Itinerary (timeline, start time, checklist, save) · Route Preview (drawn on a Canvas) · Saved Meetups (All/Upcoming/Completed, long-press select, delete) · Detail (live countdown, notes, route, checklist) · pop-ups: place sheet, plan options, Mark Complete (mood + photos), Edit, Delete confirm, photo viewer.
All data screens have loading / content / empty / error states and previews (one with fontScale 1.3).

## Server calls (base `BuildConfig.API_BASE_URL`)
| Call | Body | Used for |
|---|---|---|
| `GET api/meetplanner/{coupleId}` | – | list plans (cached 5 min) |
| `POST api/meetplanner` | `{coupleId, plan{title, meetDate, budget, currency, cityName, cityLat, cityLng, stops, routeGeometry, totalDistanceKm, totalDurationMin, travelMode, checklist, notes, createdBy}, senderRole, myName}` | save itinerary |
| `PATCH api/meetplanner/{id}` | `{coupleId, plan{…}}` | edit, checklist tick |
| `DELETE api/meetplanner/{id}` | `{coupleId}` | delete |
| `POST api/meetplanner/{id}/complete` | `{coupleId, mood, photos, extraNotes, senderRole, myName}` | Mark Complete → Memory Globe |
| `GET api/globe/{coupleId}` | – | photos of a completed meetup |
| `POST api/media/upload` | multipart `file`, `coupleId` → `{url}` | completion photos |
| `POST api/search/overpass` | `{query}` | nearby places |
Errors: network/server errors become `AppError` and show as a toast or the error state with Retry.

## Decisions
- Free public map services are called directly (Nominatim city search, Photon suggestions, OSRM driving route), same as the web app. This is an exception to R-60; they need no key. Route falls back to a straight line if OSRM fails.
- Route is drawn with a Compose Canvas, not a tile map: the map SDK needed a newer Kotlin than the project uses. Tiles are not shown.
- End date and trip type are kept in a hidden first line of the notes (`[[mp:{...}]]`), exactly as the web app does, so old and new plans open on both.
- Destination photos copied from the web app into `res/drawable-nodpi/meetplanner_dest_*`.

## Not done / known gaps
- Not compiled or run here (no Android SDK). Build in Android Studio first.
- Draft plan is lost if Android kills the app process.
- No pull-to-refresh (Saved has Retry on error only).
- Map tiles and web "lightbox" zoom are not ported.
- `meetup_plans` table has no migration in the repo; see `backend-changes/meetplanner/meetup_plans.sql` (only if missing).
