# Chat (2.2)

Screen: bottom-bar **Chat** tab (`Routes.Chat`). Code: `feature/chat/`. API: `routes/chat.js`, `routes/media.js` of the old server (no server change).

## What it does (matched to the web chat)
| Web | Android | Notes |
|---|---|---|
| Page bar "Chat" with Search + Sync | Same (`TwinTopBar`) | Sync re-reads the last 200 messages. |
| Chat header: back, avatar, name, "Online / Away / Last seen 3d ago", call, video, ★, search, ⋮ | Same | Thresholds as the web (online < 35 s, away < 2 min). Call / video show the "built in a later step" note (Calls = 2.3). |
| Bubbles: mine burgundy right, partner grey left, time, ticks (sending ⏱, sent ✓, delivered ✓✓, read ✓✓ bright, failed ⚠ tap to retry) | Same | Day pills "Today / Yesterday / 21 OCT 2026". |
| Reply with quote | Long-press → Reply; quote shown inside the bubble and above the composer | Sent as `replyTo`. |
| Star | Long-press → Star / Unstar; ★ button lists starred messages | Stars are private per person (server `starred_by`). |
| Copy | Long-press → Copy | |
| Photo (gallery + camera) | Gallery picker and system camera; photo is shrunk to 1600 px JPEG, then uploaded | Message type `image`. |
| Voice note | Tap mic → timer → send or cancel; play / pause in the bubble | Recorded as m4a, uploaded to `upload-voice`, message type `voice` with `duration`. |
| Search (All / Media / Links / Docs) | ⌕ button opens search | 350 ms after you stop typing. |
| Scheduled messages | ⋮ → Scheduled messages: write, pick 15 min / 1 h / 3 h / 1 day, list, cancel | Text only (as the server route allows from the web composer). |

States: loading skeleton, empty ("No messages yet"), error with retry, banner for send/star errors.

## Live updates
No realtime channel yet. While Chat is on screen: new messages every 3 s, full refresh (ticks, stars) every 12 s, "I am here" heartbeat every 15 s. Leaving the screen sends "away".

## Not in this step (other features)
Calls, reactions, edit/delete, forward, pin, view-once opening, disappearing messages, wallpaper, dark-mode switch and "Shared media" of the ⋮ menu, typing indicator, push notifications, sidebar menu button. The web ⋮ items "Clear chat", "Auto-lock" and "Do-not-disturb" are Settings-level features.

## Differences / limits
- Voice playback uses the framework `MediaPlayer` and the camera uses the system camera app (no CameraX / Media3 dependency was added; see SHARED_CHANGES).
- Starred list is built from the newest 200 loaded messages.

## Tests
`ChatDomainTest` (6), `ChatMapperTest` (4), `ChatViewModelTest` (8). Not compiled in the sandbox.
