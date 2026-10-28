# SHARED_CHANGES — Fight Log (v2: new files only)

The zip adds NEW files only. It does not overwrite any file you already have. Make these small edits by hand (about 2 minutes). Do not copy whole files from GitHub: your project has newer core files than GitHub.

1. **Gradle / catalog:** none. **Manifest:** none. **Backend:** none.

2. **core/navigation/Routes.kt** — inside `object Routes`, add one line (next to the other routes):
```kotlin
@Serializable data object Fights
```

3. **core/navigation/FeatureGraphs.kt**
   - add the import: `import com.twinhearts.app.feature.fights.fightsGraph`
   - inside `featureGraphs(...)`, add one line after the other graph lines: `fightsGraph(navController) // Fight Log`

4. **feature/home/HomeRoute.kt** — inside the `when (shortcut)`, above the `else ->` line, add:
```kotlin
HomeShortcut.FightLog -> navController.navigate(Routes.Fights)
```

5. **core/navigation/TwinApp.kt** (show the bottom bar on Fight Log, as in your reference). Find the line that starts `val showBottomBar = ...`. Rename it to `val baseShowBottomBar = ...` (keep everything after the `=` as it is), then add directly below it:
```kotlin
val showBottomBar = baseShowBottomBar || destination?.hasRoute<Routes.Fights>() == true
```
   `hasRoute` and `destination` are already used in that file.

6. **AI Help (Twin AI)** — file `feature/twinai/ui/TwinAiViewModel.kt`:
   - add the import `import com.twinhearts.app.core.common.TwinPromptHandoff`
   - add a second constructor parameter: `private val handoff: TwinPromptHandoff,` (after `private val repository: TwinAiRepository,`)
   - at the very end of the `launch { ... }` block inside `fun refresh()` (after the `_state.update { ... }` that sets `syncing = false`) add:
```kotlin
// A question handed over by another page (e.g. Fight Log > AI Help) is asked once.
handoff.take()?.let { ask(it) }
```
   - in `app/src/test/.../twinai/TwinAiViewModelTest.kt` change `TwinAiViewModel(repository)` to `TwinAiViewModel(repository, TwinPromptHandoff())` and add `import com.twinhearts.app.core.common.TwinPromptHandoff`.

7. The new file `core/common/TwinPromptHandoff.kt` (in the zip) is a Change Request: it lets Fight Log hand a question to Twin AI.
