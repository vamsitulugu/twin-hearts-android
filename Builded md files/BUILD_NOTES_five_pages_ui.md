# Memory, Music, Movie, Meet Planner, Daily Routine: same look as Home/Chat/Map/Twin/Settings

Unzip over the project root, then Sync and Run. Not compiled here (no Android SDK).

Changed
- Near-black page (#0A0A0C), grey glass cards/fields/sheets/dialogs, burgundy buttons and "on" switches, light-grey links. No pink.
- One new style file per page (`feature/<page>/ui/components/<Page>Style.kt`); each page's public Screen now wraps its old body in it.
- Logo removed from the top bar on Music, Meet Planner, Daily Routine (back arrow + title stay).
- `core/navigation/TwinApp.kt`: strip above the gesture bar is near-black on these 5 pages.

Test
1. Open each of the 5 pages: no pink/wine glow, black/grey cards, burgundy buttons.
2. Open a bottom sheet/dialog on each (add routine, add movie, song menu, delete memory): grey, not pink.
3. Gesture-bar area: black, no wine band.
4. Music: open Now Playing, Lyrics, Sing.
5. Meet Planner: go through Plan, Search, Stops, Itinerary.
