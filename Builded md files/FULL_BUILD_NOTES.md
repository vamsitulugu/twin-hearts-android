# Meet Planner + Places — full build notes
Not compiled by Claude (no Android SDK here). Build it in Android Studio first.

## Before you start
Your `Routes.kt` must still have your own routes (e.g. `Routes.Call`). If an earlier zip replaced it,
restore it first: right-click Routes.kt → Local History → Show History → pick the version before the change.
Do the same for `FeatureGraphs.kt` / `HomeRoute.kt` if their own lines are missing.

## Steps (Windows)
1. Close Android Studio (or just let it reload files).
2. Unzip this zip over your project folder. It only adds new files (feature folders, strings, docs, tools).
3. Open PowerShell in the project folder and run:
   `powershell -ExecutionPolicy Bypass -File tools\apply_wiring.ps1`
   It adds the routes, the two graph lines and the two Home chips to YOUR files (a `.bak` copy is made). Run it twice safely.
   If it prints a yellow line, do that one edit by hand (the text tells you what to add).
4. Android Studio → Sync Gradle → Build → Make Project. Send me the first error if any.

## Manual tests
Meet Planner: Home chip opens it; Plan New → Goa → date → Create Plan → add 3 places → Build Itinerary → Save → see it in Saved → open, tick checklist, Mark Complete with a photo.
Places: Home chip opens it; Add Place with banner + photos → card shows; open it, add a note, open a photo, Edit, Delete.
