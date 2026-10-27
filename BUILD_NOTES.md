# BUILD_NOTES — Watch Together v1 (not compiled in the Claude sandbox)

## Steps (Windows)
1. Unzip over your project folder (overwrite when asked).
2. Android Studio → **File → Sync Project with Gradle Files** (downloads Media3).
3. Run on your phone. Home → tap the **Movie** chip.

## Tests
- Unit tests: right-click `app/src/test/.../feature/movie/MovieLogicTest.kt` → Run. (ViewModel tests are not written yet; listed as a gap.)

## Manual test cases (needs two phones signed in as partners, both on the same movie file)
1. **Library:** Library tab → Search online → search "Interstellar" → tap result → Add to Watchlist → it shows in Library and Home.
2. **Pick:** Watch tab → Choose Movie From Device → pick a video on both phones. Both show "Both versions match ✓" (lengths within 3 s).
3. **Start:** both tap I'M READY → one taps START TOGETHER → other phone shows MOVIE NIGHT → ACCEPT → both see 3·2·1 → both start playing.
4. **Sync:** pause on one phone → other pauses within ~2 s. Drag the bar on one → other jumps. Tap Sync → "✓ In Sync".
5. **Chat + end:** Message → send; partner sees it float. Log lists it. End → confirm → both return; Watch History (clock icon) shows the movie.
Cloud: Watch tab → Cloud Storage lists movies uploaded on the web (needs the server's storage to be set up).

## Known gaps
Cloud upload, subtitles, reactions, sleep timer, in-movie calls (see docs/features/movie.md). First request can be slow while Render wakes up.

## If it fails
Send the exact Android Studio error text.
