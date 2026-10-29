# SHARED_CHANGES — Meet Planner v1
(Named with `_meetplanner` so it does not overwrite the other features' file. Everything below is already applied in this zip.)

## Gradle / catalog lines
None. (Uses Coil, Hilt, Retrofit, Compose already in the catalog.)

## Manifest entries
None. INTERNET and location permissions already exist.

## AppNavHost line (YOU add these 3 lines by hand — v2 zip does NOT overwrite these files)
`core/navigation/FeatureGraphs.kt`: `meetPlannerGraph(navController) // 4.x Meet Planner` + import.
`core/navigation/Routes.kt`: `@Serializable data object MeetPlanner`.
`feature/home/HomeRoute.kt`: `HomeShortcut.MeetPlanner -> navController.navigate(Routes.MeetPlanner)`.

## TwinDatabase entities
None.

## Strings / permissions
New file `res/values/strings_meetplanner.xml` (all `meetplanner_*`). Location permission is asked at the moment of "Use my location".

## Backend changes
Optional: `backend-changes/meetplanner/meetup_plans.sql` if the `meetup_plans` table is missing.

## CHANGE REQUEST (not applied)
Promote to core later: `MeetPalette` amber/category colours (now in `MeetComponents.kt`), `MeetDateField`, `MeetTextArea`.
