# Games update

All 8 games from the web Games page are now playable on Android (previously Mind Reader, Draw & Guess,
Don't Touch The Bomb and Picture Puzzle showed "Soon").

## New games
- Mind Reader (58 questions copied from the web app, modes, compatibility %, achievements, history)
- Draw & Guess (pen / highlighter / eraser / bucket fill, undo / redo, hints, synonym-aware guess check, voice word)
- Don't Touch The Bomb (4 difficulties, hint + fog modes, moving bombs, guide pings, chat + emoji)
- Picture Puzzle (upload a photo, tiles split between both players, rotate, chat + emoji)

## Hub UI (matches web games.html)
- Game artwork copied to res/drawable-nodpi/games_*.webp, shown as 16:9 card pictures
- Web gradients per game, New / Trending badges, serif titles, tag chips, "Play Now / Continue" button
- Rank cards use the web's pink wash + magenta outline

## Where things are
- Engines (pure Kotlin): feature/games/domain/engine/{MindReader,Draw,Bomb,Puzzle}Engine.kt
- ViewModels + screens: feature/games/ui/play/
- Strings: res/values/strings_games.xml, strings_games_mindreader.xml
- Tests: app/src/test/.../games/NewGameEnginesTest.kt

## Not verified here
The Android SDK/Gradle was not available in the build environment, so the Compose UI has not been compiled
or run on a device. The engines were compiled with Kotlin and all tests pass. Run `./gradlew testDebugUnitTest`
and a debug build first.
