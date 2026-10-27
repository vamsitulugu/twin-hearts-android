# SHARED_CHANGES — Phase 2.2 Chat (v2)

## 1. AppNavHost / FeatureGraphs.kt
Unchanged from v1 (already done): `chatGraph(navController)` registered, Chat pending line removed.

## 2. AndroidManifest.xml
Add next to the other permissions (needed only for "Location" in the + menu):
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```
The zip contains the full AndroidManifest.xml with these two lines already added (RECORD_AUDIO and the `chatfiles` FileProvider were already there). `res/xml/chat_file_paths.xml` now also allows `chat_export/`.

## 3. Gradle / catalog
None required. **Optional (GIF search):** the Giphy key must not be typed in code. If you want GIF search:
1. In `local.properties` (never committed): `GIPHY_API_KEY=your_key`
2. In `app/build.gradle.kts` → `android { buildFeatures { buildConfig = true } defaultConfig { buildConfigField("String", "GIPHY_API_KEY", "\"${localProps.getProperty("GIPHY_API_KEY", "")}\"") } }` (read `local.properties` into `localProps`).
Without it the GIF sheet says "GIF search is not set up yet" and everything else works.

## 4. TwinDatabase / strings / permissions
No entities. `strings_chat.xml` extended (all `chat_*`). Permissions: location (above), microphone (existing).

## 5. Backend changes
None.

## CHANGE REQUESTS (not done, need a catalog line)
- CameraX, Media3 (as before).
- `io.coil-kt:coil-gif` for animated GIFs.
- A backend endpoint for the typing indicator.
