# Twin AI page UI update (neutral look, full-width switch, grey-glass typing bar)

Unzip over the project root, then Sync and Run. Only files inside `feature/twinai/` changed. No core/ edits, no new dependencies.

What changed
- Page background: near-black (#0A0A0C), no pink/burgundy shading (same as Chat and Home).
- YOU / BOTH switch: now full width, grey glass track, burgundy selected side.
- Typing bar: grey glass bar + lighter grey glass text field; "+" is a grey glass button, send stays burgundy.
- Header buttons, chips, Twin answer bubbles, hero card: grey glass instead of pink-tinted glass.
- BOTH tab: grey cards, numbered badges in "How it works", burgundy main buttons, no pink text.

Manual tests
1. Open Twin AI: background is black/grey, no pink glow at the top.
2. YOU / BOTH switch spans the whole width; tap both sides.
3. Type a message: bar and field look grey and glassy; send works.
4. BOTH tab: Start Together, open a discussion, submit an answer.
5. Font scale 1.3: nothing is cut off.

## Update 2
- Top bar: logo and back arrow removed. Left: menu. Then Twin AI title, search, sync. System back goes to Home.
- Bottom: the strip under the typing bar (system navigation area) is now black like the page, not burgundy.
  Needs one small core edit in `core/navigation/TwinApp.kt` (Twin AI owns the bottom inset, same as Chat).
