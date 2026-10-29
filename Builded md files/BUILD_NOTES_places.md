# BUILD_NOTES — Places v1
Not compiled by Claude (no Android SDK here). The pure logic (JSON mapping, search, sort, stats) was compiled and checked.

## Steps (Windows)
1. Unzip `th-p5-places-v1.zip` over your project folder (it only adds new files).
2. Add the 3 lines from `SHARED_CHANGES_places.md` (Routes, nav graph, Home shortcut).
3. Android Studio → Sync Gradle → Build → Make Project. Send me the first error if any.
4. Optional: `./gradlew.bat testDebugUnitTest`.

## Manual tests
1. Home → Places chip opens the page; back returns to Home.
2. Add Place: name + date + banner + 2 photos → Save → card appears; stats update.
3. Open it: add a note (⭐), tick favorite, open a photo, remove it; Map tab → Open in Maps (after "Use My Current Location" in Edit).
4. Search by name; turn on ⭐ Favorites filter.
5. Delete the place → confirm → it disappears. Check the web app shows the same places.
