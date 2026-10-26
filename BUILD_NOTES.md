# BUILD_NOTES — Phase 2.1 Home v3 (card photo menu + editor, card look)

**Not compiled here.** Build in Android Studio and send me the first red error if there is one.

## Apply (Windows)
1. Unzip `th-p2-home-v3.zip` over your project folder (replace files). Paths inside are project-relative.
2. **File > Sync Project with Gradle Files**, then press Run.
3. Optional: `gradlew.bat testDebugUnitTest` for the unit tests.

## What you should see (compare with `docs/reference/images.pdf`, page 2)
- First chip says **Memories**.
- Stay Connected card: caption pill top-left, round 3-dots button top-right, three rounded glass buttons at the bottom.

## Tests (by hand, use your real account)
1. Tap the **3 dots** on the card: menu shows "Set my own image" and "Use default image" (greyed).
2. Tap **Set my own image**: "Card photos" opens on the tab of the picture the card was showing. Tap **Change photo**, pick a photo: grid lines and the slider appear. Drag to move, pinch or use the slider to zoom.
3. Tap **Save**: "Saving…", then green "Photo saved 💕". Close the editor: the card shows your photo. Open the web app with the same account: the same photo shows there.
4. Tap the 3 dots again: "Use default image" is now active. Tap it: green "Back to the default picture" and the built-in picture returns.
5. In the editor switch between Idle / Hug / Touch / Miss You: each tab keeps its own picked photo until you save. "Use default picture" appears under the frame only on tabs that have your own photo.
6. Turn on flight mode and Save: a red error shows, the picked photo stays so you can try again.
7. Large font (Settings > Display > Font size): card buttons, caption and the editor still fit.

## Known gaps
Search bar, other chips, grid button, and the page shortcuts still show "This screen is built in a later step." until their pages exist. A picked but unsaved photo is lost if Android closes the app in the background.
