# Touch → long vibration (partner's phone)
Files: core/notifications/NotificationType.kt, NotificationChannels.kt, TwinNotifier.kt
- Touch channel id is now `twin_touch_v2` (old `twin_touch` is deleted on first start), buzz = 3s·3 pulses ≈ 10 s.
- Pre-Android 8 phones: buzz set on the notification itself.
- Server: no change (routes/data.js already sends tag "touch" with high FCM priority).
Test: phone A taps Touch; phone B (app closed, screen off) must buzz ~10 s and show "Touch" notification.
Note: not compiled in the sandbox.
