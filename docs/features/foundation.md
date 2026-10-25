# Phase 0 — Foundation (audit + explanation)

## What this is
The base every feature stands on: build setup, design system, network, local data, navigation.
No user-facing feature yet. After this step the app opens to a small "built in a later step" page.

## What is in it
| Part | Where | Plain-English meaning |
|---|---|---|
| 0.1 Project setup | `gradle/libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts`, `AndroidManifest.xml` | One list of library versions. Hilt wired. Release builds are shrunk by R8; nothing debug-only is in release. |
| 0.2 Design system | `core/design/` | Colours, fonts, spacing, and the 15 shared components (R-52). Debug builds have a **Design gallery** screen to see them all. |
| 0.3 Network | `core/network/` | One Retrofit. `safeApiCall` turns any failure into an `AppError`. Long timeouts so a sleeping Render server does not crash the app. |
| 0.4 Navigation | `core/navigation/` | One Activity, type-safe `Routes`, bottom bar (Home · Chat · Twin orb · Map · Settings) shown only on those five screens. |
| 0.5 Local data | `core/data/` | DataStore (holds the signed-in `Session`) and an empty-ish Room database. |

## Decisions made in this phase
- **Versions** are pinned to the toolchain already building on your PC (AGP 8.13.2, Kotlin 2.0.21). Newer library lines that need a newer Kotlin or AGP are avoided on purpose. `compileSdk`/`targetSdk` = 36.
- **Session (R-64):** the old server has no cookies or tokens. `routes/auth.js` returns `userId`, `coupleId`, `myName`, `partnerName`, `role`, `paired`, … and later calls carry those ids. So `Session` + `SessionStore` live in `core/data` (DataStore). No auth interceptor is needed. Not verified yet: how each later route reads the ids (checked per feature, R-65).
- **Room needs one table**, so `TwinDatabase` ships a tiny `cache_entry` key/value table. Features add real tables later (R-45).
- **Pending screens:** until a feature exists, its route shows a small stand-in page so the bottom bar can never open a missing screen. Each feature delivery removes its line from `FeatureGraphs.kt`.
- **No logging of requests.** Request URLs contain user ids, so no logging interceptor is added.
- **API address** comes from `local.properties` (`API_BASE_URL=...`), with the public Render address as the fallback. It is not a secret (the web JS already contains it).

## Not ported / left for later
- Light theme (R-54): tokens are a class, so it can be added without touching features.
- Firebase / `google-services` plugin: added in Phase 2.5 (needs your `google-services.json`).
- MapLibre, Media3, CameraX: each is added by its own feature, after looking up the version (R-15).

## Behaviour notes
- First screen: **Get Started** if nobody is signed in, **Home** if a session exists.
- Animations: the Twin orb and the loading skeleton stop when the phone's "remove animations" setting is on.
- Window insets: `TwinTopBar` pads the status bar, `TwinBottomBar` pads the gesture bar. Screens without the bottom bar get the gesture-bar padding from `TwinApp`.
