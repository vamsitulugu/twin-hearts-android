# SHARED CHANGES — Cinema 3D, C1 spike

Needs a build + a phone test. Nothing here was compiled (no Android SDK in the sandbox).

Everything below is also in `cinema3d-c1-shared-lines.patch`. From the project root: `git apply cinema3d-c1-shared-lines.patch`. Apply it, then delete the patch file.

## Gradle/catalog lines
`gradle/libs.versions.toml`, under `[versions]`:

    filament = "1.77.3"

`gradle/libs.versions.toml`, under `[libraries]`:

    filament-android = { group = "com.google.android.filament", name = "filament-android", version.ref = "filament" }
    gltfio-android = { group = "com.google.android.filament", name = "gltfio-android", version.ref = "filament" }

`app/build.gradle.kts`, in `dependencies { }`:

    implementation(libs.filament.android)
    implementation(libs.gltfio.android)

R-15: 1.77.3 was the newest on Maven Central when this was written (Nov 2026). **Look it up again before you commit.** Do not add `filament-utils` (it is Kotlin).

`app/proguard-rules.pro` (release builds shrink with R8):

    -keep class com.google.android.filament.** { *; }
    -dontwarn com.google.android.filament.**

## Manifest entries
None. (Optional later: `<uses-feature android:glEsVersion="0x00030000" android:required="true"/>` once you decide to hide the app on phones without OpenGL ES 3.0. Not needed for the spike.)

## AppNavHost line
`core/navigation/Routes.kt`, next to `Globe`:

    @Serializable data object Cinema3dSpike

`core/navigation/FeatureGraphs.kt`: import `com.twinhearts.app.feature.cinema3d.cinema3dGraph` and add

    cinema3dGraph(navController) // Cinema 3D spike (C1)

`core/navigation/TwinApp.kt`, in `noPullRefreshRoutes` (so pull-to-refresh does not fight the camera drag):

    Routes.Cinema3dSpike::class,

`core/navigation/TwinSidebar.kt`, in the Explore group, after the Globe entry (this is how you open the spike):

    SidebarEntry(Routes.Cinema3dSpike, R.string.cinema3d_sidebar_spike, Icons.Outlined.MovieCreation),

## TwinDatabase entities
None.

## Strings/permissions
New file `app/src/main/res/values/strings_cinema3d.xml` (included in this delivery): `cinema3d_sidebar_spike`. No permissions.

## Backend changes
None.

## Remove after the go/no-go
The sidebar entry, the `Routes.Cinema3dSpike` route and its `TwinApp` line go when C2 replaces the spike with the real cinema screen.
