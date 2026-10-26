# SHARED_CHANGES — Phase 2.1 Home

## Gradle/catalog lines
None.

## Manifest entries
None.

## AppNavHost line
Finished files are included (replace the old ones):
- `core/navigation/FeatureGraphs.kt` — adds `homeGraph(navController)` and removes the stand-in Home screen.
- `core/navigation/AppViewModel.kt` — signed-in users start on Home again (last time I made unpaired users start on Pairing as a temporary fix; Home now has the "Connect with your partner ❤️" card).

## TwinDatabase entities
None.

## Strings/permissions
New file `strings_home.xml`. New pictures: `home_card_idle/hug/touch/miss.webp`. No permissions.

## Backend changes
None needed. (Optional later: a small route returning only touch/hug/missYou, see docs/features/home.md.)
