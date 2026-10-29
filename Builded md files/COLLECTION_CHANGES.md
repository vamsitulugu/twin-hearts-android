# Collection page — what changed

Android version of `us-app/public/collection.html`, laid out like `docs/reference/images.pdf` page 35.

## New: `feature/collection/`
- `domain/` — models + `CollectionLogic` (filter / sort / search / durations / grouping; ported from the web JS, unit tested)
- `data/` — `CollectionApi` (uses existing `api/chat/{coupleId}`, `api/chat/{id}/star`, `DELETE api/chat/{id}`, `api/data/state`), lenient `CollectionMapper`, `CollectionRepository`, `CollectionPrefs` (local favourites, like the web's localStorage)
- `ui/` — `CollectionScreen`, `CollectionParts`, `CollectionViewModel`, `VoicePlayer`
- `di/CollectionModule.kt`, `CollectionGraph.kt`
- `res/values/strings_collection.xml`
- test: `app/src/test/.../collection/CollectionLogicTest.kt`

## Edited (3 one-line changes)
- `core/navigation/Routes.kt` — `Routes.Collection`
- `core/navigation/FeatureGraphs.kt` — `collectionGraph(navController)`
- `feature/home/HomeRoute.kt` — Home "Collection" shortcut now navigates to it

## Not changed
- No Gradle, manifest or backend changes. No new permissions (downloads use DownloadManager).
- The old "Collection" tab inside `feature/us` is untouched.
