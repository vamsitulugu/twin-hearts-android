# BUILD_NOTES — Phase 2.1 Home v2 (Coming up + Mood)

**Not compiled here.** Build in Android Studio and send me the first red error if there is one.

## Apply (Windows)
1. Unzip `th-p2-home-v2.zip` over your project folder (replace files).
2. **Sync Project with Gradle Files**, then ▶ Run.

## What you should see
Under Stay Connected: **Coming up** (cards, scroll sideways), then the **Mood** card, then Us / Explore Together / My Space / Plans.

## Tests (by hand)
1. Tap 😊 on the Mood card: it highlights, green "Mood saved 😊". Tap again: it clears.
2. Open the same account on the web: today's mood matches. Change it on the web, wait ~8 s: the app card follows.
3. Tap the Mood card (not an emoji): the Mood Calendar opens. Pick a past day, choose a mood, go back/close: the card graph updates. Future days cannot be tapped.
4. Two accounts paired: You / partner tabs appear; partner's mood is shown but cannot be changed.
5. Not paired: no partner line or tabs; Coming up shows "Nothing planned yet" until a birthday/anniversary/event exists.
6. Large font: quick mood buttons and day columns still fit.

## Known gaps
Card photo menu (3 dots) not built yet. Calendar and Love Counter pages do not exist yet, so tapping an item shows the "built in a later step" note. Search bar, chips and grid button are still look-only until their pages exist.

Unit tests: `gradlew.bat testDebugUnitTest`.
