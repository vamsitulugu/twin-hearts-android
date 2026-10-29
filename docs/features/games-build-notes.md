# Build Notes — Games (hub + Truth or Dare · Ludo · Tic Tac Toe · Memory Match)

**How to apply:** unzip over the project root (paths are project-relative). The three core edits are already inside. Sync Gradle, run.

**How to test (needs two phones on the paired accounts, or one phone + the web app is NOT enough — web games use a different store):**
1. Home → Games chip → hub shows both players' level/XP/coins/wins/streak and 8 game cards.
2. Heart on a card toggles favourite; opening a game turns its button into "Continue" next time.
3. On both phones open Tic Tac Toe: moves appear on the other phone within ~2 s; win → pop-up, +8 XP; Play Again keeps scores.
4. Memory: flip two cards, wrong pair turns back after ~1 s and the turn passes; matches score and keep the turn; both phones see the final result.
5. Ludo: roll (tap dice), a 6 leaves base, tap a glowing token to move, landing on a non-safe cell captures; 30 s bar auto-plays/skips; exact roll needed for home.
6. Truth or Dare: spin (tap, button or flick), pick Truth/Dare, write (suggestions, 🎙️ Voice), other phone answers/Completed/Skip, reactions float, 🔊 Read speaks.
7. Partner opens a game → your hub shows "Playing …" and a green dot.
8. Airplane mode → one "connection problem" toast, game keeps working locally and catches up after.
9. Mind Reader / Draw / Bomb / Puzzle cards → "Coming in the next update".

**Known gaps:** see "Not in this delivery" in `games.md`. **Not compiled in this sandbox** (no Android SDK): the game rules and XP maths were compiled and unit-tested here (22 tests pass); screens must be built in Android Studio. Please send me any compile error text and I will fix it.
