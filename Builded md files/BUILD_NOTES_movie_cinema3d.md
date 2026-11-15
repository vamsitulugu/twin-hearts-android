# BUILD NOTES — Twin Cinema 3D (Android)

1. Unzip over the project root. Sync Gradle, run the app on a phone (an emulator's WebGL is slow).
2. Deploy `us-app` first (it now contains the cinema, `public/cinema`, and `public/cinema-invite.js`). The theatre itself is bundled in the APK (see BUILD_NOTES_cinema_bundled.md).

## Manual tests (use two accounts that are paired)
1. **Card:** Home → Movie. "Watch in Twin Cinema 3D" is on the Home tab and the Watch tab. Tap it → "Movie night" box → Open cinema lobby → the theatre opens.
2. **Both ready, two buttons:** both phones pick the same movie file → I'M READY on both → "Both Ready" shows START TOGETHER **and** 3D MOVIE (Twin Cinema).
3. **Both go in together:** tap 3D MOVIE on phone A. Phone B (on the same ready screen) goes into the theatre by itself within ~2 seconds. Both are on the avatar page.
   Try web ↔ Android, Android ↔ Android and Android → web.
4. **No second choose-movie:** in the theatre lobby neither phone asks for the movie file. After the show starts both play the file picked on Watch Together.
   (A shared cloud movie also plays by itself.)
5. **Invite card:** phone A taps the card → Open cinema lobby → invites from the lobby. Phone B (anywhere on Watch Together) gets "<name> invited you" → Accept ♥.
6. **Leave:** "Leave theatre" in the cinema closes the theatre and you are back on the Watch Together lobby.

Not compiled in the chat session (no Android SDK there) — if Gradle shows an error, send it back and it will be fixed.
