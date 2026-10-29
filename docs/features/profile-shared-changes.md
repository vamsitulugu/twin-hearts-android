# Shared-file changes — Phase 2.6 — Our Profile

All feature code is in `feature/profile/`, texts in `res/values/strings_profile.xml`. No new dependencies (Coil, Retrofit, Hilt, material3 already present), no manifest change, no permissions.

## FeatureGraphs.kt (already edited in this zip)
Added `import com.twinhearts.app.feature.profile.profileGraph` and, after `liveMapGraph(...)`: `profileGraph(navController) // 2.6 Our Profile`.
Removed `pending<Routes.Profile>(...)` from `pendingGraphs`. `Routes.Profile` already existed, so `Routes.kt` is unchanged.

## README tracker row (add by hand)
`| 2026-10-28 | Phase 2 — 2.6 Our Profile | th-p2-profile-v1.zip | Not compiled here. No new catalog lines or manifest entries. Search, Settings rows (placeholder), account details not ported (see docs/features/profile.md). |` and set row 2.6 to ✅.
