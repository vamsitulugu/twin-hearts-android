# SHARED_CHANGES — Phase 2.4 Twin AI

(This replaces the old Chat file; the Chat steps are already in your project.)

## 1. Gradle / catalog
None. Every library used is already in your project.

## 2. Manifest entries
**Already applied in the zip** (file `app/src/main/AndroidManifest.xml` is included). It adds one `<queries>` block with `android.speech.RecognitionService`, `android.intent.action.TTS_SERVICE` and `android.speech.action.RECOGNIZE_SPEECH`, so voice mode, the mic button and Listen find the phone's speech apps on Android 11+. If you had already pasted your own `<queries>` block, keep only one (this one).
No new permission: `RECORD_AUDIO` and `ACCESS_NETWORK_STATE` are already in the manifest (the offline orb uses the second).

## 3. AppNavHost line — file `core/navigation/FeatureGraphs.kt`
a) Add the import and the line inside `featureGraphs(...)`, after `chatGraph(navController)`:
```kotlin
import com.twinhearts.app.feature.twinai.twinAiGraph
...
    twinAiGraph(navController) // 2.4 Twin AI
```
b) In `pendingGraphs(...)` **delete**:
```kotlin
    pending<Routes.TwinAi>(R.string.core_nav_twin, openGallery)
```

## 4. CHANGE REQUEST (applied in the zip): hide the bottom bar on the Twin page
Why: in the web app and your screenshot the Twin page has no bottom bar; the orb sits on the input bar. File `core/navigation/TwinApp.kt` is included with this one line changed:
```kotlin
val showBottomBar = selectedItem != null && selectedItem.key != Routes.TwinAi
```
Affects only the Twin page.

## 5. CHANGE REQUEST (optional, later): `subtitle` in TwinTopBar
The Twin header needs a second line ("Your Personal AI"). `TwinTopBar` has none, so the feature has its own private header (`TwinAiHeader`). Later, add `subtitle: String? = null` to `TwinTopBar` and I will delete the private copy.

## 6. TwinDatabase entities
None.

## 7. Strings / permissions
`res/values/strings_twinai.xml` is new (in the zip). No new permission.

## 8. Backend changes
None required. Uses `/api/ai/chat/stream`, `/api/ai/chat`, `/api/ai-both/*`, `/api/pro/status`, and (new, read only) `/api/ai-both/names/:coupleId` and `/api/data/state/:coupleId` for names, photos and the latest journal mood.

## 9. Files outside `feature/twinai` that this update touches
`AndroidManifest.xml` (queries block) and `core/navigation/TwinApp.kt` (one line). Nothing else.
