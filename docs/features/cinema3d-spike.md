# Cinema 3D — C1 spike (Filament inside Compose)

Purpose: answer one question before any more 3D work — **does the hall and lobby render well, and fast enough, on your cheapest phone?** This is a go/no-go test page, not the cinema.

## What it does
- Starts Google Filament on a `SurfaceView` inside Compose and loads `theatre_hall.glb` and `theatre_lobby.glb` from `assets/cinema3d/`.
- One directional light (no shadows), flat ambient light, dark skybox. Baked lighting comes later.
- Camera: **one finger looks around, pinch walks forward/back** (inside the hall and lobby). The **Orbit** preset circles the hall from outside.
- Presets from `theatre_layout.json`: Seat E8, Hall back, Lobby, Orbit.
- Test buttons: Lobby on/off, Quality high/low (FXAA + 4x MSAA vs none), Brightness slider (exposure).
- Top-left readout: fps, average and worst frame time, load time per model, node and mesh counts, and whether the names we need (`screen_surface`, `seat_E8`, `door_left`, `poster_0`…) were found.
- **Copy test report** puts phone model, speed and load numbers on the clipboard so you can paste them back.

## Files (`feature/cinema3d/`)
| File | Job |
|---|---|
| `Cinema3dGraph.kt` | Route registration (R-43) |
| `ui/Cinema3dSpikeScreen.kt` | Compose page, touch, readout, test buttons |
| `ui/Cinema3dSpikeViewModel.kt` | Load state + button state |
| `render/FilamentStage.kt` | All Filament code: engine, scene, camera, lights, glTF loading, frame loop, clean-up |
| `data/CinemaAssets.kt` | Reads the glb files + layout json off the main thread |
| `domain/CameraRig.kt` | Pure camera maths (walk + orbit), no Android types |
| `domain/ViewPreset.kt`, `TheatreLayout.kt` | Camera presets and the layout file model |
| `domain/FrameStatsTracker.kt` | Pure fps / frame-time maths |

## The test (about 10 minutes, on the cheapest phone first)
1. Apply `SHARED_CHANGES_cinema3d_spike.md`, build, open **Sidebar → Explore → 3D theatre (test)**.
2. **Seat E8**: you should see the screen straight ahead, seat backs in front, walls on both sides, a ceiling. Walls must not be missing from the inside.
3. **Hall back** and **Lobby**: turn around with one finger, walk with pinch. Look for missing walls, black or white surfaces, flickering, stretched textures.
4. Readout: the green `names found` lines should say all found.
5. Try Quality low/high and Lobby on/off and note the fps change. Press **Copy test report** at each setting.
6. Leave the page and come back 5 times, and rotate the phone once. Look for a crash or fps getting slower each time (a leak).

## Go / no-go (proposed, change if you disagree)
- **Go**: 30 fps or better at Quality low on your cheapest phone, nothing missing from inside the hall, no crash on leave/return.
- **Fix first**: 20–30 fps. C2 starts with baked lighting and a lower triangle count.
- **No-go**: under 20 fps at low, or Filament will not start. Fallback plan: raw OpenGL ES like the globe, or a much lighter hall.

## Not verified (honest list)
- Not compiled, not run on a GPU. Filament calls were written from its documented API; a first build may need small signature fixes.
- Brightness defaults (sun 100,000 lux, ambient 30,000, exposure f/16 1/125 ISO100) are standard daylight values, not tuned for this hall. The slider is there for that.
- Models load on the main thread: expect the screen to pause for roughly a second. `FilamentStage.loadModel` shows the real number. The real build should use Filament's async loader.
- Orbit shows the hall from outside, so you see the roof and walls, not the inside. That is expected.
