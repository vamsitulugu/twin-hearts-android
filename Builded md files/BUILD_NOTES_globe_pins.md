# BUILD_NOTES — Globe: auto-spin, burgundy pins, tap animation, sliding card (not compiled in the sandbox)

1. Unzip over the project root (paths are project-relative). Overwrites 5 Kotlin files in `feature/globe`.
2. Android Studio → Sync → Run on your phone (internet on).

## What changed
- `GlobeMapView.kt` — the globe now **starts turning again by itself** 2.5 s after your finger leaves it, whenever the Earth is what you see (also after you zoom back out). It stays paused while a pin is open. Tapped pin gets its own layer: spring "pop" + burgundy ripple ring. After a tap the camera waits ~0.4 s so you see the hop, then flies down (map top half, trip card bottom half, as before).
- `GlobeStyle.kt` — pins redrawn as a classic **burgundy map-location pin** (gradient, white rim, gloss, cream hole with the mood emoji, count badge). Cluster bubbles are burgundy too. Colours are fixed (`PIN_BURGUNDY*`), not theme-driven.
- `GlobeCanvas.kt` — on the 3D globe the tapped pin hops (squash/stretch) with a ripple on the ground.
- `GlobeComponents.kt` (`SheetFrame`) — the card is now a **sliding card**: drag the handle/title up = almost full screen, down = back to half, down again = slides away and closes. Pulling the content down while it is scrolled to the top also drags the card. X button, handle tap and back still work.
- `GlobeScreen.kt` — card slides up from the bottom on open and slides down on close; passes `spinPaused` to the map.

## Manual tests
1. Open Globe, do nothing: it turns. Drag it, let go: it coasts, then turns again after ~2.5 s.
2. Pinch in to the map, then pinch back out to the globe: after a moment it turns again.
3. Pins are burgundy teardrops with a cream centre and the emoji.
4. Tap a pin: it hops + ripple, then the camera flies down; half map / half trip card.
5. Drag the card handle up (grows), down (half), down again (slides away; pin deselects). Pull the card's content down when scrolled to top: same.
6. Back button closes the card.

## If it fails, send me
The Gradle error (file + line) or Logcat lines with `GlobeMap` / `Style`.

## SHARED_CHANGES
None: no new dependencies, strings, manifest entries or DB changes. Only files inside `feature/globe` (R-30).
