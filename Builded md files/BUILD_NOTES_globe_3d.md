# BUILD_NOTES — Globe 3D v1 (not compiled in the sandbox)

1. Unzip over the project root (paths are project-relative). Overwrites 5 Kotlin files in `feature/globe`, adds 2 Kotlin files + `assets/globe_earth.jpg`.
2. Android Studio → Sync → Run on your phone (internet on).
3. Open Home → Globe.

## Manual tests
1. **Globe tab**: you should see a blue-marble Earth with blue glow, stars, white borders, "AFRICA / EUROPE / Indian Ocean" names. Drag to spin, release to coast.
2. **Pins**: pin a memory; the heart pin sits on the right city while spinning (may trail one frame).
3. **Zoom**: pinch in. Country names appear around 2.4x. At about 2.8x the satellite map with city names fades in on the same Globe tab (the Globe tab stays selected). Pinch out again: the globe returns.
4. **Street level**: zoom the Map to 15+: roads, names, grey 3D buildings, tilted camera, badge "Street level — real roads & buildings".
5. **Offline**: turn data off, open Map: it falls back to the plain street style (no crash).

## If it fails, send me
Logcat lines containing `EarthRenderer`, `Style` or `GLES`, plus a screenshot.

6. **Tap a pin on the globe**: spin stops, globe closes in, satellite map zooms down to the city, half-height trip sheet opens. Handle tap = bigger sheet.
7. **Reset view**: back to the spinning globe.
