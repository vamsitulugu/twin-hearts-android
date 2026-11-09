# REQUIRES BACKEND CHANGE: deliver every push right away (us-app `routes/auth.js`)

## Why (5 lines)
Today only calls and Touch are sent as "high priority". Chat, Hug, Miss You, reminders and the rest go as
"normal", and Android can hold normal messages for minutes while the phone sleeps (Doze). That is why
some notifications arrive late or "never". Fix: make every push high priority and keep it for one day.

## Change in `sendFCMToPartner` (routes/auth.js)

Find:
```js
  const isTimeSensitive = isIncomingCall || payload.tag === 'touch';
```
Replace with:
```js
  const isTimeSensitive = true; // every Twin Hearts push must land at once
```

Find:
```js
      android: {
        priority: isTimeSensitive ? 'high' : undefined,
      }
```
Replace with:
```js
      android: {
        priority: 'high',
        // Calls are useless after a minute; everything else is kept up to a day if the phone is off.
        ttl: isIncomingCall ? 60 * 1000 : 24 * 60 * 60 * 1000,
      }
```
Then redeploy on Render. No database change.
