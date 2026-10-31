# Build Notes — Phase 3 — Us (Love Counter · Collection · Dream Goals)

**What this delivery adds:** The Us tab container with three screens:
1. **Love Counter** — days-together ring, time grid (years/months/days/hours/minutes/seconds), live dot, milestone card, anniversary card.
2. **Collection** — hero stat tiles, search field, shared memory list rows (play / favourite / delete).
3. **Dream Goals** — hero stats, insight card, category progress cards, dream cards grid (emoji, title, target, progress, complete/edit/delete).

**How to apply:**
1. Unzip this delivery over the `twin-hearts-android/` project root. Paths inside the zip are project-relative.
2. Apply the two core edits listed in `SHARED_CHANGES.md` (Routes.kt + FeatureGraphs.kt).
3. Add `google-services.json` to `app/` if you haven't already (Firebase project — user-only task).
4. Set `API_BASE_URL` in `local.properties` (user-only task; not committed).
5. Open the project in Android Studio, Sync Project with Gradle Files, then Run on your phone.

**How to test:**
1. Sign in and pair with a partner (Phase 1 features must be ✅ first).
2. Navigate to the Us tab from the bottom bar.
3. Love Counter: verify days-together count matches the web app's lovecounter.html for your anniversary.
4. Collection: verify the memory list shows shared items from the shared state (`photos` key).
5. Dream Goals: verify the dream cards grid shows bucket-list items from the shared state (`bucket` key).
6. Rotate the phone — state survives (ViewModel + rememberSaveable where relevant).
7. Turn on system "remove animations" — skeletons stay still, no crash.

**Known gaps:**
- Dream goal creation form is not built yet (Add Goal FAB refreshes the screen only).
- Collection upload from camera/gallery not built (CameraX is Phase 3).
- Real-time push when partner adds a dream/memory — FCM push is Phase 2.5; this release loads on open.
- Not compiled in this sandbox (no Android SDK). Build and verify in Android Studio on your PC/phone.

**Manual test cases:**
1. Open Us tab → shows Love Counter by default with days-together number.
2. Tap Collection tab → shows hero stats + memory list (or empty hint if none).
3. Tap Dream Goals tab → shows dream cards grid (or empty hint if none).
4. Pull/refresh → reloads from the server (Render may sleep on first request — wait for loading skeleton).
5. Large font (fontScale 1.3) → layout still readable, no overflow.
6. Network off → error state with retry button shows, no crash.

## v4 manual tests
1. Add Movie: pick a poster, fill review + scene, save. Open it: poster, About and Favorite Scene show. Tap Edit, change, save (needs backend steps 1-2).
2. Upload a big movie, press Home / lock the screen: the notification keeps moving; tap Cancel in it to stop.
3. Reactions: tap one on the app, see it on the partner's web page, and the other way round (needs backend steps 3-4).
