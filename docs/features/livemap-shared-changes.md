# Shared-file changes — Phase 5 — Live Map

All feature code is in `feature/livemap/`, texts in `res/values/strings_livemap.xml`. The shared files below are **already edited in this zip**; this list says what changed so you can review it.

## Gradle / catalog lines
`gradle/libs.versions.toml`
```toml
[versions]
maplibre = "11.13.5"
[libraries]
maplibre-android = { group = "org.maplibre.gl", name = "android-sdk", version.ref = "maplibre" }
```
`app/build.gradle.kts` → `implementation(libs.maplibre.android)`

Version note: MapLibre Native Android 13.6.1 is the newest release I found. I could not check here whether it builds with this project's Kotlin 2.0.21 / AGP, so I chose 11.13.5 (an older release the changelog confirms). If you want the newest, change only the `maplibre = "…"` line and Sync.

## Manifest entries
Three permissions: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, `POST_NOTIFICATIONS` (location permissions were already there), and one service:
```xml
<service android:name=".feature.livemap.data.share.LiveShareService"
    android:exported="false" android:foregroundServiceType="location" />
```

## FeatureGraphs.kt
Added `import com.twinhearts.app.feature.livemap.liveMapGraph` and, after `usGraph(...)`: `liveMapGraph(navController) // 5.3 Live Map`.
Removed `pending<Routes.LiveMap>(...)` from `pendingGraphs`. `Routes.LiveMap` already existed, so no change to `Routes.kt`.

