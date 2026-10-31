# Notifications: server part

**Nothing is required.** The server (us-app `routes/auth.js`) already sends every push (chat, calls, hug, touch,
miss you, partner requests, safety alerts, events, music …) as FCM *data* messages, and already has
`POST /api/auth/register-fcm-token`. The Android app now uses exactly those.

Only needs on Render: env var `FIREBASE_SERVICE_ACCOUNT` (already set if the web-app APK gets pushes).

## Optional: forget this phone on log out
Without it, a signed-out phone keeps getting the old person's pushes until that person signs in elsewhere.
Add to `routes/auth.js`, next to `/register-fcm-token`:

```js
router.post('/unregister-fcm-token', async (req, res) => {
  const { userId, token } = req.body;
  if (!userId || !token) return res.status(400).json({ error: 'Missing fields' });
  await supabase.from('fcm_tokens').delete().eq('user_id', userId).eq('token', token);
  res.json({ ok: true });
});
```
