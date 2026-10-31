# SHARED_CHANGES — Ads (AdMob)

All of these are already applied inside the zip. This list is what changed outside `feature/ads/`.

## Gradle/catalog lines
`gradle/libs.versions.toml`
```toml
# [versions]
playServicesAds = "24.0.0"
# [libraries]
play-services-ads = { group = "com.google.android.gms", name = "play-services-ads", version.ref = "playServicesAds" }
```
`app/build.gradle.kts`: reads ADMOB_* from local.properties, adds BuildConfig fields `ADS_ENABLED`,
`ADMOB_BANNER_ID`, `ADMOB_INTERSTITIAL_ID`, manifest placeholder `admobAppId`, and
`implementation(libs.play.services.ads)`.

Version note (R-15): 24.0.0 is the newest Google Mobile Ads line that works with Kotlin 2.0.21.
24.1.0 and newer need Kotlin 2.1+. Do not "update" it without updating Kotlin first.

## Manifest entries
`app/src/main/AndroidManifest.xml`, inside `<application>`:
```xml
<meta-data android:name="com.google.android.gms.ads.APPLICATION_ID" android:value="${admobAppId}" />
```
(The ads library adds the AD_ID permission by itself.)

## AppNavHost line
None. One line in `core/navigation/TwinApp.kt` (inside `TwinNavigation`, before `val destination`):
```kotlin
AdBreakWatcher(navController)
```
plus its import `com.twinhearts.app.feature.ads.ui.AdBreakWatcher`.

## Other shared files touched
- `MainActivity.kt`: `@Inject lateinit var adsManager: AdsManager` and `onStart() { super.onStart(); adsManager.onAppStart(this) }`.
- `feature/home/ui/HomeScreen.kt`: `AdSlot()` between `HomeAllPages(onShortcut)` and `HomeFooterQuote(nowMillis)` + import.

## TwinDatabase entities
None.

## Strings/permissions
`res/values/strings_ads.xml` (`ads_label`). No new permissions.

## Backend changes
None.

## You must do (only you can)
1. Create a free AdMob account, add the Android app, create one Banner and one Interstitial ad unit.
2. Put the three ids in `local.properties` (see BUILD_NOTES_ads.md). Debug builds work without them (test ads).
