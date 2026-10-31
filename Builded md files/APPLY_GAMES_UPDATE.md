# How to apply (games only)

This zip contains ONLY the 36 games files (new/changed). Nothing else in your repo is touched,
so your newer globe / meetplanner / Twin AI work stays as it is.
Verified against GitHub commit ad5aec4 ("Update to remove the pro section in android version").

## Option A - copy files
Unzip into the root of your twin-hearts-android folder and choose "merge / replace".
Paths already match the project (app/src/main/...).

## Option B - git patch (recommended, shows a clean diff)
    cd twin-hearts-android
    git apply --check games-update.patch
    git apply games-update.patch

Then run:  ./gradlew testDebugUnitTest   and build + play each game on two devices.
See GAMES_UPDATE.md for what each file does.
