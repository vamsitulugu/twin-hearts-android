# Chat (2.2) — v2, all web features

Screen: bottom-bar **Chat** tab (`Routes.Chat`). Code: `feature/chat/`. Old server files: `routes/chat.js`, `routes/media.js` (no server change). Reference screenshots: `docs/reference/images.pdf` pages 5–6.

## Screens and what they do
| Web | Android | API (method path → body → answer) |
|---|---|---|
| Page bar "Chat", Search, Sync | `TwinTopBar` | Sync = re-read last 200 |
| Header: back, avatar, name (or your nickname), Online / Away / Last seen, 🔇 when muted, call, video, ★, search, ⋮ | Same, round glass buttons | `GET /api/chat/:couple/presence` → `[{role,status,last_seen}]`; heartbeat `POST /api/chat/:couple/presence {role,status}` |
| Messages list, day pills, grouped bubble corners, ticks | Same | `GET /api/chat/:couple?limit=200` and `?after=<iso>` every 3 s |
| Text with *bold* _italic_ ~strike~ ```mono``` \|\|spoiler\|\| and links + link card | Same | `POST /api/chat {type:"text"}` (upsert by clientId) |
| Photos (1 = `image`, many = `image_group`), caption, rotate, **view once** | Pick/camera → preview sheet → send; grid of up to 4 with "+N"; full-screen viewer (double-tap zoom) | `POST /api/media/upload` (multipart) then `POST /api/chat`; `viewOnce` flag |
| View-once open | Tap tile → full-screen, screenshots blocked, burns after close | `POST /api/chat/:id/view {coupleId,role}` → `{mediaUrl}`; 410 = already opened |
| Video | Pick → upload → tap to play in system player | upload + `type:"video"` |
| Document, Audio file | Pick → upload → file card (icon, name, size, EXT) | `POST /api/media/upload-doc`, `/upload-audio` |
| Voice note + speed 1×/1.5×/2× + waveform | Tap mic, send/cancel; play bubble | `POST /api/media/upload-voice`, `type:"voice"` |
| Location | Asks permission, one GPS fix, card opens Maps | `type:"location" {lat,lng}` |
| Contact card, Poll (+ votes counted from replies), Sticker, Gift | Sheets with same lists | `type:"contact"/"poll"/"sticker"/"gift"` |
| GIF search | Sheet with search grid (needs a Giphy key, see below) | Giphy public API |
| Emoji picker (tabs, search, recent) | Sheet; also "…" in reaction row | local |
| Long-press menu: Select, React (6 + more), Reply, Forward, Copy, Pin, Star, Edit, Info, Delete for everyone / for me | Bottom sheet, same order | `POST /:id/react`, `/:id/pin {coupleId,pinned}`, `/:id/star`, `PATCH /:id {coupleId,senderRole,text}`, `DELETE /:id {coupleId,senderRole,mode}` |
| Select mode (many): reply, forward, copy, star, delete | Header turns into selection bar; tap bubbles | same calls |
| Swipe to reply, reply quote (tap = jump) | Same | `replyTo` |
| Pinned bar | Under header; tap = jump, ✕ = unpin | |
| Jump-to-latest button with new-message count | Same | |
| Send effects (long-press send): confetti, hearts, balloons; plays for partner too | Same | `effect` field |
| Send later (scheduled) | ⋮ and "+" → Scheduled | `POST /:couple/schedule`, `GET /:couple/scheduled`, `DELETE /scheduled/:id` |
| Search All/Media/Links/Docs | Same | `GET /:couple/search?q&filter` |
| ⋮ menu: Mute, Shared media, Chat wallpaper, Clear chat, Disappearing messages, Auto-lock, Do-not-disturb (+ Scheduled, Nickname, Export) | Same card | see below |
| Shared media (Media / Docs / Links) | Full-screen, 3 tabs | from loaded messages |
| Chat wallpaper: default, 4 colours, own photo with move & scale, dim 0–90, "both of us" | Full-screen with live preview and cutter | `GET/POST /:couple/wallpaper {mode,swatch,image_url,dim,role}`; photo → `/api/media/upload` |
| Disappearing messages Off/1 h/24 h/7 d | Choice dialog | `GET/POST /:couple/disappearing {seconds}` |
| Export chat | Writes a .txt, opens share sheet | `GET /:couple/export` (text) |
| Clear chat | Confirm → delete-for-me on every message | `DELETE` mode "me" per message |
| Auto-lock chat | Off / 1 / 5 / 15 min idle (and when app goes to background); opens with the **phone screen lock** | local (web uses the vault PIN; the Vault is Phase 4) |
| Draft kept per chat, recent emoji, nickname, mute, DND window | DataStore | local |

States: loading skeleton, empty, error + retry, banner (green info / red error). Every state has a @Preview (plus 1.3 font).

## Live updates
Same as the web: poll new messages every 3 s, full refresh every 12 s, heartbeat every 15 s while the screen is on; "away" when it stops.

## Not ported (and why)
- **Typing indicator** — the web uses Supabase realtime broadcast; the app may only talk to the Express API (README), and no endpoint exists. Needs a backend endpoint first.
- **Dark-mode switch in ⋮** — app is dark only (R-54).
- **Mark as unread** — it only sets a flag for the nav badge, which belongs to core.
- **Attach → Memories** — belongs to the Memories feature.
- **Call buttons** — show the "later step" note until Calls (2.3).
- **Mute / Do-not-disturb** are saved and shown, but only matter once push notifications exist (they silence nothing today, like the web where they only silence in-app toasts).
- **Animated GIFs** show as still pictures (animation needs `coil-gif`, not in the catalog).
- **Voice/video in-app camera & ExoPlayer** — system camera / MediaPlayer / system video player (CameraX, Media3 not in the catalog).

## Tests
`ChatDomainTest`, `ChatMapperTest`, `ChatViewModelTest` (text rules, grouping, polls, wallpaper math, mapper kinds, send/edit/delete/pin/select). Not compiled in the sandbox — run `gradlew.bat testDebugUnitTest`.
