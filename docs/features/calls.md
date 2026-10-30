# Calls (2.3) — v2: what changed

Calls already existed (voice and video from chat, signalling through the same `/api/call/signal` rows as the web app). This version closes the gaps against the web app.

| Gap | Now |
|---|---|
| Live Map **Video call** tile only showed a message | Opens `Routes.Call(video = true)` and starts an outgoing video call |
| No way to turn a voice call into video | **Video** button on a connected voice call. Same five signals as the web: `video-upgrade-request` → `accept` / `decline` → `offer` → `answer`. Partner sees "<name> wants to turn on video — Not now / Turn on" |
| Any network blip ended the call | `DISCONNECTED` shows "Poor connection · reconnecting…" and waits 12 s (same as the web) before ending; coming back cancels it |
| Silent ringing for the caller | Ringback tone while calling; phone vibrates with the ringtone when a call comes in |
| Call stopped when the app went to the background | `CallService` (foreground service, microphone + camera) keeps it running; its notification has **Hang up** |
| Polling would hit the server's 60 requests/min/IP limit on shared Wi-Fi | Connected calls poll every 3 s instead of 2 s; idle polling stops when no screen is watching |

## Known limits
- A phone with the app **fully closed** still cannot ring: that needs a push message (Firebase) and a server change. Not part of this version.
- If the web app sends a video request while this app is not connected yet, the request is declined automatically.

## Backend
No backend change is needed: all signals already exist in `routes/call.js`.
