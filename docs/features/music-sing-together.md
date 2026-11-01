# Music: Sing Together (video-call karaoke) — feature audit

Web files read: `public/couple-karaoke.js`, `public/music/library.js` (tile, song menu), `public/music/nowplaying.js` (player button), `public/music/app.js` (back handling), `routes/data.js`, `routes/call.js` (turn-creds).

## Screens
| Screen | What it does |
|---|---|
| Entry points | Music home chip **Sing Together**, song menu **Sing together (video)**, Now Playing button |
| Song picker | "Choose a song to sing together" (tap a song = invite) |
| Invite card (partner) | "❤️ <name> invited you to sing." Accept / Decline |
| Waiting card (me) | "Waiting for your partner to accept…" Cancel |
| Room | Partner picture full screen, my small tile, song chip, Leave, current + next lyric (coloured by singer), progress, 5 reactions, prev/play/next, mic, camera, flip, speaker, "Connecting…" and "Reconnecting…" |

## How the two phones talk (same as web, so web <-> Android works)
- `GET /api/data/state/:coupleId` and `POST /api/data/state {coupleId, state:{ck_<role>:[msgs]}, senderRole}`; message = `{type, from, ts, seq, ...}`.
- Types: invite, invite_accepted, invite_declined, invite_cancel, ready, offer, answer, ice, sync, control(play/pause/seek/track), request_sync, reaction, leave.
- TURN: `GET /api/call/turn-creds` (fallback Google STUN).
- Lyrics: `GET /api/music/song/:id`, `GET /api/lyrics/status/:id`, `POST /api/lyrics/auto-fetch`.

## Behaviour
Host drives the song; guest mirrors (one-song queue, so it can't skip alone). Drift over 0.3 s is corrected; guest asks `request_sync` after 4 s of silence. Camera/mic denied = joins lyrics-only.

## Changes from web (on purpose)
- Invite card shows the PARTNER's name (web showed your own).
- Old invites (>90 s) are ignored; an unanswered invite expires after 90 s.
- Self tile sits below the top bar so Leave is never covered.

## Not ported
| Item | Reason |
|---|---|
| "Record Our Duet" button | Not built: needs both video feeds and both voices mixed into one file, which I can not test without compiling. Own session. |
| Room in background | Android stops the camera when the app is hidden; music continues. |

## v3
Guest lyrics: retries the cache 5 times and the host sends the text over the channel (`lyrics` message). Faster Music page: invite listening starts after 2.5 s and polls every 4 s while idle; relay servers are fetched in parallel.

## Known cost
Signalling polls the whole couple document (2 s idle while Music is open, 0.7 s in a room), like the web. A lighter endpoint would be a backend change (R-63).
