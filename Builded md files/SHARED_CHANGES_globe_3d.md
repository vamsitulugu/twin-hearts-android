# SHARED_CHANGES — Globe 3D (v1)

## Gradle / catalog lines
None. Uses OpenGL ES (part of Android), MapLibre (already there), `org.json` (part of Android).

## Manifest entries
Optional, recommended so Play-style filtering is correct (not needed for sideloading):
`<uses-feature android:glEsVersion="0x00030000" android:required="false" />` in `app/src/main/AndroidManifest.xml`.

## AppNavHost line
None.

## TwinDatabase entities
None.

## Strings / permissions
None. `INTERNET` already declared.

## Backend changes
None.

## Proguard
None needed.

## CHANGE REQUEST: none
