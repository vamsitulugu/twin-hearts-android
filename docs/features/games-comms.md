# Games — voice call + chat (talking while you play)

Web: `public/game-comms.js` (📞 pill + 💬 sheet in every game). Android: the same two features in all 8 games.

## What it does
- **📞 voice call** (header): audio-only call inside the game. No call page — a small draggable pill with only **mute, speaker, end**. Speaker is on by default; on Android it really switches loudspeaker/earpiece. When the partner calls, a small card (Decline / Accept) shows inside the game.
- **💬 chat** (header, red unread badge): bottom sheet with normal text messages and one 🎙 button that records and sends a voice message. Back closes the sheet first. Not paired → "Connect with your partner".
- Leaving the game ends the call and clears the chat (like the web page).

## Same protocol as the web (so web ↔ Android work)
- Voice: `/api/call/signal` rows tagged `gameCtx = "v:<game>"` + `callId` (offer / answer / ice / end / decline). `CallEngine` ignores `gameCtx` signals, so it never collides with a normal call.
- Chat: shared state keys `gm_gchat_u1` / `gm_gchat_u2` = `{ m: [{id,t,x,u,d,ts}], ts }` — each player writes only their own key. Voice messages go through `/api/media/upload-voice` (m4a). No backend change.

## Where the code is
- `calls/data/GameVoiceEngine.kt` — the voice call (new file in Calls; no existing Calls file edited). `calls/ui/GameVoiceBridge.kt` — ViewModel + microphone permission.
- `games/ui/comms/` — `GameCommsLayer` (header buttons, polling, wiring), `GameChatSheet`, `GameChatViewModel`, `GameVoiceOverlay` (pill + incoming card), `GameVoiceTools` (recorder/player), `GameVoiceHooks` (plain types).
- `games/data/GameChatRepository.kt`, `data/mapper/GameChatMapper.kt`, `domain/engine/GameChatRules.kt`, `domain/model/GameChatModels.kt`.
- `GamesGraph` is the only bridge between Games and Calls (R-83). `GameHost` got one line.
- Tests: `GameChatRulesTest`, `GameChatMapperTest`.

## Behaviour notes
- Video and voice never run together: starting video ends a game voice call; a normal/video call ending the mic ends the game voice call; starting voice while a call is on says "End your other call first".
- If both tap call at once, user2 gives way to user1 (like the web).
- Polling only while a game is on screen and the app is visible: chat every 2 s (sheet open) / 4 s, voice signals every 3 s idle / 1.5 s in a call.

## Known gaps
- No foreground notification for game voice (`CallService` belongs to `CallEngine`): lock the phone or leave the app and Android may mute the mic. Meant for use while the game is on screen.
- No push when a partner calls/chats and the app is closed (same as web).
