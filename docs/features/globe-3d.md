# Memory Globe v2 — real 3D Earth + satellite street level

## What was wrong
The Globe tab drew flat grey country shapes on a dark ball (screenshots 1–2). The web app shows a satellite Earth that zooms down to streets and 3D buildings (screenshots 3–6, Cesium).

## What changed
| Part | Before | Now |
|---|---|---|
| Globe tab | Canvas-drawn grey ball | **OpenGL ES 3 sphere** with NASA Blue Marble satellite picture (`assets/globe_earth.jpg`, public domain), blue atmosphere, stars, round shading, same colour grading as the web (contrast / saturation / gamma) |
| On the globe | – | Country borders, continent + ocean names (zoomed out), country names (zoomed in), visited-country tint. Pins, trip arcs, live dots, search pin: unchanged, drawn by Compose on top |
| Zoom in | Stopped at 9x | Pinch in past 2.8x → the **satellite 3D map fades in on the same Globe tab**, same spot and scale; pinch out below 2.3x → the globe is back (no tab change, like Cesium) |
| Map tab | Light street map | **Satellite (Esri) + white place names + roads + borders + 3D buildings**, camera tilts 50° from zoom 14. Built from the OpenFreeMap "liberty" style at run time, so fonts and tiles stay correct |
| Fallback | – | If the phone has no OpenGL ES 3, or the picture cannot load, the old Canvas globe is used. If the satellite style cannot be built (offline), the old street style is used |

## How the alignment works
`GlobeAxes` in `EarthGl.kt` repeats the maths of `GlobeView` (right / up / forward). The shader turns each pixel into a point on the ball and reads the picture there. Pins use `GlobeView`, so both agree. Ball centre and radius are sent every frame by `EarthGlState.push`.

## Files
New: `ui/globe/EarthGl.kt`, `ui/map/SatelliteStyle.kt`, `assets/globe_earth.jpg`.
Changed: `GlobeCanvas.kt`, `GlobeStyle.kt` (palette gets `background`), `GlobeMapView.kt`, `GlobeViewModel.kt` (`CameraRequest.jump`, `enterStreetLevel`), `GlobeScreen.kt` (one argument).

## Not ported (and why)
| Item | Why |
|---|---|
| Google Photorealistic 3D Tiles | Paid / needs a Google key (R-21, R-23). 3D extruded OSM buildings are used instead. |
| Terrain relief | MapLibre 11.13.5 has no terrain. |
| Real globe mode in MapLibre | Only in a pre-release (android-v2026.10.19-globe) with no catalog version (R-15). Own GL globe used. |

## Known limits
- Not compiled here (no Android SDK). Review by reading; build in Android Studio.
- The globe picture is 4096 px: it looks soft between 3x and 4.2x, which is why the satellite map fades in at 2.8x.
- The sphere is drawn on a separate GL thread, so during a fast spin the pins can trail by one frame.
- Esri World Imagery is free to view with attribution but has its own terms; check them if you publish the app. The URL is one constant (`SATELLITE_TILES`).
- Continent / ocean names are English text in `GlobeCanvas.kt` (they are map data, like place names), not in `strings.xml`.
- Inside the Globe tab the satellite map is a MapLibre map in texture mode laid over the GL globe; the swap is a 450 ms fade, so the two pictures are matched by scale but not pixel-identical (the map is flat, the globe is round).

## Tapping a pin (v3)
1. Any tap or flight stops the idle spin (it only starts again with **Reset view**).
2. The globe turns to the city and closes in (1.4 s).
3. The satellite 3D map takes over at the same scale and zooms down to street level with the camera tilted (2.8 s).
4. A **half-height sheet** opens over the map: the trip itself if the city has one memory, otherwise the city list. The map keeps the city in the visible top half (map padding). Tap the sheet handle to expand to 90%, tap again to shrink.
5. Reset view returns to the globe.

## v4 fixes (satellite map not appearing)
- Style is downloaded when the Globe page opens and remembered, so zooming in does not wait for the network.
- If the map engine rejects the satellite style, the plain street style is used (logcat tag `GlobeMap`).
- If the map has not appeared after 10 s, a toast is shown and the globe comes back (before, a blank map could cover the globe).
- Pinching the map in past zoom 14 leans the camera 50° so 3D buildings stand up; buildings start at zoom 13.

## v5 fixes (from your screenshots)
- The map hand-over could only happen once: after coming back to the globe it stayed switched off, so the globe zoomed to its 9x limit and went blurry. Every new pinch can now open the map.
- The way back to the globe is now measured from where the map started (zoomed out 0.7 steps), not from a scale formula, so the map no longer bounces straight back to a small globe.
- When the map takes over, the pinch on the globe stops; lift your fingers and pinch again on the map.
