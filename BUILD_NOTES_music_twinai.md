# Music: Sing Together card · Twin AI Both mode

Not compiled here (no Android SDK). Build in Android Studio: Sync, Run.

## Music
- The big "Liked Songs" card on the Music page is replaced by a big "Sing Together" card
  ("Pick a song and sing with <partner> on video"). Tap: song picker -> invite -> video sing (existing flow).
- The small Sing Together chip was removed (the card replaces it).

## Twin AI Both
- Second person to submit now sees the "Analyzing…" card at once (before it showed "waiting").
- If analysis takes over 1 minute: "This is taking longer than usual" + Retry.
- A strange AI answer (a list instead of text) can no longer break the round.
- IMPORTANT: server fix in backend-changes/twinai-both/both-analysis-stuck.patch.md. The round stays
  "analyzing" forever when the server's AI step crashes; Retry only works after that patch.

## Test
1. Music: first card says Sing Together; tap -> "Choose a song to sing together".
2. Both: both phones enter a problem -> Analyzing card on both -> result appears.
3. If it stalls 1 min -> Retry button appears; after the server patch, Retry produces the result.
