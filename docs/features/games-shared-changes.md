# SHARED_CHANGES — Games

The three edits below are **already applied** in this zip. They are listed so you can review them.

## Gradle / catalog lines
None.

## Manifest entries
None. (Voice input uses the phone's speech dialog, which needs no permission; read-aloud uses the built-in text-to-speech.)

## `core/navigation/Routes.kt`
```kotlin
    // Games page (hub + Tic Tac Toe, Memory Match, Ludo, Truth or Dare), opened from the Home "Games" chip
    @Serializable data object Games
```

## `core/navigation/FeatureGraphs.kt`
```kotlin
import com.twinhearts.app.feature.games.gamesGraph
...
    gamesGraph(navController) // Games (hub + Tic Tac Toe · Memory Match · Ludo · Truth or Dare)
```

## `feature/home/HomeRoute.kt`
```kotlin
                    HomeShortcut.Games -> navController.navigate(Routes.Games)
```
