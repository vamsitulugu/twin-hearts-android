# SHARED_CHANGES — Phase 3 — Us (Love Counter · Collection · Dream Goals)

All new files are inside `feature/us/` and `res/values/strings_us.xml`. The two shared core edits below must be applied by the user.

## Gradle / catalog lines
None. (No new dependencies — uses existing Retrofit, Hilt, Compose, kotlinx.serialization.)

## Manifest entries
None. (No new permissions.)

## AppNavHost line
None. (Us is registered in `FeatureGraphs.featureGraphs`, not directly in `AppNavHost`.)

## TwinDatabase entities
None. (Us reads the shared state JSON via `/api/data/state`, not Room.)

## Strings / permissions
`app/src/main/res/values/strings_us.xml` — all `us_*` strings. No new permission.

## Backend changes
None. Reuses existing `GET /api/data/state/{coupleId}` and `POST /api/data/state` (routes/data.js).

---

## Core edits to apply (copy-paste snippets)

### 1. `app/src/main/java/com/twinhearts/app/core/navigation/Routes.kt`
Add the `Us` route object (after `Movie`, before `DesignGallery`):

```kotlin
    // Phase 3: Us tab container (Love Counter · Collection · Dream Goals)
    @Serializable data object Us
```

### 2. `app/src/main/java/com/twinhearts/app/core/navigation/FeatureGraphs.kt`
Add the import and the registration line in `featureGraphs`:

```kotlin
import com.twinhearts.app.feature.us.UsRoute.usGraph
```

and inside `featureGraphs`, after `movieGraph(...)`:

```kotlin
    usGraph(navController) // 3.1 Us (Love Counter · Collection · Dream Goals)
```

After that, remove `Routes.Us` from `pendingGraphs` (it currently shows as `pending<Routes.Profile>` only; if you add `Us` there, remove it).

---

## Tracker update (README.md §12)
Mark **3.1 Us** as 🟨 in progress in the Phase 3 tracker, and add a handoff row.
