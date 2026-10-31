# SHARED_CHANGES: Search

All of these are already applied in the zip. They are listed so you can check them.

## Gradle/catalog lines
None

## Manifest entries
None

## AppNavHost line
`core/navigation/FeatureGraphs.kt`
```kotlin
import com.twinhearts.app.feature.search.searchGraph
...
searchGraph(navController) // Global search (Home search bar)
```

## TwinDatabase entities
None

## Strings/permissions
New file `app/src/main/res/values/strings_search.xml`. No permissions.

## Routes (core)
`core/navigation/Routes.kt`
```kotlin
/** Global search, opened from the Home search bar. */
@Serializable data object Search
```

## Home
`feature/home/HomeRoute.kt`: inside the `when (shortcut)` add
```kotlin
HomeShortcut.Search -> navController.navigate(Routes.Search)
```

## Backend changes
None (uses two existing endpoints).
