# Notifications

One system for the whole app: `core/notifications/`.

**Look (same theme everywhere):** the Twin Hearts logo as the status-bar icon (`ic_stat_twin`, made from the logo's shape) and as the
large picture, burgundy accent (`#C2185B`), notifications stacked per feature, heartbeat vibration for love signals,
ringtone for calls. Android draws the card itself, so its background follows the phone's light/dark theme.

**Receive:** the server (`routes/auth.js`, already live) sends an FCM *data* message `{title, body, tag, senderName, type?, …}`
→ `TwinMessagingService` → `NotificationType.fromServerTag(tag)` → `TwinNotifier.show`. Works with the app open, in the background or closed.
**Register:** `PushTokenRegistrar` posts the phone token to `POST /api/auth/register-fcm-token` after every sign-in.
**Send:** nothing in the app. The server already pushes when the app saves a hug / touch / miss you, sends a chat message, starts a call, etc.
Calendar and Study reminders ring from phone alarms (`ReminderReceiver`) through the same notifier.

**Buttons:** chat Reply + Mark as read, call Answer / Decline (full-screen over the lock screen), hug/miss/touch "Send back",
SOS "Open map" / "Call", reminders "Done".

**Settings > Notifications:** master switch, lock-screen details, quiet hours (calls and SOS still ring), one switch for each of the 39 types.
Period, Money and Vault notifications never show their text on the lock screen.

**Tap** opens the right page (`NotificationTarget` → `Routes`), also from a cold start.

**To switch pushes on:** in the Firebase project `us-app` add an Android app with package `com.twinhearts.app`, download its
`google-services.json` into `app/`. Until the file exists the app builds and runs normally; only remote pushes are off.
