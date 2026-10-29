# BUILD_NOTES — Fight Log

**Not compiled here** (no Android SDK in my sandbox). Build it in Android Studio.

## Apply
1. Unzip over the project root (it only ADDS new files; nothing of yours is replaced). Then make the 6 small edits in SHARED_CHANGES_fights.md.
2. **File > Sync Project with Gradle Files**, then **Run**.
3. Tests: `gradlew.bat testDebugUnitTest --tests "com.twinhearts.app.feature.fights.*"`

## Where to open it
Home > the "Fight Log" shortcut in the Us group.

## Manual tests
1. **Empty:** a couple with no fights shows the ring with "—", "No fights logged", both tiles "Not enough history yet", and "No conflicts recorded."
2. **Log:** Log a Fight > type a title > Next > pick feelings > Next > pick approach > Save Fight. You return to the dashboard with the new row; open the web app and check it shows there too.
3. **Title needed:** tap Next with an empty title: you stay on step 1 and see the message.
4. **Draft:** type a title, tap Save Draft, leave the page, come back and tap Log a Fight: "Draft restored".
5. **Resolve:** tap an open fight > Mark resolved. The ring and counts update.
6. **AI Help:** tap an open fight > AI Help. The Twin page opens and asks the question.
7. **Delete:** open a fight > Delete > confirm.
8. **Back:** inside the wizard, Back goes one step back, then to the dashboard, then leaves the page.
9. **Activities:** Reconnect Activities > filter tabs > tap a card to read the steps.
10. **Partner sync:** log a fight on the web, press Sync on Android: it appears.

## If something fails, send me
The exact Android Studio error text (or a screenshot) and the file name. I will fix it and send only the changed files.
