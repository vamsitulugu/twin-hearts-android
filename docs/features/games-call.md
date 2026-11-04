# Games — face-to-face video call

Web: `GameVideo` in `public/games.html` (📹 button in the game header, video strip under the game).
Android: same button in every game, using the app's existing call engine (same one as chat and Watch Together).

- `GameCallHooks` + `LocalGameCallHooks` (games/ui) — the bridge, so Games never imports Calls (R-83).
- `GamesGraph` — fills the hooks: start video (asks camera + mic), hang up, "in call?", call window.
- `GameHost` — shows 📹 (becomes red-pink "end" icon during a call) at the top-right and draws the call window.
  All 8 games use `GameHost`, so all 8 get the call.
- Incoming call while playing: the app's normal call page opens (same as everywhere else).

Not ported: the web's split video strip under the game and its in-game accept/decline card. Android uses the small draggable window (same as Watch Together).
Known gap: a web partner's in-game call (tagged `gameCtx`) is ignored by Android's CallEngine on purpose, so web → Android in-game calls will not ring. Android → anyone, and Android ↔ Android, work as a normal call.
