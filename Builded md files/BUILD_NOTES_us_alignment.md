# Love Counter + Dream Goals alignment fix

Unzip over the project root, Sync, Run. Only files in `feature/us/ui/` changed. No new dependencies.

Fixed
- Tab bar (Love Counter / Collection / Dream Goals) no longer sits under the status bar (clock, signal).
- Love Counter: time grid is now 3 x 2 with equal cards; labels (MINUTES, SECONDS) stay on one line; long numbers shrink instead of overflowing. Page scrolls. Even spacing between Milestones and Anniversary. Hero number uses real font size (no scale hack that pushed layout).
- Dream Goals: page was a non-scrolling column with a grid inside it, so cards were squeezed and cut off. Now one scrolling list: 2 x 2 stats, insight card, progress cards, then goal cards in two even columns with equal heights and one-line actions.

Manual tests
1. Open Love Counter: tabs start below the status bar.
2. Check six time cards are the same size; scroll down to the Anniversary card.
3. Open Dream Goals with 0, 1 and 3 goals; scroll to the last card (not hidden by the nav bar).
4. Font scale 1.3: no cut-off labels.

## Update 2
- Top bar added like the other pages: menu button on the left, name of the open page: Love Counter, Collection or Dream Goals (changes when you switch tab), sync on the right. It also keeps the tabs below the status bar.

## Update 3: Dream Goals opened Love Counter (bug fix)
Cause: Love Counter and Dream Goals shortcuts both went to the same Us page, which always started on tab 0 (Love Counter). The side-menu "Dream Goals" row had no route at all.
Fix: `Routes.Us` now carries a tab (`Routes.Us(tab = 2)` = Dream Goals, 0 = Love Counter, 1 = Collection).
- Home "Dream Goals" shortcut -> Dream Goals; "Love Counter" -> Love Counter.
- Side menu: Dream Goals row now works, and the right row is highlighted for the open tab.
- Profile "goals" button opens Dream Goals.
Files: Routes.kt, UsRoute.kt, UsScreen.kt, HomeRoute.kt, ProfileGraph.kt, TwinSidebar.kt, TwinApp.kt.
Tests: tap Dream Goals from Home, from the side menu, and from Profile; each opens on the Dream Goals tab. Love Counter opens on Love Counter. Switching tabs inside the page still works.
