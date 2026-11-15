# SHARED_CHANGES — Games voice call + chat
- Gradle/catalog lines: None
- Manifest entries: None (RECORD_AUDIO, MODIFY_AUDIO_SETTINGS, VIBRATE already declared by Calls)
- AppNavHost line: None (`gamesGraph` already registered)
- TwinDatabase entities: None
- Strings/permissions: new file `res/values/strings_games_comms.xml` (included)
- Backend changes: None (uses existing `/api/call/signal`, games state, `/api/media/upload-voice`)
- Other features touched: Calls got two NEW files only (`calls/data/GameVoiceEngine.kt`, `calls/ui/GameVoiceBridge.kt`); no existing Calls file was edited.
- Edited Games files: `GamesGraph.kt`, `GameCallHooks.kt`, `GameHost.kt`, `GamesApi.kt` (+`uploadVoice`), `GamesModule.kt` (+binding)
