# BUILD NOTES — Live Map directions/navigation + Calls v2

**Not compiled here.** The environment has no Android SDK. What *was* compiled and run on the laptop-style JVM (Kotlin 2.0.21): the whole navigation core (`domain/nav`), the real OSRM DTO classes and `toNavRoutes` mapper — 13 unit tests pass. All Compose / Android / WebRTC code was reviewed by hand only. Expect to fix a few small compile errors on first Sync (most likely: a Material icon name, or an import).

## Build
1. Unzip over the project root, Gradle Sync, Run on a **real phone** (GPS and WebRTC do not work well in the emulator).
2. Run unit tests: `gradlew testDebugUnitTest` (new: `NavTrackerTest`, `OsrmNavMapperTest`; `LiveMapViewModelTest` now builds the ViewModel with a `NavEngine` mock).

## Manual tests
1. **Directions.** Live Map → search "railway station" → tap a result. Expect Drive/Walk/Bike buttons and 1–3 routes with time and arrival. Tap Walk: time grows. Tap another route: it turns purple, the old one grey.
2. **Navigate.** Tap **Start** and walk/drive. Expect: big turn banner, voice "In 200 metres, turn left…", map rotates with your heading, bottom bar shows time left. Lock the screen: the notification shows the next turn and keeps voice going.
3. **Reroute.** Go the wrong way for ~10 s. Expect "Rerouting" and a new line. Switch airplane mode on: expect the "Could not find a new route" message, the old line stays.
4. **Arrive.** Reach the place: "You have arrived" card with distance and time; **Done** returns to the normal map.
5. **Video call from the map.** Live Map → **Video call** tile. Partner phone (or web app) should ring and show video.
6. **Voice → video.** Start a voice call from chat, tap **Video**. Partner sees "wants to turn on video"; **Turn on** → both pictures appear. **Not now** → you see "did not turn on video".
7. **Background call.** During a call press Home: the notification "Call with …" stays and audio continues.

## Known gaps
No live traffic, lane guidance, speed limits or public-transport routes (free servers do not offer them). The free FOSSGIS servers allow 1 request/second and are meant for light use; if this app grows, host your own OSRM. No push-to-ring when the app is fully closed.
