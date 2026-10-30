# Twin Hearts fixes — what changed

## Phase 1 — data-erasing state bug (Android, 11 pages)
Server `GET /api/data/state/:coupleId` returns the saved document itself. The 11 pages expected `{ "state": ... }`,
read an empty document, and then saved lists built from that empty one over the real data.

* NEW  `app/src/main/java/com/twinhearts/app/core/network/StateEnvelope.kt` – one shared reader that accepts both shapes
  (and JSON `null` when nothing is saved yet).
* EDIT the response class of each page (no repository code changed):
  Money, Collection, Us (Dream Goals / Love Counter), Level, Profile, My Space, Bucket, Vault, Calendar, Study, Period.
* NEW  test `app/src/test/.../core/network/StateEnvelopeTest.kt`.
Side effects fixed by the same change: Bucket no longer resets XP to 8, Profile edits keep avatar/moods,
Calendar/Money/Study keep their offline copy, Vault delete no longer empties the vault.

## Phase 2 — server rate limit (us-app + Android)
* `us-app/server.js`: API limit 60 -> 300 calls/min per IP, standard headers, clearer message.
* Android `ChatScreen.kt`: Chat poll 1 s -> 3 s, heartbeat every 5 polls (still every 15 s).

## Phase 3 — backend patches from backend-changes/ now applied to us-app
* `routes/routes-movie.js`   – movie reaction routes (GET /reactions, POST /reaction)
* `routes/movie-favorites.js`– Edit Movie + favourite scene
* `routes/globe.js`          – multi-photo adds (`mediaList`); `public/globe.js` (server code exposed publicly) removed
* `public/movie.js`          – web reactions also reach the Android partner
* `migrations/movie_scene_column.sql`, `migrations/meetup_plans.sql` – RUN THESE in the Supabase SQL editor

## Deploy order
1. Supabase SQL editor: run `migrations/movie_scene_column.sql` and `migrations/meetup_plans.sql` (both safe to repeat).
2. Push `us-app` to GitHub, let Render redeploy (and Vercel for `public/movie.js`).
3. Build the Android app from `twin-hearts-android` and install.

## NOT done
* Push notifications (FCM): needs your own Firebase project + `google-services.json`; separate feature.
* Not compiled or run here (no Android SDK / live server) — build in Android Studio and run the unit tests first.
* Media upload still buffers in memory (150 MB cap); on Render's free tier keep videos small.
