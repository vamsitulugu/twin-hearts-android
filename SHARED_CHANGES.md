# SHARED_CHANGES — Phase 2.2 Chat

## 1. AppNavHost / FeatureGraphs.kt (2 lines, file `core/navigation/FeatureGraphs.kt`)
a) Add the import and the registration line inside `featureGraphs(...)`, after `homeGraph(navController)`:
```kotlin
import com.twinhearts.app.feature.chat.chatGraph
...
    chatGraph(navController) // 2.2 Chat
```
b) In `pendingGraphs(...)` **delete** this line (Chat is no longer pending):
```kotlin
    pending<Routes.Chat>(R.string.core_nav_chat, openGallery)
```
`Routes.Chat` already exists, so `Routes.kt` does not change.

## 2. AndroidManifest.xml
Inside `<manifest>` next to the other permissions:
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```
Inside `<application>`:
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.chatfiles"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/chat_file_paths" />
</provider>
```
(`res/xml/chat_file_paths.xml` is in the zip.) Without the permission the mic button shows "Allow the microphone…"; without the provider the camera button shows "Couldn't open that photo". Everything else works.

## 3. Gradle / catalog
None. (Check that `androidx.core` is available; it comes with Compose/Activity. If `FileProvider` is red, add `implementation("androidx.core:core-ktx")` using the catalog's existing core line.)

## 4. TwinDatabase / strings / backend
No entities. `strings_chat.xml` is new. No backend change.

## CHANGE REQUEST: CameraX and Media3
README R-08 names CameraX (camera) and Media3 (audio). Neither is in the version catalog, and R-15 allows only Phase 0 to add versions, so this step uses the system camera (TakePicture) and the framework `MediaPlayer` for voice notes. If you want in-app camera preview / ExoPlayer later, add the two catalog lines in a separate step and I will swap them in (only `ChatScreen.kt` / `VoiceTools.kt` change).
