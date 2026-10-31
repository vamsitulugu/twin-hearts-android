# BUILD NOTES: Search

Not compiled in the sandbox (no Android SDK). The pure logic (matcher, privacy rule) was compiled with kotlinc 2.0.21 and its 12 tests pass. Build in Android Studio.

## Steps (Windows)
1. Unzip over the project root. Android Studio: **File > Sync Project with Gradle Files**.
2. Run the app, sign in, open **Home**.
3. Tap the search bar.

## Manual tests
1. Type `notif`: **Notifications (in Settings)** appears at once. Tap it: Settings opens.
2. Type `dark mode`: **Appearance**. Type `ludo`: **Ludo (in Games)**.
3. Type a word you sent in chat: under **Chat messages** your message shows with who sent it and the date. Tap: Chat opens.
4. Add a calendar event "Dinner date", then search `dinner`: it shows under **Inside your pages**. Tap: Calendar opens.
5. Turn on airplane mode, search `calendar`: pages still work and a short error line explains messages are offline.
6. Close and reopen Search: your last searches are listed under **Recent searches**.

## If something fails, send me
The Android Studio build error text, or a screenshot of the Search page and the query you typed.
