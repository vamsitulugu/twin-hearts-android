# SHARED_CHANGES — Phase 5 Watch Together v2 (remaining features)
All items are **already applied inside this zip**. Listed for review.

## Gradle / catalog lines
None.

## Manifest entries
`app/src/main/AndroidManifest.xml`, on `MainActivity`:
```xml
android:supportsPictureInPicture="true"
android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation"
```
(configChanges stops the Activity restarting when the PiP window opens.)

## AppNavHost line
None (already added in v1).

## TwinDatabase entities
None.

## Strings / permissions
`res/values/strings_movie.xml` (more `movie_*`). No new permission.

## Backend changes
**OPTIONAL — REQUIRES BACKEND CHANGE** for reactions: `backend-changes/movie/`. Uploads need R2 set up on Render (already used by the web page).
