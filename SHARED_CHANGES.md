# SHARED_CHANGES — Phase 5 Watch Together (movies) v1

All items below are **already applied inside this zip** (unzip over the project root). Listed so you can review.

## Gradle / catalog lines
`gradle/libs.versions.toml`:
```toml
[versions]    media3 = "1.4.1"
[libraries]   androidx-media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
              androidx-media3-ui = { group = "androidx.media3", name = "media3-ui", version.ref = "media3" }
```
`app/build.gradle.kts` → `dependencies { }`:
```kotlin
implementation(libs.androidx.media3.exoplayer)
implementation(libs.androidx.media3.ui)
```
(1.4.1 chosen to match Kotlin 2.0.21 / Compose BOM 2024.09 already in the catalog. Not looked up online; bump later if you want.)

## Manifest entries
None. (No new permission: the file picker needs none; INTERNET exists.)

## AppNavHost line
`core/navigation/FeatureGraphs.kt`: `movieGraph(navController) // 5.2 Watch Together` + its import.

## CHANGE REQUEST: Routes.Movie (applied)
`core/navigation/Routes.kt`: added `@Serializable data object Movie`. Affects: only Home.
`feature/home/HomeRoute.kt`: added one line `HomeShortcut.Movie -> navController.navigate(Routes.Movie)`.

## TwinDatabase entities
None.

## Strings / permissions
`res/values/strings_movie.xml` (all `movie_*`). No permissions.

## Backend changes
None.
