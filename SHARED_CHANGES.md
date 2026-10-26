# SHARED_CHANGES — Phase 2.1 Home v2 (Coming up + Mood)

## Gradle/catalog lines
None.

## Manifest entries
None.

## AppNavHost line
None. `homeGraph` is already registered. The Mood Calendar is a dialog inside Home, so no new route.

## TwinDatabase entities
None.

## Strings/permissions
`strings_home.xml` is replaced (new strings added at the end). No permissions.

## Backend changes
None.

## CHANGE REQUEST: mood colour tokens
Why: the 8 mood colours (web `MOODS[..].c`) are hard-coded in `feature/home/ui/HomeWidgets.kt` (`MoodKind.color()`), which R-50 normally forbids. They identify a mood, not the theme.
Proposed: add `moodExcited ... moodAngry` to `TwinColors`, then replace `color()` with those tokens.
Affects: Home only (later: Wellness/Journal if they show moods).
