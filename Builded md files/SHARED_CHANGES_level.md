# SHARED_CHANGES — Couple Level
Already applied inside this zip; listed for review.

## Gradle / catalog lines
None.

## Manifest entries
None.

## core/navigation/Routes.kt
`@Serializable data object Level`

## core/navigation/FeatureGraphs.kt
`import com.twinhearts.app.feature.level.levelGraph` and, in `featureGraphs`: `levelGraph(navController) // 3.2 Couple Level`

## feature/home/HomeRoute.kt
One line in the shortcut `when`: `HomeShortcut.Level -> navController.navigate(Routes.Level)`

## Strings / permissions
New file `res/values/strings_level.xml`. No permissions.

## Backend changes
None.
