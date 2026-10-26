# SHARED_CHANGES — Phase 2.1 Home v3 (card photo menu + editor, card look)

## Gradle/catalog lines
None. (Coil, Activity Compose and OkHttp are already in the catalog.)

## Manifest entries
None. The photo picker needs no permission.

## AppNavHost line
None. `homeGraph` is already registered. The Card photos editor is a full-screen dialog inside Home, so no new route.

## TwinDatabase entities
None.

## Strings/permissions
`res/values/strings_home.xml` is replaced (new `home_photo_*` and `home_card_*` strings at the end; `home_chip_camera` is renamed `home_chip_memories` with the text "Memories"). No permissions.

## Backend changes
None. Uses existing `POST api/media/upload`, `DELETE api/media/delete` and `POST api/data/state`.

## CHANGE REQUEST: card overlay tokens
Why: the picture card draws black/white overlays (shades 74/32/22 %, caption pill, button glass, menu fill, 24 dp card radius). R-50 forbids hard-coded colours in a feature, but these are not theme colours, so they sit in `feature/home/ui/HomeCardStyle.kt`.
Proposed: add the same values to `TwinColors` / `TwinShapes` (`cardShade`, `cardChip`, `cardGlass`, `cardRadius` ...) and make `HomeCardStyle` read them.
Affects: Home only (later: any screen that puts text on a photo).

## CHANGE REQUEST: mood colour tokens (still open from v2)
Why: the 8 mood colours are in `feature/home/ui/HomeWidgets.kt` (`MoodKind.color()`).
Proposed: add `moodExcited ... moodAngry` to `TwinColors`.
Affects: Home only.
