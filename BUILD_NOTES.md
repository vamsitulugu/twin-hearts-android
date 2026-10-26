# BUILD_NOTES — Phase 2.2 Chat

**Not compiled here.** Build in Android Studio and send me the first red error if there is one.

## Apply (Windows)
1. Unzip `th-p2-chat.zip` over your project folder.
2. Do the edits in `SHARED_CHANGES.md` (FeatureGraphs.kt 2 lines, Manifest permission + provider).
3. **Sync Project with Gradle Files**, then ▶ Run.

## Test by hand (two phones or phone + web)
1. Open Chat: old messages load, newest at the bottom, day pills, ticks on your messages.
2. Send a text on the phone: ⏱ then ✓. Open the web chat: it arrives; reply there: the phone gets it within ~3 s.
3. Long-press a message: Reply (quote shows), Copy, Star. ★ button lists starred ones.
4. Gallery icon: pick a photo; camera icon: take one. Both appear in the chat on web too.
5. Mic: allow microphone, speak 3 s, send. Tap ▶ in the bubble to play. On the web it plays too.
6. Search (🔍): type a word; try Media / Links / Docs.
7. ⋮ → Scheduled messages: schedule "hi" in 15 min; it is listed; cancel it.
8. Turn Wi-Fi/data off and send: the bubble shows "Failed · tap to retry"; turn it on and tap it.
9. Font size Large: header and composer still fit.

Unit tests: `gradlew.bat testDebugUnitTest`.
