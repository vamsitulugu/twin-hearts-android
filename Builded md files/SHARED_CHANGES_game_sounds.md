# SHARED_CHANGES — game sound effects

Requested directly by the user ("add the game based sounds for all the games, only a small number of new sounds"),
so this deliberately touches `core/sound` (README R-30 / R-31 normally forbid that). Same cues as web `twin-sounds.js` v3.

## Files changed
| File | Change |
|---|---|
| `core/sound/TwinSound.kt` | +5 synthesized cues: `flip`, `match`, `snap`, `boom`, `dice`. No new library, no audio files. |
| `feature/games/ui/play/GameEvents.kt` | `GameCue` +8 entries: `Flip, Match, Snap, Boom, Tick, SpinStart, SpinEnd, Brush`. |
| `feature/games/ui/components/GameFeedback.kt` | New cues play the soft `TwinSound` sounds. `Dice` now uses the new rattle. The old beeps now respect the mute switch. |
| `Memory / Puzzle / Bomb / MindReader / TruthOrDare / Draw / TicTacToe ViewModel` | One-line cue changes (see map). Ludo needed none: it already emits `Dice` and `Place`. |

## Sound map
| Game | Moment | Sound |
|---|---|---|
| Memory Match | card turned / pair found | new `flip` / new `match` |
| Picture Puzzle | piece locks in | new `snap` |
| Don't Touch The Bomb | 3 · 2 · 1 and last 5 seconds / bomb goes off | existing `tick` / new `boom` |
| Real Ludo | dice roll | new `dice` rattle (replaces the single beep) |
| Mind Reader | exact match | new `match` |
| Truth or Dare | bottle spins / stops | existing `spinStart` / `spinEnd` |
| Draw & Guess | each stroke | existing `scratch`, very quiet (one grain per stroke) |
| Tic Tac Toe | placing a mark | existing `place` beep |

Everything obeys the single mute switch (`TwinSound.setEnabled`). No Gradle, manifest or DB changes.
Not compiled here (no Android SDK in the build sandbox): please run `./gradlew :app:compileDebugKotlin` once.
