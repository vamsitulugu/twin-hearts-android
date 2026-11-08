# Cinema 3D — theatre and lobby model files (C0: assets only)

Why this exists: the web cinema builds its hall and lobby with code, so there were no model files. These were exported once from `twin-cinemas` so the Android 3D theatre (Filament) can load them. No Kotlin or Gradle change is in this delivery.

## Files (in `app/src/main/assets/cinema3d/`)
| File | Size | Triangles | What it is |
|---|---|---|---|
| `theatre_hall.glb` | 1.7 MB | ~136k | Hall shell, carpet, stepped floor, 135 seats, ceiling frames, wall panels, speakers, sconces, screen, door |
| `theatre_lobby.glb` | 0.7 MB | ~14k | Lobby, canteen counter, popcorn machine, menu TVs, sofas, poster frames |
| `theatre_layout.json` | 26 KB | – | Seat positions, floor height table, lobby collision boxes, screen centre (for walking and sitting in Kotlin) |

Units are metres, Y up. The screen is at -Z, the hall door at +Z (z = 6.6), and the lobby continues beyond the door in +Z. Both models use the same world coordinates, so load both with no offset.

## Names inside the models (use these to find parts in Filament)
- `seat_A1` … `seat_I15` — one node per seat (rows A–I, 15 columns). Each seat has its own material, so a seat can be highlighted on its own.
- `screen_surface` — the movie screen. Replace its material with the ExoPlayer external texture. `screen_bezel` is its frame.
- `door_left`, `door_right` — door leaves; rotate around Y to open.
- `poster_0` … `poster_9` — grey placeholder planes in the lobby for movie posters and TV images.
- `downlights` — small ceiling light discs (merged mesh).

## What was changed from the web version, and why
- **No lights in the files.** Android adds its own (one directional light + image-based light). Glowing parts stay glowing through emissive materials.
- **Inside-out wall shells made double-sided.** glTF has no back-face setting, so walls would vanish from the inside otherwise.
- **Shader-material poster frames and instanced downlights replaced** with plain meshes (glTF cannot store shaders or rely on instancing in Filament's loader).
- **Seats simplified** (222k → 136k triangles). Only seat meshes were reduced; walls, ceiling frames and the lobby are unchanged.
- **Not in the files:** bump maps (glTF core has none), additive glow sprites' blending, the door animation, TV and poster content, the lobby crowd. These are done in Kotlin.
- Textures are the web's procedural canvases saved as PNG (carpet, wood, floor, letters, exit signs). They total under 1 MB.

## Not verified
Exported and checked with the official glTF validator (0 errors). It has **not** been opened in Filament or on a phone, because the sandbox has no GPU. First check in the C1 spike: load `theatre_hall.glb` and confirm the walls, seats and screen show from inside the hall.

## How to regenerate (in the `twin-cinemas` repo)
    npm install
    npm i --no-save canvas @gltf-transform/core @gltf-transform/functions @gltf-transform/extensions meshoptimizer
    node scripts/export-theatre.mjs export-out
    node scripts/optimize-theatre.mjs export-out
Then copy the three files from `export-out/` into `app/src/main/assets/cinema3d/`.
