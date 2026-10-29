# Fix: TwinApp.kt "Unresolved reference 'Call'" (33 errors)

Cause: the Meet Planner zip contained OLD copies of 3 shared files. Unzipping it over the project
deleted the routes Call, Music, Us, Collection, Calendar, Money, Study, and the Live Map / Music /
Us / Calendar / Money / Study / Collection graphs.

This zip puts them all back AND keeps Meet Planner:
- core/navigation/Routes.kt
- core/navigation/FeatureGraphs.kt
- feature/home/HomeRoute.kt

Steps (Windows): close Android Studio files -> unzip over the project root (replace 3 files)
-> File > Sync Project with Gradle Files -> Build > Rebuild Project.
Tip: before unzipping any future zip, commit first, then check `git diff` for shared files.
