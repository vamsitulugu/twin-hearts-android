# SHARED_CHANGES — Profile Highlights

## Gradle/catalog lines
None (Coil, Media3, Hilt, CameraX-free `CaptureVideo` are already in the project).
## Manifest entries
None (uses the existing `${applicationId}.chatfiles` FileProvider and the existing CAMERA permission).
## AppNavHost line
None (the row is inside the existing Profile page).
## TwinDatabase entities
None.
## Strings/permissions
New file `res/values/strings_profile_highlights.xml` (included in the patch).
## Backend changes
**REQUIRES BACKEND CHANGE** — in the web repo (`us-app`): run `migrations/highlights.sql` in Supabase, deploy `routes/highlights.js` + the `server.js` line.
## Edits to existing files
- `MainPage.kt`: new optional `highlights` slot parameter, called under the hero card.
- `ProfileScreen.kt`: passes `highlights = { HighlightsSection() }`.
