# Fix pack: chat capital letter, in-app message alerts, partner photo, Live Map last seen

Not compiled here (no Android SDK). Build in Android Studio: Sync, then Run.

## Test
1. Chat: type "hello" -> keyboard starts with a capital "H" (first letter of each sentence).
2. Alerts: partner sends a message while you are on Home / Map / Settings -> a notification appears.
   While you are INSIDE the Chat screen no alert shows (the chat shows it).
3. Chat header: partner's profile photo shows (first letter only if they have no photo).
4. Live Map: partner offline -> "Last seen 12 min ago", "Last seen yesterday at 6:42 PM", "Last seen Mon at 6:42 PM".
   Sharing paused / never shared -> "Last seen a while ago" (no more huge "days").

## If alerts still do not show on other screens
The phone side is fixed. If the server skips the push while your partner's presence says you are "online"
(`routes/chat.js` / `routes/auth.js` in us-app), that check must be relaxed on the server. Send me that file.
