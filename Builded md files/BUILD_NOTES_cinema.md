# BUILD_NOTES — Twin Cinema v1

**Not compiled here** (no Android SDK in the sandbox). Build it in Android Studio. If it fails, paste the error and I send only the changed files.

## Apply (Windows)
1. Close Android Studio (or leave it open, it will reload).
2. Unzip `th-cinema-v1.zip` **over your project folder** (the folder that has `README.md` and `app\`). Choose "Replace" if asked.
   If you changed `TwinApp.kt`, `HomeComponents.kt` or the notification files after the last delivery, apply the snippets from
   `SHARED_CHANGES_cinema.md` by hand instead of replacing those files.
3. Android Studio → **File → Sync Project with Gradle Files**.
4. Run on your phone (USB debugging).

## One-time setup (cinema backend, only you can do this)
1. Supabase → SQL Editor → run `migrations/cinema_sessions.sql` from the **us-app** repo.
2. Render → deploy the current us-app.
3. (No Vercel project or env vars are needed any more: the theatre is bundled in the APK and uses the web app's public Supabase keys. Do add `https://appassets.androidplatform.net` to Render CORS, see BUILD_NOTES_cinema_bundled.md.)

## Test (two phones, both paired as a couple)
1. **Phone A (host):** Home → **Cinema** chip (scroll the chip row if needed). Expect: loading bars, then the 3D lobby full screen.
2. Phone A: tap **Invite** in the lobby.
3. **Phone B (partner), app open:** within about 6 seconds an invite card appears on any screen: "<name> invited you" → **Accept ♥**. Expect: the cinema opens on B with the other avatar.
4. Phone A: press **Start**. Both phones go into the hall.
5. Both: **Choose movie file**, pick the same file. Expect: the file picker opens (this is the WebView file chooser) and the movie plays in sync.
6. **Leave:** press the cinema's **Leave** button → back in the app. Also try the phone's Back button → "Leave cinema?" → **Stay** / **Leave**.
7. **Not now:** host invites again, partner taps **Not now** → card closes, the host is told.
8. **Push:** close the app on B, host invites → notification "… invited you" → tap → app opens → card shows.

## Expected / known gaps
- First open can take up to a minute when the free Render server is asleep (the screen shows loading, then Retry on failure).
- The lobby movie frames use the server library (`&lib=` is not sent from Android yet).
- Android shows the cinema's own Voice/Video buttons as buttons only (same as the cinema README).
- Unit tests added: `CinemaLinksTest`, `CinemaViewModelTest` (host / partner / error / insecure link, invite answered once). Run: `.\gradlew.bat testDebugUnitTest --tests "com.twinhearts.app.feature.cinema.*"`.
