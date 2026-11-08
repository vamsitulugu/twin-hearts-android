# BUILD_NOTES — Games voice call + chat
- NOT compiled: the Android SDK is not available in the sandbox. Only `GameChatModels.kt` + `GameChatRules.kt` were compiled with kotlinc 2.0.21 and `GameChatRulesTest` (6 tests) passed against a JUnit stand-in.
- `GameChatMapperTest` was written but not run (kotlinx-serialization jar unreachable). Run both tests in Android Studio.
- Please test with two phones: call connects, mute, speaker (loudspeaker <-> earpiece), end from either side, chat text, voice message, unread badge, Back closes sheet, video-vs-voice exclusion. Also test Android <-> web in the same game.
- Check the three header buttons do not crowd the game title on narrow phones.
