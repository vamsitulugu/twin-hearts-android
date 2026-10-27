# BUILD_NOTES — Watch Together v2 (not compiled in the Claude sandbox)

## Steps (Windows)
1. Unzip over the project folder (overwrite when asked).
2. Android Studio → File → Sync Project with Gradle Files. Run on your phone.
3. Optional: paste `backend-changes/movie/reactions.js.txt` into `routes/routes-movie.js` and redeploy.

## Manual tests
1. **Upload:** Watch → Cloud Storage → Upload Movie → pick an MP4 → bar moves → "Uploaded ❤️" → movie in list. Try Cancel upload mid-way: list stays clean.
2. **Delete:** 🗑 on a row → confirm → "Movie deleted". Try during a session: server message appears.
3. **Cloud watch:** both phones tap ▶ on the same movie → ready → start → plays on both.
4. **Player ⋮:** Subtitles (.srt/.vtt), Screen Fit, Sleep Timer (try 15 min), Keep Screen On, Movie Info, Picture in Picture.
5. **Reactions:** tap 😊 in the top bar, pick an emoji (partner sees it only with the backend patch).
6. **Library:** Add Movie form; ⭐ on posters; Favorites filter; owner chips.

## Known gaps
In-movie calls (needs Phase 2.3), poster/review/scene fields, background upload. Unit tests added for owner/favorite filters and sleep steps; still no ViewModel tests.

## If it fails
Send the exact Android Studio error text; I will send only the changed files.
