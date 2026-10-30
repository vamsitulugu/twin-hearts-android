# Meet Planner: real map + real in-app navigation

## Two ways to apply
1. Copy the folder `files/` over your project root (paths are repo-relative), OR
2. `git apply meetplanner_real_map_navigation.patch` from the twin-hearts-android root.

## What changed
- Your Stops: the line drawing is now a real map (MapLibre, same engine as Live Map) with numbered stops.
- Route Preview: real map with the road route and numbered stops.
- Start Navigation: real turn-by-turn INSIDE the app (reuses Live Map's NavEngine): next-turn banner,
  voice, rerouting, ETA bar, keeps running with screen off. Goes stop 1 -> 2 -> 3, with a "Next stop" card between.
  Back during navigation ends the trip.
- "Open in Maps app" button keeps the old Google Maps hand-off. Transit mode, no internet, or a failed
  route also falls back to it automatically.
- Starts from your current location (asks location permission on first use). If you are already at a stop
  (within 150 m) it heads to the next one.

## Files
Modified: livemap/ui/map/{LiveMapView,MapModels}.kt (numbered badges, rounded-corner texture mode, scroll lock),
meetplanner/ui/{MeetActions,MeetUiState,MeetPlannerViewModel,MeetPlannerScreen}.kt,
meetplanner/ui/screens/{StopsScreen,RouteScreen}.kt, res/values/strings_meetplanner.xml
New: meetplanner/ui/components/MeetMap.kt

No new dependencies, manifest entries or permissions (MapLibre, NavigationService and location permissions already exist).
`RouteCanvas` is now unused; you can delete it later.

## Checked against
Your latest commit 36dd171 ("UPdate", includes the new Globe changes). Patch applies cleanly; the Globe files are untouched.

## Not tested
Written without an Android SDK, so it was not compiled or run. Please build it (Sync + Run) and send
me the first error or screenshot if anything shows up.
