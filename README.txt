twin-hearts-android — weak-network changes
Option A: copy everything inside the "files" folder into your project, same paths, replace when asked.
Option B: in your project folder run:  git apply twin-hearts-android.patch
Then in Android Studio: Sync Gradle, Build > Rebuild, and run the app.
(WorkManager 2.9.1 is the only new dependency.)
