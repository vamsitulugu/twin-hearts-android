# SHARED CHANGES — Live Map directions + navigation, Calls v2

All shared-file edits are **already applied in this zip**. This list is for review.

## Gradle/catalog lines
None. (No new libraries: routing uses the same Retrofit/OkHttp, voice uses Android's built-in TextToSpeech.)

## Manifest entries
`app/src/main/AndroidManifest.xml`
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CAMERA" />
<uses-permission android:name="android.permission.VIBRATE" />

<service android:name=".feature.calls.data.CallService"
    android:exported="false" android:foregroundServiceType="microphone|camera" />
<service android:name=".feature.livemap.data.nav.NavigationService"
    android:exported="false" android:foregroundServiceType="location" />
```

## AppNavHost line
None. `Routes.Call` and `Routes.LiveMap` already existed; `LiveMapGraph.kt` now passes `onVideoCall = { navController.navigate(Routes.Call(video = true, incoming = false)) }`.

## TwinDatabase entities
None.

## Strings/permissions
New strings are in `strings_livemap.xml` (`livemap_dir_*`, `livemap_nav_*`) and `strings_calls.xml` (`calls_video`, `calls_upgrade_*`, `calls_reconnecting`, `calls_notification_*`). Removed `livemap_msg_calls_pending`.
Runtime permissions are unchanged (location, microphone, camera, notifications).

## Backend changes
None. REQUIRES BACKEND CHANGE: no.
