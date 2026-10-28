# BUILD_NOTES — Meet Planner v1
Not compiled by Claude. Please build it first.

## Steps (Windows)
1. Unzip `th-p4-meetplanner-v1.zip` over your project folder (say yes to replace).
2. Open in Android Studio → Sync Gradle.
3. Build → Make Project. Send me the first error text if any.
4. Run `./gradlew.bat testDebugUnitTest` (optional).
5. Run the app on a phone/emulator, sign in as a paired couple.

## Manual tests
1. Home → Meet Planner chip opens the screen; back returns to Home.
2. Plan New → pick Goa, a date, Create Plan → add 3 places → Build Itinerary → Save → it appears in Saved.
3. Saved → open it → countdown shows; tick a checklist item, reopen: still ticked.
4. Long-press a card → select → delete → confirm → toast "Deleted".
5. Past-dated plan → Mark Complete with a photo → "Saved to Memory Globe!" → View Photos.

## Add these 3 lines by hand (v2 does not touch your shared files)
1. `core/navigation/Routes.kt`, inside `object Routes { ... }`:
   `@Serializable data object MeetPlanner`
2. Where your nav graphs are registered (FeatureGraphs.kt or TwinApp.kt), add:
   `meetPlannerGraph(navController)` and `import com.twinhearts.app.feature.meetplanner.ui.meetPlannerGraph`
3. `feature/home/HomeRoute.kt`, in the `when (shortcut)`:
   `HomeShortcut.MeetPlanner -> navController.navigate(Routes.MeetPlanner)`
