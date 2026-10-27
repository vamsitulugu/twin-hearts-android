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
| Edit | Long-press → Edit; edit text and save; shows "edited" with timestamp | PATCH /api/chat/:id. |
| Delete | Long-press → Delete for me / Delete for everyone | DELETE /api/chat/:id. |
| React | Long-press → React; pick emoji; reaction chips show under bubble | POST /api/chat/:id/react. |
| Photo (gallery + camera) | Gallery picker and system camera; photo is shrunk to 1600 px JPEG, then uploaded | Message type `image`. |
| Voice note | Tap mic → timer → send or cancel; play / pause in the bubble | Recorded as m4a, uploaded to `upload-voice`, message type `voice` with `duration`. |
| View-once media | Tap the lock badge on a view-once photo to open (and burn) it | POST /api/chat/:id/view. |
| Search (All / Media / Links / Docs) | ⌕ button opens search | 350 ms after you stop typing. |
| Scheduled messages | ⋮ → Scheduled messages: write, pick 15 min / 1 h / 3 h / 1 day, list, cancel | Text only (as the server route allows from the web composer). |

States: loading skeleton, empty ("No messages yet"), error with retry, banner for send/star/edit/delete errors.

## Live updates
No realtime channel yet. While Chat is on screen: new messages every 3 s, full refresh (ticks, stars, reactions) every 12 s, "I am here" heartbeat every 15 s. Leaving the screen sends "away".

## Not in this step (other features)
Calls, forward, pin, disappearing messages, wallpaper, dark-mode switch and "Shared media" of the ⋮ menu, typing indicator, push notifications, sidebar menu button, voice playback speed, link preview. The web ⋮ items "Clear chat", "Auto-lock" and "Do-not-disturb" are Settings-level features.

## Differences / limits
- Voice playback uses the framework `MediaPlayer` and the camera uses the system camera app (no CameraX / Media3 dependency was added; see SHARED_CHANGES).
- Starred list is built from the newest 200 loaded messages.
- Delete "for everyone" only works on messages I sent; the server enforces this.

## Tests
`ChatDomainTest` (8), `ChatMapperTest` (6), `ChatViewModelTest` (14). Not compiled in the sandbox.
