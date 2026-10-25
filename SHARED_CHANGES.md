# SHARED_CHANGES — Phase 1.3 Partner pairing

## Gradle/catalog lines
None.

## Manifest entries
None.

## AppNavHost line
Included as finished files (replace the old ones):
- `core/navigation/FeatureGraphs.kt` — adds `pairingGraph(navController)` and removes the stand-in Pairing screen.
- `feature/auth/AuthRoute.kt` — one change: after **Sign Up** the app opens Pairing instead of Home.

## TwinDatabase entities
None.

## Strings/permissions
New file `strings_pairing.xml`. No permissions.

## Backend changes
None.
