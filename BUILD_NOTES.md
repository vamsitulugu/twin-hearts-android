# BUILD_NOTES — Twin AI (2.4)

**Not compiled here** (no Android SDK in my sandbox). Build it in Android Studio.

## Apply
1. Unzip over the project root `C:\Users\vamsi\...\twin-hearts-android` (paths are project-relative). It adds the `feature/twinai` folder, tests, one strings file, docs, and overwrites `SHARED_CHANGES.md`, `BUILD_NOTES.md`, `README.md`.
2. Make the 3 small edits in `SHARED_CHANGES.md` (manifest `<queries>`, `FeatureGraphs.kt`, `TwinApp.kt`).
3. Android Studio: **File > Sync Project with Gradle Files**, then **Run**.
4. Unit tests: `gradlew.bat testDebugUnitTest --tests "com.twinhearts.app.feature.twinai.*"`

## What to open
Sign in, then tap the **centre Twin orb**.

## Manual tests
1. **Welcome:** you see "Hey <your name>," and "I'm Twin AI.", the robot, and 5 chips (Plan a date, We argued, Write something sweet, Deep talk, Surprise idea).
2. **Ask:** tap "Plan a date". Your bubble appears, then "Thinking...", then Twin's answer grows word by word. Chips disappear. Copy / Regenerate / Listen show under the answer.
3. **Errors:** turn on airplane mode, ask something. You get an error bubble with Retry. Turn it off, tap Retry.
4. **Mic:** tap the mic, say a sentence. It is sent to Twin.
5. **BOTH:** if the tab shows, tap BOTH > Start Together, write your side, Submit privately. Open the web app as your partner, submit there; the result appears here within about 3 seconds.
6. **Back:** inside a discussion, Back returns to the list; on the list, Back goes Home.

## If something fails, send me
The exact Android Studio error text (or a screenshot) and the file name. I will fix it and send only changed files.

## Known gaps (see docs/features/twinai.md)
Search and menu buttons show "built in a later step". Chat history is kept only while the app is open (same as the web page). The first question after a long idle can take ~1 minute (free Render server waking up).
