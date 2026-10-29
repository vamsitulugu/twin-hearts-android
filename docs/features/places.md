# Feature: Places (Places Memory)

Native Compose screens; the web page `public/places.html` was used only as a UI/behaviour reference.
Entry: Home "Places" chip → `Routes.Places`. Code: `feature/places/`.

## Screens
- **List**: 4 stat tiles (Places, Favorites, Photos, Memories), search, ⭐ Favorites filter, "+ Add Place", 2-column grid of cards (banner, star, photo count, name, location, date). States: loading, content, empty ("No places yet…"), no search match, error with Retry.
- **Detail**: banner with name/location/date, description, tabs **Gallery** (photo grid, + Add, full-screen swipe viewer with Remove), **Timeline & Notes** (add note with date and ⭐, favorite/delete), **Map** (coordinates + "Open in Maps"), Edit Place, Delete Place (confirm).
- **Add/Edit form**: name*, location, visit date, description, banner upload, photos, latitude/longitude, "Use My Current Location", favorite toggle.
All with previews (one with fontScale 1.3).

## Server calls (base `BuildConfig.API_BASE_URL`)
| Call | Use |
|---|---|
| `GET api/data/state/{coupleId}` | read the shared state; we use only key `placesMemories` (JSON `null` = nothing yet) |
| `POST api/data/state` `{coupleId, state:{placesMemories:[…]}, senderRole, myName}` | save. The server MERGES top-level keys, so other data is untouched |
| `POST api/media/upload` multipart `file`, `coupleId` → `{url}` | banner and photos |
Place JSON fields (same as web): `id, name, location, visitDate(yyyy-MM-dd), description, banner, photos[], lat, lng, favorite, memories[{id,date,text,favorite}], createdAt`.
Errors: shown as a toast or the error state with Retry. A failed save shows the error and re-reads the list from the server.

## Decisions
- Changes show at once, then save in the background (saves run one after another).
- The map tab opens the phone's maps app instead of drawing tiles (no map SDK added; see Meet Planner notes).
- Photos removed from a place are not deleted from storage (the web app does not either).
- New file `strings_places.xml`, all names `places_*`. No new permission (location already in the manifest).

## Known gaps
- Not compiled or run here (no Android SDK). Build in Android Studio first.
- Whole list is sent on every save: if both phones edit at the same moment, the last save wins (same as web).
- No pull-to-refresh; Retry only on error. Form draft is lost if the app is killed.
- No in-app map tiles; no pinch-zoom in the photo viewer.
