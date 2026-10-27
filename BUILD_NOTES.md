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


## Update 2: what to test (remaining Twin features)
1. Open Twin. You should see **YOU / BOTH** straight away (no flag needed).
2. Tap **BOTH**. Pro couple: the Together page opens and the subtitle says "For Both of Us". Not Pro: a card says "AI Both Mode is a Pro feature" and you stay on YOU.
3. On BOTH: history cards show intent, date, title, rounds. Tap the vertical dots, then "Delete Discussion", read the warning, cancel.
4. Start Together, type or tap the mic in the answer box, submit, tap Remind (toast with the partner's name).
5. Press Back: discussion goes to the list; list goes to YOU; YOU goes Home.
6. On YOU tap the **mic**: allow the microphone, say "give me a date idea". Watch Listening, Thinking, Speaking, and the screen closing by itself. Try tapping the mic while Twin speaks (interrupts), the X, the Back button, and staying silent (closes after about 1.2 s).
7. Leave the chips alone for 2 s after scrolling them: they glide back to "Write something sweet".
8. Manifest: add the third `<queries>` entry from SHARED_CHANGES.md (RecognitionService), otherwise voice mode says voice is not supported on Android 11+.


## Update 3: what to test (web parity pass)
**Not compiled here.** Apply the zip, sync Gradle, run. Also run `gradlew.bat testDebugUnitTest --tests "com.twinhearts.app.feature.twinai.*"` (new: `BothViewModelTest`, link tests, offline-orb test).
1. **No bottom bar** on the Twin page; the orb sits on the input bar.
2. **Voice / Listen:** on an Android 11+ phone, voice mode and Listen work (they used to say "unavailable").
3. **Mood:** write a journal mood on the web, ask Twin on Android; the answer should reflect it.
4. **Offline:** turn on airplane mode; the dock orb goes grey. Turn it off; it returns.
5. **Chat feel:** new bubbles pop in; a blinking bar follows the last word while Twin writes; the send button grows when you type and spins while Twin answers; tap the page outside the "+" sheet to close it.
6. **Links:** ask Twin for a link (e.g. "send me a recipe link in markdown"); it is pink, underlined and opens the browser.
7. **BOTH:** hero shows the two robots; Start Together has a chevron. Open a discussion: you see "Opening discussion…" and then names with photos. Kill the app while a discussion is open and reopen Twin > BOTH: it comes back to that discussion.
8. **Fixes:** edit an answer while the partner submits first (round number stays); submit second (says "analyzing", not "waiting"); Remind unlocks after 2 s and shows the server's 5-minute message; sign out and open BOTH (error card with Retry, not "Loading…").
9. **Colour:** hero title, YOU/BOTH tab, send and BOTH buttons are the solid wine `#6B093A`.
