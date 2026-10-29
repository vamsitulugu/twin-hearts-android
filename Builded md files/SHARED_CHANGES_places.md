# SHARED_CHANGES — Places v1
This zip contains ONLY new Places files. It does NOT overwrite any shared file. Please add these 3 lines by hand.

## Gradle / catalog lines
None.

## Manifest entries
None. (INTERNET and location permissions already exist.)

## AppNavHost line (add by hand)
1. `core/navigation/Routes.kt`, inside `object Routes`:  `@Serializable data object Places`
2. Where your feature graphs are registered (`FeatureGraphs.kt`, or `TwinApp.kt` in your copy):
   `placesGraph(navController) // Places` and `import com.twinhearts.app.feature.places.ui.placesGraph`
3. `feature/home/HomeRoute.kt`, in `when (shortcut)`:  `HomeShortcut.Places -> navController.navigate(Routes.Places)`

## TwinDatabase entities
None.

## Strings / permissions
New file `res/values/strings_places.xml` (all `places_*`). Location permission is asked only when "Use My Current Location" is tapped.

## Backend changes
None. Uses the existing `api/data/state` and `api/media/upload`.

## CHANGE REQUEST (not applied)
Promote later: favourite star gold colour (`FavoriteGold`) as a core token; `PlacesDateField` / `PlacesTextArea` (same as Meet Planner's `MeetDateField` / `MeetTextArea`).
