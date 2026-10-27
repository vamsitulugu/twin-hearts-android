# SHARED_CHANGES — Phase 2.4 Twin AI

(This replaces the old Chat file; the Chat steps are already in your project.)

## 1. Gradle / catalog
None. Every library used is already in your project.

## 2. Manifest entries
File `app/src/main/AndroidManifest.xml`. Put this inside `<manifest>`, next to the other tags (it lets the mic button find the phone's speech app on Android 11+):
```xml
<queries>
    <intent>
        <action android:name="android.speech.action.RECOGNIZE_SPEECH" />
    </intent>
    <intent>
        <action android:name="android.intent.action.TTS_SERVICE" />
    </intent>
    <intent>
        <action android:name="android.speech.RecognitionService" />
    </intent>
</queries>
```
The third entry is **new in this update**: the full-screen voice mode listens inside the app and Android 11+ needs it to find the phone's speech recognizer. If you already added the first two, just add this one.
No new permission: `RECORD_AUDIO` is already in the manifest for Chat. Voice mode asks for it at the moment you tap the mic, and shows "Microphone access is needed to talk with Twin" if you say no.

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

## 4. CHANGE REQUEST: hide the bottom bar on the Twin page (one line)
Why: in the web app and in your screenshot the Twin page has **no bottom bar**; the orb sits on the input bar instead. File `core/navigation/TwinApp.kt`, change:
```kotlin
val showBottomBar = selectedItem != null
```
to:
```kotlin
val showBottomBar = selectedItem != null && selectedItem.key != Routes.TwinAi
```
Affects only the Twin page. If you skip this, the page still works, but the normal bottom bar shows under the input bar.

## 5. CHANGE REQUEST (optional, later): `subtitle` in TwinTopBar
The Twin header needs a second line ("Your Personal AI"). `TwinTopBar` has none, so the feature has its own private header (`TwinAiHeader`). Later, add `subtitle: String? = null` to `TwinTopBar` and I will delete the private copy.

## 6. TwinDatabase entities
None.

## 7. Strings / permissions
`res/values/strings_twinai.xml` is new (in the zip). No new permission.

## 8. Backend changes
None required. Uses `/api/ai/chat/stream`, `/api/ai/chat`, `/api/ai-both/*` and `/api/pro/status` as they are.
