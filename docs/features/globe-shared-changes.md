# SHARED_CHANGES — Memory Globe

All new files are inside `feature/globe/`, plus `res/values/strings_globe.xml`, `assets/globe_countries.bin`, and tests. Three small core edits are **already applied in the zip**; the snippets are here in case your files changed since.

## Gradle / catalog lines
None. Uses Retrofit, Hilt, Compose, kotlinx.serialization, Coil, Media3 (already there), MapLibre (already there).

## Manifest entries
None. `INTERNET`, `RECORD_AUDIO`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` are already declared.

## New asset
`app/src/main/assets/globe_countries.bin` (77 KB). The world map: country outlines (Natural Earth 1:50m, public domain, simplified), ISO codes and common names. Android packages the `assets` folder automatically.

## Core edits (applied in the zip)
1. `core/navigation/Routes.kt`: add `@Serializable data object Globe` (after `Movie`).
2. `core/navigation/FeatureGraphs.kt`: `import com.twinhearts.app.feature.globe.globeGraph` and, inside `featureGraphs`, `globeGraph(navController) // 6.x Memory Globe`.
3. `feature/home/HomeRoute.kt`: add `HomeShortcut.Globe -> navController.navigate(Routes.Globe)` to the `when`.

## TwinDatabase entities
None.

## Backend changes
None needed. Optional fixes for the web page are in `backend-changes/globe/`.

## Tracker update (README.md §12)
Add the handoff row below (already done in the README copy in this zip). Mark **Memory Globe** as 🟨 in Phase 6.
