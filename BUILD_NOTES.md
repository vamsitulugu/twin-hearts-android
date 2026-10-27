# BUILD_NOTES — Phase 2.2 Chat v2

**Not compiled here.** Build in Android Studio and send me the first red error if there is one.

## Apply (Windows)
1. Close Android Studio. Unzip `th-p2-chat-v2.zip` over your project folder (replace files when asked).
2. Read `SHARED_CHANGES.md` (manifest is already in the zip; GIF key is optional).
3. Android Studio → **Sync Project with Gradle Files** → ▶ Run.

## Test by hand (phone + web chat open)
1. Send text, then *bold* _italic_ ~strike~ and a link: styles show, link card opens the browser.
2. Long-press a message: react ❤️, reply, edit (your own), pin (bar appears), star, info, delete for me / everyone. Check the web matches.
3. "+" → Photos: pick 3, rotate one, caption, send. Tap it: full-screen. Send one photo with "View once"; open it on the other side.
4. "+" → Poll: create, vote from web (reply) and see counts. Also try Location, Sticker, Gift, Document.
5. ⋮ → Chat wallpaper: pick a photo, move & scale, dim, save; ⋮ → Shared media; ⋮ → Disappearing messages; long-press send → Hearts effect.
6. Wi-Fi off, send: "Failed · tap to retry".
7. Font size Large: header and composer still fit.

Unit tests: `gradlew.bat testDebugUnitTest`.
