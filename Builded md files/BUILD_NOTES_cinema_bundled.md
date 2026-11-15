# BUILD NOTES — Cinema bundled inside the APK

## Install
1. Unzip over the Android project root (adds `app/src/main/assets/cinema/**`, `core/cinema/`, tools, docs; replaces 8 small files).
2. Android Studio → File → Sync Project with Gradle Files → Build → Rebuild. Run on a phone (not an emulator, WebGL is slow there).
3. Web repo (`us-app`): apply `cinema-bundled-us-app.patch` (one CORS line), commit, push. Render redeploys by itself.
   Optional instead of the patch: Render → Environment → `ALLOWED_ORIGINS` = `https://appassets.androidplatform.net`.
4. Cloudflare R2 bucket → CORS → add `https://appassets.androidplatform.net` to AllowedOrigins (movie upload/stream).
5. Delete the old Vercel project `twin-cinemas` (and archive the `twin-cinemas` GitHub repo). Nothing uses them any more.

## Updating the cinema later (one command)
Change the cinema in `us-app/cinema`, then from the Android project root:
    powershell -ExecutionPolicy Bypass -File tools\sync-cinema.ps1
(builds the web cinema, copies it to app/src/main/assets/cinema). Rebuild the app. Commit the changed assets.

## Test (two phones, paired)
1. Phone A: Home → Cinema. Lobby opens at once. Phone B: accept the invite. Both avatars appear.
2. Turn Wi-Fi off after the lobby loads: the room is lost (needs internet) but the 3D hall already loaded from the APK.
3. Watch Together → Twin Cinema 3D → pick the movie file on both phones → both play together.
4. Back button → "Leave cinema?" → Leave returns to the app.
